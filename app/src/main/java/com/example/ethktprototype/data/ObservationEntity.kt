package com.example.ethktprototype.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "observations")
data class ObservationEntity(
    @PrimaryKey val id: String,
    val status: String,
    val code: String,
    val subjectId: String,
    val encounter: String,
    val effectiveDateTime: String,
    val valueQuantity: Float
)