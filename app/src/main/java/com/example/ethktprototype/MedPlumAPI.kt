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
import com.example.medplum.GetPatientNameQuery
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
import com.example.ethktprototype.data.ConsentDisplayItem
import com.example.ethktprototype.data.ConsentEntity
import com.example.ethktprototype.data.HealthSummaryResult
import com.example.ethktprototype.data.SharedResourceInfo
import com.example.medplum.GetPractitionerCompleteQuery
import com.example.medplum.GetPractitionerNameQuery
import com.example.medplum.GetPractitionersListQuery
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
            val identifier = practitioner.identifier?.firstOrNull { it.system == "access-policy" }?.value ?: ""

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

    suspend fun fetchPractitionersList(name: String): List<PractitionerEntity>? {
        return try {
            if (!ensureApolloClientInitialized()) return null

            val response = apolloClient.query(GetPractitionersListQuery(name)).execute()
            Log.d("MedPlum", "GraphQL response: $response")

            if (response.hasErrors()) {
                Log.e("MedPlum", "GraphQL errors: ${response.errors}")
                return null
            }

            val practitioners = response.data?.PractitionerList ?: return null

            return practitioners.map { practitioner ->
                // Name
                val given = practitioner?.name?.firstOrNull()?.given?.firstOrNull() ?: ""
                val family = practitioner?.name?.firstOrNull()?.family ?: ""
                val fullName = "$given $family".trim()

                // Gender
                val gender = "unknown"

                // Identifier
                val identifier = practitioner?.identifier?.firstOrNull { it.system == "access-policy" }?.value ?: ""

                // Telecom (take the first available contact method)
                val telecom = ""

                // Address (optional future enhancement)
                val address = "" // Not included in this query

                Log.d("MedPlum", "PractitionerId on fetch: ${practitioner?.id}")
                PractitionerEntity(
                    id = practitioner?.id ?: "unknown",
                    name = fullName,
                    gender = gender,
                    identifier = identifier,
                    telecom = telecom,
                    address = address,
                    qualification = ""
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

    suspend fun fetchPatientName(patientId: String): String {
        return try {
            if (!ensureApolloClientInitialized()) return ""

            Log.d("MedPlum", "Fetching patient name for $patientId")
            val response = apolloClient.query(GetPatientNameQuery(patientId)).execute()
            Log.d("MedPlum", "GraphQL response: $response")

            if (response.hasErrors()) {
                Log.e("MedPlum", "GraphQL errors: ${response.errors}")
                return ""
            }

            response.data?.Patient?.name?.firstOrNull()?.let { name ->
                val given = name.given?.firstOrNull() ?: ""
                val family = name.family ?: ""
                "$given $family".trim()
            } ?: ""
        } catch (e: ApolloHttpException) {
            Log.e("MedPlum", "HTTP error ${e.statusCode}: ${e.message}", e)
            val errorBody = e.body?.use { it.readUtf8() }
            Log.e("MedPlum", "Error body: $errorBody")
            ""
        } catch (e: Exception) {
            Log.e("MedPlum", "Unexpected error", e)
            ""
        }
    }

    suspend fun fetchPatientsNames(patientIds: List<String>): Map<String,String> {
        return patientIds.associateWith { patientId ->
            fetchPatientName(patientId)
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

    override suspend fun fetchDiagnosticReports(subjectId: String, isPractitioner: Boolean): List<DiagnosticReportEntity>? {
        return try {
            if (!ensureApolloClientInitialized()) return null

            val response = apolloClient.query(GetPatientDiagnosticReportQuery(subjectId)).execute()
            Log.d("MedPlum", "GraphQL response: $response")
            if (response.hasErrors()) {
                Log.e("MedPlum", "GraphQL errors: ${response.errors}")
            }
            Log.d("MedPlum", "Raw diagnostic report list: ${response.data}")
            val data = response.data?.DiagnosticReportList ?: return null
            val observationDao = AppDatabase.getDatabase(application, viewModel.getDbPassphrase()).transactionDao()

            val practitionerId = viewModel.getLoggedInUsertId()

            data.map { report ->
                val reportId = report?.id ?: ""
                val code = report?.code?.text ?: ""
                val status = report?.status ?: ""
                val effectiveDateTime = report?.effectiveDateTime ?: ""

                if (isPractitioner) {
                    val hasConsent = checkConsentExists(
                        practitionerId = practitionerId,
                        resourceId = "DiagnosticReport/$reportId"
                    )
                    Log.d("MedPlum", "Has consent for report $reportId: $hasConsent")
                    if (!hasConsent) {
                        Log.w("MedPlum", "No consent for report $reportId")
                        return@map DiagnosticReportEntity(
                            id = reportId,
                            code = "",
                            status = "NO_CONSENT",
                            effectiveDateTime = "",
                            result = "",
                            subjectId = subjectId
                        )
                    }
                }

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
                    result = formattedResults,
                    subjectId = subjectId
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
                    subjectId = subjectId
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
                    end = it?.effectivePeriod?.end ?: "",
                    subjectId = subjectId
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
                    lotNumber = it?.lotNumber ?: "",
                    subjectId = subjectId
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
                    recordedDate = it?.recordedDate ?: "",
                    subjectId = subjectId,
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
                    manufactureDate = it?.manufactureDate ?: "",
                    subjectId = subjectId,
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
                    performedDateTime = it?.performedDateTime ?: "",
                    subjectId = subjectId,
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
            if (response.hasErrors()) {
                Log.e("MedPlum", "GraphQL errors: ${response.errors}")
                return null
            }
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
            put("status", record.status)
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
                record.result.split(",").forEach { ref ->
                    if (ref.isNotBlank()) {
                        put(JSONObject().apply {
                            put("reference", ref.trim())
                        })
                    }
                }
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

    suspend fun getObservationIdsFromDiagnosticReport(
        diagnosticReportId: String
    ): List<String> = withContext(Dispatchers.IO) {
        val accessToken = getAccessToken()
        if (accessToken == null) {
            Log.e("MedPlum", "No access token available.")
            return@withContext emptyList()
        }
        val client = OkHttpClient()

        val reportUrl = "https://api.medplum.com/fhir/R4/$diagnosticReportId"
        val request = Request.Builder()
            .url(reportUrl)
            .addHeader("Authorization", "Bearer $accessToken")
            .get()
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val json = JSONObject(response.body?.string() ?: "")
                    val results = json.optJSONArray("result") ?: JSONArray()
                    Log.d("MedPlum", "Observation IDs: $results")
                    return@withContext List(results.length()) { i ->
                        results.getJSONObject(i).optString("reference", "")
                    }.filter { it.startsWith("Observation/") }
                } else {
                    Log.e("MedPlum", "Failed to fetch DiagnosticReport: ${response.code}")
                    return@withContext emptyList()
                }
            }
        } catch (e: Exception) {
            Log.e("MedPlum", "Error fetching DiagnosticReport", e)
            return@withContext emptyList()
        }
    }

    suspend fun createConsentResource(
        patientId: String,
        practitionerId: String,
        resourceId: String,
        accessPolicyId: String
    ): Boolean = withContext(Dispatchers.IO) {
        val accessToken = getAccessToken()
        if (accessToken == null) {
            Log.e("MedPlum", "No access token available.")
            return@withContext false
        }
        val client = OkHttpClient()

        val dataRefs = mutableListOf(resourceId)

        if (resourceId.startsWith("DiagnosticReport/")) {
            getObservationIdsFromDiagnosticReport(resourceId).forEach {
                if (it.isNotEmpty()) {
                    dataRefs.add(it)
                }
            }
            Log.d("MedPlum", "Data Refs: $dataRefs")
        }

        val json = JSONObject().apply {
            put("resourceType", "Consent")
            put("status", "active")
            put("policy", JSONArray().apply {
                put(JSONObject().apply {
                    put("authority", "https://api.medplum.com")///fhir/R4")
                    put("uri", "AccessPolicy/$accessPolicyId")
                })
            })
            put("scope", JSONObject().apply {
                put("coding", JSONArray().apply {
                    put(JSONObject().apply {
                        put("system", "http://terminology.hl7.org/CodeSystem/consentscope")
                        put("code", "patient-privacy")
                    })
                })
            })
            put("category", JSONArray().apply {
                put(JSONObject().apply {
                    put("coding", JSONArray().apply {
                        put(JSONObject().apply {
                            put("system", "http://terminology.hl7.org/CodeSystem/consentcategorycodes")
                            put("code", "INFA")
                        })
                    })
                })
            })
            put("patient", JSONObject().apply {
                put("reference", patientId)
            })
            put("performer", JSONArray().apply {
                put(JSONObject().apply {
                    put("reference", "Practitioner/$practitionerId")
                })
            })
            put("provision", JSONObject().apply {
                put("type", "permit")
                put("actor", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", JSONObject().apply {
                            put("coding", JSONArray().apply {
                                put(JSONObject().apply {
                                    put("system", "http://terminology.hl7.org/CodeSystem/consentactorrole")
                                    put("code", "PRCP")
                                })
                            })
                        })
                        put("reference", JSONObject().apply {
                            put("reference", "Practitioner/$practitionerId")
                        })
                    })
                })
                put("data", JSONArray().apply {
                    dataRefs.forEach { ref ->
                        put(JSONObject().apply {
                            put("meaning", "instance")
                            put("reference", JSONObject().apply {
                                put("reference", ref) // e.g., "DiagnosticReport/abc123" or "Observation/xyz456"
                            })
                        })
                    }
                })
            })
        }

        Log.d("ConsentPayload", json.toString(2))


        val body = json.toString().toRequestBody("application/fhir+json".toMediaType())
        val request = Request.Builder()
            .url("https://api.medplum.com/fhir/R4/Consent")
            .addHeader("Authorization", "Bearer $accessToken")
            .post(body)
            .build()

        try {
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

    suspend fun getAccessPolicyIdFromPractitioner(practitionerId: String): String? {
        val practitioner = viewModel.getPractitionerById(practitionerId)
        return practitioner?.identifier
    }


    /*suspend fun grantFullDiagnosticReportAccess(
        resourceId: String,
        practitionerId: String,
        projectId: String,
        patientId: String
    ): Boolean = withContext(Dispatchers.IO) {
        val accessToken = getAccessToken() ?: return@withContext false
        val client = OkHttpClient()

        // Step 1: Get existing AccessPolicy ID from ProjectMembership
        val policyId = getAccessPolicyIdFromPractitioner(practitionerId)
            ?: return@withContext false

        Log.d("MedPlum", "Found AccessPolicy ID: $policyId")
        // Step 2: Fetch the DiagnosticReport
        val reportUrl = "https://api.medplum.com/fhir/R4/DiagnosticReport/$resourceId"

        // Step 3: Fetch the current AccessPolicy
        val getPolicyRequest = Request.Builder()
            .url("https://api.medplum.com/fhir/R4/AccessPolicy/$policyId")
            .addHeader("Authorization", "Bearer $accessToken")
            .get()
            .build()

        val updatedPolicy = client.newCall(getPolicyRequest).execute().use { response ->
            Log.d("MedPlum", "Response: ${response.body}")
            if (!response.isSuccessful) {
                Log.e("MedPlum", "Failed to fetch AccessPolicy: ${response.code}")
                return@withContext false
            }
            response.body?.string()?.let { JSONObject(it) } ?: return@withContext false
        }
        // Step 4: Add DiagnosticReport and Observations to the policy if missing
        val resourceArray = updatedPolicy.optJSONArray("resource") ?: JSONArray()

        fun alreadyIncluded(resourceType: String, criteria: String?): Boolean {
            return (0 until resourceArray.length()).any {
                val obj = resourceArray.optJSONObject(it)
                obj?.optString("resourceType") == resourceType &&
                        (criteria == null || obj.optString("criteria") == criteria)
            }
        }

        if (!alreadyIncluded("DiagnosticReport", "DiagnosticReport?_id=$resourceId")) {
            /*resourceArray.put(JSONObject().apply {
                put("resourceType", "DiagnosticReport")
                put("criteria", "DiagnosticReport?_id=$resourceId")
            })
            updatedPolicy.put("resource", resourceArray)*/

            if (createConsentResource(patientId, practitionerId, "DiagnosticReport/$resourceId", policyId)) {
                Log.d("MedPlum", "Consent created successfully for DiagnosticReport")
            } else {
                Log.e("MedPlum", "Failed to create Consent for DiagnosticReport")
                return@withContext false
            }

            // PUT now to ensure DR is visible before fetching result[]
            /*val updateBody = updatedPolicy.toString().toRequestBody("application/fhir+json".toMediaType())
            val putRequest = Request.Builder()
                .url("https://api.medplum.com/fhir/R4/AccessPolicy/$policyId")
                .addHeader("Authorization", "Bearer $accessToken")
                .put(updateBody)
                .build()

            val response = client.newCall(putRequest).execute()
            if (!response.isSuccessful) {
                Log.e("MedPlum", "Failed to update AccessPolicy with DiagnosticReport access")
                return@withContext false
            }
            Log.d("MedPlum", "AccessPolicy updated with DiagnosticReport access")*/
        }

        // Fetch DiagnosticReport again
        val reportJson = client.newCall(Request.Builder()
            .url(reportUrl)
            .addHeader("Authorization", "Bearer $accessToken")
            .get()
            .build()).execute().use { it.body?.string()?.let(::JSONObject) }

        val observationIds = mutableListOf<String>()
        val results = reportJson?.optJSONArray("result")
        if (results != null) {
            for (i in 0 until results.length()) {
                val ref = results.getJSONObject(i).optString("reference", "")
                if (ref.startsWith("Observation/")) {
                    observationIds.add(ref.removePrefix("Observation/"))
                }
            }
        }
        Log.d("MedPlum", "Observation IDs: $observationIds")

        // Add each Observation to the policy
        /*val resourceArrayPhase2 = updatedPolicy.optJSONArray("resource") ?: JSONArray()
        observationIds.forEach { obsId ->
            val criteria = "Observation?_id=$obsId"
            if (!alreadyIncluded("Observation", criteria)) {
                resourceArrayPhase2.put(JSONObject().apply {
                    put("resourceType", "Observation")
                    put("criteria", criteria)
                })
            }
        }
        updatedPolicy.put("resource", resourceArrayPhase2)

        // Final PUT
        val finalUpdate = updatedPolicy.toString().toRequestBody("application/fhir+json".toMediaType())
        val finalRequest = Request.Builder()
            .url("https://api.medplum.com/fhir/R4/AccessPolicy/$policyId")
            .addHeader("Authorization", "Bearer $accessToken")
            .put(finalUpdate)
            .build()

        val finalResponse = client.newCall(finalRequest).execute()
        if (!finalResponse.isSuccessful) {
            Log.e("MedPlum", "Failed to add Observations to AccessPolicy")
            return@withContext false
        }

        Log.d("MedPlum", "AccessPolicy updated with Observations")*/
        return@withContext true
    }*/

    suspend fun grantFullDiagnosticReportAccess(
        resourceId: String,
        practitionerId: String,
        projectId: String,
        patientId: String
    ): Boolean = withContext(Dispatchers.IO) {
        val accessToken = getAccessToken() ?: return@withContext false
        val client = OkHttpClient()

        // Step 1: Fetch existing consents for this patient
        val consentUrl = "https://api.medplum.com/fhir/R4/Consent?patient=$patientId&status=active"
        val consentRequest = Request.Builder()
            .url(consentUrl)
            .addHeader("Authorization", "Bearer $accessToken")
            .get()
            .build()

        val matchingConsentExists = client.newCall(consentRequest).execute().use { res ->
            if (!res.isSuccessful) {
                Log.e("MedPlum", "Failed to fetch consents: ${res.code}")
                return@use false
            }
            val body = res.body?.string() ?: return@use false
            Log.d("MedPlum", "Consents response: $body")
            val json = JSONObject(body)
            val entries = json.optJSONArray("entry") ?: return@use false

            for (i in 0 until entries.length()) {
                val consent = entries.getJSONObject(i).optJSONObject("resource") ?: continue
                val provision = consent.optJSONObject("provision") ?: continue

                val actors = provision.optJSONArray("actor") ?: continue
                val matchesDoctor = (0 until actors.length()).any {
                    val ref = actors.getJSONObject(it).optJSONObject("reference")?.optString("reference")
                    ref == "Practitioner/$practitionerId"
                }

                val dataRefs = provision.optJSONArray("data") ?: continue
                val matchesResource = (0 until dataRefs.length()).any {
                    val ref = dataRefs.getJSONObject(it).optJSONObject("reference")?.optString("reference")
                    ref == resourceId
                }

                if (matchesDoctor && matchesResource) {
                    Log.d("MedPlum", "Consent already exists for $resourceId and $practitionerId")
                    return@use true
                }
            }

            false
        }

        // Step 2: If not already consented, create new Consent
        if (!matchingConsentExists) {
            Log.d("MedPlum", "Creating new Consent for $resourceId → $practitionerId")

            val policyId = getAccessPolicyIdFromPractitioner(practitionerId) ?: return@withContext false

            val success = createConsentResource(
                patientId = patientId,
                practitionerId = practitionerId,
                resourceId = resourceId,
                accessPolicyId = policyId
            )
            return@withContext success
        }

        return@withContext true
    }

    suspend fun loadSharedResourcesFromConsents(
        practitionerId: String,
    ): Map<String, List<JSONObject>> = withContext(Dispatchers.IO) {
        val accessToken = getAccessToken()
        if (accessToken == null) {
            Log.e("MedPlum", "No access token available.")
            return@withContext emptyMap()
        }

        val client = OkHttpClient()
        val result = mutableMapOf<String, MutableList<JSONObject>>()

        Log.d("MedPlum", "Fetching consents for practitioner $practitionerId")

        // 1. Fetch all Consents (filter manually since actor search doesn't work directly)
        val consentUrl2 = "https://api.medplum.com/fhir/R4/Consent?status=active"
        val consentRequest = Request.Builder()
            .url(consentUrl2)
            .addHeader("Authorization", "Bearer $accessToken")
            .get()
            .build()

        Log.d("MedPlum", "Consent request URL: ${consentRequest.url}, Headers: ${consentRequest.headers}")
        val consentsResponse = client.newCall(consentRequest).execute().use { res ->
            if (!res.isSuccessful) {
                Log.e("MedPlum", "Failed to fetch consents: ${res.code}")
                Log.e("MedPlum", "Response body: ${res.body?.string()}")
                return@withContext emptyMap()
            }
            val body = res.body?.string() ?: return@withContext emptyMap()
            Log.d("MedPlum", "Consents response: $body")
            JSONObject(body)
        }

        val entries = consentsResponse.optJSONArray("entry") ?: return@withContext emptyMap()

        for (i in 0 until entries.length()) {
            val consent = entries.getJSONObject(i).optJSONObject("resource") ?: continue
            val performer = consent.optJSONArray("performer")?.optJSONObject(0) ?: continue
            Log.d("MedPlum", "Performer: $performer")
            val provision = consent.optJSONObject("provision") ?: continue
            Log.d("MedPlum", "Provision: $provision")

            // Only include consents that mention this practitioner as an actor
            val actors = provision.optJSONArray("actor") ?: continue
            val matchesPractitioner = (0 until actors.length()).any {
                val ref = actors.getJSONObject(it).optJSONObject("reference")?.optString("reference")
                ref == practitionerId
            }
            if (!matchesPractitioner) continue

            val dataArray = provision.optJSONArray("data") ?: continue
            for (j in 0 until dataArray.length()) {
                val ref = dataArray.getJSONObject(j).optJSONObject("reference")?.optString("reference") ?: continue
                val parts = ref.split("/")
                if (parts.size != 2) continue

                val type = parts[0]
                val id = parts[1]

                val resourceUrl = "https://api.medplum.com/fhir/R4/$type/$id"
                val resourceRequest = Request.Builder()
                    .url(resourceUrl)
                    .addHeader("Authorization", "Bearer $accessToken")
                    .get()
                    .build()

                try {
                    val resourceResponse = client.newCall(resourceRequest).execute().use { res ->
                        if (!res.isSuccessful) {
                            Log.w("MedPlum", "Failed to fetch $type/$id: ${res.code}")
                            null
                        } else {
                            val json = res.body?.string() ?: return@use null
                            JSONObject(json)
                        }
                    }

                    resourceResponse?.let { json ->
                        result.getOrPut(type) { mutableListOf() }.add(json)
                    }

                } catch (e: Exception) {
                    Log.e("MedPlum", "Error fetching $type/$id", e)
                }
            }
        }

        return@withContext result
    }

    suspend fun checkConsentExists(practitionerId: String, resourceId: String): Boolean = withContext(Dispatchers.IO) {
        val accessToken = getAccessToken() ?: return@withContext false

        Log.d("MedPlum", "Checking consent for $practitionerId and $resourceId")
        val consentUrl = "https://api.medplum.com/fhir/R4/Consent?status=active"
        val request = Request.Builder()
            .url(consentUrl)
            .addHeader("Authorization", "Bearer $accessToken")
            .get()
            .build()

        OkHttpClient().newCall(request).execute().use { res ->
            if (!res.isSuccessful) {
                Log.e("MedPlum", "Failed to fetch consents: ${res.code}")
                return@use false
            }
            val body = res.body?.string() ?: return@use false
            val json = JSONObject(body)
            val entries = json.optJSONArray("entry") ?: return@use false

            for (i in 0 until entries.length()) {
                val consent = entries.getJSONObject(i).optJSONObject("resource") ?: continue
                val provision = consent.optJSONObject("provision") ?: continue

                val actors = provision.optJSONArray("actor") ?: continue
                val matchesDoctor = (0 until actors.length()).any {
                    val ref = actors.getJSONObject(it).optJSONObject("reference")?.optString("reference")
                    ref == practitionerId
                }

                val dataRefs = provision.optJSONArray("data") ?: continue
                val matchesResource = (0 until dataRefs.length()).any {
                    val ref = dataRefs.getJSONObject(it).optJSONObject("reference")?.optString("reference")
                    ref == resourceId
                }

                if (matchesDoctor && matchesResource) {
                    return@use true
                }
            }

            false
        }
    }

    suspend fun fetchConsentsByPatient(patientId: String): List<ConsentDisplayItem> = withContext(Dispatchers.IO) {
        val accessToken = getAccessToken() ?: return@withContext emptyList()
        val result = mutableListOf<ConsentEntity>()

        val url = "https://api.medplum.com/fhir/R4/Consent?status=active"
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $accessToken")
            .get()
            .build()

        Log.d("MedPlum", "Fetching consents for patient $patientId")

        OkHttpClient().newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                Log.e("MedPlum", "Failed to fetch consents: ${response.code}")
                return@withContext emptyList()
            }

            val body = response.body?.string() ?: return@withContext emptyList()
            val json = JSONObject(body)
            val entries = json.optJSONArray("entry") ?: return@withContext emptyList()
            Log.d("MedPlum", "Found ${entries.length()} consents")

            for (i in 0 until entries.length()) {
                val consent = entries.getJSONObject(i).optJSONObject("resource") ?: continue
                val patientRef = consent.optJSONObject("patient")?.optString("reference") ?: continue

                if (patientRef == "Patient/$patientId") {
                    val id = consent.optString("id", "")
                    val status = consent.optString("status", "")
                    val performerId = consent.optJSONArray("performer")
                        ?.optJSONObject(0)
                        ?.optString("reference")
                        ?.removePrefix("Practitioner/") ?: ""
                    val policyUri = consent.optJSONArray("policy")
                        ?.optJSONObject(0)
                        ?.optString("uri") ?: ""
                    val lastUpdated = consent.optJSONObject("meta")
                        ?.optString("lastUpdated") ?: ""

                    val dataRefs = consent.optJSONObject("provision")
                        ?.optJSONArray("data")
                        ?.let { dataArray ->
                            List(dataArray.length()) { idx ->
                                dataArray.getJSONObject(idx).optJSONObject("reference")
                                    ?.optString("reference") ?: ""
                            }
                        } ?: emptyList()

                    result.add(
                        ConsentEntity(
                            id = id,
                            status = status,
                            patientId = patientId,
                            performerId = performerId,
                            policyUri = policyUri,
                            dataReferences = dataRefs,
                            lastUpdated = lastUpdated
                        )
                    )
                }
            }
        }

        return@withContext resolveConsentDisplayItems(result)
    }

    suspend fun resolveConsentDisplayItems(
        consents: List<ConsentEntity>,
    ): List<ConsentDisplayItem> = withContext(Dispatchers.IO) {
        val accessToken = getAccessToken() ?: return@withContext emptyList()

        val client = OkHttpClient()
        val result = mutableListOf<ConsentDisplayItem>()

        for (consent in consents) {
            // Fetch practitioner details
            Log.d("MedPlum", "Fetching practitionerId ${consent.performerId}")
            val matched = viewModel.practitioners.value.find { it.id == consent.performerId } //practitionerList.find { it.id == consent.performerId }
            val practitioner = matched?.name ?: "Unknown"
            Log.d("MedPlum", "Resolved practitioner: $practitioner, matched: $matched")
            //val practitioner = fetchPractitionerNameById(consent.performerId)

            // Resolve resource references
            val sharedResources = mutableListOf<SharedResourceInfo>()

            for (ref in consent.dataReferences) {
                val parts = ref.split("/")
                if (parts.size != 2) continue
                val type = parts[0]
                val id = parts[1]

                val resourceUrl = "https://api.medplum.com/fhir/R4/$type/$id"
                val request = Request.Builder()
                    .url(resourceUrl)
                    .addHeader("Authorization", "Bearer $accessToken")
                    .get()
                    .build()

                try {
                    val resourceJson = client.newCall(request).execute().use { res ->
                        if (res.isSuccessful) res.body?.string()?.let { JSONObject(it) } else null
                    }

                    val description = when (type) {
                        "DiagnosticReport" -> resourceJson?.optJSONObject("code")?.optString("text") ?: "Diagnostic Report"
                        "Observation" -> resourceJson?.optJSONObject("code")?.optString("text") ?: "Observation"
                        "MedicationRequest" -> resourceJson?.optJSONObject("medicationCodeableConcept")?.optString("text") ?: "Medication"
                        "AllergyIntolerance" -> resourceJson?.optJSONObject("code")?.optString("text") ?: "Allergy"
                        "Procedure" -> resourceJson?.optJSONObject("code")?.optString("text") ?: "Procedure"
                        else -> type
                    }

                    Log.d("MedPlum", "Resolved $type/$id to $description")

                    sharedResources.add(
                        SharedResourceInfo(
                            type = type,
                            description = description,
                            id = id
                        )
                    )
                } catch (e: Exception) {
                    // Skip any failing resource
                    Log.e("MedPlum", "Error fetching $type/$id", e)
                }
            }

            Log.d("MedPlum", "Creating ConsentDisplayItem for consent ID: ${consent.id}, Practitioner: ${practitioner}, Shared Resources: ${sharedResources.size}")

            result.add(
                ConsentDisplayItem(
                    id = consent.id,
                    practitionerId = consent.performerId,
                    practitionerName = practitioner ?: "Unknown Doctor",
                    sharedResources = sharedResources,
                    lastUpdated = consent.lastUpdated
                )
            )
        }

        return@withContext result
    }

    fun revokeAccessPermission(permissionId: String) {

    }

}