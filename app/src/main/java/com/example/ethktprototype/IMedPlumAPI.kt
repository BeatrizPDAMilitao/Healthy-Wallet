package com.example.ethktprototype

import com.example.ethktprototype.data.AllergyIntoleranceEntity
import com.example.ethktprototype.data.ConditionEntity
import com.example.ethktprototype.data.DeviceEntity
import com.example.ethktprototype.data.DiagnosticReportEntity
import com.example.ethktprototype.data.ImmunizationEntity
import com.example.ethktprototype.data.MedicationRequestEntity
import com.example.ethktprototype.data.MedicationStatementEntity
import com.example.ethktprototype.data.ObservationEntity
import com.example.ethktprototype.data.PatientEntity
import com.example.ethktprototype.data.PractitionerEntity
import com.example.ethktprototype.data.ProcedureEntity

interface IMedPlumAPI {
    fun getAccessToken(): String?
    suspend fun fetchPatient(): PatientEntity?
    suspend fun fetchPatientComplete(patientId: String): PatientEntity?
    suspend fun fetchPractitioner(practitionerId: String): PractitionerEntity?
    suspend fun getMedplumAccessToken(clientId: String, clientSecret: String): String?
    suspend fun fetchConditions(subjectId: String): List<ConditionEntity>?
    suspend fun fetchObservations(subjectId: String): List<ObservationEntity>?
    suspend fun fetchDiagnosticReports(subjectId: String): List<DiagnosticReportEntity>?
    suspend fun fetchMedicationRequests(subjectId: String): List<MedicationRequestEntity>?
    suspend fun fetchMedicationStatements(subjectId: String): List<MedicationStatementEntity>?
    suspend fun fetchImmunizations(subjectId: String): List<ImmunizationEntity>?
    suspend fun fetchAllergies(subjectId: String): List<AllergyIntoleranceEntity>?
    suspend fun fetchDevices(subjectId: String): List<DeviceEntity>?
    suspend fun fetchProcedures(subjectId: String): List<ProcedureEntity>?
    suspend fun fetchObservationByID(observationId: String): ObservationEntity?
}