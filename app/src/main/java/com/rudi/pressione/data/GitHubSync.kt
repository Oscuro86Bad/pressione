package com.rudi.pressione.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Impostazioni del backup su GitHub (salvate in un file separato, mai incluse nel backup stesso). */
data class SyncImpostazioni(
    val token: String = "",
    val gistId: String = "",
    val automatico: Boolean = false
)

/**
 * Backup dei dati su GitHub tramite gist privato (visibile solo al tuo account).
 * - A ogni modifica dei dati (se attivato) aggiorna il file pressione.json nel gist.
 * - "Ripristina" scarica il backup e lo unisce ai dati locali.
 */
object GitHubSync {
    private val gson = Gson()
    private lateinit var ctx: Context
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    private var occupato = false
    private var inCoda = false

    private val _impostazioni = MutableStateFlow(SyncImpostazioni())
    val impostazioni: StateFlow<SyncImpostazioni> = _impostazioni

    private val _stato = MutableStateFlow<String?>(null)
    val stato: StateFlow<String?> = _stato

    val configurato: Boolean
        get() = _impostazioni.value.token.isNotBlank()

    fun init(context: Context) {
        ctx = context.applicationContext
        try {
            val f = File(ctx.filesDir, "github_sync.json")
            if (f.exists()) {
                _impostazioni.value = gson.fromJson(f.readText(), SyncImpostazioni::class.java) ?: SyncImpostazioni()
            }
        } catch (_: Exception) {
        }
    }

    private fun salvaImpostazioni() = scope.launch {
        try {
            File(ctx.filesDir, "github_sync.json").writeText(gson.toJson(_impostazioni.value))
        } catch (_: Exception) {
        }
    }

    fun salva(token: String, automatico: Boolean) {
        _impostazioni.value = _impostazioni.value.copy(token = token.trim(), automatico = automatico)
        salvaImpostazioni()
    }

    /** Chiamato da Store a ogni modifica dei dati. */
    fun dopoCambioDati() {
        if (!configurato || !_impostazioni.value.automatico) return
        scope.launch { eseguiSincronizzazione() }
    }

    /** Esegue il backup (crea il gist al primo utilizzo, poi lo aggiorna). Ritorna "OK" o un messaggio di errore. */
    suspend fun eseguiSincronizzazione(): String {
        if (!configurato) return "Inserisci prima il token GitHub"
        val esito = try {
            mutex.withLock {
                if (occupato) {
                    inCoda = true
                    return "Backup già in corso"
                }
                occupato = true
            }
            try {
                do {
                    inCoda = false
                    aggiornaGist()
                } while (inCoda)
                "OK"
            } finally {
                mutex.withLock { occupato = false }
            }
        } catch (e: Exception) {
            "Errore: ${e.message ?: "sconosciuto"}"
        }
        _stato.value = if (esito == "OK") {
            "Ultimo backup riuscito: " + SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.ITALY).format(Date())
        } else esito
        return esito
    }

    private fun aggiornaGist() {
        val backup = Backup(misurazioni = Store.measurements.value, impostazioni = Store.thresholds.value)
        val contenuto = gson.toJson(backup)
        val id = _impostazioni.value.gistId

        if (id.isBlank()) {
            val corpo = gson.toJson(
                mapOf(
                    "description" to "Backup Pressione (dati dell'app)",
                    "public" to false,
                    "files" to mapOf("pressione.json" to mapOf("content" to contenuto))
                )
            )
            val conn = apriConnessione("https://api.github.com/gists", "POST")
            val risposta = inviaELeggi(conn, corpo)
            val json = JsonParser.parseString(risposta).asJsonObject
            val nuovoId = json.get("id")?.asString ?: throw Exception("Risposta inattesa da GitHub")
            _impostazioni.value = _impostazioni.value.copy(gistId = nuovoId)
            salvaImpostazioni()
        } else {
            val corpo = gson.toJson(
                mapOf("files" to mapOf("pressione.json" to mapOf("content" to contenuto)))
            )
            val conn = apriConnessione("https://api.github.com/gists/$id", "PATCH")
            inviaELeggi(conn, corpo)
        }
    }

    /** Scarica il backup dal gist e lo applica ai dati locali. */
    suspend fun ripristina(): Boolean {
        val id = _impostazioni.value.gistId
        if (id.isBlank()) throw Exception("Nessun backup su GitHub: esegui prima un backup")
        val conn = apriConnessione("https://api.github.com/gists/$id", "GET")
        if (conn.responseCode != 200) throw Exception("GitHub ha risposto ${conn.responseCode}")
        val risposta = conn.inputStream.bufferedReader().readText()
        val json = JsonParser.parseString(risposta).asJsonObject
        val file = json.getAsJsonObject("files")?.get("pressione.json")?.asJsonObject
            ?: throw Exception("File di backup non presente nel gist")

        var contenuto = file.get("content")?.asString
        val troncato = file.get("truncated")?.asBoolean ?: false
        if (contenuto == null || troncato) {
            val raw = file.get("raw_url")?.asString ?: throw Exception("Contenuto non disponibile")
            val c2 = URL(raw).openConnection() as HttpURLConnection
            c2.requestMethod = "GET"
            c2.connectTimeout = 15000
            c2.readTimeout = 15000
            if (c2.responseCode != 200) throw Exception("GitHub ha risposto ${c2.responseCode}")
            contenuto = c2.inputStream.bufferedReader().readText()
        }
        val backup = gson.fromJson(contenuto, Backup::class.java)
        return Store.applicaBackup(backup)
    }

    // ---------- dettagli HTTP ----------

    private fun apriConnessione(url: String, metodo: String): HttpURLConnection {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.requestMethod = metodo
        conn.setRequestProperty("Authorization", "Bearer ${_impostazioni.value.token}")
        conn.setRequestProperty("Accept", "application/vnd.github+json")
        conn.setRequestProperty("Content-Type", "application/json")
        conn.connectTimeout = 15000
        conn.readTimeout = 15000
        conn.doOutput = metodo != "GET"
        return conn
    }

    private fun inviaELeggi(conn: HttpURLConnection, corpo: String): String {
        OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(corpo) }
        val codice = conn.responseCode
        val flusso = if (codice in 200..299) conn.inputStream else conn.errorStream
        val testo = flusso?.bufferedReader()?.readText() ?: ""
        if (codice !in 200..299) {
            val motivo = if (codice == 401) "token non valido" else if (codice == 403) "token senza permessi o limite raggiunto" else testo.take(200)
            throw Exception("GitHub ha risposto $codice ($motivo)")
        }
        return testo
    }
}
