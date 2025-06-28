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
import com.example.medplum.GetPatientListForPractitionerQuery
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
import com.example.ethktprototype.data.HealthSummaryResult
import com.example.medplum.GetPractitionerCompleteQuery
import org.json.JSONArray
import java.security.MessageDigest
import java.security.SecureRandom

class MedPlumAPI(private val application: Application, private val viewModel: WalletViewModel) : IMedPlumAPI {
    val sharedPreferences = application.getSharedPreferences("WalletPrefs", Context.MODE_PRIVATE)

    private val CLIENT_ID = "01968b74-d07a-766d-8331-1cefab3a8922"
    private val CLIENT_SECRET = "a6bcb43c6607ad32e3ae324ff6689b6716d6abcaad6a330e6e5b330616cb71ac"
    private val redirectUri: String = "myapp://oauth2redirect"
    private val TAG = "MedplumAuth"
    private val authUrl = "https://api.medplum.com/oauth2/authorize"
    private val tokenUrl = "https://api.medplum.com/oauth2/token"
    private var codeVerifier: String? = null

    fun logout(context: Context) {
        sharedPreferences.edit()
            .remove("access_token")
            .remove("refresh_token")
            .remove("profile_id")
            .remove("token_expiration")
            .remove("user_profile")
            .remove("code_verifier")
            .apply()
        viewModel.deletePatientIdUiState()
        viewModel.deleteAllDataFromDb()
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
            .appendQueryParameter("scope", "openid profile user/*.* offline_access")
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
                Log.d(TAG, "refreshToken: $refreshToken")

                if (refreshToken != null) {
                    sharedPreferences.edit().putString("refresh_token", refreshToken).apply()
                    Log.d(TAG, "sharedPreferences refresh token: ${sharedPreferences.getString("refresh_token", null)}")
                } else {
                    Log.w(TAG, "No refresh token returned")
                }

                val profileObj = json.optJSONObject("profile")
                val profileRef = profileObj?.optString("reference", null)
                if (profileRef != null) {
                    sharedPreferences.edit().putString("user_profile", profileRef).apply()
                }
                val expiresIn = json.optInt("expires_in", 3600)
                val expirationTime = System.currentTimeMillis() + (expiresIn * 1000)
                sharedPreferences.edit().putLong("token_expiration", expirationTime).apply()

                sharedPreferences.edit()
                    .putString("access_token", accessToken)
                    .apply()

                Log.d(TAG, "Access token: $accessToken")
                setupApolloClient(accessToken)
                //viewModel.updatePatientIdUiState()
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

    private fun isTokenExpired(): Boolean {
        val expirationTime = sharedPreferences.getLong("token_expiration", 0)
        Log.d(TAG, "Token expiration: $expirationTime")
        return System.currentTimeMillis() > expirationTime
    }


    suspend fun refreshAccessTokenIfNeeded(): String? = withContext(Dispatchers.IO) {

        val refreshToken = sharedPreferences.getString("refresh_token", null)
        if (refreshToken.isNullOrBlank()) {
            Log.e(TAG, "No refresh token available")
            return@withContext null
        }

        Log.d(TAG, "Refreshing access token with refresh token: $refreshToken")

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

                Log.d(TAG, "Refresh token response: $json")
                if (json.has("error")) {
                    Log.e(TAG, "Refresh token error: ${json.optString("error_description", "Unknown error")}")
                    logout(application.applicationContext)

                    //viewModel.redirectToLogin()
                    return@use null
                }

                val newAccessToken = json.getString("access_token")
                sharedPreferences.edit().putString("access_token", newAccessToken).apply()

                val newRefreshToken = json.optString("refresh_token", null)
                if (!newRefreshToken.isNullOrBlank()) {
                    sharedPreferences.edit().putString("refresh_token", newRefreshToken).apply()
                }

                val expiresIn = json.optInt("expires_in", 3600) // default 1 hour
                val expirationTime = System.currentTimeMillis() + (expiresIn * 1000)
                sharedPreferences.edit().putLong("token_expiration", expirationTime).apply()

                setupApolloClient(newAccessToken)
                newAccessToken
            }
        } catch (e: Exception) {
            Log.e(TAG, "Refresh token failed", e)
            null
        }
    }

    private suspend fun ensureApolloClientInitialized(): Boolean {
        if (!::apolloClient.isInitialized || isTokenExpired()) {
            var token: String = ""
            if (isTokenExpired()) {
                token = refreshAccessTokenIfNeeded() ?: return false
            }
            else {
                token = getAccessToken() ?: return false
            }
            Log.d(TAG, "Access token: $token")
            if (token == "") {
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

            Log.d("MedPlum", "Patient ID: $patientId")

            val response = apolloClient.query(GetPatientCompleteQuery(patientId)).execute()
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
        } catch (e: ApolloHttpException) {
            val errorBody = e.body?.use { it.readUtf8() }
            Log.e("MedPlum", "HTTP error ${e.statusCode}: ${e.message}")
            Log.e("MedPlum", "Error body: $errorBody")
            null
        } catch (e: Exception) {
            Log.e("MedPlum", "Unexpected error", e)
            null
        }
    }



    override suspend fun fetchPractitioner(practitionerId: String): PractitionerEntity? {
        return try {
            if (!ensureApolloClientInitialized()) return null

            val response = apolloClient.query(GetPractitionerCompleteQuery(practitionerId)).execute()
            Log.d("MedPlum", "GraphQL response: $response")

            if (response.hasErrors()) {
                Log.e("MedPlum", "GraphQL errors: ${response.errors}")
                return null
            }

            val practitioner = response.data?.Practitioner ?: return null

            // Name
            val given = practitioner.name?.firstOrNull()?.given?.firstOrNull() ?: ""
            val family = practitioner.name?.firstOrNull()?.family ?: ""
            val name = "$given $family".trim()

            // Identifier (take the first one if present)
            val identifier = practitioner.identifier?.firstOrNull()?.value ?: ""

            // Gender
            val gender = practitioner.gender ?: "unknown"

            // Telecom (take the first available contact method)
            val telecom = practitioner.telecom?.firstOrNull()?.value ?: ""

            val qualification = practitioner.qualification?.firstOrNull()?.let { qual ->
                val codeDisplay = qual.code.coding?.firstOrNull()?.display ?: "Unknown"
                val start = qual.period?.start ?: ""
                val end = qual.period?.end ?: ""

                if (start.isNotEmpty() && end.isNotEmpty()) {
                    "$codeDisplay ($start - $end)"
                } else {
                    codeDisplay
                }
            } ?: "N/A"

            // Address (format first address as string)
            val addressObj = practitioner.address?.firstOrNull()
            val address = listOfNotNull(
                addressObj?.line?.firstOrNull(),
                addressObj?.city,
                addressObj?.state,
                addressObj?.postalCode,
                addressObj?.country
            ).joinToString(", ")

            return PractitionerEntity(
                id = practitioner.id ?: "unknown",
                name = name,
                gender = gender,
                identifier = identifier,
                telecom = telecom,
                address = address,
                qualification = qualification
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

    suspend fun fetchPatientListOfPractitioner(practitionerId: String): List<PatientEntity>? {
        return try {
            if (!ensureApolloClientInitialized()) return null

            val response = apolloClient.query(GetPatientListForPractitionerQuery(practitionerId)).execute()
            Log.d("MedPlum", "GraphQL response: $response")

            if (response.hasErrors()) {
                Log.e("MedPlum", "GraphQL errors: ${response.errors}")
                return null
            }

            val patients = response.data?.PatientList ?: return null

            patients.mapNotNull { patient ->
                val id = patient?.id ?: return@mapNotNull null

                val given = patient.name?.firstOrNull()?.given?.firstOrNull() ?: ""
                val family = patient.name?.firstOrNull()?.family ?: ""
                val name = "$given $family".trim()

                val gender = patient.gender ?: "unknown"
                val birthDate = patient.birthDate ?: "unknown"

                // Use empty strings for fields not included in the lightweight query
                PatientEntity(
                    id = id,
                    name = name,
                    birthDate = birthDate,
                    gender = gender,
                    identifier = "",
                    address = "",
                    healthUnit = "",
                    doctor = ""
                )
            }

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

    suspend fun fetchHealthSummary(patientId: String): HealthSummaryResult {
        val diagnostics = fetchDiagnosticReports(patientId)
        val allergies = fetchAllergies(patientId)
        val meds = fetchMedicationStatements(patientId)
        val procedures = fetchProcedures(patientId)
        val devices = fetchDevices(patientId)
        val immunizations = fetchImmunizations(patientId)

        return HealthSummaryResult(diagnostics, allergies, meds, procedures, devices, immunizations)
    }

    suspend fun postRecordToMedplum(jsonBody: String, resourceType: String): String? = withContext(Dispatchers.IO) {
        val accessToken = getAccessToken()
        if (accessToken.isNullOrEmpty()) {
            Log.e("MedPlum", "Access token is missing.")
            return@withContext null
        }

        val mediaType = "application/fhir+json".toMediaType()
        val requestBody = jsonBody.toRequestBody(mediaType)

        val request = Request.Builder()
            .url("https://api.medplum.com/fhir/R4/$resourceType")
            .addHeader("Authorization", "Bearer $accessToken")
            .addHeader("Content-Type", "application/fhir+json")
            .post(requestBody)
            .build()

        try {
            val client = OkHttpClient()
            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string()
                if (response.isSuccessful && responseBody != null) {
                    val json = JSONObject(responseBody)
                    val id = json.optString("id", null)
                    Log.d("MedPlum", "Successfully created $resourceType with ID: $id")
                    return@withContext id
                } else {
                    Log.e("MedPlum", "Failed to create $resourceType: ${response.code}")
                    Log.e("MedPlum", "Response body: $responseBody")
                    return@withContext null
                }
            }
        } catch (e: Exception) {
            Log.e("MedPlum", "Error posting $resourceType to MedPlum", e)
            return@withContext null
        }
    }


     suspend fun createMedplumRecord(recordType: String, record: Any, patientId: String, performerId: String): String {
         val jsonBody = when (recordType) {
            "DiagnosticReport" -> createDiagnosticReport(record as DiagnosticReportEntity, patientId, performerId)
            "Immunization" -> createImmunization(record as ImmunizationEntity)
            "Allergy" -> createAllergy(record as AllergyIntoleranceEntity)
            "Condition" -> createCondition(record as ConditionEntity)
            "Observation" -> createObservation(record as ObservationEntity)
            "MedicationRequest" -> createMedicationRequest(record as MedicationRequestEntity)
            "Procedure" -> createProcedure(record as ProcedureEntity, patientId)
            else -> throw IllegalArgumentException("Unsupported record type: $recordType")
        }
         return postRecordToMedplum(jsonBody, recordType) ?: throw Exception("Failed to create $recordType on MedPlum")
     }

    fun createDiagnosticReport(record: DiagnosticReportEntity, patientId: String, performerId: String): String {
        Log.d("EffectiveDateTime", "Effective DateTime: ${record.effectiveDateTime}")
        val json = JSONObject().apply {
            put("resourceType", "DiagnosticReport")
            put("status", "final")
            put("code", JSONObject().apply {
                put("text", record.code)
            })
            put("subject", JSONObject().apply {
                put("reference", "Patient/${patientId}")
            })
            put("performer", JSONArray().apply {
                put(JSONObject().apply {
                    put("reference", performerId)
                })
            })
            put("effectiveDateTime", record.effectiveDateTime)
            put("result", JSONArray().apply {
                // Add result references if needed
            })
        }

        return json.toString()
    }

    fun createImmunization(record: ImmunizationEntity): String {
        val json = JSONObject().apply {
            put("resourceType", "Immunization")
            put("status", record.status)
            put("vaccineCode", JSONObject().apply {
                put("text", record.vaccine)
            })
            put("occurrenceDateTime", record.occurrenceDateTime)
            put("lotNumber", record.lotNumber)
        }

        return json.toString()
    }

    fun createAllergy(record: AllergyIntoleranceEntity): String {
        val json = JSONObject().apply {
            put("resourceType", "AllergyIntolerance")
            put("clinicalStatus", JSONObject().apply {
                put("text", record.status)
            })
            put("code", JSONObject().apply {
                put("text", record.code)
            })
            put("onsetDateTime", record.onset)
            put("recordedDate", record.recordedDate)
        }

        return json.toString()
    }

    fun createCondition(record: ConditionEntity): String {
        val json = JSONObject().apply {
            put("resourceType", "Condition")
            put("clinicalStatus", JSONObject().apply {
                put("text", record.clinicalStatus)
            })
            put("code", JSONObject().apply {
                put("text", record.code)
            })
            put("onsetDateTime", record.onsetDateTime)
            put("subject", JSONObject().apply {
                put("reference", "Patient/${record.subjectId}")
            })
        }

        return json.toString()
    }

    fun createObservation(record: ObservationEntity): String {
        val json = JSONObject().apply {
            put("resourceType", "Observation")
            put("status", record.status)
            put("code", JSONObject().apply {
                put("text", record.code)
            })
            put("subject", JSONObject().apply {
                put("reference", "Patient/${record.subjectId}")
            })
            put("effectiveDateTime", record.effectiveDateTime)
            put("valueQuantity", JSONObject().apply {
                put("value", record.valueQuantity)
                put("unit", record.unit)
            })
        }

        return json.toString()
    }

    fun createMedicationRequest(record: MedicationRequestEntity): String {
        val json = JSONObject().apply {
            put("resourceType", "MedicationRequest")
            put("status", record.status)
            put("medicationCodeableConcept", JSONObject().apply {
                put("text", record.medication)
            })
            put("authoredOn", record.authoredOn)
            put("dosageInstruction", JSONArray().apply {
                put(JSONObject().apply {
                    put("text", record.dosage)
                })
            })
        }

        return json.toString()
    }

    fun createProcedure(record: ProcedureEntity, patientId: String): String {
        val json = JSONObject().apply {
            put("resourceType", "Procedure")
            put("status", record.status)
            put("code", JSONObject().apply {
                put("text", record.code)
            })
            put("performedDateTime", record.performedDateTime)
            put("subject", JSONObject().apply {
                put("reference", "Patient/${patientId}")
            })
        }

        return json.toString()
    }


    suspend fun createConsentResource(
        patientId: String,
        practitionerId: String,
        resourceId: String
    ): Boolean = withContext(Dispatchers.IO) {
        val accessToken = getAccessToken()
        if (accessToken == null) {
            Log.e("MedPlum", "No access token available.")
            return@withContext false
        }

        val json = JSONObject().apply {
            put("resourceType", "Consent")
            put("status", "active")
            put("scope", JSONObject().apply {
                put("coding", listOf(JSONObject().apply {
                    put("system", "http://terminology.hl7.org/CodeSystem/consentscope")
                    put("code", "patient-privacy")
                }))
            })
            put("patient", JSONObject().apply {
                put("reference", "Patient/$patientId")
            })
            put("performer", listOf(JSONObject().apply {
                put("reference", "Practitioner/$practitionerId")
            }))
            put("provision", JSONObject().apply {
                put("type", "permit")
                put("actor", listOf(JSONObject().apply {
                    put("role", JSONObject().apply {
                        put("coding", listOf(JSONObject().apply {
                            put("system", "http://terminology.hl7.org/CodeSystem/consentactorrole")
                            put("code", "PRCP")
                        }))
                    })
                    put("reference", JSONObject().apply {
                        put("reference", "Practitioner/$practitionerId")
                    })
                }))
                put("resource", listOf(JSONObject().apply {
                    put("reference", resourceId)  // e.g. "DiagnosticReport/abc123"
                }))
            })
        }

        val body = json.toString().toRequestBody("application/fhir+json".toMediaType())
        val request = Request.Builder()
            .url("https://api.medplum.com/fhir/R4/Consent")
            .addHeader("Authorization", "Bearer $accessToken")
            .post(body)
            .build()

        try {
            val client = OkHttpClient()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Log.d("MedPlum", "Consent created successfully")
                    return@withContext true
                } else {
                    Log.e("MedPlum", "Failed to create Consent: ${response.code}")
                    Log.e("MedPlum", "Response body: ${response.body?.string()}")
                    return@withContext false
                }
            }
        } catch (e: Exception) {
            Log.e("MedPlum", "Error creating Consent", e)
            return@withContext false
        }
    }

    suspend fun createAccessPolicy(
        name: String,
        resourceType: String,
        resourceId: String,
        practitionerId: String
    ): Boolean = withContext(Dispatchers.IO) {
        val accessToken = getAccessToken()
        if (accessToken == null) {
            Log.e("MedPlum", "No access token available.")
            return@withContext false
        }

        val json = JSONObject().apply {
            put("resourceType", "AccessPolicy")
            put("name", name)  // e.g., "AllowDrSmithAccessToXray"
            put("resource", "$resourceType/$resourceId")  // e.g., "DiagnosticReport/xyz123"
            put("action", listOf("read"))  // or "write", "read,write", etc.
            put("condition", JSONObject().apply {
                put("expression", "requester.id == 'Practitioner/$practitionerId'")
            })
        }

        val body = json.toString().toRequestBody("application/fhir+json".toMediaType())
        val request = Request.Builder()
            .url("https://api.medplum.com/fhir/R4/AccessPolicy")
            .addHeader("Authorization", "Bearer $accessToken")
            .post(body)
            .build()

        try {
            val client = OkHttpClient()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Log.d("MedPlum", "AccessPolicy created successfully")
                    return@withContext true
                } else {
                    Log.e("MedPlum", "Failed to create AccessPolicy: ${response.code}")
                    Log.e("MedPlum", "Response body: ${response.body?.string()}")
                    return@withContext false
                }
            }
        } catch (e: Exception) {
            Log.e("MedPlum", "Error creating AccessPolicy", e)
            return@withContext false
        }
    }

    // Adds an AccessPolicy allowing a doctor to fully access a specific DiagnosticReport
    suspend fun grantFullDiagnosticReportAccess(
        resourceId: String,
        practitionerId: String,
        projectId: String
    ): Boolean = withContext(Dispatchers.IO) {
        val accessToken = getAccessToken() ?: return@withContext false

        val accessPolicy = JSONObject().apply {
            put("resourceType", "AccessPolicy")
            put("name", "FullAccessToReport_$resourceId$practitionerId")
            put("resource", JSONArray().apply {
                put(JSONObject().apply {
                    put("resourceType", "DiagnosticReport")
                    put("criteria", "DiagnosticReport?_id=$resourceId")
                })
            })
            put("meta", JSONObject().apply {
                put("project", projectId)
                put("compartment", JSONArray().apply {
                    put(JSONObject().apply {
                        put("reference", "Project/$projectId")
                    })
                })
            })
        }

        val body = accessPolicy.toString().toRequestBody("application/fhir+json".toMediaType())
        val request = Request.Builder()
            .url("https://api.medplum.com/fhir/R4/AccessPolicy")
            .addHeader("Authorization", "Bearer $accessToken")
            .post(body)
            .build()

        try {
            OkHttpClient().newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Log.d("MedPlum", "AccessPolicy created successfully")
                    return@withContext true
                } else {
                    Log.e("MedPlum", "Failed to create AccessPolicy: ${response.code}")
                    Log.e("MedPlum", "Response body: ${response.body?.string()}")
                    return@withContext false
                }
            }
        } catch (e: Exception) {
            Log.e("MedPlum", "Error creating AccessPolicy", e)
            return@withContext false
        }
    }

    private suspend fun linkPolicyToProjectMembership(
        practitionerId: String,
        projectId: String,
        policyId: String
    ): Boolean = withContext(Dispatchers.IO) {
        val accessToken = getAccessToken() ?: return@withContext false

        val membershipId = getMembershipIdForPractitioner(practitionerId, projectId) ?: return@withContext false

        val patchBody = JSONArray().apply {
            put(JSONObject().apply {
                put("op", "replace")
                put("path", "/accessPolicy")
                put("value", JSONObject().apply {
                    put("reference", "AccessPolicy/$policyId")
                })
            })
        }

        val body = patchBody.toString().toRequestBody("application/json-patch+json".toMediaType())
        val request = Request.Builder()
            .url("https://api.medplum.com/fhir/R4/ProjectMembership/$membershipId")
            .addHeader("Authorization", "Bearer $accessToken")
            .patch(body)
            .build()

        try {
            OkHttpClient().newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Log.d("MedPlum", "ProjectMembership patched successfully")
                    return@withContext true
                } else {
                    Log.e("MedPlum", "Failed to patch ProjectMembership: ${response.code}")
                    return@withContext false
                }
            }
        } catch (e: Exception) {
            Log.e("MedPlum", "Error patching ProjectMembership", e)
            return@withContext false
        }
    }




    // Optional helper to get ProjectMembership ID for a Practitioner
    private suspend fun getMembershipIdForPractitioner(
        practitionerId: String,
        projectId: String
    ): String? = withContext(Dispatchers.IO) {
        val accessToken = getAccessToken() ?: return@withContext null

        val query = "ProjectMembership?profile=Practitioner/$practitionerId&_project=$projectId"
        val request = Request.Builder()
            .url("https://api.medplum.com/fhir/R4/$query")
            .addHeader("Authorization", "Bearer $accessToken")
            .get()
            .build()

        try {
            OkHttpClient().newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bundle = JSONObject(response.body?.string() ?: return@use null)
                    val entry = bundle.optJSONArray("entry")?.optJSONObject(0)?.optJSONObject("resource")
                    return@withContext entry?.optString("id")
                } else {
                    Log.e("MedPlum", "Failed to retrieve ProjectMembership: ${response.code}")
                    return@withContext null
                }
            }
        } catch (e: Exception) {
            Log.e("MedPlum", "Error retrieving ProjectMembership", e)
            return@withContext null
        }
    }
}