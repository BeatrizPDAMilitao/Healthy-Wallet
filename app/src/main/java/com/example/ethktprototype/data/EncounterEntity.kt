package com.example.ethktprototype.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "encounters")
data class EncounterEntity(
    @PrimaryKey val id: String,
    val status: String,
    val classCode: String,
    val subjectId: String,
    val effectiveDateTime: String,
    val valueQuantity: Float
)
