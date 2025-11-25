package com.example.ethktprototype.data

import org.json.JSONObject
import org.json.JSONArray

/**
 * This object contains GraphQL queries for fetching data from MedPlum.
 * These queries are used to log actions on the blockchain.
 */

object GraphQLQueries {

    fun buildPatientCompleteQuery(patientId: String): String {
        return """
        query {
          Patient(id: "$patientId") {
            id
            name {
              given
              family
            }
            birthDate
            gender
            telecom {
              value
            }
            address {
              line
              city
              state
              postalCode
            }
            identifier {
              system
              value
            }
            generalPractitioner {
              reference
              display
            }
            managingOrganization {
              reference
              display
            }
          }
        }
    """.trimIndent()
    }

    fun buildGetConditionsForPatientQuery(subjectId: String): String = """
    query {
      ConditionList(subject: "$subjectId") {
        resourceType
        id
        code {
          text
        }
        onsetDateTime
        clinicalStatus {
          text
        }
      }
    }
""".trimIndent()

    fun buildGetPatientDiagnosticReportQuery(subjectId: String): String = """
    query {
      DiagnosticReportList(subject: "$subjectId", _sort: "-date") {
        id
        status
        code {
          text
        }
        effectiveDateTime
        result {
          display
          reference
        }
      }
    }
""".trimIndent()

    fun buildGetPatientMedicationRequestsQuery(subjectId: String): String = """
    query {
      MedicationRequestList(subject: "$subjectId", _sort: "-date") {
        id
        medicationCodeableConcept {
          text
        }
        authoredOn
        status
        dosageInstruction {
          text
        }
      }
    }
""".trimIndent()

    fun buildGetPatientMedicationStatementsQuery(subjectId: String): String = """
    query {
      MedicationStatementList(subject: "$subjectId") {
        id
        medicationCodeableConcept {
          text
        }
        status
        effectivePeriod {
          start
          end
        }
      }
    }
""".trimIndent()

    fun buildGetPatientImmunizationsQuery(patientId: String): String = """
    query {
      ImmunizationList(patient: "$patientId", _sort: "-date") {
        id
        vaccineCode {
          text
        }
        occurrenceDateTime
        status
        lotNumber
      }
    }
""".trimIndent()

    fun buildGetPatientAllergiesQuery(patientId: String): String = """
    query {
      AllergyIntoleranceList(patient: "$patientId", _sort: "-date") {
        id
        clinicalStatus {
          text
        }
        code {
          text
        }
        onsetDateTime
        recordedDate
      }
    }
""".trimIndent()

    fun buildGetPatientDevicesQuery(patientId: String): String = """
    query {
      DeviceList(patient: "$patientId") {
        id
        type {
          text
        }
        status
        manufactureDate
      }
    }
""".trimIndent()

    fun buildGetPatientProceduresQuery(subjectId: String): String = """
    query {
      ProcedureList(subject: "$subjectId", _sort: "-date") {
        id
        status
        code {
          text
        }
        performedDateTime
      }
    }
""".trimIndent()

    fun buildGetObservationsQuery(subjectId: String): String = """
    query {
      ObservationList(subject: "$subjectId", _sort: "-date") {
        id
        status
        code {
          text
        }
        valueQuantity {
          value
          unit
        }
        effectiveDateTime
      }
    }
""".trimIndent()

    /////////////////////////////////////////////////////////////////
    // GraphQL queries for fetching token balances and NFTs
    fun getTokenBalancesQuery(
        addresses: List<String>,
        first: Int = 10
    ): JSONObject {
        return JSONObject().apply {
            put("query", """
                query TokenBalances(${"$"}addresses: [Address!]!, ${"$"}first: Int) {
                    portfolioV2(addresses: ${"$"}addresses) {
                        tokenBalances {
                            totalBalanceUSD
                            byToken(first: ${"$"}first) {
                                totalCount
                                edges {
                                    node {
                                        symbol
                                        tokenAddress
                                        balance
                                        balanceUSD
                                        price
                                        imgUrlV2
                                        name
                                        network {
                                            name
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            """.trimIndent())

            put("variables", JSONObject().apply {
                put("addresses", JSONArray().apply {
                    addresses.forEach { put(it) }
                })
                put("first", first)
            })
        }
    }

    fun getNftUsersTokensQuery(
        owners: List<String>,
        network: String,
        first: Int = 10
    ): JSONObject {
        val graphQLQuery = """
            query(${"$"}owners: [Address!]!, ${"$"}network: Network, ${"$"}first: Int) {
                nftUsersTokens(owners: ${"$"}owners, network: ${"$"}network, first: ${"$"}first) {
                    edges {
                        node {
                            id
                            tokenId
                            name
                            collection {
                                name
                                address
                            }
                            mediasV3 {
                                images {
                                    edges {
                                        node {
                                            original
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        """.trimIndent()

        return JSONObject().apply {
            put("query", graphQLQuery)
            put("variables", JSONObject().apply {
                put("owners", JSONArray().apply {
                    owners.forEach { put(it) }
                })
                put("network", network)
                put("first", first)
            })
        }
    }

    fun buildPractitionerCompleteQuery(practitionerId: String): String {
        return """
        query {
          Practitioner(id: "$practitionerId") {
            id
            name {
              given
              family
            }
            gender
            telecom {
              system
              value
              use
            }
            address {
              line
              city
              state
              postalCode
              country
            }
            qualification {
              code {
                coding {
                  display
                }
              }
              period {
                start
                end
              }
            }
            identifier {
              system
              value
            }
          }
        }
    """.trimIndent()
    }

    fun buildPractitionersListQuery(name: String): String {
        return """
        query {
          PractitionerList(name: "$name") {
            id
            name {
              given
              family
            }
            gender
            telecom {
              system
              value
              use
            }
            identifier {
              system
              value
            }
          }
        }
    """.trimIndent()
    }

    fun buildGetPatientListForPractitionerQuery(practitionerId: String): String {
        return """
        query {
          PatientList(general_practitioner: "$practitionerId") {
            id
            name {
              given
              family
            }
            gender
            birthDate
          }
        }
    """.trimIndent()
    }

    fun buildConsentListQuery(): String {
        return """
        query {
          ConsentList(status: "active") {
            id
            status
            patient {
              reference
            }
            performer {
              reference
            }
            policy {
              uri
              authority
            }
            provision {
              type
              actor {
                reference {
                  reference
                }
              }
              data {
                reference {
                  reference
                }
              }
            }
            meta {
              lastUpdated
            }
          }
        }
    """.trimIndent()
    }

    fun buildSharedResourcesText(): String {
        return "Resource shared via active Consent are fetched individually by ID, based on the references listed in provision.data[].reference where the practitioner is listed as a provision.actor."
    }

    fun buildRevokeConsentMutation(consentId: String): String {
        return """
        mutation {
          updateConsent(id: "$consentId", input: {
            status: "inactive"
          }) {
            id
            status
          }
        }
    """.trimIndent()
    }

    fun buildConsentsForPatientQuery(patientId: String): String {
        return """
        query {
          ConsentList(status: "active") {
            id
            status
            patient {
              reference
            }
            performer {
              reference
            }
            policy {
              uri
            }
            meta {
              lastUpdated
            }
            provision {
              data {
                reference {
                  reference
                }
              }
            }
          }
        }
    """.trimIndent()
    }

    fun buildCheckConsentExistsQuery(patientId: String): String {
        return """
        query {
          ConsentList(status: "active", patient: "Patient/$patientId") {
            id
            status
            patient {
              reference
            }
            performer {
              reference
            }
            provision {
              actor {
                reference {
                  reference
                }
              }
              data {
                reference {
                  reference
                }
              }
            }
          }
        }
    """.trimIndent()
    }

    fun buildCreateConsentMutation(
        patientId: String,
        practitionerId: String,
        resourceId: String
    ): String {
        return """
        mutation {
          createConsent(input: {
            status: "active"
            patient: {
              reference: "Patient/$patientId"
            }
            performer: [{
              reference: "Practitioner/$practitionerId"
            }]
            scope: {
              coding: [{
                system: "http://terminology.hl7.org/CodeSystem/consentscope"
                code: "patient-privacy"
              }]
            }
            category: [{
              coding: [{
                system: "http://terminology.hl7.org/CodeSystem/consentcategorycodes"
                code: "INFA"
              }]
            }]
            provision: {
              type: "permit"
              actor: [{
                role: {
                  coding: [{
                    system: "http://terminology.hl7.org/CodeSystem/consentactorrole"
                    code: "PRCP"
                  }]
                }
                reference: {
                  reference: "Practitioner/$practitionerId"
                }
              }]
              data: [{
                meaning: "instance"
                reference: {
                  reference: "$resourceId"
                }
              }]
            }
          }) {
            id
            status
          }
        }
    """.trimIndent()
    }


}