package com.example.ethktprototype.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "allergies")
data class AllergyIntoleranceEntity(
    @PrimaryKey val id: String,
    val code: String,
    val status: String,
    val onset: String,
    val recordedDate: String,
    val subjectId: String,
)