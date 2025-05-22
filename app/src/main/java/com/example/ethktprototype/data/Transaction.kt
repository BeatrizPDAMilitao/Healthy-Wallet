package com.example.ethktprototype.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.room.TypeConverters

@Entity(tableName = "transactions")
@TypeConverters(Converters::class)
data class TransactionEntity(
    @PrimaryKey val id: String,
    val date: String,
    val status: String,
    val type: String,
    val recordId: String,
    val patientId: String,
    val practitionerId: String,
    val practitionerAddress: String,
    val documentReferenceId: String,
    val medicationRequestId: String,
    val conditionId: String,
    val encounterId: String,
    val observationId: String,
    val conditionsJson: String? = null
)

fun TransactionEntity.toTransaction(): Transaction {
    val conditions = conditionsJson?.let { Converters.toConditionsList(it) }
    return Transaction(id = this.id, date = this.date, status = this.status, recordId = this.recordId,
        practitionerId = this.practitionerId, practitionerAddress = this.practitionerAddress, type = this.type, patientId = this.patientId, conditions = conditions, )
}

@Entity(
    tableName = "zkp_transactions",
    foreignKeys = [
        ForeignKey(
            entity = TransactionEntity::class,
            parentColumns = ["id"],
            childColumns = ["id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
@TypeConverters(Converters::class)
data class ZkpEntity(
    @PrimaryKey val id: String,
    val conditionsJson: String,
    val qrCodeFileName: String,
)