package com.example.ethktprototype

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
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
    private val sharedPreferencesAuth =
        application.getSharedPreferences("auth", Context.MODE_PRIVATE)
    private val walletAddressKey = "wallet_address"

    private val _uiState = MutableStateFlow(WalletUiState())
    val uiState: StateFlow<WalletUiState> = _uiState.asStateFlow()

    private var nextTransactionId = 1
    private var nextNotificationId = 1

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
            loadMedicationRequestsFromDb()
            loadMedicationStatementsFromDb()
            loadImmunizationsFromDb()
            loadAllergiesFromDb()
            loadDevicesFromDb()
            loadProceduresFromDb()
        }

        //Add sample transactions for testing
        //addSampleTransactions()
    }

    fun updatePatientIdUiState() {
        updateUiState {
            it.copy(patientId = sharedPreferencesAuth.getString("user_profile", null).toString())
        }
    }
    fun deletePatientIdUiState() {
        updateUiState {
            it.copy(patientId = "")
        }
    }

    fun getLoggedInPatientId(): String {
        Log.d("MedplumAuth", "Patient ID: ${uiState.value.patientId}")
        return sharedPreferencesAuth.getString("user_profile", null).toString()
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

    fun callAcceptContract(transactionId: String, recordId: String, requester: String) {
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

            fetchOperation()
        } catch (e: Exception) {
            Log.e("AccessControl", "Access log or fetch failed: ${e.message}", e)
            null
        }
    }


    //////////////////// MedPlum API Tests with and without blockchain ////////////////////

    fun testFetchPrescriptions(subjectId: String) {
        viewModelScope.launch {
            val withBlockchainTimes = mutableListOf<Long>()
            val withoutBlockchainTimes = mutableListOf<Long>()

            // Measure with blockchain
            for (i in 1..20) {
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
            for (i in 1..20) {
                val duration = withContext(Dispatchers.IO) {
                    val start = System.currentTimeMillis()
                    getMedicationRequestsSuspend(subjectId)
                    System.currentTimeMillis() - start
                }
                withoutBlockchainTimes.add(duration)
                Log.d("PerformanceTest", "Without blockchain [$i]: $duration ms")
                delay(2000)
            }

            val avgWith = withBlockchainTimes.average()
            val avgWithout = withoutBlockchainTimes.average()

            Log.d("PerformanceTest", "Average WITH blockchain: ${"%.2f".format(avgWith)} ms")
            Log.d("PerformanceTest", "Average WITHOUT blockchain: ${"%.2f".format(avgWithout)} ms")
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

    private val _patient = MutableStateFlow<PatientEntity?>(null)
    val patient: StateFlow<PatientEntity?> = _patient
    fun getPatientComplete() {
        val patientId = getLoggedInPatientId().removePrefix("Patient/")
        Log.d("MedplumAuth", "Patient ID: $patientId")
        viewModelScope.launch {

            val patientData = withContext(Dispatchers.IO) {
                withLoggedAccess(
                    requesterId = uiState.value.walletAddress, //FOR now. Wallet address or medplum user id?
                    recordIds = listOf(patientId),
                    resourcesType = "Patient/"
                ) {
                    medPlumAPI.fetchPatientComplete(patientId)
                }
            }

            if (patientData != null) {
                _patient.value = patientData
                transactionDao.insertPatient(patientData)
            } else {
                Log.e("MedplumAuth", "Access denied or failed")
            }
        }
    }

    fun loadPatientFromDb() {
        viewModelScope.launch {
            val cached = withContext(Dispatchers.IO) {
                transactionDao.getPatientById("019706de-81bf-77d0-a864-2db46cad1d8c")
            }
            _patient.value = cached
        }
    }

    //TODO: Change to be like the others
    fun getPractitionerData(practitionerId: String) {
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

    private val _conditions = MutableStateFlow<List<ConditionEntity>>(emptyList())
    val conditions: StateFlow<List<ConditionEntity>> = _conditions
    fun getConditions(subjectId: String) {
        viewModelScope.launch {
            try {
                val conditions = withContext(Dispatchers.IO) {
                    medPlumAPI.fetchConditions(subjectId)
                }
                conditions?.let {
                    _conditions.value = it
                    transactionDao.insertConditions(it)
                }
                Log.d("ConditionsData", "Conditions: $conditions")
            } catch (e: Exception) {
                // Handle errors
                Log.e("ConditionsData", "Error fetching ConditionsData : ${e.message}", e)
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
    fun getDiagnosticReports() {
        val subjectId = getLoggedInPatientId()
        viewModelScope.launch {
            try {
                val reports = withContext(Dispatchers.IO) {
                    medPlumAPI.fetchDiagnosticReports(subjectId)
                }
                reports?.let {
                    _diagnosticReports.value = it
                    transactionDao.insertDiagnosticReports(it)
                }
                Log.d("DiagnosticReportsData", "Diagnostic Reports: $reports")
            } catch (e: Exception) {
                Log.e("DiagnosticReportsData", "Error fetching diagnostic reports: ${e.message}", e)
                null
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
        val subjectId = getLoggedInPatientId()
        viewModelScope.launch {
            try {
                val requests = withContext(Dispatchers.IO) {
                    medPlumAPI.fetchMedicationRequests(subjectId)
                }
                requests?.let {
                    _medicationRequests.value = it
                    transactionDao.insertMedicationRequests(it)
                }
                Log.d("MedicationRequestsData", "Medication Requests: $requests")
            } catch (e: Exception) {
                Log.e("MedicationRequestsData", "Error fetching medication requests: ${e.message}", e)
                null
            }
        }
    }

    suspend fun getMedicationRequestsSuspend(subjectId: String): List<MedicationRequestEntity>? {
        return withContext(Dispatchers.IO) {
            medPlumAPI.fetchMedicationRequests(subjectId)
        }
    }

    suspend fun getMedicationRequestsWithBlockchain(subjectId: String) {
        Log.d("getMedicationRequestsWithBlockchain", "Subject ID: $subjectId")
        //viewModelScope.launch {

            val requests = withContext(Dispatchers.IO) {
                withLoggedAccess(
                    requesterId = uiState.value.walletAddress, //FOR now. Wallet address or medplum user id?
                    recordIds = listOf(subjectId),
                    resourcesType = "Patient/"
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
        val subjectId = getLoggedInPatientId()
        viewModelScope.launch {
            try {
                val statements = withContext(Dispatchers.IO) {
                    medPlumAPI.fetchMedicationStatements(subjectId)
                }
                statements?.let {
                    _medicationStatements.value = it
                    transactionDao.insertMedicationStatements(it)
                }
                Log.d("MedicationStatementsData", "Medication Statements: $statements")
            } catch (e: Exception) {
                Log.e("MedicationStatementsData", "Error fetching medication statements: ${e.message}", e)
                null
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
        val subjectId = getLoggedInPatientId()
        viewModelScope.launch {
            try {
                val immunizations = withContext(Dispatchers.IO) {
                    medPlumAPI.fetchImmunizations(subjectId)
                }
                immunizations?.let {
                    _immunizations.value = it
                    transactionDao.insertImmunizations(it)
                }
                Log.d("ImmunizationsData", "Medication Requests: $immunizations")
            } catch (e: Exception) {
                Log.e("ImmunizationsData", "Error fetching immunizations: ${e.message}", e)
                null
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
        val subjectId = getLoggedInPatientId()
        viewModelScope.launch {
            try {
                val allergies = withContext(Dispatchers.IO) {
                    medPlumAPI.fetchAllergies(subjectId)
                }
                allergies?.let {
                    _allergies.value = it
                    transactionDao.insertAllergies(it)
                }
                Log.d("AllergiesData", "Allergies: $allergies")
            } catch (e: Exception) {
                Log.e("AllergiesData", "Error fetching allergies: ${e.message}", e)
                null
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
        val subjectId = getLoggedInPatientId()
        viewModelScope.launch {
            try {
                val devices = withContext(Dispatchers.IO) {
                    medPlumAPI.fetchDevices(subjectId)
                }
                devices?.let {
                    _devices.value = it
                    transactionDao.insertDevices(it)
                }
                Log.d("DevicesData", "Devices: $devices")
            } catch (e: Exception) {
                Log.e("DevicesData", "Error fetching devices: ${e.message}", e)
                null
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
        val subjectId = getLoggedInPatientId()
        viewModelScope.launch {
            try {
                val procedures = withContext(Dispatchers.IO) {
                    medPlumAPI.fetchProcedures(subjectId)
                }
                procedures?.let {
                    _procedures.value = it
                    transactionDao.insertProcedures(it)
                }
                Log.d("ProceduresData", "Procedures: $procedures")
            } catch (e: Exception) {
                Log.e("ProceduresData", "Error fetching procedures: ${e.message}", e)
                null
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
            try {
                val observations = withContext(Dispatchers.IO) {
                    medPlumAPI.fetchObservations(subjectId)
                }
                observations?.let {
                    _observations.value = it
                    transactionDao.insertObservations(it)
                }
                Log.d("ObservationsData", "Observations: $observations")
            } catch (e: Exception) {
                Log.e("ObservationsData", "Error fetching observations: ${e.message}", e)
                null
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

    /**
     * Handles the notification received for a transaction.
     *
     * @param transaction The transaction associated with the notification.
     */
    suspend fun onNotificationReceived(context: Context, id: String) {
        var newTransaction = Transaction(
            id = id,
            date = getCurrentDate(),
            status = "pending",
            recordId = "local$id",
            practitionerId = "dsa987654321",
            practitionerAddress = "0xd0c4753de208449772e0a7e43f7ecda79df32bc7",
            type = "Head CT",
            patientId = "01968b59-76f3-7228-aea9-07db748ee2ca"
        )
        val mnemonic = getMnemonic()
        viewModelScope.launch {
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
        }
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

