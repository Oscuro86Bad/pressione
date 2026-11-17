package com.rudi.pressione.data

import java.util.UUID

data class Measurement(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val systolic: Int,
    val diastolic: Int,
    val pulse: Int = 0,
    val irregular: Boolean = false,
    val note: String = ""
)
