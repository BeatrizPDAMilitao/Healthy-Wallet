package com.example.ethktprototype

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.core.graphics.translationMatrix
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ethktprototype.data.AllergyIntoleranceEntity
import com.example.ethktprototype.data.AppDatabase
import com.example.ethktprototype.data.ConditionEntity
import com.example.ethktprototype.data.Converters
import com.example.ethktprototype.data.DeviceEntity
import com.example.ethktprototype.data.DiagnosticReportEntity
import com.example.ethktprototype.data.ImmunizationEntity
import com.example.ethktprototype.data.MedicationRequestEntity
import com.example.ethktprototype.data.MedicationStatementEntity
import com.example.ethktprototype.data.ObservationEntity
import com.example.ethktprototype.data.PatientEntity
import com.example.ethktprototype.data.ProcedureEntity
import com.example.ethktprototype.data.TokenBalance
import com.example.ethktprototype.data.Transaction
import com.example.ethktprototype.data.TransactionEntity
import com.example.ethktprototype.data.ZkpEntity
import com.example.ethktprototype.data.ZkpExamRequestPayload
import com.example.ethktprototype.data.toTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import utils.addressToEnsResolver
import utils.ensResolver
import utils.loadBip44Credentials
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.navigation.NavHostController
import com.example.ethktprototype.data.GraphQLQueries.buildGetPatientAllergiesQuery
import com.example.ethktprototype.data.GraphQLQueries.buildGetPatientDevicesQuery
import com.example.ethktprototype.data.GraphQLQueries.buildGetPatientDiagnosticReportQuery
import com.example.ethktprototype.data.GraphQLQueries.buildGetPatientImmunizationsQuery
import com.example.ethktprototype.data.GraphQLQueries.buildGetPatientListForPractitionerQuery
import com.example.ethktprototype.data.GraphQLQueries.buildGetPatientMedicationRequestsQuery
import com.example.ethktprototype.data.GraphQLQueries.buildGetPatientMedicationStatementsQuery
import com.example.ethktprototype.data.GraphQLQueries.buildGetPatientProceduresQuery
import com.example.ethktprototype.data.GraphQLQueries.buildPatientCompleteQuery
import com.example.ethktprototype.data.GraphQLQueries.buildPractitionerCompleteQuery
import com.example.ethktprototype.data.HealthSummaryResult
import com.example.ethktprototype.data.PractitionerEntity
import com.example.medplum.GetPatientDiagnosticReportQuery
import kotlinx.serialization.InternalSerializationApi
import org.json.JSONObject
import org.web3j.utils.Numeric
import java.math.BigInteger
import kotlin.collections.mapOf
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.security.MessageDigest


/**
 * ViewModel to manage the wallet state.
 *
 * @param application The application associated with this ViewModel.
 */

class WalletViewModel(application: Application) : AndroidViewModel(application) {
    private val transactionDao = AppDatabase.getDatabase(application).transactionDao()

    private val walletRepository = WalletRepository(application)
    private val medPlumAPI = MedPlumAPI(application, this)
    private val sharedPreferences =
        application.getSharedPreferences("WalletPrefs", Context.MODE_PRIVATE)
    private val walletAddressKey = "wallet_address"

    private val _uiState = MutableStateFlow(WalletUiState())
    val uiState: StateFlow<WalletUiState> = _uiState.asStateFlow()

    private var nextTransactionId = 1
    private var nextNotificationId = 1

    private val HEALTH_SUMMARY_KEY = "HealthSummary"
    private val MEDICATION_REQUESTS_KEY = "MedicationRequests"
    private val DIAGNOSTIC_REPORTS_KEY = "DiagnosticReports"
    private val IMMUNIZATIONS_KEY = "Immunizations"
    private val MEDICATION_STATEMENTS_KEY = "MedicationStatements"
    private val PATIENT_KEY = "Patient"
    private val CONDITIONS_KEY = "Conditions"
    private val ALLERGIES_KEY = "Allergies"
    private val DEVICES_KEY = "Devices"
    private val PROCEDURES_KEY = "Procedures"
    private val OBSERVATIONS_KEY = "Observations"
    private val PRACTITIONER_KEY = "Practitioner"
    private val projectId = "01968b55-0883-771d-8f28-b35784bda289"


    init {
        val savedWalletAddress = sharedPreferences.getString(walletAddressKey, "") ?: ""
        updateUiState { it.copy(walletAddress = savedWalletAddress) }

        val lastNetwork = walletRepository.getLastSelectedNetwork()
        updateUiState { it.copy(selectedNetwork = lastNetwork) }

        if (savedWalletAddress.isNotEmpty()) {
            getBalances()
            checkForEnsName(savedWalletAddress)
        }

        loadMnemonicFromPrefs()
        getTokenBlocklist()

        viewModelScope.launch {
            loadPatientFromDb()
            loadConditionsFromDb()
            loadDiagnosticReportsFromDb()
            loadMedicationRequestsFromDb()
            loadMedicationStatementsFromDb()
            loadImmunizationsFromDb()
            loadAllergiesFromDb()
            loadDevicesFromDb()
            loadProceduresFromDb()
            loadPractitionerFromDb()
            loadPatientsFromDb()
        }

        //Add sample transactions for testing
        //addSampleTransactions()
    }

    fun updateIsAppLoading(isLoading: Boolean) {
        updateUiState { it.copy(isAppLoading = isLoading) }
    }

    fun updatePatientIdUiState() {
        updateUiState {
            it.copy(patientId = sharedPreferences.getString("user_profile", null).toString())
        }
    }
    fun deletePatientIdUiState() {
        updateUiState {
            it.copy(patientId = "")
        }
    }

    fun updateHasFetched(key: String, value: Boolean) {
        updateUiState { state ->
            state.copy(hasFetched = state.hasFetched.toMutableMap().apply { put(key, value) })
        }
    }

    fun getLoggedInUsertId(): String {
        Log.d("MedplumAuth", "Patient ID: ${uiState.value.patientId}")
        return sharedPreferences.getString("user_profile", null).toString()
    }

    fun redirectToLogin(navController: NavHostController) {
        navController.navigate("loginScreen") {
            popUpTo(0) { inclusive = true } // Clears the back stack
        }
    }

    fun callMedSkyContract2() {
        //loadContract()
        viewModelScope.launch {
            try {
                val exists = walletRepository.recordExists("sampleRecordId")
                Log.d("MedskyContract", "Record exists: $exists")
                // Handle the result as needed
            } catch (e: Exception) {
                // Handle errors
                Log.e("MedskyContract", "Exception caught", e)
            }
        }
    }

    fun callDenyContract(transactionId: String, recordId: String, requester: String) {
        val mnemonic = getMnemonic()
        viewModelScope.launch {
            setTransactionProcessing(true)
            try {
                if (!mnemonic.isNullOrEmpty()) {
                    val credentials = loadBip44Credentials(mnemonic)
                    credentials.let {
                        val hash = withContext(Dispatchers.IO) {
                            walletRepository.loadHealthyContract(credentials)
                        }
                    }
                    try {
                        val receipt = withContext(Dispatchers.IO) {
                            walletRepository.denyAccess2(recordId, requester, credentials)
                        }
                        Log.d("DenyContract", "Access denied: ${receipt.transactionHash}")
                        // Handle the result as needed
                        updateUiState { state ->
                            state.copy(
                                transactionHash = receipt.transactionHash,
                                showPayDialog = false,
                                showDenyDialog = true,
                                showSuccessModal = false,
                                showWalletModal = false,
                            )
                        }
                        updateTransactionStatus(transactionId, "denied")
                    } catch (e: Exception) {
                        // Handle errors
                        Log.e("DenyContract", "Exception caught", e)
                    }
                }
            } catch (e: Exception) {
                // Handle errors
                //updateUiState { it.copy(showPayDialog = false) }
                Log.d("DenyContract", "Error loading contract: ${e.message}")
            }
            setTransactionProcessing(false)
        }
    }

    fun callAcceptContract(transactionId: String, practitionerId: String, recordId: String, requester: String) {
        val mnemonic = getMnemonic()
        viewModelScope.launch {
            setTransactionProcessing(true)
            try {
                if (!mnemonic.isNullOrEmpty()) {
                    val credentials = loadBip44Credentials(mnemonic)
                    credentials.let {
                        val hash = withContext(Dispatchers.IO) {
                            walletRepository.loadHealthyContract(credentials)
                        }
                    }
                    try {
                        val receipt = withContext(Dispatchers.IO) {
                            walletRepository.acceptAccess2(recordId, requester, credentials)
                        }
                        Log.d("AcceptContract", "Access given: ${receipt.transactionHash}")
                        updateUiState { state ->
                            state.copy(
                                transactionHash = receipt.transactionHash,
                                showDenyDialog = true,
                            )
                        }
                        updateTransactionStatus(transactionId, "accepted")

                        Log.d("MedPlumGrant", "Granting policy...")
                        val granted = withContext(Dispatchers.IO) {
                            medPlumAPI.grantFullDiagnosticReportAccess(recordId, practitionerId, projectId)
                        }
                        Log.d("MedPlumGrant", "Policy granted: $granted")
                    } catch (e: Exception) {
                        // Handle errors
                        Log.e("AcceptContract", "Exception caught", e)
                    }
                }
            } catch (e: Exception) {
                // Handle errors
                //updateUiState { it.copy(showPayDialog = false) }
                Log.d("AcceptContract", "Error loading contract: ${e.message}")
            }
            setTransactionProcessing(false)
        }
    }

    fun syncTransactionWithHealthyContract() {
        val mnemonic = getMnemonic()
        viewModelScope.launch {
            setTransactionProcessing(true)
            try {
                if (!mnemonic.isNullOrEmpty()) {
                    val credentials = loadBip44Credentials(mnemonic)

                    withContext(Dispatchers.IO) {
                        walletRepository.loadHealthyContract(credentials)
                    }
                    Log.d("SyncedLog", "Before syncTransactionWithHealthyContract")
                    val logs = withContext(Dispatchers.IO) {
                        walletRepository.syncTransactionWithHealthyContract2(credentials)
                    }
                    Log.d("SyncedLog", "Logs=${logs.size}")
                    logs.forEach { log ->
                        val date = SimpleDateFormat(
                            "yyyy-MM-dd",
                            Locale.getDefault()
                        ).format(Date(log.date.toLong() * 1000))
                        Log.d(
                            "SyncedLog",
                            "Doctor=${log.practitionerAddress}, Patient=${log.patientId}, Type=${log.type}, Timestamp=${log.date}"
                        )
                        //call medplum API to get the necessary data
                        //walletRepository.fetchPatient(/*log.patientId*/)
                        val transaction = TransactionEntity(
                            id = getTransactionId().toString(),
                            date = date,
                            status = log.status,
                            type = log.type,
                            recordId = log.recordId,
                            patientId = log.patientId,
                            practitionerId = log.practitionerId,
                            practitionerAddress = log.practitionerAddress,
                            documentReferenceId = "", // Placeholder if unknown
                            medicationRequestId = "",
                            conditionId = "",
                            encounterId = "",
                            observationId = ""
                        )

                        withContext(Dispatchers.IO) {
                            if (transactionDao.transactionExists(transaction.id) == 0) {
                                transactionDao.insertTransaction(transaction)
                            }
                        }
                        _uiState.value = _uiState.value.copy(showSyncSuccessDialog = true)
                    }

                    updateTransactions()
                }
            } catch (e: Exception) {
                Log.e("SyncedLog", "Sync failed: ${e.message}", e)
                _uiState.value = _uiState.value.copy(showSyncErrorDialog = true)
            }
            setTransactionProcessing(false)
        }
    }

    val simulateCreateTimes = mutableListOf<Long>()
    val simulateCreateFees = mutableListOf<BigInteger>()
    suspend fun callCreateRecordContract(recordType: String, record: Any, hash: String, patientId: String) {
        val mnemonic = getMnemonic()

        val duration = withContext(Dispatchers.IO) {
            val start = System.currentTimeMillis()
            setTransactionProcessing(true)
            try {
                if (!mnemonic.isNullOrEmpty()) {
                    val recordId = withContext(Dispatchers.IO) {
                        medPlumAPI.createMedplumRecord(recordType, record, patientId, getLoggedInUsertId())
                    }

                    val credentials = loadBip44Credentials(mnemonic)
                    credentials.let {
                        val hash = withContext(Dispatchers.IO) {
                            walletRepository.loadHealthyContract(credentials)
                        }
                    }
                    try {
                        val receipt = withContext(Dispatchers.IO) {
                            walletRepository.createRecord(recordId, hash, credentials)
                        }
                        val gasPriceHex = receipt.effectiveGasPrice

                        val gasPrice = Numeric.decodeQuantity(
                            if (gasPriceHex.startsWith("0x")) gasPriceHex else "0x$gasPriceHex"
                        )
                        val gasUsed = receipt.gasUsed
                        val gasFee = gasUsed * gasPrice
                        Log.d("CreateRecord", "Gas fee: $gasFee, Gas used: $gasUsed, Gas price: $gasPrice")
                        simulateCreateFees.add(gasFee)
                        Log.d("CreateRecord", "Record Created: ${receipt.transactionHash}")
                        // Handle the result as needed
                        updateUiState { state ->
                            state.copy(
                                transactionHash = receipt.transactionHash,
                                showPayDialog = false,
                                showDenyDialog = true,
                                showSuccessModal = false,
                                showWalletModal = false,
                            )
                        }
                    } catch (e: Exception) {
                        // Handle errors
                        Log.e("DenyContract", "Exception caught", e)
                    }
                }
            } catch (e: Exception) {
                // Handle errors
                //updateUiState { it.copy(showPayDialog = false) }
                Log.d("DenyContract", "Error loading contract: ${e.message}")
            }
            setTransactionProcessing(false)
            System.currentTimeMillis() - start
        }
        simulateCreateTimes.add(duration)
    }

    fun printCreateEHR(){
        Log.d("CreateEHR", "Create EHR Times: $simulateCreateTimes")
        Log.d("CreateEHR", "Mean Create EHR Time: ${simulateCreateTimes.average()}")
        Log.d("CreateEHR", "Create EHR Fees: $simulateCreateFees")
        if (simulateCreateFees.isNotEmpty()) {
            val meanGasFee = simulateCreateFees.reduce(BigInteger::add)
                .divide(BigInteger.valueOf(simulateCreateFees.size.toLong()))
            Log.d("CreateEHR", "Mean Create EHR Gas Fee: $meanGasFee")
        }
    }

    val gson: Gson = GsonBuilder()
        .serializeNulls()
        .disableHtmlEscaping()
        .setPrettyPrinting()
        .create()

    fun calculateRecordHash(record: Any): String {
        // Serialize the object to canonical JSON
        val jsonString = gson.toJson(record)

        // Calculate SHA-256 hash
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(jsonString.toByteArray())

        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    @OptIn(InternalSerializationApi::class)
    fun calculateRecordHash(fields: Map<String, String>): String { //TODO: Devia ser Any. E ter um para cada tipo de record
        val jsonString = JSONObject(fields).toString()
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(jsonString.toByteArray())
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    suspend fun <T> withLoggedAccess(
        requesterId: String,
        recordIds: List<String>,
        resourcesType: String,
        fetchOperation: suspend () -> T
    ): T? {
        val mnemonic = getMnemonic()
        if (mnemonic.isNullOrEmpty()) return null
        val credentials = loadBip44Credentials(mnemonic)

        return try {
            walletRepository.loadAccessesContract(credentials)

            val formattedRecordIds = recordIds.map { resourcesType + it }
            val receipt = walletRepository.logAccess(formattedRecordIds, credentials)

            Log.d("LogAccess", "TransactionHash: ${receipt.transactionHash}")
            val gasPriceHex = receipt.effectiveGasPrice

            val gasPrice = Numeric.decodeQuantity(
                if (gasPriceHex.startsWith("0x")) gasPriceHex else "0x$gasPriceHex"
            )
            val gasUsed = receipt.gasUsed
            val gasFee = gasUsed * gasPrice
            Log.d("AccessControl", "Gas fee: $gasFee, Gas used: $gasUsed, Gas price: $gasPrice")
            gasFees.add(gasFee)
            fetchOperation()
        } catch (e: Exception) {
            Log.e("AccessControl", "Access log or fetch failed: ${e.message}", e)
            null
        }
    }

    val gasFees = mutableListOf<BigInteger>()

    suspend fun <T> withLoggedAccessPerScreen(
        screen: String,
        queries: List<String>,
        fetchOperation: suspend () -> T?
    ): Boolean {
        val mnemonic = getMnemonic() ?: return false
        val credentials = loadBip44Credentials(mnemonic)

        val key = "access_time_$screen"
        val now = System.currentTimeMillis()
        val lastAccessTime = walletRepository.getLastAccessTime(key)
        val twentyFourHoursMillis = 24 * 60 * 60 * 1000

        val fetchedData = fetchOperation()

        // If fetch failed, don't log or continue
        if (fetchedData == null) {
            Log.e("AccessControl", "[$screen] Data fetch failed, skipping access log")
            return false
        }

        Log.d("AccessControl", "lastAccessTime = $lastAccessTime, now = $now")
        // Only send transaction if 24h passed
        if ( uiState.value.hasFetched[screen] != true ||(now - lastAccessTime >= twentyFourHoursMillis) || lastAccessTime == 0L) {
            return try {
                walletRepository.loadAccessesContract(credentials)
                Log.d("AccessControl", "[$screen] Loaded contract")
                val receipt = walletRepository.logAccess(queries, credentials)
                val gasPriceHex = receipt.effectiveGasPrice

                val gasPrice = Numeric.decodeQuantity(
                    if (gasPriceHex.startsWith("0x")) gasPriceHex else "0x$gasPriceHex"
                )
                val gasUsed = receipt.gasUsed
                val gasFee = gasUsed * gasPrice
                Log.d("AccessControl", "[$screen] Gas fee: $gasFee, Gas used: $gasUsed, Gas price: $gasPrice")
                gasFees.add(gasFee)
                walletRepository.updateLastAccessTime(key, now)
                Log.d("AccessControl", "[$screen] Access logged: ${receipt.transactionHash}")
                true
            } catch (e: Exception) {
                Log.e("AccessControl", "[$screen] Access log failed", e)
                false
            }
        }

        // Already logged, just allow use of fetched data
        return true
    }

    fun getGasFees() {
        Log.d("GasStats", "Gas fees: $gasFees")
        if (!gasFees.isEmpty()) {
            val meanGasFee = gasFees.reduce(BigInteger::add).divide(BigInteger.valueOf(gasFees.size.toLong()))
            Log.d("GasStats", "Mean gas fee: $meanGasFee")

        }
    }


    //////////////////// MedPlum API Tests with and without blockchain ////////////////////

    fun testFetchPrescriptions(subjectId: String) {
        viewModelScope.launch {
            //gasFees.removeAll { it > BigInteger.ZERO } // Clear previous gas fees
            val withBlockchainTimes = mutableListOf<Long>()
            val withoutBlockchainTimes = mutableListOf<Long>()

            // Measure with blockchain
            for (i in 1..30) {
                val duration = withContext(Dispatchers.IO) {
                    val start = System.currentTimeMillis()
                    getMedicationRequestsWithBlockchain(subjectId)
                    System.currentTimeMillis() - start
                }
                withBlockchainTimes.add(duration)
                Log.d("PerformanceTest", "With blockchain [$i]: $duration ms")
                delay(2000)
            }

            // Measure without blockchain
            for (i in 1..30) {
                val duration = withContext(Dispatchers.IO) {
                    val start = System.currentTimeMillis()
                    getMedicationRequestsWithoutBlockchain()
                    System.currentTimeMillis() - start
                }
                withoutBlockchainTimes.add(duration)
                Log.d("PerformanceTest", "Without blockchain [$i]: $duration ms")
                delay(2000)
            }

            val avgWith = withBlockchainTimes.average()
            val avgWithout = withoutBlockchainTimes.average()

            Log.d("GasStats", "Gas fees: $gasFees")
            if (!gasFees.isEmpty()) {
                val meanGasFee = gasFees.reduce(BigInteger::add).divide(BigInteger.valueOf(gasFees.size.toLong()))
                Log.d("GasStats", "Mean gas fee: $meanGasFee")

            }


            Log.d("PerformanceTest", "Average WITH blockchain: ${"%.2f".format(avgWith)} ms")
            Log.d("PerformanceTest", "Values With blockchain: $withBlockchainTimes")
            Log.d("PerformanceTest", "Average WITHOUT blockchain: ${"%.2f".format(avgWithout)} ms")
            Log.d("PerformanceTest", "Values Without blockchain: $withoutBlockchainTimes")
        }
    }

    //////////////////// MedPlum API Calls ////////////////////

    /*fun getPatientData(patientId: String) {
        viewModelScope.launch {
            try {
                val patientData = withContext(Dispatchers.IO) {
                    medPlumAPI.fetchPatient(/*patientId*/)
                }
                updateUiState { state ->
                    state.copy(
                        showDataDialog = true,
                    )
                }
                // Handle the patient data as needed
                Log.d("PatientData", "Patient data: $patientData")
            } catch (e: Exception) {
                // Handle errors
                Log.e("PatientData", "Error fetching patient data: ${e.message}", e)
            }
        }
    }*/


    fun getHealthSummaryData() {
        val patientId = getLoggedInUsertId()
        Log.d("MedplumAuth", GetPatientDiagnosticReportQuery(patientId).document())
        val queries = listOf(
            buildGetPatientDiagnosticReportQuery(patientId),
            buildGetPatientAllergiesQuery(patientId),
            buildGetPatientMedicationStatementsQuery(patientId),
            buildGetPatientProceduresQuery(patientId),
            buildGetPatientDevicesQuery(patientId),
            buildGetPatientImmunizationsQuery(patientId),
        )

        viewModelScope.launch {
            _uiState.update { it.copy(isHealthSummaryLoading = true) }

            try {
                val result = withContext(Dispatchers.IO) {
                    var fetched: HealthSummaryResult? = null
                    val success = withLoggedAccessPerScreen(HEALTH_SUMMARY_KEY, queries) {
                        val attempt = medPlumAPI.fetchHealthSummary(patientId)
                        // If any of the lists is null, we treat as failure
                        if (attempt.diagnostics != null && attempt.allergies != null &&
                            attempt.meds != null && attempt.procedures != null &&
                            attempt.devices != null && attempt.immunizations != null
                        ) {
                            fetched = attempt
                            true
                        } else {
                            false
                        }
                    }
                    if (success == true) fetched else null
                }

                result?.let {
                    _diagnosticReports.value = it.diagnostics.orEmpty()
                    _allergies.value = it.allergies.orEmpty()
                    _medicationStatements.value = it.meds.orEmpty()
                    _procedures.value = it.procedures.orEmpty()
                    _devices.value = it.devices.orEmpty()
                    _immunizations.value = it.immunizations.orEmpty()

                    withContext(Dispatchers.IO) {
                        transactionDao.insertDiagnosticReports(it.diagnostics ?: emptyList())
                        transactionDao.insertAllergies(it.allergies ?: emptyList())
                        transactionDao.insertMedicationStatements(it.meds ?: emptyList())
                        transactionDao.insertProcedures(it.procedures ?: emptyList())
                        transactionDao.insertDevices(it.devices ?: emptyList())
                        transactionDao.insertImmunizations(it.immunizations ?: emptyList())
                    }

                    updateHasFetched(DIAGNOSTIC_REPORTS_KEY, true)
                    updateHasFetched(DIAGNOSTIC_REPORTS_KEY, true)
                    updateHasFetched(ALLERGIES_KEY, true)
                    updateHasFetched(MEDICATION_STATEMENTS_KEY, true)
                    updateHasFetched(PROCEDURES_KEY, true)
                    updateHasFetched(DEVICES_KEY, true)
                    updateHasFetched(IMMUNIZATIONS_KEY, true)
                    updateHasFetched(HEALTH_SUMMARY_KEY, true)
                    val timestamp = walletRepository.getLastAccessTime(HEALTH_SUMMARY_KEY)
                    walletRepository.updateLastAccessTime(DIAGNOSTIC_REPORTS_KEY, timestamp)
                    walletRepository.updateLastAccessTime(ALLERGIES_KEY, timestamp)
                    walletRepository.updateLastAccessTime(MEDICATION_STATEMENTS_KEY, timestamp)
                    walletRepository.updateLastAccessTime(PROCEDURES_KEY, timestamp)
                    walletRepository.updateLastAccessTime(DEVICES_KEY, timestamp)
                    walletRepository.updateLastAccessTime(IMMUNIZATIONS_KEY, timestamp)
                } ?: Log.e("MedplumAccess", "Access or fetch failed")

            } catch (e: Exception) {
                Log.e("HealthSummary", "Unexpected error", e)
            } finally {
                _uiState.update { it.copy(isHealthSummaryLoading = false, isAppLoading = false) }
            }
        }
    }

    fun getUser() {
        val userId = getLoggedInUsertId()
        if (userId.startsWith("Patient/")) {
            getPatientComplete()
        } else if (userId.startsWith("Practitioner/")) {
            getPractitionerData()
        } else {
            Log.e("MedplumAuth", "Unknown user type: $userId")
        }
    }


    private val _patient = MutableStateFlow<PatientEntity?>(null)
    val patient: StateFlow<PatientEntity?> = _patient
    fun getPatientComplete() {
        val patientId = getLoggedInUsertId().removePrefix("Patient/")
        val query = buildPatientCompleteQuery(patientId)
        Log.d("MedplumAuth", "Patient ID: $patientId")
        viewModelScope.launch {
            _uiState.update { it.copy(isPatientLoading = true) }
            try {
                val result = withContext(Dispatchers.IO) {
                    var patient: PatientEntity? = null
                    val success = withLoggedAccessPerScreen(PATIENT_KEY, listOf(query)) {
                        val data = medPlumAPI.fetchPatientComplete(patientId)
                        patient = data
                        data != null
                    }
                    if (success) patient else null
                }

                result?.let {
                    _patient.value = it
                    transactionDao.insertPatient(it)
                    updateHasFetched(PATIENT_KEY, true)
                } ?: Log.e("MedplumAuth", "Access denied or failed")
            } catch (e: Exception) {
                Log.e("Exams", "Error fetching", e)
            } finally {
                _uiState.update {
                    it.copy(isPatientLoading = false, isAppLoading = false)
                }
            }
        }
    }

    fun loadPatientFromDb() {
        if (!getLoggedInUsertId().startsWith("Patient/")) return
        viewModelScope.launch {
            val cached = withContext(Dispatchers.IO) {
                transactionDao.getPatientById(getLoggedInUsertId().removePrefix("Patient/"))
            }
            _patient.value = cached
        }
    }

    private val _patients = MutableStateFlow<List<PatientEntity?>>(emptyList())
    val patients: StateFlow<List<PatientEntity?>> = _patients
    fun getPatientListForPractitioner() {
        val subjectId = getLoggedInUsertId()
        Log.d("MedplumAuth", "Subject ID: $subjectId")
        val query = buildGetPatientListForPractitionerQuery(subjectId)
        Log.d("MedplumAuth", "User ID: $subjectId")
        viewModelScope.launch {
            _uiState.update { it.copy(isPatientLoading = true) }
            try {
                val result = withContext(Dispatchers.IO) {
                    var patients: List<PatientEntity>? = null
                    val success = withLoggedAccessPerScreen(PATIENT_KEY, listOf(query)) {
                        val data = medPlumAPI.fetchPatientListOfPractitioner(subjectId)
                        patients = data
                        data != null
                    }
                    if (success) patients else null
                }
                result?.let {
                    _patients.value = it
                    transactionDao.insertPatients(it)
                    updateHasFetched(PATIENT_KEY, true)
                }
                Log.d("PatientsListData", "Patients List: $result")
            } catch (e: Exception) {
                Log.e("Exams", "Error fetching", e)
            } finally {
                _uiState.update {
                    it.copy(isPatientLoading = false, isAppLoading = false)
                }
            }
        }
    }

    fun loadPatientsFromDb() {
        if (!getLoggedInUsertId().startsWith("Practitioner/")) return
        viewModelScope.launch {
            val cached = withContext(Dispatchers.IO) {
                transactionDao.getPatients()
            }
            _patients.value = cached
        }
    }

    private val _practitioner = MutableStateFlow<PractitionerEntity?>(null)
    val practitioner: StateFlow<PractitionerEntity?> = _practitioner
    fun getPractitionerData(practitionerId: String = getLoggedInUsertId().removePrefix("Practitioner/")) {
        val query = buildPractitionerCompleteQuery(practitionerId)
        Log.d("MedplumAuth", "Practitioner ID: $practitionerId")
        viewModelScope.launch {
            _uiState.update { it.copy(isPatientLoading = true) }
            try {
                val result = withContext(Dispatchers.IO) {
                    var practitioner: PractitionerEntity? = null
                    val success = withLoggedAccessPerScreen(PRACTITIONER_KEY, listOf(query)) {
                        val data = medPlumAPI.fetchPractitioner(practitionerId)
                        practitioner = data
                        data != null
                    }
                    if (success) practitioner else null
                }

                result?.let {
                    if (getLoggedInUsertId().startsWith("Practitioner/")) {
                        _practitioner.value = it
                    }
                    transactionDao.insertPractitioner(it)
                    updateHasFetched(PRACTITIONER_KEY, true)
                } ?: Log.e("MedplumAuth", "Access denied or failed")
            } catch (e: Exception) {
                Log.e("Exams", "Error fetching", e)
            } finally {
                _uiState.update {
                    it.copy(isPatientLoading = false, isAppLoading = false)
                }
            }
        }
        viewModelScope.launch {
            try {
                val practitionerData = withContext(Dispatchers.IO) {
                    medPlumAPI.fetchPractitioner(practitionerId)
                }
                updateUiState { state ->
                    state.copy(
                        practitionerData = practitionerData
                    )
                }
                // Handle the practitioner data as needed
                Log.d("PractitionerData", "Practitioner data: $practitionerData")
            } catch (e: Exception) {
                // Handle errors
                Log.e("PractitionerData", "Error fetching practitioner data: ${e.message}", e)
            }
        }
    }

    fun loadPractitionerFromDb() {
        if (!getLoggedInUsertId().startsWith("Practitioner/")) return
        viewModelScope.launch {
            val cached = withContext(Dispatchers.IO) {
                transactionDao.getPractitionerById(getLoggedInUsertId().removePrefix("Practitioner/"))
            }
            _practitioner.value = cached
        }
    }

    private val _conditions = MutableStateFlow<List<ConditionEntity>>(emptyList())
    val conditions: StateFlow<List<ConditionEntity>> = _conditions
    fun getConditions(subjectId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isConditionsLoading = true) }
            try {
                val conditions = withContext(Dispatchers.IO) {
                    withLoggedAccess(
                        requesterId = uiState.value.walletAddress, //FOR now. Wallet address or medplum user id?
                        recordIds = listOf(subjectId),
                        resourcesType = "Condition/"
                    ) {
                        medPlumAPI.fetchConditions(subjectId)
                    }
                }
                conditions?.let {
                    _conditions.value = it
                    transactionDao.insertConditions(it)
                    updateHasFetched(CONDITIONS_KEY, true)
                }
            } catch (e: Exception) {
                Log.e("Exams", "Error fetching", e)
            } finally {
                _uiState.update { it.copy(isConditionsLoading = false) }
            }
        }
    }

    fun loadConditionsFromDb() {
        viewModelScope.launch {
            val cached = withContext(Dispatchers.IO) {
                transactionDao.getConditions()
            }
            _conditions.value = cached
        }
    }

    fun getConditionRequirement(condition: String): ObservationEntity? {
        return runBlocking {
            transactionDao.findByCode(condition)
        }
    }

    private val _diagnosticReports = MutableStateFlow<List<DiagnosticReportEntity>>(emptyList())
    val diagnosticReports: StateFlow<List<DiagnosticReportEntity>> = _diagnosticReports
    fun getDiagnosticReports(subjectId: String = getLoggedInUsertId()) {
        val query = buildGetPatientDiagnosticReportQuery(subjectId)
        viewModelScope.launch {
            _uiState.update { it.copy(isDiagnosticReportsLoading = true) }
            try {
                val result = withContext(Dispatchers.IO) {
                    var reports: List<DiagnosticReportEntity>? = null
                    val success = withLoggedAccessPerScreen(DIAGNOSTIC_REPORTS_KEY, listOf(query)) {
                        val data = medPlumAPI.fetchDiagnosticReports(subjectId)
                        reports = data
                        data != null
                    }
                    if (success) reports else null
                }
                result?.let {
                    _diagnosticReports.value = it
                    transactionDao.insertDiagnosticReports(it)
                    updateHasFetched(DIAGNOSTIC_REPORTS_KEY, true)
                }
                Log.d("DiagnosticReportsData", "Diagnostic Reports: $result")
            } catch (e: Exception) {
                Log.e("Exams", "Error fetching", e)
            } finally {
                _uiState.update { it.copy(isDiagnosticReportsLoading = false) }
            }
        }
    }

    fun loadDiagnosticReportsFromDb() {
        viewModelScope.launch {
            val cached = withContext(Dispatchers.IO) {
                transactionDao.getDiagnosticReports()
            }
            _diagnosticReports.value = cached
        }
    }


    private val _medicationRequests = MutableStateFlow<List<MedicationRequestEntity>>(emptyList())
    val medicationRequests: StateFlow<List<MedicationRequestEntity>> = _medicationRequests
    fun getMedicationRequests() {
        val subjectId = getLoggedInUsertId()
        val query = buildGetPatientMedicationRequestsQuery(subjectId)
        viewModelScope.launch {
            _uiState.update { it.copy(isMedicationRequestsLoading = true) }
            try {
                val result = withContext(Dispatchers.IO) {
                    var requests: List<MedicationRequestEntity>? = null
                    val success = withLoggedAccessPerScreen(MEDICATION_REQUESTS_KEY, listOf(query)) {
                        val data = medPlumAPI.fetchMedicationRequests(subjectId)
                        requests = data
                        data != null
                    }
                    if (success) requests else null
                }
                result?.let {
                    _medicationRequests.value = it
                    transactionDao.insertMedicationRequests(it)
                    updateHasFetched(MEDICATION_REQUESTS_KEY, true)
                }
                Log.d("MedicationRequestsData", "Medication Requests: $result")
            } catch (e: Exception) {
                Log.e("Exams", "Error fetching", e)
            } finally {
                _uiState.update { it.copy(isMedicationRequestsLoading = false) }
            }
        }
    }

    suspend fun getMedicationRequestsSuspend(subjectId: String): List<MedicationRequestEntity>? {
        return withContext(Dispatchers.IO) {
            medPlumAPI.fetchMedicationRequests(subjectId)
        }
    }

    suspend fun getMedicationRequestsWithoutBlockchain() {
        val subjectId = getLoggedInUsertId()
        try {
            val requests = withContext(Dispatchers.IO) {
                medPlumAPI.fetchMedicationRequests(subjectId)
            }
            requests?.let {
                _medicationRequests.value = it
                transactionDao.insertMedicationRequests(it)
                updateHasFetched(MEDICATION_REQUESTS_KEY, true)
            }
            Log.d("MedicationRequestsData", "Medication Requests: $requests")
        } catch (e: Exception) {
            Log.e("MedicationRequestsData", "Error fetching medication requests: ${e.message}", e)
            null
        }
    }

    suspend fun getMedicationRequestsWithBlockchain(subjectId: String) {
        Log.d("getMedicationRequestsWithBlockchain", "Subject ID: $subjectId")
        //viewModelScope.launch {

            val requests = withContext(Dispatchers.IO) {
                withLoggedAccess(
                    requesterId = uiState.value.walletAddress, //FOR now. Wallet address or medplum user id?
                    recordIds = listOf(subjectId),
                    resourcesType = "MedicationRequest/"
                ) {
                    medPlumAPI.fetchMedicationRequests(subjectId)
                }
            }
            if (requests == null) {
                Log.e("getMedicationRequestsWithBlockchain", "Access denied or failed")
            }
        //}
    }

    fun loadMedicationRequestsFromDb() {
        viewModelScope.launch {
            val cached = withContext(Dispatchers.IO) {
                transactionDao.getMedicationRequests()
            }
            _medicationRequests.value = cached
        }
    }

    private val _medicationStatements = MutableStateFlow<List<MedicationStatementEntity>>(emptyList())
    val medicationStatements: StateFlow<List<MedicationStatementEntity>> = _medicationStatements
    fun getMedicationStatements() {
        val subjectId = getLoggedInUsertId()
        val query = buildGetPatientMedicationStatementsQuery(subjectId)
        viewModelScope.launch {
            _uiState.update { it.copy(isMedicationStatementsLoading = true) }
            try {
                val result = withContext(Dispatchers.IO) {
                    var statements: List<MedicationStatementEntity>? = null
                    val success = withLoggedAccessPerScreen(MEDICATION_STATEMENTS_KEY, listOf(query)) {
                        val data = medPlumAPI.fetchMedicationStatements(subjectId)
                        statements = data
                        data != null
                    }
                    if (success) statements else null
                }
                result?.let {
                    _medicationStatements.value = it
                    transactionDao.insertMedicationStatements(it)
                    updateHasFetched(MEDICATION_STATEMENTS_KEY, true)
                }
                Log.d("MedicationStatementsData", "Medication Statements: $result")
            } catch (e: Exception) {
                Log.e("Exams", "Error fetching", e)
            } finally {
                _uiState.update { it.copy(isMedicationStatementsLoading = false) }
            }
        }
    }

    fun loadMedicationStatementsFromDb() {
        viewModelScope.launch {
            val cached = withContext(Dispatchers.IO) {
                transactionDao.getMedicationStatements()
            }
            _medicationStatements.value = cached
        }
    }

    private val _immunizations = MutableStateFlow<List<ImmunizationEntity>>(emptyList())
    val immunizations: StateFlow<List<ImmunizationEntity>> = _immunizations
    fun getImmunizations() {
        val subjectId = getLoggedInUsertId()
        val query = buildGetPatientImmunizationsQuery(subjectId)
        viewModelScope.launch {
            _uiState.update { it.copy(isImmunizationsLoading = true) }
            try {
                val result = withContext(Dispatchers.IO) {
                    var immunizations: List<ImmunizationEntity>? = null
                    val success = withLoggedAccessPerScreen(IMMUNIZATIONS_KEY, listOf(query)) {
                        val data = medPlumAPI.fetchImmunizations(subjectId)
                        immunizations = data
                        data != null
                    }
                    if (success) immunizations else null
                }
                result?.let {
                    _immunizations.value = it
                    transactionDao.insertImmunizations(it)
                    updateHasFetched(IMMUNIZATIONS_KEY, true)
                }
                Log.d("ImmunizationsData", "Medication Requests: $result")
            } catch (e: Exception) {
                Log.e("Exams", "Error fetching", e)
            } finally {
                _uiState.update { it.copy(isImmunizationsLoading = false) }
            }
        }
    }

    fun loadImmunizationsFromDb() {
        viewModelScope.launch {
            val cached = withContext(Dispatchers.IO) {
                transactionDao.getImmunizations()
            }
            _immunizations.value = cached
        }
    }

    private val _allergies = MutableStateFlow<List<AllergyIntoleranceEntity>>(emptyList())
    val allergies: StateFlow<List<AllergyIntoleranceEntity>> = _allergies
    fun getAllergies() {
        val subjectId = getLoggedInUsertId()
        viewModelScope.launch {
            _uiState.update { it.copy(isAllergiesLoading = true) }
            try {
                val allergies = withContext(Dispatchers.IO) {
                    withLoggedAccess(
                        requesterId = uiState.value.walletAddress, //FOR now. Wallet address or medplum user id?
                        recordIds = listOf(subjectId),
                        resourcesType = "AllergyIntolerance/"
                    ) {
                        medPlumAPI.fetchAllergies(subjectId)
                    }
                }
                allergies?.let {
                    _allergies.value = it
                    transactionDao.insertAllergies(it)
                    updateHasFetched(ALLERGIES_KEY, true)
                }
                Log.d("AllergiesData", "Allergies: $allergies")
            } catch (e: Exception) {
                Log.e("Exams", "Error fetching", e)
            } finally {
                _uiState.update { it.copy(isAllergiesLoading = false) }
            }
        }
    }

    fun loadAllergiesFromDb() {
        viewModelScope.launch {
            val cached = withContext(Dispatchers.IO) {
                transactionDao.getAllergies()
            }
            _allergies.value = cached
        }
    }

    private val _devices = MutableStateFlow<List<DeviceEntity>>(emptyList())
    val devices: StateFlow<List<DeviceEntity>> = _devices
    fun getDevices() {
        val subjectId = getLoggedInUsertId()
        viewModelScope.launch {
            _uiState.update { it.copy(isDevicesLoading = true) }
            try {
                val devices = withContext(Dispatchers.IO) {
                    withLoggedAccess(
                        requesterId = uiState.value.walletAddress, //FOR now. Wallet address or medplum user id?
                        recordIds = listOf(subjectId),
                        resourcesType = "Device/"
                    ) {
                        medPlumAPI.fetchDevices(subjectId)
                    }
                }
                devices?.let {
                    _devices.value = it
                    transactionDao.insertDevices(it)
                    updateHasFetched(DEVICES_KEY, true)
                }
                Log.d("Devices", "Devices: $devices")
            } catch (e: Exception) {
                Log.e("Exams", "Error fetching", e)
            } finally {
                _uiState.update { it.copy(isDevicesLoading = false) }
            }
        }
    }

    fun loadDevicesFromDb() {
        viewModelScope.launch {
            val cached = withContext(Dispatchers.IO) {
                transactionDao.getDevices()
            }
            _devices.value = cached
        }
    }

    private val _procedures = MutableStateFlow<List<ProcedureEntity>>(emptyList())
    val procedures: StateFlow<List<ProcedureEntity>> = _procedures
    fun getProcedures() {
        val subjectId = getLoggedInUsertId()
        viewModelScope.launch {
            _uiState.update { it.copy(isProceduresLoading = true) }
            try {
                val procedures = withContext(Dispatchers.IO) {
                    withLoggedAccess(
                        requesterId = uiState.value.walletAddress, //FOR now. Wallet address or medplum user id?
                        recordIds = listOf(subjectId),
                        resourcesType = "Procedure/"
                    ) {
                        medPlumAPI.fetchProcedures(subjectId)
                    }
                }
                procedures?.let {
                    _procedures.value = it
                    transactionDao.insertProcedures(it)
                    updateHasFetched(PROCEDURES_KEY, true)
                }
                Log.d("ProceduresData", "Procedures: $procedures")
            } catch (e: Exception) {
                Log.e("Exams", "Error fetching", e)
            } finally {
                _uiState.update { it.copy(isProceduresLoading = false) }
            }
        }
    }

    fun loadProceduresFromDb() {
        viewModelScope.launch {
            val cached = withContext(Dispatchers.IO) {
                transactionDao.getProcedures()
            }
            _procedures.value = cached
        }
    }

    private val _observations = MutableStateFlow<List<ObservationEntity>>(emptyList())
    val observations: StateFlow<List<ObservationEntity>> = _observations
    fun getObservations(subjectId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isObservationsLoading = true) }
            try {
                val observations = withContext(Dispatchers.IO) {
                    withLoggedAccess(
                        requesterId = uiState.value.walletAddress, //FOR now. Wallet address or medplum user id?
                        recordIds = listOf(subjectId),
                        resourcesType = "Observation/"
                    ) {
                        medPlumAPI.fetchObservations(subjectId)
                    }
                }
                observations?.let {
                    _observations.value = it
                    transactionDao.insertObservations(it)
                    updateHasFetched(OBSERVATIONS_KEY, true)
                }
                Log.d("ObservationsData", "Observations: $observations")
            } catch (e: Exception) {
                Log.e("Exams", "Error fetching", e)
            } finally {
                _uiState.update { it.copy(isObservationsLoading = false) }
            }
        }
    }

    fun loadObservationsFromDb() {
        viewModelScope.launch {
            val cached = withContext(Dispatchers.IO) {
                transactionDao.getObservations()
            }
            _observations.value = cached
        }
    }

    fun deleteAllDataFromDb() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                //transactionDao.deleteAllTransactions()
                transactionDao.deleteAllPatients()
                transactionDao.deleteAllObservations()
                transactionDao.deleteAllConditions()
                transactionDao.deleteAllDiagnosticReports()
                transactionDao.deleteAllMedicationRequests()
                transactionDao.deleteAllMedicationStatements()
                transactionDao.deleteAllImmunizations()
                transactionDao.deleteAllAllergies()
                transactionDao.deleteAllDevices()
                transactionDao.deleteAllProcedures()
                transactionDao.deleteAllPractitioners()
            }
            _patient.value = null
            _conditions.value = emptyList()
            _diagnosticReports.value = emptyList()
            _medicationRequests.value = emptyList()
            _medicationStatements.value = emptyList()
            _immunizations.value = emptyList()
            _allergies.value = emptyList()
            _devices.value = emptyList()
            _procedures.value = emptyList()
            _observations.value = emptyList()
            _practitioner.value = null
            walletRepository.updateLastAccessTime(HEALTH_SUMMARY_KEY, 0L) // Reset last access time for health summary
            Log.d("Logout", "Last access time: ${walletRepository.getLastAccessTime(HEALTH_SUMMARY_KEY)}")
            //_uiState.value = WalletUiState() // Reset UI state
            _uiState.update { it.copy(patientId = "", hasFetched = mapOf(
                "Patient" to false,
                "DiagnosticReports" to false,
                "Conditions" to false,
                "Observations" to false,
                "MedicationRequests" to false,
                "MedicationStatements" to false,
                "Immunizations" to false,
                "Allergies" to false,
                "Devices" to false,
                "Procedures" to false,
                "HealthSummary" to false,
                "Practitioner" to false
            ), isPatientLoading = false, isDiagnosticReportsLoading = false, isConditionsLoading = false,
                isObservationsLoading = false, isMedicationRequestsLoading = false, isMedicationStatementsLoading = false, isImmunizationsLoading = false, isAllergiesLoading = false, isDevicesLoading = false, isProceduresLoading = false, isHealthSummaryLoading = false, medPlumToken = false) }
        }
    }

    suspend fun giveConsent(){
        val success = medPlumAPI.createConsentResource(
            patientId = "abc123",
            practitionerId = "def456",
            resourceId = "DiagnosticReport/xyz789"
        )
    }

    suspend fun addPolicy(){
        val success = medPlumAPI.createAccessPolicy(
            name = "AllowDrJonesAccessToReport123",
            resourceType = "DiagnosticReport",
            resourceId = "xyz123",
            practitionerId = "def456"
        )
    }

    fun setShowDataDialog(show: Boolean) {
        updateUiState { it.copy(showDataDialog = show) }
    }

    fun setShowSyncSuccessDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showSyncSuccessDialog = show)
    }

    fun setShowSyncErrorDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showSyncErrorDialog = show)
    }

    fun setTransactionProcessing(isProcessing: Boolean) {
        updateUiState { it.copy(isTransactionProcessing = isProcessing) }
    }

    fun resetContractCounter() {
        val mnemonic = getMnemonic()
        viewModelScope.launch {
            setTransactionProcessing(true)
            try {
                if (!mnemonic.isNullOrEmpty()) {
                    val credentials = loadBip44Credentials(mnemonic)
                    credentials.let {
                        val hash = withContext(Dispatchers.IO) {
                            walletRepository.loadHealthyContract(credentials)
                        }
                    }
                    try {
                        val receipt = withContext(Dispatchers.IO) {
                            walletRepository.resetSyncPointer()
                        }
                        Log.d("Reset Pointer", "Reset Pointer with success: ${receipt.transactionHash}")
                        // Handle the result as needed
                        updateUiState { state ->
                            state.copy(
                                transactionHash = receipt.transactionHash,
                            )
                        }
                    } catch (e: Exception) {
                        // Handle errors
                        Log.e("Reset Pointer", "Exception caught", e)
                    }
                }
            } catch (e: Exception) {
                // Handle errors
                //updateUiState { it.copy(showPayDialog = false) }
                Log.d("Reset Pointer", "Error loading contract: ${e.message}")
            }
            setTransactionProcessing(false)
        }
    }


    fun callMedSkyContract() {
        val mnemonic = getMnemonic()
        viewModelScope.launch {
            try {
                if (!mnemonic.isNullOrEmpty()) {
                    val credentials = loadBip44Credentials(mnemonic)
                    credentials.let {
                        val hash = withContext(Dispatchers.IO) {
                            walletRepository.loadMedSkyContract(credentials)
                        }

                        /*updateUiState { state ->
                            state.copy(
                                transactionHash = hash.toString(),
                                showPayDialog = false,
                                showSuccessModal = true
                            )
                        }*/
                    }
                    try {
                        val exists = withContext(Dispatchers.IO) {
                            walletRepository.recordExists("sampleRecordId")
                        }
                        Log.d("MedskyContract", "Record exists: $exists")
                        // Handle the result as needed
                    } catch (e: Exception) {
                        // Handle errors
                        Log.e("MedskyContract", "Exception caught", e)
                    }
                }
            } catch (e: Exception) {
                // Handle errors
                //updateUiState { it.copy(showPayDialog = false) }
                Log.d("MedskyContract", "Error loading contract: ${e.message}")
            }
        }
    }

    fun setShowRecordDialog(show: Boolean) {
        updateUiState { it.copy(showRecordDialog = show) }
    }

    fun setShowDenyDialog(show: Boolean) {
        updateUiState { it.copy(showDenyDialog = show) }
    }

    /**
     * Updates the UI state with the provided function.
     *
     * @param update Function that defines the updates to the UI state.
     */
    private fun updateUiState(update: (WalletUiState) -> WalletUiState) {
        _uiState.update(update)
    }

    /**
     * Gets the stored mnemonic.
     *
     * @return The mnemonic or null if not available.
     */
    private fun getMnemonic(): String? {
        return walletRepository.getMnemonic()
    }

    /**
     * Updates the wallet address in the repository and UI state.
     *
     * @param walletAddress The wallet address to store.
     */
    fun storeWallet(walletAddress: String) {
        walletRepository.storeWallet(walletAddress)
        updateUiState { it.copy(walletAddress = walletAddress) }
    }

    fun storeMedPlumToken(token: String) {
        walletRepository.storeMedPlumToken(token)
        updateUiState { it.copy(medPlumToken = true) }
    }

    fun getMedPlumToken(): String {
        return walletRepository.getMedPlumToken()
    }
    suspend fun updateMedPlumToken() {
        if (walletRepository.isMedPlumTokenStored()) {
            val token = medPlumAPI.refreshAccessTokenIfNeeded()
            Log.d("MedplumAuth", "Token before login: $token")
            if (token != null) {
                updateUiState { it.copy(medPlumToken = true) }
            }
        }
    }

    /**
     * Updates the mnemonic in the repository and UI state.
     *
     * @param mnemonic The mnemonic to store.
     */
    fun storeMnemonic(mnemonic: String) {
        walletRepository.storeMnemonic(mnemonic)
        updateUiState { it.copy(mnemonicLoaded = true) }
    }

    /**
     * Loads the mnemonic from shared preferences and updates the UI state.
     */
    private fun loadMnemonicFromPrefs() {
        val storedMnemonic = walletRepository.loadMnemonicFromPrefs()
        updateUiState { it.copy(mnemonicLoaded = storedMnemonic != null) }
    }

    /**
     * Updates the token blocklist in the repository and UI state.
     *
     * @param token The token to add to the blocklist.
     */
    fun updateTokenBlockList(token:TokenBalance) {
        val updatedBlocklist = uiState.value.tokenBlocklist + token
        walletRepository.updateTokenBlockList(tokenBlocklist = updatedBlocklist)
        updateUiState { it.copy(tokenBlocklist = updatedBlocklist) }
    }

    /**
     * Loads the token blocklist from the repository and updates the UI state.
     */
    private fun getTokenBlocklist() {
        val blockList = walletRepository.getTokenBlocklist()
        updateUiState { it.copy(tokenBlocklist = blockList) }
    }

    /**
     * Fetches the balances for the wallet address and updates the UI state.
     */
    fun getBalances() {
        updateUiState { it.copy(isTokensLoading = true) }
        val walletAddress = uiState.value.walletAddress

        if (walletAddress.isEmpty()) return

        viewModelScope.launch {
            try {
                val (totalBalance, tokenBalances) = withContext(Dispatchers.IO) {
                    walletRepository.fetchBalances(walletAddress, 101)
                }

                updateUiState {
                    it.copy(
                        totalBalanceUSD = totalBalance,
                        tokens = tokenBalances,
                        selectedToken = tokenBalances.firstOrNull(),
                        isTokensLoading = false
                    )
                }
            } catch (e: Exception) {
                // Handle errors
                updateUiState { it.copy(isTokensLoading = false) }
            }
        }
    }

    fun getNftBalances() {
        updateUiState { it.copy(isNftsLoading = true) }
        val walletAddress = uiState.value.walletAddress

        if (walletAddress.isEmpty()) return

        viewModelScope.launch {
            try {
                val nftBalances = withContext(Dispatchers.IO) {
                    walletRepository.fetchNfts(walletAddress, uiState.value.selectedNetwork)
                }

                updateUiState { it.copy(nfts = nftBalances, isNftsLoading = false) }
            } catch (e: Exception) {
                // Handle errors
                updateUiState { it.copy(isNftsLoading = false) }
            }
        }
    }

    /**
     * Removes all wallet data from the repository and resets the UI state.
     */
    fun removeAllWalletData() {
        walletRepository.removeAllWalletData()
        updateUiState {
            WalletUiState() // Reset to default state
        }
    }

    /**
     * Clears the token blocklist in the repository and updates the UI state.
     */
    fun clearTokenBlocklist() {
        val emptyBlocklist = walletRepository.clearTokenBlocklist()
        updateUiState { it.copy(tokenBlocklist = emptyBlocklist) }
    }

    fun updateSelectedNetwork(network: Network) {
        val updatedNetwork = walletRepository.updateSelectedNetwork(network)
        updateUiState { it.copy(selectedNetwork = updatedNetwork) }
        // Refresh balances when network changes
        getBalances()
        getNftBalances()
    }

    fun updateSelectedToken(token: TokenBalance?) {
        updateUiState { it.copy(selectedToken = token) }
    }

    /**
     * Sets the ShowPayDialog state in the UI.
     *
     * @param show Boolean indicating whether to show the pay dialog.
     */
    fun setShowPayDialog(show: Boolean) {
        updateUiState { it.copy(showPayDialog = show) }
    }

    /**
     * Sets the ShowTokenBottomSheet state in the UI.
     *
     * @param show Boolean indicating whether to show the token bottom sheet.
     */
    fun setShowTokenBottomSheet(show: Boolean) {
        updateUiState { it.copy(showTokenBottomSheet = show) }
    }

    /**
     * Sets the ShowWalletModal state in the UI.
     *
     * @param show Boolean indicating whether to show the wallet modal.
     */
    fun setShowWalletModal(show: Boolean) {
        updateUiState { it.copy(showWalletModal = show) }
    }

    /**
     * Sets the HashValue in the UI.
     *
     * @param value The hash value to set.
     */
    fun setHashValue(value: String) {
        updateUiState { it.copy(hash = value) }
    }

    /**
     * Sets the ShowSuccessModal state in the UI.
     *
     * @param show Boolean indicating whether to show the success modal.
     */
    fun setShowSuccessModal(show: Boolean) {
        updateUiState { it.copy(showSuccessModal = show) }
    }

    /**
     * Handles the confirmation of a payment.
     *
     * @param address The address to send the payment to.
     * @param amount The amount to send.
     * @param contractAddress The contract address of the token.
     */
    fun onPayConfirmed(address: String, amount: Double, contractAddress: String) {
        val mnemonic = getMnemonic()

        updateUiState {
            it.copy(
                sentAmount = amount,
                sentCurrency = it.selectedToken?.symbol ?: ""
            )
        }

        viewModelScope.launch {
            try {
                val resolvedAddress = withContext(Dispatchers.IO) {
                    ensResolver(address)
                }

                updateUiState { it.copy(toAddress = resolvedAddress) }

                if (!mnemonic.isNullOrEmpty()) {
                    val credentials = loadBip44Credentials(mnemonic)
                    credentials.let {
                        val hash = withContext(Dispatchers.IO) {
                            walletRepository.sendTokens(
                                credentials,
                                contractAddress,
                                resolvedAddress,
                                BigDecimal.valueOf(amount)
                            )
                        }

                        updateUiState { state ->
                            state.copy(
                                transactionHash = hash,
                                showPayDialog = false,
                                showSuccessModal = true
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                // Handle errors
                updateUiState { it.copy(showPayDialog = false) }
            }
        }
    }

    /**
     * Checks for an ENS name for the given wallet address and updates the UI state.
     *
     * @param walletAddress The wallet address to check for an ENS name.
     */
    private fun checkForEnsName(walletAddress: String?) {
        if (walletAddress.isNullOrEmpty()) return

        viewModelScope.launch {
            try {
                val ens = withContext(Dispatchers.IO) {
                    addressToEnsResolver(walletAddress)
                }

                updateUiState { it.copy(userEnsName = ens) }
            } catch (e: Exception) {
                // Handle errors
            }
        }
    }

    /**
     * Gets the next notification ID and increments the counter.
     *
     * @return The next notification ID.
     */
    fun getAndIncrementNotificationId(): Int {
        return nextNotificationId++
    }

    /**
     * Gets the next transaction ID and increments the counter.
     *
     * @return The next transaction ID.
     */
    suspend fun getTransactionId(): Int { //TODO: Move to repository (SharedPreferences)
        return transactionDao.countTransactions() + 1
    }

    fun getDiagnosticReportById(recordId: String): DiagnosticReportEntity? {
        return runBlocking {
            transactionDao.getDiagnosticReportById(recordId)
        }
    }
    fun getMedicationRequestById(recordId: String): MedicationRequestEntity? {
        return runBlocking {
            transactionDao.getMedicationRequestById(recordId)
        }
    }
    fun getObservationById(recordId: String): ObservationEntity? {
        return runBlocking {
            transactionDao.getObservationById(recordId)
        }
    }
    fun getConditionById(recordId: String): ConditionEntity? {
        return runBlocking {
            transactionDao.getConditionById(recordId)
        }
    }
    fun getAllergyById(recordId: String): AllergyIntoleranceEntity? {
        return runBlocking {
            transactionDao.getAllergyById(recordId)
        }
    }
    fun getDeviceById(recordId: String): DeviceEntity? {
        return runBlocking {
            transactionDao.getDeviceById(recordId)
        }
    }
    fun getProcedureById(recordId: String): ProcedureEntity? {
        return runBlocking {
            transactionDao.getProcedureById(recordId)
        }
    }
    fun getImmunizationById(recordId: String): ImmunizationEntity? {
        return runBlocking {
            transactionDao.getImmunizationById(recordId)
        }
    }
    fun getMedicationStatementById(recordId: String): MedicationStatementEntity? {
        return runBlocking {
            transactionDao.getMedicationStatementById(recordId)
        }
    }

    fun <T> getResource(recordType: String, recordId: String): T? {
        when (recordType) {
            "DiagnosticReport" -> {
                if (!uiState.value.hasFetched.getOrDefault(DIAGNOSTIC_REPORTS_KEY, false)) {
                    getDiagnosticReports()
                }
                return getDiagnosticReportById(recordId) as? T
            }
            "MedicationRequest" -> {
                if (!uiState.value.hasFetched.getOrDefault(MEDICATION_REQUESTS_KEY, false)) {
                    getMedicationRequests()
                }
                return getMedicationRequestById(recordId) as? T
            }
            "MedicationStatement" -> {
                if (!uiState.value.hasFetched.getOrDefault(MEDICATION_STATEMENTS_KEY, false)) {
                    getMedicationStatements()
                }
                return getMedicationStatementById(recordId) as? T
            }
            "Immunization" -> {
                if (!uiState.value.hasFetched.getOrDefault(IMMUNIZATIONS_KEY, false)) {
                    getImmunizations()
                }
                return getImmunizationById(recordId) as? T
            }
            "AllergyIntolerance" -> {
                if (!uiState.value.hasFetched.getOrDefault(ALLERGIES_KEY, false)) {
                    getAllergies()
                }
                return getAllergyById(recordId) as? T
            }
            "Device" -> {
                if (!uiState.value.hasFetched.getOrDefault(DEVICES_KEY, false)) {
                    getDevices()
                }
                return getDeviceById(recordId) as? T
            }
            "Procedure" -> {
                if (!uiState.value.hasFetched.getOrDefault(PROCEDURES_KEY, false)) {
                    getProcedures()
                }
                return getProcedureById(recordId) as? T
            }
            "Observation" -> {
                if (!uiState.value.hasFetched.getOrDefault(OBSERVATIONS_KEY, false)) {
                    getObservations(getLoggedInUsertId())
                }
                return getObservationById(recordId) as? T
            }
            else -> return null
        }
    }

    suspend fun requestAccess(recordId: String, recordType: String) {
        try {
            val mnemonic = getMnemonic()
            if (!mnemonic.isNullOrEmpty()) {
                val credentials = loadBip44Credentials(mnemonic)
                credentials.let {
                    val hash = withContext(Dispatchers.IO) {
                        walletRepository.loadHealthyContract(credentials)
                    }
                }
                try {
                    val receipt = withContext(Dispatchers.IO) {
                        walletRepository.requestAccess(
                            uiState.value.walletAddress,
                            uiState.value.walletAddress,
                            getLoggedInUsertId().removePrefix("Practitioner/"),
                            recordId,
                            recordType,
                            credentials
                        )
                    }
                    val gasPriceHex = receipt.effectiveGasPrice

                    val gasPrice = Numeric.decodeQuantity(
                        if (gasPriceHex.startsWith("0x")) gasPriceHex else "0x$gasPriceHex"
                    )
                    val gasUsed = receipt.gasUsed
                    val gasFee = gasUsed * gasPrice
                    Log.d(
                        "RequestAccess",
                        "Gas fee: $gasFee, Gas used: $gasUsed, Gas price: $gasPrice"
                    )
                    simulateTransactionFees.add(gasFee)
                    Log.d("RequestAccess", "Access requested: ${receipt.transactionHash}")
                } catch (e: Exception) {
                    Log.e("RequestAccess", "Exception caught", e)
                }
            }
        } catch (e: Exception) {
            Log.d("RequestAccess", "Error loading contract: ${e.message}")
        }
    }

    /**
     * Handles the notification received for a transaction.
     *
     * @param transaction The transaction associated with the notification.
     */
    val simulateTransactionTimes = mutableListOf<Long>()
    val simulateTransactionFees = mutableListOf<BigInteger>()
    suspend fun onNotificationReceived(context: Context, id: String) {
        var type = ""
        var i = getTransactionId()
        if ( i % 3 == 0) {
            type = "Head CT"
        }
        else if (i % 3 == 1) {
            type = "Blood Test"
        } else {
            type = "MRI"
        }
        var newTransaction = Transaction(
            id = id,
            date = getCurrentDate(),
            status = "pending",
            recordId = "local$id",
            practitionerId = "01968b55-08af-70ce-8159-23b14e09a48a",
            practitionerAddress = "0xd0c4753de208449772e0a7e43f7ecda79df32bc7",
            type = type,
            patientId = "01968b59-76f3-7228-aea9-07db748ee2ca"
        )
        val mnemonic = getMnemonic()
        val duration = withContext(Dispatchers.IO) {
            val start = System.currentTimeMillis()
            try {
                if (!mnemonic.isNullOrEmpty()) {
                    val credentials = loadBip44Credentials(mnemonic)
                    credentials.let {
                        val hash = withContext(Dispatchers.IO) {
                            walletRepository.loadHealthyContract(credentials)
                        }
                    }
                    try {
                        val receipt = withContext(Dispatchers.IO) {
                            walletRepository.requestAccess(newTransaction.practitionerAddress, uiState.value.walletAddress, newTransaction.practitionerId, newTransaction.recordId, newTransaction.type, credentials)
                        }
                        val gasPriceHex = receipt.effectiveGasPrice

                        val gasPrice = Numeric.decodeQuantity(
                            if (gasPriceHex.startsWith("0x")) gasPriceHex else "0x$gasPriceHex"
                        )
                        val gasUsed = receipt.gasUsed
                        val gasFee = gasUsed * gasPrice
                        Log.d("RequestAccess", "Gas fee: $gasFee, Gas used: $gasUsed, Gas price: $gasPrice")
                        simulateTransactionFees.add(gasFee)
                        Log.d("RequestAccess", "Access requested: ${receipt.transactionHash}")
                        updateUiState { state ->
                            state.copy(
                                transactionHash = receipt.transactionHash,
                                showRecordDialog = true,
                            )
                        }
                        addTransaction(newTransaction)
                        sendNotification(context, this@WalletViewModel, "New Transaction", "You have a new transaction pending. With ID: $id", id)
                    } catch (e: Exception) {
                        // Handle errors
                        Log.e("RequestAccess", "Exception caught", e)
                    }
                }
            } catch (e: Exception) {
                // Handle errors
                //updateUiState { it.copy(showPayDialog = false) }
                Log.d("RequestAccess", "Error loading contract: ${e.message}")
            }
            System.currentTimeMillis() - start
        }
        simulateTransactionTimes.add(duration)
    }

    fun getSimulateTransactionFees() {
        Log.d("GasStats", "Gas fees: $simulateTransactionFees")
        if (!simulateTransactionFees.isEmpty()) {
            val meanGasFee = simulateTransactionFees.reduce(BigInteger::add).divide(BigInteger.valueOf(simulateTransactionFees.size.toLong()))
            Log.d("GasStats", "Mean gas fee: $meanGasFee")

        }
    }

    fun showSimulateTransactionTimes() {
        Log.d("SimulateTransactionTimes", "Transaction times: $simulateTransactionTimes")
    }

    suspend fun handleZkpRequestJson(json: String) {
        val payload = Json.decodeFromString<ZkpExamRequestPayload>(json)

        val transaction = Transaction(
            id = getTransactionId().toString(),
            date = getCurrentDate(),
            status = "pending",
            recordId = payload.requestId,
            practitionerId = payload.issuer, // or other value if needed
            practitionerAddress = payload.issuer, //TODO: check
            type = payload.examType,
            patientId = uiState.value.walletAddress,
            conditions = payload.conditions
        )

        addTransaction(transaction)
    }


    /**
     * Gets the current date in the format "yyyy-MM-dd".
     *
     * @return The current date as a string.
     */
    fun getCurrentDate(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return dateFormat.format(Date())
    }

    /**
     * Gets a transaction by its ID.
     *
     * @param transactionId The ID of the transaction to retrieve.
     * @return The transaction with the specified ID, or null if not found.
     */
    suspend fun getTransactionById(transactionId: String): Transaction? {
        return withContext(Dispatchers.IO) {
            transactionDao.getTransactionWithProofById(transactionId)?.toTransaction()
        }
    }

    suspend fun getPatientById(patientId: String): PatientEntity? {
        return withContext(Dispatchers.IO) {
            transactionDao.getPatientById(patientId)
        }
    }

    /**
     * Gets all transactions from the database.
     *
     * @return A list of all transactions.
     */
    fun getTransactions(): List<Transaction> {
        updateTransactions()
        return uiState.value.transactions
    }

    fun updateTransactions() {
        viewModelScope.launch {
            val transactions = withContext(Dispatchers.IO) {
                transactionDao.getTransactionsWithProof().map { it.toTransaction() }
            }
            updateUiState { it.copy(transactions = transactions) }
            Log.d("ExampleTestSample", "getTransactions: $transactions")
        }
    }

    /**
     * Adds a transaction to the UI state.
     *
     * @param transaction The transaction to add.
     */
    fun addTransactionOld(transaction: Transaction) {
        val updatedTransactions = uiState.value.transactions + transaction
        updateUiState { it.copy(transactions = updatedTransactions) }
    }

    fun addTransaction(transaction: Transaction) {
        val transactionEntity = TransactionEntity(
            id = transaction.id,
            date = transaction.date,
            status = transaction.status,
            type = transaction.type,
            recordId = transaction.recordId,
            patientId = transaction.patientId,
            practitionerAddress = transaction.practitionerAddress,
            practitionerId = transaction.practitionerId,
            documentReferenceId = "",
            medicationRequestId = "",
            conditionId = "",
            encounterId = "",
            observationId = "",
            conditionsJson = Converters.fromConditionsList(transaction.conditions)
        )
        val zkpEntity = transaction.conditions?.let {
            ZkpEntity(
                id = transaction.id,
                conditionsJson = Converters.fromConditionsList(it).toString(),
                qrCodeFileName = "" // Or set later when available
            )
        }
        viewModelScope.launch {
            var wasInserted = false
            withContext(Dispatchers.IO) {
                if (transactionDao.transactionExists(transaction.id) == 0) {
                    Log.d("ExampleTestSample", "Adding transaction: ${transactionEntity.id}")
                    transactionDao.insertTransaction(transactionEntity)
                    zkpEntity?.let { transactionDao.insertZkpTransaction(it) }
                    wasInserted = true
                } else {
                    Log.d("ExampleTestSample", "Transaction already exists: ${transactionEntity.id}")
                }
            }

            if (wasInserted) {
                // Fetch just the new transaction and add it to UI state
                val insertedTransaction = withContext(Dispatchers.IO) {
                    transactionDao.getTransactionWithProofById(transaction.id)?.toTransaction()
                }
                insertedTransaction?.let { newTx ->
                    updateUiState { state ->
                        state.copy(transactions = state.transactions + newTx)
                    }
                }
            }
        }
    }

    /**
     * Adds a list of transactions to the UI state.
     *
     * @param transactions The list of transactions to add.
     */
    fun addTransactions(transactions: List<Transaction>) {
        //val updatedTransactions = uiState.value.transactions + transactions
        //updateUiState { it.copy(transactions = updatedTransactions) }
        transactions.forEach { addTransaction(it) }
    }

    /**
     * Updates the status of a transaction in the UI state.
     *
     * @param transactionId The ID of the transaction to update.
     * @param newStatus The new status to set for the transaction.
     */
    fun updateTransactionStatusOld(transactionId: String, newStatus: String) {
        val updatedTransactions = uiState.value.transactions.map { transaction ->
            if (transaction.id == transactionId) {
                transaction.copy(status = newStatus)
            } else {
                transaction
            }
        }
        updateUiState { it.copy(transactions = updatedTransactions) }
    }
    fun updateTransactionStatus(transactionId: String, newStatus: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                transactionDao.updateTransactionStatus(transactionId, newStatus)
                val updatedEntity = transactionDao.getById(transactionId)
                val updated = updatedEntity?.toTransaction()
                if (updated != null) {
                    withContext(Dispatchers.Main) {
                        updateUiState { state ->
                            val updatedList = state.transactions.map {
                                if (it.id == transactionId) updated else it
                            }
                            state.copy(transactions = updatedList)
                        }
                    }
                }
            }
        }
    }

    fun updateTransactionQrCode(transactionId: String, qrCodeFileName: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                transactionDao.updateZkpQrCode(transactionId, qrCodeFileName)
                val updated = transactionDao.getTransactionWithProofById(transactionId)?.toTransaction()
                if (updated != null) {
                    withContext(Dispatchers.Main) {
                        updateUiState { state ->
                            val updatedList = state.transactions.map {
                                if (it.id == transactionId) updated else it
                            }
                            state.copy(transactions = updatedList)
                        }
                    }
                }
            }
        }
    }


    fun deleteAllTransactions() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                transactionDao.deleteAllTransactions()
            }
            updateUiState { it.copy(transactions = emptyList()) }
        }
    }
}

