package com.example.ethktprototype.data

import org.json.JSONObject
import org.json.JSONArray

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
      DiagnosticReportList(subject: "$subjectId") {
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
      MedicationRequestList(subject: "$subjectId") {
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
      ImmunizationList(patient: "$patientId") {
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
      AllergyIntoleranceList(patient: "$patientId") {
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
      ProcedureList(subject: "$subjectId") {
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
      ObservationList(subject: "$subjectId") {
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


}