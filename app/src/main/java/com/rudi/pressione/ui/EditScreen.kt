package com.rudi.pressione.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.rudi.pressione.data.Measurement
import com.rudi.pressione.util.Fmt
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditScreen(bozza: Bozza, onAnnulla: () -> Unit, onSalva: (Measurement) -> Unit) {
    var sys by remember { mutableStateOf(bozza.systolic?.toString() ?: "") }
    var dia by remember { mutableStateOf(bozza.diastolic?.toString() ?: "") }
    var pul by remember { mutableStateOf(bozza.pulse?.toString() ?: "") }
    var irr by remember { mutableStateOf(bozza.irregular) }
    var nota by remember { mutableStateOf(bozza.note) }
    var ts by remember { mutableStateOf(bozza.timestamp) }
    var errore by remember { mutableStateOf<String?>(null) }
    var mostraData by remember { mutableStateOf(false) }
    var mostraOra by remember { mutableStateOf(false) }

    fun salva() {
        val s = sys.toIntOrNull()
        val d = dia.toIntOrNull()
        val p = pul.toIntOrNull()
        when {
            s == null || d == null -> errore = "Inserisci la massima e la minima."
            s !in 40..260 || d !in 20..160 -> errore = "Valori fuori dall'intervallo plausibile."
            s <= d -> errore = "La massima deve essere maggiore della minima."
            p != null && p !in 20..250 -> errore = "Pulsazioni fuori dall'intervallo plausibile."
            else -> onSalva(
                Measurement(
                    id = bozza.esisteId ?: UUID.randomUUID().toString(),
                    timestamp = ts,
                    systolic = s,
                    diastolic = d,
                    pulse = p ?: 0,
                    irregular = irr,
                    note = nota.trim()
                )
            )
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            when {
                bozza.esisteId != null -> "Modifica misurazione"
                bozza.bitmap != null -> "Controlla i valori rilevati"
                else -> "Nuova misurazione"
            },
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(Modifier.height(12.dp))

        bozza.bitmap?.let { bmp ->
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp),
                contentScale = ContentScale.Fit
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Valori letti automaticamente dalla foto. Verifica e correggi se necessario.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = sys,
                onValueChange = { sys = it },
                modifier = Modifier.weight(1f),
                label = { Text("Massima (mmHg)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )
            OutlinedTextField(
                value = dia,
                onValueChange = { dia = it },
                modifier = Modifier.weight(1f),
                label = { Text("Minima (mmHg)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )
        }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = pul,
            onValueChange = { pul = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Pulsazioni (bpm) — opzionale") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true
        )
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(checked = irr, onCheckedChange = { irr = it })
            Spacer(Modifier.width(10.dp))
            Text("Battito irregolare")
        }
        Spacer(Modifier.height(8.dp))

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp)) {
                Text(
                    "Data e ora della misurazione",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(Fmt.dateTime(ts).replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.titleMedium)
                Row {
                    TextButton(onClick = { mostraData = true }) { Text("Cambia data") }
                    TextButton(onClick = { mostraOra = true }) { Text("Cambia ora") }
                }
            }
        }
        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = nota,
            onValueChange = { nota = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Note (opzionale)") }
        )

        errore?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }

        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = { salva() }, modifier = Modifier.weight(1f)) { Text("Salva") }
            OutlinedButton(onClick = onAnnulla, modifier = Modifier.weight(1f)) { Text("Annulla") }
        }
        Spacer(Modifier.height(24.dp))
    }

    if (mostraData) {
        val statoData = rememberDatePickerState(initialSelectedDateMillis = ts)
        DatePickerDialog(
            onDismissRequest = { mostraData = false },
            confirmButton = {
                TextButton(onClick = {
                    statoData.selectedDateMillis?.let { ts = Fmt.combinaData(ts, it) }
                    mostraData = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { mostraData = false }) { Text("Annulla") } }
        ) {
            DatePicker(state = statoData)
        }
    }

    if (mostraOra) {
        val adesso = java.time.Instant.ofEpochMilli(ts).atZone(java.time.ZoneId.systemDefault())
        val statoOra = rememberTimePickerState(
            initialHour = adesso.hour,
            initialMinute = adesso.minute,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { mostraOra = false },
            title = { Text("Scegli l'ora") },
            text = { TimePicker(state = statoOra) },
            confirmButton = {
                TextButton(onClick = {
                    ts = Fmt.combinaOra(ts, statoOra.hour, statoOra.minute)
                    mostraOra = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { mostraOra = false }) { Text("Annulla") } }
        )
    }
}
