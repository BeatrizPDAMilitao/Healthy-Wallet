package com.example.ethktprototype.data

class SharedEHRState(
    val diagnosticReports: List<DiagnosticReportEntity> = emptyList(),
    val observations: List<ObservationEntity> = emptyList(),
    val immunizations: List<ImmunizationEntity> = emptyList(),
    val medicationStatements: List<MedicationStatementEntity> = emptyList(),
    val allergies: List<AllergyIntoleranceEntity> = emptyList(),
    val medicationRequests: List<MedicationRequestEntity> = emptyList(),
    val procedures: List<ProcedureEntity> = emptyList(),
    val conditions: List<ConditionEntity> = emptyList()
)