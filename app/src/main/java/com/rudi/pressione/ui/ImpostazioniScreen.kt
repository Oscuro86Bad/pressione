package com.rudi.pressione.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.rudi.pressione.data.Store
import com.rudi.pressione.data.Thresholds
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ImpostazioniScreen(toast: (String) -> Unit, modifier: Modifier = Modifier) {
    val soglie by Store.thresholds.collectAsState()

    var sysAlta by remember(soglie) { mutableStateOf(soglie.sysHigh.toString()) }
    var sysBassa by remember(soglie) { mutableStateOf(soglie.sysLow.toString()) }
    var diaAlta by remember(soglie) { mutableStateOf(soglie.diaHigh.toString()) }
    var diaBassa by remember(soglie) { mutableStateOf(soglie.diaLow.toString()) }
    var pulAlte by remember(soglie) { mutableStateOf(soglie.pulseHigh.toString()) }
    var pulBasse by remember(soglie) { mutableStateOf(soglie.pulseLow.toString()) }
    var varSys by remember(soglie) { mutableStateOf(soglie.variazioneSys.toString()) }
    var varDia by remember(soglie) { mutableStateOf(soglie.variazioneDia.toString()) }

    val adesso = SimpleDateFormat("yyyyMMdd_HHmm", Locale.ITALY).format(Date())

    val exportCsv = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        uri?.let { toast(if (Store.exportCsv(it)) "CSV esportato" else "Errore durante l'esportazione") }
    }
    val exportJson = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let { toast(if (Store.exportJson(it)) "Backup esportato" else "Errore durante l'esportazione") }
    }
    val importJson = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { toast(if (Store.importJson(it)) "Backup importato" else "File non valido o errore di importazione") }
    }

    fun salvaSoglie() {
        val v = listOf(
            sysAlta, sysBassa, diaAlta, diaBassa, pulAlte, pulBasse, varSys, varDia
        ).map { it.toIntOrNull() }
        if (v.any { it == null }) {
            toast("Inserisci tutti i valori (numeri interi)")
            return
        }
        val sH = v[0]!!
        val sL = v[1]!!
        val dH = v[2]!!
        val dL = v[3]!!
        val pH = v[4]!!
        val pL = v[5]!!
        val vS = v[6]!!
        val vD = v[7]!!
        if (sH <= sL || dH <= dL || pH <= pL) {
            toast("Le soglie alte devono essere maggiori di quelle basse")
            return
        }
        Store.saveThresholds(Thresholds(sH, sL, dH, dL, pH, pL, vS, vD))
        toast("Soglie salvate")
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Impostazioni", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))

        Text("Soglie personali per gli avvisi", style = MaterialTheme.typography.titleMedium)
        Text(
            "Se una misurazione supera queste soglie, l'app mostra un avviso informativo. Non è un consiglio medico.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CampoNumero(sysAlta, { sysAlta = it }, "Massima alta", Modifier.weight(1f))
            CampoNumero(sysBassa, { sysBassa = it }, "Massima bassa", Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CampoNumero(diaAlta, { diaAlta = it }, "Minima alta", Modifier.weight(1f))
            CampoNumero(diaBassa, { diaBassa = it }, "Minima bassa", Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CampoNumero(pulAlte, { pulAlte = it }, "Pulsazioni alte", Modifier.weight(1f))
            CampoNumero(pulBasse, { pulBasse = it }, "Pulsazioni basse", Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CampoNumero(varSys, { varSys = it }, "Variazione max", Modifier.weight(1f))
            CampoNumero(varDia, { varDia = it }, "Variazione min", Modifier.weight(1f))
        }
        Text(
            "«Variazione» = differenza (in mmHg) rispetto alla media delle ultime misurazioni oltre la quale l'app segnala una variazione significativa.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )
        Spacer(Modifier.height(12.dp))
        Button(onClick = { salvaSoglie() }, modifier = Modifier.fillMaxWidth()) {
            Text("Salva soglie")
        }

        Spacer(Modifier.height(24.dp))
        Text("Dati", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = { exportCsv.launch("pressione_$adesso.csv") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.FileDownload, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Esporta in CSV (per Excel)")
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = { exportJson.launch("backup_pressione_$adesso.json") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.FileDownload, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Esporta backup completo")
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = { importJson.launch(arrayOf("*/*")) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.FileUpload, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Importa backup")
        }

        Spacer(Modifier.height(24.dp))
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row {
                    Icon(Icons.Default.Info, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Informazioni", style = MaterialTheme.typography.titleSmall)
                }
                Text(
                    "Tutti i dati restano solo sul telefono: nessun account, nessun cloud, nessun abbonamento. Il riconoscimento delle foto avviene offline, il modello è incluso nell'app.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Gli avvisi sono puramente informativi e non sostituiscono il parere di un medico. Non usare questa app per diagnosticare o modificare una terapia. Per valori molto elevati (ad esempio 180/120 o più) o in presenza di sintomi importanti (mal di testa forte, dolore al petto, difficoltà a respirare, disturbi alla vista), segui le indicazioni sanitarie appropriate.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Pressione v1.0 — uso personale",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun CampoNumero(
    valore: String,
    onChange: (String) -> Unit,
    etichetta: String,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = valore,
        onValueChange = onChange,
        modifier = modifier,
        label = { Text(etichetta) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true
    )
}
