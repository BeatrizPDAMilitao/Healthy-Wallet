package com.example.ethktprototype

import com.example.ethktprototype.data.AllergyIntoleranceEntity
import com.example.ethktprototype.data.ConditionEntity
import com.example.ethktprototype.data.ConsentDisplayItem
import com.example.ethktprototype.data.ConsentEntity
import com.example.ethktprototype.data.DeviceEntity
import com.example.ethktprototype.data.DiagnosticReportEntity
import com.example.ethktprototype.data.HealthSummaryResult
import com.example.ethktprototype.data.ImmunizationEntity
import com.example.ethktprototype.data.MedicationRequestEntity
import com.example.ethktprototype.data.MedicationStatementEntity
import com.example.ethktprototype.data.ObservationEntity
import com.example.ethktprototype.data.PatientEntity
import com.example.ethktprototype.data.PractitionerEntity
import com.example.ethktprototype.data.ProcedureEntity
import org.json.JSONObject

interface IMedPlumAPI {
    fun getAccessToken(): String?
    suspend fun fetchPatientComplete(patientId: String): PatientEntity?
    suspend fun fetchPractitioner(practitionerId: String): PractitionerEntity?
    suspend fun fetchPractitionersList(name: String): List<PractitionerEntity>?
    suspend fun fetchPatientListOfPractitioner(practitionerId: String): List<PatientEntity>?
    suspend fun fetchPatientName(patientId: String): String
    suspend fun fetchPatientsNames(patientIds: List<String>): Map<String,String>
    suspend fun getMedplumAccessToken(clientId: String, clientSecret: String): String?
    suspend fun fetchConditions(subjectId: String): List<ConditionEntity>?
    suspend fun fetchObservations(subjectId: String): List<ObservationEntity>?
    suspend fun fetchDiagnosticReports(subjectId: String, isPractitioner: Boolean = false): List<DiagnosticReportEntity>?
    suspend fun fetchMedicationRequests(subjectId: String): List<MedicationRequestEntity>?
    suspend fun fetchMedicationStatements(subjectId: String): List<MedicationStatementEntity>?
    suspend fun fetchImmunizations(subjectId: String): List<ImmunizationEntity>?
    suspend fun fetchAllergies(subjectId: String): List<AllergyIntoleranceEntity>?
    suspend fun fetchDevices(subjectId: String): List<DeviceEntity>?
    suspend fun fetchProcedures(subjectId: String): List<ProcedureEntity>?
    suspend fun fetchObservationByID(observationId: String): ObservationEntity?
    suspend fun fetchHealthSummary(patientId: String): HealthSummaryResult
    suspend fun postRecordToMedplum(jsonBody: String, resourceType: String): String?
    suspend fun createMedplumRecord(recordType: String, record: Any, patientId: String, performerId: String): String
    fun createDiagnosticReport(record: DiagnosticReportEntity, patientId: String, performerId: String): String
    fun createImmunization(record: ImmunizationEntity): String
    fun createAllergy(record: AllergyIntoleranceEntity): String
    fun createCondition(record: ConditionEntity): String
    fun createObservation(record: ObservationEntity): String
    fun createMedicationRequest(record: MedicationRequestEntity): String
    fun createProcedure(record: ProcedureEntity, patientId: String): String
    suspend fun getObservationIdsFromDiagnosticReport(diagnosticReportId: String): List<String>
    suspend fun createConsentResource(patientId: String, practitionerId: String, resourceId: String, accessPolicyId: String): Boolean
    suspend fun grantFullResourceAccess(resourceId: String, practitionerId: String, patientId: String): Boolean
    suspend fun loadSharedResourcesFromConsents(practitionerId: String, ): Map<String, List<JSONObject>>
    suspend fun checkConsentExists(practitionerId: String, resourceId: String): Boolean
    suspend fun fetchConsentsByPatient(patientId: String): List<ConsentDisplayItem>
    suspend fun resolveConsentDisplayItems(consents: List<ConsentEntity>, ): List<ConsentDisplayItem>
    suspend fun revokeAccessPermission(permissionId: String): Boolean
}