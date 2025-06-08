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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPatient(patient: PatientEntity)

    @Query("SELECT * FROM patients WHERE id = :patientId")
    suspend fun getPatientById(patientId: String): PatientEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertObservation(observation: ObservationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertObservations(observations: List<ObservationEntity>)

    @Query("SELECT * FROM observations WHERE code = :code LIMIT 1")
    suspend fun findByCode(code: String): ObservationEntity?

    @Query("SELECT * FROM observations")
    suspend fun getObservations(): List<ObservationEntity>

    // CONDITIONS
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConditions(conditions: List<ConditionEntity>)

    @Query("SELECT * FROM conditions")
    suspend fun getConditions(): List<ConditionEntity>

    // DIAGNOSTIC REPORTS
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDiagnosticReports(reports: List<DiagnosticReportEntity>)

    @Query("SELECT * FROM diagnostic_reports")
    suspend fun getDiagnosticReports(): List<DiagnosticReportEntity>

    // MEDICATION REQUESTS
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedicationRequests(requests: List<MedicationRequestEntity>)

    @Query("SELECT * FROM medication_requests")
    suspend fun getMedicationRequests(): List<MedicationRequestEntity>

    // MEDICATION STATEMENTS
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedicationStatements(statements: List<MedicationStatementEntity>)

    @Query("SELECT * FROM medication_statements")
    suspend fun getMedicationStatements(): List<MedicationStatementEntity>

    // IMMUNIZATIONS
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImmunizations(immunizations: List<ImmunizationEntity>)

    @Query("SELECT * FROM immunizations")
    suspend fun getImmunizations(): List<ImmunizationEntity>

    // ALLERGIES
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllergies(allergies: List<AllergyIntoleranceEntity>)

    @Query("SELECT * FROM allergies")
    suspend fun getAllergies(): List<AllergyIntoleranceEntity>

    // DEVICES
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDevices(devices: List<DeviceEntity>)

    @Query("SELECT * FROM devices")
    suspend fun getDevices(): List<DeviceEntity>

    // PROCEDURES
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProcedures(procedures: List<ProcedureEntity>)

    @Query("SELECT * FROM procedures")
    suspend fun getProcedures(): List<ProcedureEntity>

    @Query("DELETE FROM patients")
    suspend fun deleteAllPatients()

    @Query("DELETE FROM observations")
    suspend fun deleteAllObservations()

    @Query("DELETE FROM conditions")
    suspend fun deleteAllConditions()

    @Query("DELETE FROM diagnostic_reports")
    suspend fun deleteAllDiagnosticReports()

    @Query("DELETE FROM medication_requests")
    suspend fun deleteAllMedicationRequests()

    @Query("DELETE FROM medication_statements")
    suspend fun deleteAllMedicationStatements()

    @Query("DELETE FROM immunizations")
    suspend fun deleteAllImmunizations()

    @Query("DELETE FROM allergies")
    suspend fun deleteAllAllergies()

    @Query("DELETE FROM devices")
    suspend fun deleteAllDevices()

    @Query("DELETE FROM procedures")
    suspend fun deleteAllProcedures()
}