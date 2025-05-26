package com.example.ethktprototype.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "diagnostic_reports")
data class DiagnosticReportEntity(
    @PrimaryKey val id: String,
    val code: String,
    val status: String,
    val effectiveDateTime: String,
    val result: String
)