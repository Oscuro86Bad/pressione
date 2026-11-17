package com.rudi.pressione.data

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File

/** Archivio locale: due file JSON nella memoria interna, nessun account, nessun cloud. */
object Store {
    private val gson = Gson()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var ctx: Context

    private val _measurements = MutableStateFlow<List<Measurement>>(emptyList())
    val measurements: StateFlow<List<Measurement>> = _measurements

    private val _thresholds = MutableStateFlow(Thresholds())
    val thresholds: StateFlow<Thresholds> = _thresholds

    fun init(context: Context) {
        ctx = context.applicationContext
        _measurements.value = leggi("misurazioni.json", object : TypeToken<List<Measurement>>() {})
            ?: emptyList()
        _thresholds.value = leggi("impostazioni.json", object : TypeToken<Thresholds>() {})
            ?: Thresholds()
        GitHubSync.init(context)
    }

    private fun <T> leggi(nome: String, tipo: TypeToken<T>): T? = try {
        val f = File(ctx.filesDir, nome)
        if (f.exists()) gson.fromJson<T>(f.readText(), tipo.type) else null
    } catch (e: Exception) {
        null
    }

    private fun scrivi(nome: String, valore: Any) = scope.launch {
        try {
            File(ctx.filesDir, nome).writeText(gson.toJson(valore))
        } catch (_: Exception) {
        }
    }

    fun add(m: Measurement) {
        _measurements.value = (_measurements.value + m).sortedByDescending { it.timestamp }
        scrivi("misurazioni.json", _measurements.value)
        GitHubSync.dopoCambioDati()
    }

    fun update(m: Measurement) {
        _measurements.value = _measurements.value
            .map { if (it.id == m.id) m else it }
            .sortedByDescending { it.timestamp }
        scrivi("misurazioni.json", _measurements.value)
        GitHubSync.dopoCambioDati()
    }

    fun delete(id: String) {
        _measurements.value = _measurements.value.filter { it.id != id }
        scrivi("misurazioni.json", _measurements.value)
        GitHubSync.dopoCambioDati()
    }

    fun saveThresholds(t: Thresholds) {
        _thresholds.value = t
        scrivi("impostazioni.json", t)
        GitHubSync.dopoCambioDati()
    }

    /** Applica un backup già caricato (da file o da GitHub), unendolo ai dati esistenti. */
    fun applicaBackup(backup: Backup): Boolean = try {
        val nuovi = backup.misurazioni ?: emptyList()
        val esistenti = _measurements.value.associateBy { it.id }
        val uniti = nuovi.filterNotNull().fold(esistenti) { acc, m -> acc + (m.id to m) }
        _measurements.value = uniti.values.sortedByDescending { it.timestamp }
        scrivi("misurazioni.json", _measurements.value)
        backup.impostazioni?.let {
            _thresholds.value = it
            scrivi("impostazioni.json", it)
        }
        true
    } catch (e: Exception) {
        false
    }

    /** Esporta le misurazioni in CSV (separatore ';', con BOM per Excel in italiano). */
    fun exportCsv(uri: Uri): Boolean = try {
        val sb = StringBuilder("\uFEFFData;Ora;Sistolica;Diastolica;Pulsazioni;Battito irregolare;Note\n")
        _measurements.value.forEach { m ->
            val d = com.rudi.pressione.util.Fmt
            sb.append(d.dataCsv(m.timestamp)).append(';')
                .append(m.systolic).append(';').append(m.diastolic).append(';').append(m.pulse).append(';')
                .append(if (m.irregular) "Si" else "No").append(';')
                .append(m.note.replace(';', ',').replace('\n', ' ')).append('\n')
        }
        ctx.contentResolver.openOutputStream(uri)?.use { it.write(sb.toString().toByteArray(Charsets.UTF_8)) }
        true
    } catch (e: Exception) {
        false
    }

    /** Backup completo (misurazioni + impostazioni) in JSON. */
    fun exportJson(uri: Uri): Boolean = try {
        val backup = Backup(misurazioni = _measurements.value, impostazioni = _thresholds.value)
        ctx.contentResolver.openOutputStream(uri)?.use { it.write(gson.toJson(backup).toByteArray(Charsets.UTF_8)) }
        true
    } catch (e: Exception) {
        false
    }

    /** Importa un backup e unisce i dati a quelli esistenti (senza duplicati per id). */
    fun importJson(uri: Uri): Boolean = try {
        val testo = ctx.contentResolver.openInputStream(uri)?.bufferedReader()?.readText() ?: return false
        val backup = gson.fromJson<Backup>(testo, object : TypeToken<Backup>() {}.type)
        if (applicaBackup(backup)) true else false
    } catch (e: Exception) {
        false
    }
}

data class Backup(
    val versione: Int? = 1,
    val misurazioni: List<Measurement>? = null,
    val impostazioni: Thresholds? = null
)
