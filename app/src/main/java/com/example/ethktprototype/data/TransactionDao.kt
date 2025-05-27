package com.example.ethktprototype.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface TransactionDao {
    @Insert
    suspend fun insertTransaction(transaction: TransactionEntity)

    @Insert
    suspend fun insertZkpTransaction(zkp: ZkpEntity)

    @Query("SELECT * FROM transactions")
    suspend fun getAllTransactions(): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE id = :transactionId")
    suspend fun getById(transactionId: String): TransactionEntity?

    @Query("UPDATE transactions SET status = :status WHERE id = :transactionId")
    suspend fun updateTransactionStatus(transactionId: String, status: String)

    @Query("UPDATE zkp_transactions SET qrCodeFileName = :qrCodeFileName WHERE id = :transactionId")
    suspend fun updateZkpQrCode(transactionId: String, qrCodeFileName: String)

    @Query("SELECT COUNT(*) FROM transactions WHERE id = :transactionId")
    suspend fun transactionExists(transactionId: String): Int

    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions()

    @Query("SELECT COUNT(*) FROM transactions")
    suspend fun countTransactions(): Int

    @Query("SELECT * FROM transactions WHERE id = :transactionId")
    suspend fun getTransactionWithDetails(transactionId: String): TransactionWithDetails?

    @Query("SELECT * FROM transactions")
    suspend fun getTransactionsWithProof(): List<TransactionWithProof>

    @Query(" SELECT * FROM transactions WHERE id = :transactionId")
    suspend fun getTransactionWithProofById(transactionId: String): TransactionWithProof?

    @Insert
    suspend fun insertPatient(patient: PatientEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateObservation(observation: ObservationEntity)

    @Query("SELECT * FROM observations WHERE code = :code LIMIT 1")
    suspend fun findByCode(code: String): ObservationEntity?

}