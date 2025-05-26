package com.example.ethktprototype.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "patients")
data class PatientEntity(
    @PrimaryKey val id: String,
    val name: String,
    val birthDate: String,
    val gender: String,
    val identifier: String,
    val address: String,
    val healthUnit: String,    // Organization name
    val doctor: String
)
