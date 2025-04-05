package com.example.ethktprototype.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "medication_requests")
data class MedicationRequestEntity(
    @PrimaryKey val id: String,
    val status: String,
    val intent: String,
    val subjectId: String,
    val medication: String,
    val allowed: Boolean,
    val medicationCodeableConcept: String,
    val authoredOn: String,
    val dosageInstruction: String
)
