package com.rudi.pressione.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object Fmt {
    private val ita = Locale.ITALY

    fun dateTime(ms: Long): String =
        Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("EEEE d MMMM yyyy, HH:mm", ita))

    fun dataBreve(ms: Long): String =
        Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("d MMM", ita))

    fun dataCsv(ms: Long): String =
        Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", ita))

    fun dataOggi(): String =
        LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", ita))

    /** Combina una data scelta (millisecondi UTC del DatePicker) con l'ora attuale della misurazione. */
    fun combinaData(tsCorrente: Long, utcMillis: Long): Long {
        val data = Instant.ofEpochMilli(utcMillis).atZone(ZoneId.of("UTC")).toLocalDate()
        val ora = Instant.ofEpochMilli(tsCorrente).atZone(ZoneId.systemDefault()).toLocalTime()
        return data.atTime(ora).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    /** Combina una nuova ora con la data attuale della misurazione. */
    fun combinaOra(tsCorrente: Long, ore: Int, minuti: Int): Long {
        val data = Instant.ofEpochMilli(tsCorrente).atZone(ZoneId.systemDefault()).toLocalDate()
        return data.atTime(ore, minuti).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
}
