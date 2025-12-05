package com.dailydrug.ocr.domain.model

data class DrugInfo(
    val drugName: String,
    val intakeTime: String,
    val cycle: String,
    val description: String,
    val rawText: String
)
