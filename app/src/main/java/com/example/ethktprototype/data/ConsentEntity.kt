package com.example.ethktprototype.data

data class ConsentEntity(
    val id: String,
    val status: String,
    val patientId: String,
    val performerId: String,
    val policyUri: String,
    val dataReferences: List<String>, //shared resources in the consent
    val lastUpdated: String
)

data class ConsentDisplayItem(
    val id: String,
    val practitionerId: String,
    val practitionerName: String,
    val sharedResources: List<SharedResourceInfo>,
    val lastUpdated: String
) {
    override fun toString(): String {
        return "- Last Updated: $lastUpdated\n" +
                "- Shared Resources:\n" + sharedResources.joinToString(separator = "")
    }}

data class SharedResourceInfo(
    val type: String,          // e.g., "DiagnosticReport"
    val description: String,   // e.g., "Glucose Lab Report - June 2024"
    val id: String             // MedPlum id
) {
    override fun toString(): String {
        return "    Type: $type, Description: $description, ID: $id\n"
    }
}
