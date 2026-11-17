package com.rudi.pressione.ui

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rudi.pressione.data.Store
import com.rudi.pressione.util.Avvisi
import com.rudi.pressione.util.Fmt

@Composable
fun HomeScreen(
    onScatta: () -> Unit,
    onManuale: () -> Unit,
    onVai: (Schermata) -> Unit,
    modifier: Modifier = Modifier
) {
    val mis by Store.measurements.collectAsState()
    val soglie by Store.thresholds.collectAsState()
    val ultimo = mis.firstOrNull()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Pressione", style = MaterialTheme.typography.headlineMedium)
        Text(
            Fmt.dataOggi().replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(20.dp))

        if (ultimo != null) {
            val avvisi = Avvisi.valuta(ultimo, soglie, mis.filter { it.id != ultimo.id })
            if (avvisi.isNotEmpty()) {
                Card(
                    Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(
                            "Avvisi sull'ultimo rilevamento",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        avvisi.take(3).forEach {
                            Text(
                                it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        if (avvisi.size > 3) {
                            Text(
                                "…e altri ${avvisi.size - 3} avvisi",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            Card(Modifier.fillMaxWidth()) {
                Column(
                    Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        "Ultimo rilevamento",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            "${ultimo.systolic}/${ultimo.diastolic}",
                            style = MaterialTheme.typography.displayMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "mmHg",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }
                    if (ultimo.pulse > 0) {
                        Text("${ultimo.pulse} bpm", style = MaterialTheme.typography.titleLarge)
                    }
                    if (ultimo.irregular) {
                        Text(
                            "Battito irregolare segnalato",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    Text(
                        Fmt.dateTime(ultimo.timestamp).replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (ultimo.note.isNotBlank()) {
                        Text(ultimo.note, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))

            val setteGiorni = mis.filter { it.timestamp >= System.currentTimeMillis() - 7L * 86400000L }
            if (setteGiorni.isNotEmpty()) {
                val mediaPul = setteGiorni.filter { it.pulse > 0 }
                val testoMedia = buildString {
                    append("Media ultimi 7 giorni: ")
                    append(setteGiorni.map { it.systolic }.average().toInt())
                    append("/")
                    append(setteGiorni.map { it.diastolic }.average().toInt())
                    append(" mmHg")
                    if (mediaPul.isNotEmpty()) {
                        append(" · ")
                        append(mediaPul.map { it.pulse }.average().toInt())
                        append(" bpm")
                    }
                }
                Text(testoMedia, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(horizontal = 4.dp))
                Spacer(Modifier.height(16.dp))
            }
        } else {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Nessuna misurazione", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Fotografa il display del misuratore o inserisci i valori manualmente per iniziare.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        Button(
            onClick = onScatta,
            modifier = Modifier.fillMaxWidth().height(64.dp)
        ) {
            Icon(Icons.Default.PhotoCamera, contentDescription = null)
            Spacer(Modifier.width(10.dp))
            Text("Fotografa il misuratore", style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(10.dp))
        OutlinedButton(onClick = onManuale, modifier = Modifier.fillMaxWidth()) {
            Text("Inserisci manualmente")
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = { onVai(Schermata.GRAFICI) }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.ShowChart, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Grafici")
            }
            OutlinedButton(onClick = { onVai(Schermata.STORICO) }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.History, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Storico")
            }
        }
        Spacer(Modifier.height(24.dp))
        Text(
            "Gli avvisi sono solo informativi e non sostituiscono il parere di un medico.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
