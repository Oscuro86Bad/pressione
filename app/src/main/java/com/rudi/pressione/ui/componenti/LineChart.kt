package com.rudi.pressione.ui.componenti

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.rudi.pressione.util.Fmt

data class SerieGrafico(val nome: String, val colore: Color, val punti: List<Pair<Long, Double>>)

/** Grafico a linee disegnato con Canvas: nessuna libreria esterna. */
@Composable
fun LineChart(serie: List<SerieGrafico>, modifier: Modifier = Modifier) {
    val densita = LocalDensity.current
    val pennello = remember(densita) {
        android.graphics.Paint().apply {
            color = android.graphics.Color.GRAY
            textSize = with(densita) { 10.dp.toPx() }
            isAntiAlias = true
        }
    }

    Canvas(modifier) {
        val tutto = serie.flatMap { it.punti }
        if (tutto.isEmpty()) return@Canvas

        val padS = 40.dp.toPx()
        val padD = 10.dp.toPx()
        val padA = 8.dp.toPx()
        val padB = 26.dp.toPx()

        val tMin = tutto.minOf { it.first }
        val tMax = tutto.maxOf { it.first }
        var vMin = tutto.minOf { it.second }
        var vMax = tutto.maxOf { it.second }
        val span = (vMax - vMin).coerceAtLeast(10.0)
        vMax += span * 0.1
        vMin -= span * 0.1

        fun x(t: Long): Float =
            if (tMax == tMin) (padS + (size.width - padS - padD) / 2f)
            else (padS + (t - tMin).toDouble() / (tMax - tMin) * (size.width - padS - padD)).toFloat()

        fun y(v: Double): Float =
            (padA + (1 - (v - vMin) / (vMax - vMin)) * (size.height - padA - padB)).toFloat()

        val tacche = 4
        for (i in 0..tacche) {
            val v = vMin + (vMax - vMin) * i / tacche
            val yy = y(v)
            drawLine(Color.LightGray.copy(alpha = 0.5f), Offset(padS, yy), Offset(size.width - padD, yy), 1f)
            drawContext.canvas.nativeCanvas.drawText("%.0f".format(v), 4f, yy + 10f, pennello)
        }

        val etichette = listOf(tMin, (tMin + tMax) / 2, tMax)
        etichette.forEachIndexed { i, t ->
            val largh = pennello.measureText(Fmt.dataBreve(t))
            val xx = when (i) {
                0 -> padS
                1 -> x(t) - largh / 2
                else -> size.width - padD - largh
            }
            drawContext.canvas.nativeCanvas.drawText(Fmt.dataBreve(t), xx, size.height - 6f, pennello)
        }

        serie.forEach { s ->
            val punti = s.punti.sortedBy { it.first }.map { Offset(x(it.first), y(it.second)) }
            if (punti.size == 1) {
                drawCircle(s.colore, 6f, punti[0])
            } else {
                for (i in 0 until punti.size - 1) drawLine(s.colore, punti[i], punti[i + 1], 3f)
                punti.forEach { drawCircle(s.colore, 5f, it) }
            }
        }
    }
}
