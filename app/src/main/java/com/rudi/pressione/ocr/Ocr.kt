package com.rudi.pressione.ocr

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.rudi.pressione.util.awaitTask

data class Rilevati(
    val systolic: Int? = null,
    val diastolic: Int? = null,
    val pulse: Int? = null,
    val irregular: Boolean = false,
    val rawText: String = ""
)

/** Riconoscimento testo (modello incluso nell'APK: funziona completamente offline). */
object Ocr {

    /** Restituisce le righe di testo rilevate, ordinate dall'alto verso il basso. */
    suspend fun leggi(bitmap: Bitmap): List<String> {
        val riconoscitore = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        return try {
            val testo = riconoscitore.process(InputImage.fromBitmap(bitmap, 0)).awaitTask()
            testo.textBlocks.flatMap { it.lines }
                .sortedWith(
                    compareBy({ (it.boundingBox?.top ?: 0) / 40 }, { it.boundingBox?.left ?: 0 })
                )
                .map { it.text }
        } catch (e: Exception) {
            emptyList()
        } finally {
            riconoscitore.close()
        }
    }

    /**
     * Estrae sistolica, diastolica, pulsazioni e indicatore di battito irregolare
     * dal testo OCR. Il risultato va SEMPRE verificato dall'utente prima del salvataggio.
     */
    fun analizza(righe: List<String>): Rilevati {
        val raw = righe.joinToString("\n")
        if (raw.isBlank()) return Rilevati()

        val basso = raw.lowercase()
        val irregular = listOf("irreg", "ihb", "afib", "a-fib", "aritm", "fibrill").any { basso.contains(it) }

        val numeriPerRiga = righe.map { r ->
            Regex("\\d{2,3}").findAll(r).mapNotNull { it.value.toIntOrNull() }
                .filter { it in 20..299 }.toList()
        }

        var maxima: Int? = null
        var minima: Int? = null
        var pulsazioni: Int? = null

        // 1) Etichette del display (SYS / DIA / PUL / bpm / freq)
        righe.forEachIndexed { i, r ->
            val rBasso = r.lowercase().replace(" ", "")
            val numeri = numeriPerRiga[i]
            if (rBasso.contains("sys")) maxima = numeri.firstOrNull { it in 50..260 } ?: maxima
            if (rBasso.contains("dia")) minima = numeri.firstOrNull { it in 30..160 && it != maxima } ?: minima
            if (listOf("pul", "bpm", "imp", "freq").any { rBasso.contains(it) }) {
                pulsazioni = numeri.firstOrNull { it in 35..220 && it != maxima && it != minima } ?: pulsazioni
            }
        }

        // 2) Coppia in ordine di lettura sulla stessa riga (es. "120/80 72")
        if (maxima == null || minima == null) {
            esterno@ for (numeri in numeriPerRiga) {
                for (i in 0 until numeri.size - 1) {
                    val a = numeri[i]
                    val b = numeri[i + 1]
                    if (a in 50..260 && b in 30..160 && a > b && a - b >= 10) {
                        if (maxima == null) maxima = a
                        if (minima == null) minima = b
                        if (pulsazioni == null) {
                            pulsazioni = numeri.drop(i + 2).firstOrNull { it in 35..220 && it != a && it != b }
                        }
                        break@esterno
                    }
                }
            }
        }

        // 3) Fallback: coppie valide tra righe diverse
        if (maxima == null || minima == null) {
            val tutti = numeriPerRiga.flatten()
            ciclo@ for (i in tutti.indices) {
                for (j in i + 1 until tutti.size) {
                    val a = tutti[i]
                    val b = tutti[j]
                    if (a in 50..260 && b in 30..160 && a > b && a - b >= 15) {
                        if (maxima == null) maxima = a
                        if (minima == null) minima = b
                        break@ciclo
                    }
                }
            }
        }

        // 4) Pulsazione residua
        if (pulsazioni == null) {
            pulsazioni = numeriPerRiga.flatten()
                .firstOrNull { it in 35..220 && it != maxima && it != minima }
        }

        // 5) Correzione di scambio massima/minima
        if (maxima != null && minima != null && maxima < minima) {
            val t = maxima; maxima = minima; minima = t
        }

        return Rilevati(maxima, minima, pulsazioni, irregular, raw)
    }
}
