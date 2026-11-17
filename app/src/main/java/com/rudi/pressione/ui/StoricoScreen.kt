package com.rudi.pressione.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rudi.pressione.data.Measurement
import com.rudi.pressione.data.Store
import com.rudi.pressione.util.Fmt

@Composable
fun StoricoScreen(onModifica: (Measurement) -> Unit, modifier: Modifier = Modifier) {
    val mis by Store.measurements.collectAsState()
    var daEliminare by remember { mutableStateOf<Measurement?>(null) }

    Column(modifier.fillMaxSize()) {
        Text(
            "Storico",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp)
        )
        Text(
            "${mis.size} misurazioni · tocca una voce per modificarla",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, top = 2.dp, end = 16.dp, bottom = 8.dp)
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (mis.isEmpty()) {
                item { Text("Nessuna misurazione registrata.") }
            }
            items(mis, key = { it.id }) { m ->
                Card(Modifier.fillMaxWidth().clickable { onModifica(m) }) {
                    Row(
                        Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                Fmt.dateTime(m.timestamp).replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "${m.systolic}/${m.diastolic} mmHg",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Row {
                                if (m.pulse > 0) {
                                    Text("${m.pulse} bpm", style = MaterialTheme.typography.bodyMedium)
                                }
                                if (m.irregular) {
                                    Text(
                                        (if (m.pulse > 0) "  ·  " else "") + "Battito irregolare",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                            if (m.note.isNotBlank()) {
                                Text(
                                    m.note,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        IconButton(onClick = { daEliminare = m }) {
                            Icon(Icons.Default.Delete, contentDescription = "Elimina")
                        }
                    }
                }
            }
        }
    }

    daEliminare?.let { m ->
        AlertDialog(
            onDismissRequest = { daEliminare = null },
            title = { Text("Eliminare?") },
            text = { Text("Eliminare la misurazione del ${Fmt.dataCsv(m.timestamp)}?") },
            confirmButton = {
                TextButton(onClick = {
                    Store.delete(m.id)
                    daEliminare = null
                }) { Text("Elimina") }
            },
            dismissButton = { TextButton(onClick = { daEliminare = null }) { Text("Annulla") } }
        )
    }
}
