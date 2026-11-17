package com.rudi.pressione.util

import com.rudi.pressione.data.Measurement
import com.rudi.pressione.data.Thresholds
import kotlin.math.abs

/** Avvisi puramente informativi: non sostituiscono il parere di un medico. */
object Avvisi {

    fun valuta(m: Measurement, t: Thresholds, precedenti: List<Measurement>): List<String> {
        val out = mutableListOf<String>()

        if (m.systolic >= 180 || m.diastolic >= 120) {
            out.add("Valori molto elevati: se hai sintomi importanti (mal di testa forte, dolore al petto, difficoltà a respirare, disturbi alla vista) segui subito le indicazioni sanitarie appropriate.")
        }
        if (m.systolic >= t.sysHigh) out.add("La massima (${m.systolic}) supera la soglia impostata (${t.sysHigh}).")
        if (m.diastolic >= t.diaHigh) out.add("La minima (${m.diastolic}) supera la soglia impostata (${t.diaHigh}).")
        if (m.systolic <= t.sysLow) out.add("La massima (${m.systolic}) è sotto la soglia bassa impostata (${t.sysLow}).")
        if (m.diastolic <= t.diaLow) out.add("La minima (${m.diastolic}) è sotto la soglia bassa impostata (${t.diaLow}).")
        if (m.pulse in 1 until t.pulseLow) out.add("Le pulsazioni (${m.pulse}) sono sotto la soglia impostata (${t.pulseLow}).")
        if (m.pulse >= t.pulseHigh) out.add("Le pulsazioni (${m.pulse}) superano la soglia impostata (${t.pulseHigh}).")
        if (m.irregular) out.add("Segnalato battito irregolare: se si ripete spesso, mostra i rilevamenti al medico.")

        // Variazioni significative rispetto alla media delle ultime misurazioni (max 5 negli ultimi 7 giorni)
        val recenti = precedenti
            .filter { it.timestamp < m.timestamp && it.timestamp > m.timestamp - 7L * 86400000L }
            .take(5)
        if (recenti.size >= 3) {
            val mS = recenti.map { it.systolic }.average().toInt()
            if (abs(m.systolic - mS) >= t.variazioneSys) {
                out.add("Variazione significativa della massima: ${m.systolic} rispetto alla media recente di $mS.")
            }
            val mD = recenti.map { it.diastolic }.average().toInt()
            if (abs(m.diastolic - mD) >= t.variazioneDia) {
                out.add("Variazione significativa della minima: ${m.diastolic} rispetto alla media recente di $mD.")
            }
        }
        return out
    }
}
