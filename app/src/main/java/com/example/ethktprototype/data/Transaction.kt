package com.example.ethktprototype.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey val id: String,
    val date: String,
    val status: String
)
fun TransactionEntity.toTransaction(): Transaction {
    return Transaction(id = this.id, date = this.date, status = this.status)
}