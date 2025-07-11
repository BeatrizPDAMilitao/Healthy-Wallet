package com.example.ethktprototype.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "procedures")
data class ProcedureEntity(
    @PrimaryKey val id: String,
    val code: String,
    val status: String,
    val performedDateTime: String,
    val subjectId: String,
)