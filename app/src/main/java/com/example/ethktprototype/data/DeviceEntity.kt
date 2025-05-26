package com.example.ethktprototype.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "devices")
data class DeviceEntity(
    @PrimaryKey val id: String,
    val type: String,
    val status: String,
    val manufactureDate: String
)
