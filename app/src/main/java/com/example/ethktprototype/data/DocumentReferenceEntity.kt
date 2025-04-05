package com.example.ethktprototype.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "document_references")
data class DocumentReferenceEntity(
    @PrimaryKey val id: String,
    val status: String,
    val subjectId: String, // foreign key to patient
    val content: String,
    val description: String,
    val date: String
)