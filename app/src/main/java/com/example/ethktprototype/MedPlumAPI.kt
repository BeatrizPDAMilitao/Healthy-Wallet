package com.example.ethktprototype

import android.app.Application
import android.util.Log
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.exception.ApolloHttpException
import com.example.ethktprototype.data.PatientEntity
import com.example.ethktprototype.data.PractitionerEntity
import com.example.medplum.GetPatientQuery
import com.example.medplum.GetPractitionerQuery
import com.example.ethktprototype.data.ConditionEntity
import com.example.ethktprototype.data.DiagnosticReportEntity
import com.example.ethktprototype.data.ImmunizationEntity
import com.example.ethktprototype.data.MedicationRequestEntity
import com.example.ethktprototype.data.MedicationStatementEntity
import com.example.medplum.GetConditionsForPatientQuery
import com.example.medplum.GetPatientDiagnosticReportQuery
import com.example.medplum.GetPatientImmunizationsQuery
import com.example.medplum.GetPatientMedicationRequestsQuery
import com.example.medplum.GetPatientMedicationStatementsQuery
import com.example.ethktprototype.data.AllergyIntoleranceEntity
import com.example.ethktprototype.data.AppDatabase
import com.example.ethktprototype.data.DeviceEntity
import com.example.ethktprototype.data.ObservationEntity
import com.example.ethktprototype.data.ProcedureEntity
import com.example.medplum.GetObservationByIdQuery
import com.example.medplum.GetObservationsQuery
import com.example.medplum.GetOrganizationQuery
import com.example.medplum.GetPatientAllergiesQuery
import com.example.medplum.GetPatientCompleteQuery
import com.example.medplum.GetPatientDevicesQuery
import com.example.medplum.GetPatientProceduresQuery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom


class MedPlumAPI(private val application: Application) : IMedPlumAPI {
    val sharedPreferences = application.getSharedPreferences("auth", Context.MODE_PRIVATE)

    private val CLIENT_ID = "01968b74-d07a-766d-8331-1cefab3a8922"
    private val CLIENT_SECRET = "a6bcb43c6607ad32e3ae324ff6689b6716d6abcaad6a330e6e5b330616cb71ac"
    private val redirectUri: String = "myapp://oauth2redirect"
    private val TAG = "MedplumAuth"
    private val authUrl = "https://api.medplum.com/oauth2/authorize"
    private val tokenUrl = "https://api.medplum.com/oauth2/token"
    private var codeVerifier: String? = null

    init {
        getAccessToken()
    }

    fun logout(context: Context) {
        sharedPreferences.edit()
            .remove("access_token")
            .remove("refresh_token")
            .remove("profile_id")
            .apply()
    }

    fun generateCodeVerifier(): String {
        val secureRandom = SecureRandom()
        val code = ByteArray(32)
        secureRandom.nextBytes(code)
        return Base64.encodeToString(code, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
    }

    fun generateCodeChallenge(verifier: String): String {
        val bytes = verifier.toByteArray(Charsets.US_ASCII)
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return Base64.encodeToString(digest, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
    }

    fun launchLogin(activity: Activity) {
        codeVerifier = generateCodeVerifier()
        val codeChallenge = generateCodeChallenge(codeVerifier!!)

        sharedPreferences.edit().putString("code_verifier", codeVerifier).apply()

        val uri = Uri.parse(authUrl).buildUpon()
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("client_id", CLIENT_ID)
            .appendQueryParameter("redirect_uri", redirectUri)
            .appendQueryParameter("scope", "openid profile user/*.read")
            .appendQueryParameter("code_challenge_method", "S256")
            .appendQueryParameter("code_challenge", codeChallenge)
            .build()

        val intent = Intent(Intent.ACTION_VIEW, uri)
        activity.startActivity(intent)
    }

    suspend fun handleRedirectAndExchange(intent: Intent, onTokenReceived: (accessToken: String) -> Unit) {
        val data = intent.data ?: return
        if (data.host == Uri.parse(redirectUri).host) {
            val code = data.getQueryParameter("code")
            if (code == null) {
                Log.e("LoginActivity", "No auth code in redirect")
                return
            }
            exchangeCodeForToken(code, onTokenReceived)
        }
    }

    private suspend fun exchangeCodeForToken(code: String, onTokenReceived: (String) -> Unit) = withContext(Dispatchers.IO) {
        val codeVerifier = sharedPreferences.getString("code_verifier", null)

        if (codeVerifier.isNullOrBlank()) {
            Log.e(TAG, "Missing code_verifier")
            return@withContext
        }

        val form = "grant_type=authorization_code" +
                "&code=$code" +
                "&redirect_uri=$redirectUri" +
                "&client_id=$CLIENT_ID" +
                "&code_verifier=${codeVerifier}"

        val request = Request.Builder()
            .url(tokenUrl)
            .post(form.toRequestBody("application/x-www-form-urlencoded".toMediaType()))
            .build()

        try {
            val client = OkHttpClient()
            client.newCall(request).execute().use { response ->
                val body = response.body?.string()
                if (!response.isSuccessful || body == null) {
                    Log.e(TAG, "Token exchange failed: $body")
                    return@use
                }

                val json = JSONObject(body)
                val accessToken = json.getString("access_token")
                val refreshToken = json.optString("refresh_token", null)

                if (refreshToken != null) {
                    sharedPreferences.edit().putString("refresh_token", refreshToken).apply()
                } else {
                    Log.w(TAG, "No refresh token returned")
                }

                val profileObj = json.optJSONObject("profile")
                val profileRef = profileObj?.optString("reference", null)
                if (profileRef != null) {
                    sharedPreferences.edit().putString("user_profile", profileRef).apply()
                }


                sharedPreferences.edit()
                    .putString("access_token", accessToken)
                    .apply()

                Log.d(TAG, "Access token: $accessToken")
                setupApolloClient(accessToken)
                onTokenReceived(accessToken)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during token exchange", e)
        }
    }

    private fun setupApolloClient(accessToken: String) {
        apolloClient = ApolloClient.Builder()
            .serverUrl("https://api.medplum.com/fhir/R4/\$graphql")
            .addHttpHeader("Authorization", "Bearer $accessToken")
            .build()
    }

    suspend fun refreshAccessTokenIfNeeded(): String? = withContext(Dispatchers.IO) {
        val refreshToken = sharedPreferences.getString("refresh_token", null) ?: return@withContext null

        val form = "grant_type=refresh_token" +
                "&refresh_token=$refreshToken" +
                "&client_id=$CLIENT_ID"

        val request = Request.Builder()
            .url(tokenUrl)
            .post(form.toRequestBody("application/x-www-form-urlencoded".toMediaType()))
            .build()

        try {
            val client = OkHttpClient()
            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: return@use null
                val json = JSONObject(body)

                val newAccessToken = json.getString("access_token")
                sharedPreferences.edit().putString("access_token", newAccessToken).apply()
                setupApolloClient(newAccessToken)
                newAccessToken
            }
        } catch (e: Exception) {
            Log.e(TAG, "Refresh token failed", e)
            null
        }
    }

    fun getLoggedInPatientId(): String? {
        return sharedPreferences.getString("user_profile", null)?.removePrefix("Patient/")
    }

    private suspend fun ensureApolloClientInitialized(): Boolean {
        if (!::apolloClient.isInitialized) {
            val token = getAccessToken() ?: refreshAccessTokenIfNeeded()
            Log.d(TAG, "Access token: $token")
            if (token == null) {
                Log.e(TAG, "Unable to initialize ApolloClient - no valid token")
                return false
            }
        }
        Log.d(TAG, "Access token ${getAccessToken()}")
        return true
    }


    override fun getAccessToken(): String? {
        val token = sharedPreferences.getString("access_token", null)
        if (token != null) {
            setupApolloClient(token)
        }
        return token
    }

    private lateinit var apolloClient: ApolloClient

    override suspend fun fetchPatient(): PatientEntity? {
        return try {
            if (!ensureApolloClientInitialized()) return null

            val response = apolloClient.query(GetPatientQuery("01968b59-76f3-7228-aea9-07db748ee2ca")).execute()
            Log.d("MedPlum", "GraphQL response: $response")

            if (response.hasErrors()) {
                Log.e("MedPlum", "GraphQL errors: ${response.errors}")
                return null
            }

            val patient = response.data?.Patient ?: return null

            val given = patient.name?.firstOrNull()?.given?.firstOrNull() ?: ""
            val family = patient.name?.firstOrNull()?.family ?: ""
            val name = "$given $family".trim()

            return PatientEntity(
                id = patient.id ?: "unknown",
                name = name,
                birthDate = patient.birthDate ?: "unknown",
                gender = patient.gender ?: "unknown",
                identifier = "", // not returned in this query yet
                address = "",     // not returned in this query yet
                healthUnit = "",  // not returned in this query yet
                doctor = ""       // not returned in this query yet
            )

        } catch (e: ApolloHttpException) {
            Log.e("MedPlum", "HTTP error ${e.statusCode}: ${e.message}", e)
            val errorBody = e.body?.use { it.readUtf8() }
            Log.e("MedPlum", "Error body: $errorBody")
            null
        } catch (e: Exception) {
            Log.e("MedPlum", "Unexpected error", e)
            null
        }
    }

    override suspend fun fetchPatientComplete(patientId: String): PatientEntity? {
        return try {
            Log.d("MedPlum", "Fetching full patient info for $patientId")
            if (!ensureApolloClientInitialized()) return null
            Log.d("MedPlum", "Apollo client initialized")

            val patientId2 = getLoggedInPatientId() ?: return null
            Log.d("MedPlum", "Patient ID: $patientId2")
            val response = apolloClient.query(GetPatientCompleteQuery(patientId2)).execute()
            Log.d("MedPlum", "GraphQL response: $response")
            if (response.hasErrors()) {
                Log.e("MedPlum", "GraphQL errors: ${response.errors}")
                return null
            }

            val patient = response.data?.Patient ?: return null

            // Patient Name
            val given = patient.name?.firstOrNull()?.given?.firstOrNull() ?: ""
            val family = patient.name?.firstOrNull()?.family ?: ""
            val name = "$given $family".trim()

            // Birth date & gender
            val birthDate = patient.birthDate ?: ""
            val gender = patient.gender ?: ""

            // Identifier (SNS number)
            val snsId = patient.identifier?.firstOrNull {
                it?.system?.contains("sns") == true ||
                        it?.system?.contains("utente") == true ||
                        it?.system?.contains("hl7.org/fhir/sid/us-ssn") == true
            }?.value
                ?: patient.identifier?.firstOrNull()?.value
                ?: "N/A"

            // Address as formatted string
            val addressObj = patient.address?.firstOrNull()
            val address = listOfNotNull(
                addressObj?.line?.firstOrNull(),
                addressObj?.city,
                addressObj?.state,
                addressObj?.postalCode
            ).joinToString(", ")

            // General Practitioner (Doctor)
            val doctor = patient.generalPractitioner?.firstOrNull()?.display ?: ""

            // Managing Organization (Health Unit)
            val orgRef = patient.managingOrganization?.reference
            val healthUnit = if (orgRef != null) {
                val orgId = orgRef.split("/").last()
                val orgResponse = apolloClient.query(GetOrganizationQuery(orgId)).execute()
                orgResponse.data?.Organization?.name ?: ""
            } else {
                ""
            }

            return PatientEntity(
                id = patient.id ?: "unknown",
                name = name,
                birthDate = birthDate,
                gender = gender,
                identifier = snsId,
                address = address,
                healthUnit = healthUnit,
                doctor = doctor
            )
        } catch (e: Exception) {
            Log.e("MedPlum", "Error fetching full patient info", e)
            null
        }
    }



    override suspend fun fetchPractitioner(practitionerId: String): PractitionerEntity? {
        return try {
            if (!ensureApolloClientInitialized()) return null

            val response = apolloClient.query(GetPractitionerQuery(practitionerId)).execute()
            Log.d("MedPlum", "GraphQL response: $response")

            if (response.hasErrors()) {
                Log.e("MedPlum", "GraphQL errors: ${response.errors}")
                return null
            }

            val practitioner = response.data?.Practitioner ?: return null

            val given = practitioner.name?.firstOrNull()?.given?.firstOrNull() ?: ""
            val family = practitioner.name?.firstOrNull()?.family ?: ""
            val name = "$given $family".trim()

            return PractitionerEntity(
                id = practitioner.id ?: "unknown",
                name = name,
                //gender = practitioner.gender ?: "unknown",
                //identifier = "", // not returned in this query yet
                telecom = "", // not returned in this query yet
                address = ""     // not returned in this query yet
            )

        } catch (e: ApolloHttpException) {
            Log.e("MedPlum", "HTTP error ${e.statusCode}: ${e.message}", e)
            val errorBody = e.body?.use { it.readUtf8() }
            Log.e("MedPlum", "Error body: $errorBody")
            null
        } catch (e: Exception) {
            Log.e("MedPlum", "Unexpected error", e)
            null
        }
    }


    override suspend fun getMedplumAccessToken(
        clientId: String,
        clientSecret: String
    ): String? = withContext(Dispatchers.IO) {
        val client = OkHttpClient()

        val form = "grant_type=client_credentials" +
                "&client_id=${clientId}" +
                "&client_secret=${clientSecret}"

        val request = Request.Builder()
            .url("https://api.medplum.com/oauth2/token")
            .post(form.toRequestBody("application/x-www-form-urlencoded".toMediaType()))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val responseBodyStr = response.body?.string()

                if (!response.isSuccessful || responseBodyStr == null) {
                    Log.e("MedplumAuth", "Token request failed: ${response.code}")
                    Log.e("MedplumAuth", "Body: $responseBodyStr")
                    return@withContext null
                }

                val json = JSONObject(responseBodyStr)
                val accessToken = json.getString("access_token")
                Log.d("MedplumAuth", "Access token: $accessToken")

                apolloClient = ApolloClient.Builder()
                    .serverUrl("https://api.medplum.com/fhir/R4/\$graphql")
                    .addHttpHeader("Authorization", "Bearer $accessToken")
                    .build()
                Log.d("MedplumAuth", "Apollo Client: $apolloClient")

                return@withContext accessToken
            }
        } catch (e: Exception) {
            Log.e("MedplumAuth", "Exception: ${e.message}", e)
            return@withContext null
        }
    }
    override suspend fun fetchConditions(subjectId: String): List<ConditionEntity>? {
        return try {
            if (!ensureApolloClientInitialized()) return null

            val response = apolloClient.query(GetConditionsForPatientQuery(subjectId)).execute()
            Log.d("MedPlum", "GraphQL response: $response")
            val data = response.data?.ConditionList ?: return null

            data.map {
                ConditionEntity(
                    id = it?.id ?: "",
                    code = it?.code?.text ?: "",
                    subjectId = subjectId,
                    onsetDateTime = it?.onsetDateTime ?: "",
                    clinicalStatus = it?.clinicalStatus?.text ?: "",
                    recorderId = "" //TODO: Add or remove this field
                )
            }
        } catch (e: Exception) {
            Log.e("MedPlum", "Error fetching conditions", e)
            null
        }
    }

    override suspend fun fetchObservations(subjectId: String): List<ObservationEntity>? {
        return try {
            if (!ensureApolloClientInitialized()) return null

            val response = apolloClient.query(GetObservationsQuery(subjectId)).execute()
            Log.d("MedPlum", "GraphQL response: $response")
            val data = response.data?.ObservationList ?: return null

            data.map {
                ObservationEntity(
                    id = it?.id ?: "",
                    status = it?.status ?: "",
                    code = it?.code?.text ?: "",
                    subjectId = subjectId,
                    effectiveDateTime = it?.effectiveDateTime ?: "",
                    valueQuantity = it?.valueQuantity?.value?.toString() ?: "",
                    unit = it?.valueQuantity?.unit ?: "",
                )
            }
        } catch (e: Exception) {
            Log.e("MedPlum", "Error fetching observations", e)
            null
        }
    }

    override suspend fun fetchDiagnosticReports(subjectId: String): List<DiagnosticReportEntity>? {
        return try {
            if (!ensureApolloClientInitialized()) return null

            val response = apolloClient.query(GetPatientDiagnosticReportQuery(subjectId)).execute()
            Log.d("MedPlum", "GraphQL response: $response")
            if (response.hasErrors()) {
                Log.e("MedPlum", "GraphQL errors: ${response.errors}")
            }
            Log.d("MedPlum", "Raw diagnostic report list: ${response.data}")
            val data = response.data?.DiagnosticReportList ?: return null
            val observationDao = AppDatabase.getDatabase(application).transactionDao()

            data.map { report ->
                val reportId = report?.id ?: ""
                val code = report?.code?.text ?: ""
                val status = report?.status ?: ""
                val effectiveDateTime = report?.effectiveDateTime ?: ""

                // Fetch Observations for each result reference
                val observations = report?.result?.mapNotNull { ref ->
                    val obsId = ref.reference?.split("/")?.lastOrNull()
                    obsId?.let { fetchObservationByID(it) }
                } ?: emptyList()

                val formattedResults = observations.joinToString(separator = "\n") { obs ->
                    "${obs.code}: ${obs.valueQuantity} ${obs.unit}"
                }

                var diagnostic = DiagnosticReportEntity(
                    id = reportId,
                    code = code,
                    status = status,
                    effectiveDateTime = effectiveDateTime,
                    result = formattedResults
                )
                observations.forEach { obs ->
                    // Insert or update observation in your database
                    observationDao.insertObservation(obs)
                }
                Log.d("MedPlum", "Diagnostic Report fetched: $diagnostic")
                diagnostic
            }
        } catch (e: Exception) {
            Log.e("MedPlum", "Error fetching diagnostic reports", e)
            null
        }
    }

    override suspend fun fetchMedicationRequests(subjectId: String): List<MedicationRequestEntity>? {
        return try {
            if (!ensureApolloClientInitialized()) return null

            val response = apolloClient.query(GetPatientMedicationRequestsQuery(subjectId)).execute()
            Log.d("MedPlum", "GraphQL response: $response")
            val data = response.data?.MedicationRequestList ?: return null

            data.map {
                MedicationRequestEntity(
                    id = it?.id ?: "",
                    medication = it?.medicationCodeableConcept?.text ?: "",
                    authoredOn = it?.authoredOn ?: "",
                    status = it?.status ?: "",
                    dosage = it?.dosageInstruction?.joinToString { d -> d.text ?: "" } ?: "",

                )
            }
        } catch (e: Exception) {
            Log.e("MedPlum", "Error fetching medication requests", e)
            null
        }
    }

    override suspend fun fetchMedicationStatements(subjectId: String): List<MedicationStatementEntity>? {
        return try {
            if (!ensureApolloClientInitialized()) return null

            val response = apolloClient.query(GetPatientMedicationStatementsQuery(subjectId)).execute()
            Log.d("MedPlum", "GraphQL response: $response")
            val data = response.data?.MedicationStatementList ?: return null

            data.map {
                MedicationStatementEntity(
                    id = it?.id ?: "",
                    medication = it?.medicationCodeableConcept?.text ?: "",
                    status = it?.status ?: "",
                    start = it?.effectivePeriod?.start ?: "",
                    end = it?.effectivePeriod?.end ?: ""
                )
            }
        } catch (e: Exception) {
            Log.e("MedPlum", "Error fetching medication statements", e)
            null
        }
    }

    override suspend fun fetchImmunizations(subjectId: String): List<ImmunizationEntity>? {
        return try {
            if (!ensureApolloClientInitialized()) return null

            val response = apolloClient.query(GetPatientImmunizationsQuery(subjectId)).execute()
            Log.d("MedPlum", "GraphQL response: $response")
            val data = response.data?.ImmunizationList ?: return null

            data.map {
                ImmunizationEntity(
                    id = it?.id ?: "",
                    vaccine = it?.vaccineCode?.text ?: "",
                    occurrenceDateTime = it?.occurrenceDateTime ?: "",
                    status = it?.status ?: "",
                    lotNumber = it?.lotNumber ?: ""
                )
            }
        } catch (e: Exception) {
            Log.e("MedPlum", "Error fetching immunizations", e)
            null
        }
    }

    override suspend fun fetchAllergies(subjectId: String): List<AllergyIntoleranceEntity>? {
        return try {
            if (!ensureApolloClientInitialized()) return null

            val response = apolloClient.query(GetPatientAllergiesQuery(subjectId)).execute()
            Log.d("MedPlum", "GraphQL response: $response")
            val data = response.data?.AllergyIntoleranceList ?: return null

            data.map {
                AllergyIntoleranceEntity(
                    id = it?.id ?: "",
                    code = it?.code?.text ?: "",
                    status = it?.clinicalStatus?.text ?: "",
                    onset = it?.onsetDateTime ?: "",
                    recordedDate = it?.recordedDate ?: ""
                )
            }
        } catch (e: Exception) {
            Log.e("MedPlum", "Error fetching allergies", e)
            null
        }
    }

    override suspend fun fetchDevices(subjectId: String): List<DeviceEntity>? {
        return try {
            if (!ensureApolloClientInitialized()) return null

            val response = apolloClient.query(GetPatientDevicesQuery(subjectId)).execute()
            Log.d("MedPlum", "GraphQL response: $response")
            val data = response.data?.DeviceList ?: return null

            data.map {
                DeviceEntity(
                    id = it?.id ?: "",
                    type = it?.type?.text ?: "",
                    status = it?.status ?: "",
                    manufactureDate = it?.manufactureDate ?: ""
                )
            }
        } catch (e: Exception) {
            Log.e("MedPlum", "Error fetching devices", e)
            null
        }
    }

    override suspend fun fetchProcedures(subjectId: String): List<ProcedureEntity>? {
        return try {
            if (!ensureApolloClientInitialized()) return null

            val response = apolloClient.query(GetPatientProceduresQuery(subjectId)).execute()
            Log.d("MedPlum", "GraphQL response: $response")
            val data = response.data?.ProcedureList ?: return null

            data.map {
                ProcedureEntity(
                    id = it?.id ?: "",
                    code = it?.code?.text ?: "",
                    status = it?.status ?: "",
                    performedDateTime = it?.performedDateTime ?: ""
                )
            }
        } catch (e: Exception) {
            Log.e("MedPlum", "Error fetching procedures", e)
            null
        }
    }

    override suspend fun fetchObservationByID(observationId: String): ObservationEntity? {
        return try {
            //if (!ensureApolloClientInitialized()) return null

            val response = apolloClient.query(GetObservationByIdQuery(observationId)).execute()
            Log.d("MedPlum", "GraphQL response: $response")
            val data = response.data?.Observation ?: return null

            ObservationEntity(
                id = data.id ?: "",
                status = data.status ?: "",
                code = data.code?.text ?: "",
                subjectId = data.subject?.reference?.split("/")?.getOrNull(1) ?: "",
                effectiveDateTime = data.effectiveDateTime ?: "",
                valueQuantity = data.valueQuantity?.value?.toString() ?: "",
                unit = data.valueQuantity?.unit ?: ""
            )
        } catch (e: Exception) {
            Log.e("MedPlum", "Error fetching observation by ID", e)
            null
        }
    }
}