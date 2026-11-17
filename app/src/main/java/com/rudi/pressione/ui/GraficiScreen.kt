package com.rudi.pressione.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.rudi.pressione.data.Measurement
import com.rudi.pressione.data.Store
import com.rudi.pressione.ui.componenti.LineChart
import com.rudi.pressione.ui.componenti.SerieGrafico
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

@Composable
fun GraficiScreen(modifier: Modifier = Modifier) {
    val mis by Store.measurements.collectAsState()
    var scheda by rememberSaveable { mutableStateOf(0) }

    Column(modifier.fillMaxSize()) {
        Text(
            "Grafici",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 4.dp)
        )
        TabRow(selectedTabIndex = scheda) {
            listOf("Andamento", "Medie", "Confronto").forEachIndexed { i, nome ->
                Tab(selected = scheda == i, onClick = { scheda = i }, text = { Text(nome) })
            }
        }
        when (scheda) {
            0 -> AndamentoTab(mis)
            1 -> MedieTab(mis)
            2 -> ConfrontoTab(mis)
        }
    }
}

// ---------------- Andamento ----------------

@Composable
private fun AndamentoTab(mis: List<Measurement>) {
    val opzioni = listOf(7 to "7 giorni", 30 to "30 giorni", 90 to "3 mesi", 365 to "1 anno", 0 to "Tutto")
    var giorni by rememberSaveable { mutableStateOf(30) }
    var mostraSys by rememberSaveable { mutableStateOf(true) }
    var mostraDia by rememberSaveable { mutableStateOf(true) }
    var mostraPul by rememberSaveable { mutableStateOf(false) }

    Column(Modifier.padding(horizontal = 16.dp)) {
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            opzioni.forEach { (g, nome) ->
                FilterChip(selected = giorni == g, onClick = { giorni = g }, label = { Text(nome) })
            }
        }
        Row(
            Modifier.horizontalScroll(rememberScrollState()).padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(selected = mostraSys, onClick = { mostraSys = !mostraSys }, label = { Text("Massima") })
            FilterChip(selected = mostraDia, onClick = { mostraDia = !mostraDia }, label = { Text("Minima") })
            FilterChip(selected = mostraPul, onClick = { mostraPul = !mostraPul }, label = { Text("Pulsazioni") })
        }

        val inizio = if (giorni == 0) 0L else System.currentTimeMillis() - giorni * 86400000L
        val dati = mis.filter { it.timestamp >= inizio }.sortedBy { it.timestamp }

        val serie = buildList {
            if (mostraSys) add(SerieGrafico("Massima", Color(0xFFB3261E), dati.map { it.timestamp to it.systolic.toDouble() }))
            if (mostraDia) add(SerieGrafico("Minima", Color(0xFF1565C0), dati.map { it.timestamp to it.diastolic.toDouble() }))
            if (mostraPul) add(
                SerieGrafico(
                    "Pulsazioni", Color(0xFF2E7D32),
                    dati.map { it.timestamp to it.pulse.toDouble() }.filter { it.second > 0.0 }
                )
            )
        }

        if (dati.size < 2) {
            Text(
                "Servono almeno due misurazioni nel periodo scelto.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 24.dp)
            )
        } else {
            LineChart(
                serie = serie,
                modifier = Modifier.fillMaxWidth().height(280.dp).padding(vertical = 8.dp)
            )
        }
        Legenda(serie)
    }
}

@Composable
private fun Legenda(serie: List<SerieGrafico>) {
    if (serie.isEmpty()) return
    Row(
        Modifier.horizontalScroll(rememberScrollState()).padding(top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        serie.forEach { s ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.foundation.Canvas(Modifier.size(10.dp)) { drawCircle(s.colore) }
                Spacer(Modifier.width(4.dp))
                Text(s.nome, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

// ---------------- Medie ----------------

private data class RigaMedia(
    val etichetta: String,
    val sys: Double?,
    val dia: Double?,
    val pul: Double?,
    val n: Int
)

@Composable
private fun MedieTab(mis: List<Measurement>) {
    val zona = ZoneId.systemDefault()
    var periodo by rememberSaveable { mutableStateOf(0) } // 0 = giornaliere, 1 = settimanali, 2 = mensili

    val righe = remember(mis, periodo) {
        val formattatore = when (periodo) {
            0 -> DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.ITALY)
            1 -> DateTimeFormatter.ofPattern("'settimana del' d MMM yyyy", Locale.ITALY)
            else -> DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ITALY)
        }
        val gruppi = mis.groupBy { m ->
            val d = Instant.ofEpochMilli(m.timestamp).atZone(zona).toLocalDate()
            when (periodo) {
                0 -> d.toString()
                1 -> d.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toString()
                else -> d.withDayOfMonth(1).toString()
            }
        }
        gruppi.toSortedMap().map { (chiave, v) ->
            RigaMedia(
                etichetta = LocalDate.parse(chiave).format(formattatore),
                sys = v.map { it.systolic }.average(),
                dia = v.map { it.diastolic }.average(),
                pul = v.filter { it.pulse > 0 }.map { it.pulse }.takeIf { it.isNotEmpty() }?.average(),
                n = v.size
            )
        }.reversed()
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.horizontalScroll(rememberScrollState()).padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Giornaliere", "Settimanali", "Mensili").forEachIndexed { i, nome ->
                FilterChip(selected = periodo == i, onClick = { periodo = i }, label = { Text(nome) })
            }
        }
        if (righe.isEmpty()) {
            Text("Nessuna misurazione.", Modifier.padding(horizontal = 16.dp))
        }
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(righe) { r ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text(
                            r.etichetta.replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            "${formatta(r.sys)}/${formatta(r.dia)} mmHg" + (r.pul?.let { "  ·  ${formatta(it)} bpm" } ?: ""),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            "${r.n} misurazion${if (r.n == 1) "e" else "i"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

private fun formatta(v: Double?): String = if (v == null) "—" else "%.0f".format(v)

// ---------------- Confronto tra periodi ----------------

private data class MediaP(val sys: Double?, val dia: Double?, val pul: Double?, val n: Int)

@Composable
private fun ConfrontoTab(mis: List<Measurement>) {
    val ora = System.currentTimeMillis()
    val g = 86400000L

    fun media(da: Long, a: Long): MediaP {
        val v = mis.filter { it.timestamp in da until a }
        val pul = v.filter { it.pulse > 0 }
        return MediaP(
            sys = v.takeIf { it.isNotEmpty() }?.map { it.systolic }?.average(),
            dia = v.takeIf { it.isNotEmpty() }?.map { it.diastolic }?.average(),
            pul = pul.takeIf { it.isNotEmpty() }?.map { it.pulse }?.average(),
            n = v.size
        )
    }

    val confronti = listOf(
        "Ultimi 7 giorni vs 7 giorni precedenti" to Pair(
            media(ora - 7 * g, ora), media(ora - 14 * g, ora - 7 * g)
        ),
        "Ultimi 30 giorni vs 30 giorni precedenti" to Pair(
            media(ora - 30 * g, ora), media(ora - 60 * g, ora - 30 * g)
        ),
        "Ultimi 90 giorni vs 90 giorni precedenti" to Pair(
            media(ora - 90 * g, ora), media(ora - 180 * g, ora - 90 * g)
        )
    )

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        confronti.forEach { (titolo, p) ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(titolo, style = MaterialTheme.typography.titleSmall)
                    Text(
                        "(${p.first.n} vs ${p.second.n} misurazioni)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    RigaConfronto("Massima", p.first.sys, p.second.sys)
                    RigaConfronto("Minima", p.first.dia, p.second.dia)
                    RigaConfronto("Pulsazioni", p.first.pul, p.second.pul)
                }
            }
        }
    }
}

@Composable
private fun RigaConfronto(nome: String, a: Double?, b: Double?) {
    Text(
        buildString {
            append("$nome: ")
            append(formatta(a))
            append(" → ")
            append(formatta(b))
            if (a != null && b != null) {
                val delta = a - b
                append("  (")
                append(if (delta >= 0) "+" else "-")
                append("%.0f".format(kotlin.math.abs(delta)))
                append(")")
            }
        },
        style = MaterialTheme.typography.bodyLarge
    )
}
