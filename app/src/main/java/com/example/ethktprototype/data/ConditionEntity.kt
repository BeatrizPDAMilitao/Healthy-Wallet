package com.example.ethktprototype.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conditions")
data class ConditionEntity(
    @PrimaryKey val id: String,
    val code: String,
    val subjectId: String,
    val onsetDateTime: String,
    val clinicalStatus: String,
    val recorderId: String
)
