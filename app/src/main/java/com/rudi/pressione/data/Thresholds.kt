package com.rudi.pressione.data

/** Soglie personali per gli avvisi informativi. */
data class Thresholds(
    val sysHigh: Int = 140,
    val sysLow: Int = 90,
    val diaHigh: Int = 90,
    val diaLow: Int = 60,
    val pulseHigh: Int = 100,
    val pulseLow: Int = 50,
    val variazioneSys: Int = 15,
    val variazioneDia: Int = 10
)
