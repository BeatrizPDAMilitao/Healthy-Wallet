package com.example.ethktprototype.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "immunizations")
data class ImmunizationEntity(
    @PrimaryKey val id: String,
    val vaccine: String,
    val occurrenceDateTime: String,
    val status: String,
    val lotNumber: String
)