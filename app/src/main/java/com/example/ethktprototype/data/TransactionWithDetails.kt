package com.example.ethktprototype.data

import androidx.room.Embedded
import androidx.room.Relation

data class TransactionWithDetails(
    @Embedded val transaction: TransactionEntity,

    @Relation(parentColumn = "patientId", entityColumn = "id")
    val patient: PatientEntity,

    @Relation(parentColumn = "practitionerId", entityColumn = "id")
    val practitioner: PractitionerEntity,

    //@Relation(parentColumn = "id", entityColumn = "id")
    //val zkpProof: ZkpEntity?
)

data class TransactionWithProof(
    @Embedded val transaction: TransactionEntity,
    @Relation(parentColumn = "id", entityColumn = "id")
    val zkpProof: ZkpEntity?
)

fun TransactionWithProof.toTransaction(): Transaction {
    val conditions = zkpProof?.conditionsJson.let { Converters.toConditionsList(it) }
    return Transaction(id = this.transaction.id, date = this.transaction.date, status = this.transaction.status, recordId = this.transaction.recordId,
        practitionerId = this.transaction.practitionerId, practitionerAddress = this.transaction.practitionerAddress, type = this.transaction.type, patientId = this.transaction.patientId, conditions = conditions, qrCodeFileName = zkpProof?.qrCodeFileName)
}

