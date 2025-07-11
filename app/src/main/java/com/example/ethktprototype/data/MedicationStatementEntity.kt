package com.example.ethktprototype.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "medication_statements")
data class MedicationStatementEntity(
    @PrimaryKey val id: String,
    val medication: String,
    val status: String,
    val start: String,
    val end: String,
    val subjectId: String,
)