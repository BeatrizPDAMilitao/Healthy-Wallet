package com.example.ethktprototype.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "practitioners")
data class PractitionerEntity(
    @PrimaryKey val id: String,
    val name: String,
    val telecom: String,
    val address: String
)
