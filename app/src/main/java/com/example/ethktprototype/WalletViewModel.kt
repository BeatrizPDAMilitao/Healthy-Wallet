package com.example.ethktprototype

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ethktprototype.data.AppDatabase
import com.example.ethktprototype.data.TokenBalance
import com.example.ethktprototype.data.Transaction
import com.example.ethktprototype.data.TransactionEntity
import com.example.ethktprototype.data.toTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import utils.addressToEnsResolver
import utils.ensResolver
import utils.loadBip44Credentials
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


/**
 * ViewModel to manage the wallet state.
 *
 * @param application The application associated with this ViewModel.
 */

class WalletViewModel(application: Application) : AndroidViewModel(application) {
    private val transactionDao = AppDatabase.getDatabase(application).transactionDao()

    private val walletRepository = WalletRepository(application)
    private val sharedPreferences =
        application.getSharedPreferences("WalletPrefs", Context.MODE_PRIVATE)
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

        //Add sample transactions for testing
        //addSampleTransactions()
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

    fun callDenyContract(recordId: String, requester: String) {
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
                            walletRepository.denyAccess(recordId, requester, credentials)
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
                        updateTransactionStatus(recordId, "denied")
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

    fun callAcceptContract(recordId: String, requester: String) {
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
                            walletRepository.acceptAccess(recordId, requester, credentials)
                        }
                        Log.d("AcceptContract", "Access given: ${receipt.transactionHash}")
                        updateUiState { state ->
                            state.copy(
                                transactionHash = receipt.transactionHash,
                                showPayDialog = false,
                                showDenyDialog = true,
                                showSuccessModal = false,
                                showWalletModal = false,
                            )
                        }
                        updateTransactionStatus(recordId, "accepted")
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
            try {
                if (!mnemonic.isNullOrEmpty()) {
                    val credentials = loadBip44Credentials(mnemonic)

                    withContext(Dispatchers.IO) {
                        walletRepository.loadHealthyContract(credentials)
                    }
                    Log.d("SyncedLog", "Before syncTransactionWithHealthyContract")
                    val logs = withContext(Dispatchers.IO) {
                        walletRepository.syncTransactionWithHealthyContract(credentials)
                    }
                    Log.d("SyncedLog", "Logs=${logs.size}")
                    logs.forEach { log ->
                        val transactionId = log.timestamp.toString() + "-" + log.doctor.takeLast(6)
                        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(log.timestamp.toLong() * 1000))
                        Log.d("SyncedLog", "Doctor=${log.doctor}, Patient=${log.patient}, Type=${log.recordType}, Timestamp=${log.timestamp}")


                        val transaction = TransactionEntity(
                            id = transactionId,
                            date = date,
                            status = "accepted",
                            type = log.recordType,
                            patientId = log.patient,
                            practitionerId = log.doctor,
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
        }
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
    fun onNotificationReceived(transaction: Transaction) {
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
            transactionDao.getById(transactionId)?.toTransaction()
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
                transactionDao.getAllTransactions().map { it.toTransaction() }
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
            patientId = "",
            practitionerId = transaction.practitionerId,
            documentReferenceId = "",
            medicationRequestId = "",
            conditionId = "",
            encounterId = "",
            observationId = ""
        )
        viewModelScope.launch {
            if (transactionDao.transactionExists(transaction.id) == 0) {
                Log.d("ExampleTestSample", "Adding transaction: ${transactionEntity.id}")
                transactionDao.insertTransaction(transactionEntity)
            } else {
                // Handle the case where the transaction already exists, if needed
                Log.d("ExampleTestSample", "Transaction already exists: ${transactionEntity.id}")
            }
            val updatedTransactions = transactionDao.getAllTransactions()
            updateUiState { it.copy(transactions = updatedTransactions.map { it.toTransaction() }) }
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
     * Adds sample transactions to the UI state for testing purposes.
     */
    fun addSampleTransactions() {
        val sampleTransactions = listOf(
            Transaction(id = "122", date = "2023-10-01", status = "accepted", practitionerId = "123", type = "MRI"),
            Transaction(id = "222", date = "2023-10-02", status = "pending", practitionerId = "456", type = "X-Ray"),
            Transaction(id = "322", date = "2023-10-03", status = "denied", practitionerId = "789", type = "Blood Test"),
        )
        viewModelScope.launch {
            sampleTransactions.forEach { transaction ->
                addTransaction(transaction)
            }
        }
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
                val updatedTransactions = transactionDao.getAllTransactions().map { it.toTransaction() }
                withContext(Dispatchers.Main) {
                    updateUiState { it.copy(transactions = updatedTransactions) }
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

