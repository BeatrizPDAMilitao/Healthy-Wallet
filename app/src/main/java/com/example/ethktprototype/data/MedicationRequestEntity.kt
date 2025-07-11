package com.example.ethktprototype.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "medication_requests")
data class MedicationRequestEntity(
    @PrimaryKey val id: String,
    val medication: String,
    val authoredOn: String,
    val status: String,
    val dosage: String,
    val subjectId: String,
)