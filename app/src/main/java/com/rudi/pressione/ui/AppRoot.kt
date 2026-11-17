package com.rudi.pressione.ui

import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.rudi.pressione.data.Measurement
import com.rudi.pressione.data.Store
import com.rudi.pressione.ocr.Img
import com.rudi.pressione.ocr.Ocr
import com.rudi.pressione.util.Avvisi
import kotlinx.coroutines.launch
import java.io.File

enum class Schermata { HOME, GRAFICI, STORICO, IMPOSTAZIONI }

/** Dati provvisori di una misurazione prima del salvataggio (da foto o da inserimento manuale). */
data class Bozza(
    val esisteId: String? = null,
    val bitmap: Bitmap? = null,
    val systolic: Int? = null,
    val diastolic: Int? = null,
    val pulse: Int? = null,
    val irregular: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val note: String = "",
    val ocrRaw: String? = null
)

private fun Measurement.toBozza() = Bozza(
    esisteId = id,
    systolic = systolic,
    diastolic = diastolic,
    pulse = pulse.takeIf { it > 0 },
    irregular = irregular,
    timestamp = timestamp,
    note = note
)

private data class VoceNav(val schermata: Schermata, val icona: ImageVector, val etichetta: String)

private val vociNav = listOf(
    VoceNav(Schermata.HOME, Icons.Default.Home, "Home"),
    VoceNav(Schermata.GRAFICI, Icons.Default.ShowChart, "Grafici"),
    VoceNav(Schermata.STORICO, Icons.Default.History, "Storico"),
    VoceNav(Schermata.IMPOSTAZIONI, Icons.Default.Settings, "Impostazioni")
)

@Composable
fun AppRoot() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var schermata by rememberSaveable { mutableStateOf(Schermata.HOME) }
    var bozza by remember { mutableStateOf<Bozza?>(null) }
    var avvisiAttivi by remember { mutableStateOf<List<String>>(emptyList()) }
    var fileFoto by remember { mutableStateOf<File?>(null) }

    fun toast(msg: String) = Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()

    val scatta = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val file = fileFoto
        fileFoto = null
        if (ok && file != null) {
            scope.launch {
                val bmp = Img.carica(file)
                var b = Bozza(timestamp = System.currentTimeMillis())
                if (bmp != null) {
                    val righe = Ocr.leggi(bmp)
                    val r = Ocr.analizza(righe)
                    b = Bozza(
                        bitmap = bmp,
                        systolic = r.systolic,
                        diastolic = r.diastolic,
                        pulse = r.pulse,
                        irregular = r.irregular,
                        timestamp = System.currentTimeMillis(),
                        ocrRaw = r.rawText
                    )
                } else {
                    toast("Impossibile leggere la foto: inserisci i valori manualmente")
                }
                file.delete()
                bozza = b
            }
        } else {
            file?.delete()
        }
    }

    fun scattaFoto() {
        val dir = File(context.cacheDir, "foto").apply { mkdirs() }
        val file = File(dir, "scatto_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
        fileFoto = file
        scatta.launch(uri)
    }

    fun salva(m: Measurement) {
        if (bozza?.esisteId != null) Store.update(m) else Store.add(m)
        val avvisi = Avvisi.valuta(m, Store.thresholds.value, Store.measurements.value.filter { it.id != m.id })
        bozza = null
        schermata = Schermata.HOME
        if (avvisi.isNotEmpty()) avvisiAttivi = avvisi
    }

    val b = bozza
    if (b != null) {
        EditScreen(bozza = b, onAnnulla = { bozza = null }, onSalva = { salva(it) })
    } else {
        Scaffold(
            bottomBar = {
                NavigationBar {
                    vociNav.forEach { v ->
                        NavigationBarItem(
                            selected = schermata == v.schermata,
                            onClick = { schermata = v.schermata },
                            icon = { Icon(v.icona, contentDescription = v.etichetta) },
                            label = { Text(v.etichetta) }
                        )
                    }
                }
            }
        ) { pad ->
            when (schermata) {
                Schermata.HOME -> HomeScreen(
                    onScatta = { scattaFoto() },
                    onManuale = { bozza = Bozza() },
                    onVai = { schermata = it },
                    modifier = Modifier.padding(pad)
                )
                Schermata.GRAFICI -> GraficiScreen(Modifier.padding(pad))
                Schermata.STORICO -> StoricoScreen(
                    onModifica = { bozza = it.toBozza() },
                    modifier = Modifier.padding(pad)
                )
                Schermata.IMPOSTAZIONI -> ImpostazioniScreen(
                    toast = { toast(it) },
                    modifier = Modifier.padding(pad)
                )
            }
        }
    }

    if (avvisiAttivi.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { avvisiAttivi = emptyList() },
            title = { Text("Avvisi") },
            text = {
                androidx.compose.foundation.layout.Column {
                    avvisiAttivi.forEach {
                        Text(it, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 2.dp))
                    }
                    androidx.compose.foundation.layout.Spacer(Modifier.padding(top = 8.dp))
                    Text(
                        "Avvisi puramente informativi: non sostituiscono il giudizio di un medico. Per valori molto elevati o sintomi importanti, segui le indicazioni sanitarie appropriate.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = { TextButton(onClick = { avvisiAttivi = emptyList() }) { Text("OK") } }
        )
    }
}
