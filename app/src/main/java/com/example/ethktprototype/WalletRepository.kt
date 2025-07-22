package com.example.ethktprototype

import ERC20
import android.app.Application
import android.content.Context
import android.util.Base64
import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.edit
import androidx.lifecycle.MutableLiveData
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.ethktprototype.contracts.MedskyContract
import com.example.ethktprototype.data.GraphQLData
import com.example.ethktprototype.data.GraphQLQueries
import com.example.ethktprototype.data.GraphQLResponse
import com.example.ethktprototype.data.NftValue
import com.example.ethktprototype.data.TokenBalance
//import io.sentry.Sentry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import org.web3j.abi.FunctionEncoder
import org.web3j.abi.datatypes.Address
import org.web3j.abi.datatypes.Function
import org.web3j.abi.datatypes.generated.Uint256
import org.web3j.crypto.Credentials
import org.web3j.crypto.RawTransaction
import org.web3j.crypto.TransactionEncoder
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.core.methods.request.Transaction
import org.web3j.protocol.core.methods.response.TransactionReceipt
import org.web3j.tx.Transfer
import org.web3j.tx.gas.DefaultGasProvider
import org.web3j.utils.Convert
import org.web3j.utils.Numeric
import java.math.BigDecimal
import java.math.BigInteger
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.delay
import com.example.ethktprototype.contracts.MedicalRecordAccess2
import com.example.ethktprototype.contracts.RecordAccessContract

import org.web3j.abi.datatypes.DynamicArray
import org.web3j.abi.datatypes.Utf8String
import utils.loadBip44Credentials
import java.security.SecureRandom
import java.util.UUID
import kotlin.collections.List


object JsonUtils {
    val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        isLenient = true
    }
}


class WalletRepository(private val application: Application) : IWalletRepository {
    private val context = application.applicationContext
    private val sharedPreferences =
        context.getSharedPreferences("WalletPrefs", Context.MODE_PRIVATE)
    private val walletAddressKey = "wallet_address"
    private val _selectedNetwork = mutableStateOf(Network.ARBITRUM_SEPOLIA_TESTNET)
    private val selectedNetwork: MutableState<Network> = _selectedNetwork
    private val mnemonicLoaded = MutableLiveData<Boolean>()

    val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    val encryptedPrefs = EncryptedSharedPreferences.create(
        context,
        "secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    val web3jService = Web3jService.build(selectedNetwork.value)

    val nonceManager = NonceManager(web3jService, loadBip44Credentials(getMnemonic().toString()).address)


    fun getDbPassphrase(): String? {
        return encryptedPrefs.getString("db_pass", null)
    }

    fun generateAndStorePassphrase(): String {
        var passPhrase = getDbPassphrase()
        if (passPhrase != null) {
            return passPhrase
        }
        passPhrase = generateSecurePassphrase()
        encryptedPrefs.edit {
            putString("db_pass", passPhrase)
        }
        return passPhrase
    }

    fun generateSecurePassphrase(): String {
        val bytes = ByteArray(32)
        SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    fun storeInEncryptedPrefs(key: String, value: String) {
        encryptedPrefs.edit {
            putString(key, value)
        }
    }

    fun getFromEncryptedPrefs(key: String): String? {
        return encryptedPrefs.getString(key, null)
    }

    fun hasEncryptedMnemonic(): Boolean {
        return getFromEncryptedPrefs("encrypted_mnemonic") != null
    }

    override fun storeMnemonic(mnemonic: String) {
        /*encryptMnemonic(context, mnemonic)

        val sharedPreferences = context.getSharedPreferences("WalletPrefs", Context.MODE_PRIVATE)
        val encryptedMnemonic = getEncryptedMnemonic(context)
        val encodedMnemonic = Base64.encodeToString(encryptedMnemonic, Base64.DEFAULT)
        sharedPreferences.edit().putString("encrypted_mnemonic", encodedMnemonic).apply()*/
        storeInEncryptedPrefs("encrypted_mnemonic", mnemonic)

        mnemonicLoaded.value = true
    }

    override fun loadMnemonicFromPrefs(): String? {
        /*val prefs = context.getSharedPreferences("WalletPrefs", Context.MODE_PRIVATE)
        val storedMnemonic = prefs.getString("encrypted_mnemonic", null)*/
        val storedMnemonic = getFromEncryptedPrefs("encrypted_mnemonic")
        mnemonicLoaded.value = storedMnemonic != null
        return storedMnemonic
    }

    override fun getLastSelectedNetwork(): Network {
        val json = sharedPreferences.getString("SELECTED_NETWORK_NAME", null)
        val jsonReturn = json?.let { Json.decodeFromString<Network>(it) }
        return jsonReturn ?: Network.MUMBAI_TESTNET
    }

    fun storeWallet(walletAddress: String) {
        // Store the wallet address in SharedPreferences
        sharedPreferences.edit {
            putString(walletAddressKey, walletAddress)
            apply()
        }
    }

    fun storeMedPlumToken(medPlumToken: String) {
        //Store the MedPlum token in SharedPreferences
        sharedPreferences.edit {
            putString("medPlumToken", medPlumToken)
            apply()
        }
    }

    fun getMedPlumToken(): String {
        // Retrieve the MedPlum token from SharedPreferences
        return sharedPreferences.getString("medPlumToken", null).toString()
    }

    fun isMedPlumTokenStored(): Boolean {
        // Check if the MedPlum token is stored in SharedPreferences
        return sharedPreferences.contains("medPlumToken")
    }

    fun getLastAccessTime(key: String): Long {
        return sharedPreferences.getLong("access_time_$key", 0)
    }

    fun updateLastAccessTime(key: String, time: Long) {
        sharedPreferences.edit().putLong("access_time_$key", time).apply()
    }

    fun removeLastAccessTime(key: String) {
        sharedPreferences.edit().remove("access_time_$key").apply()
    }

    override fun removeAllWalletData() {
        sharedPreferences.edit().clear().apply()
    }

    override fun getMnemonic(): String? {
        // Decrypt the mnemonic
        //return getDecryptedMnemonic(context)
        return getFromEncryptedPrefs("encrypted_mnemonic")
    }

    override fun clearTokenBlocklist(): List<TokenBalance> {
        sharedPreferences.edit().putString("TOKEN_BLOCKLIST", null).apply()
        return getTokenBlocklist()
    }

    override fun updateTokenBlockList(tokenBlocklist: List<TokenBalance>) {
        val jsonTokenBlocklist = Json.encodeToString(tokenBlocklist)
        sharedPreferences.edit { putString("TOKEN_BLOCKLIST", jsonTokenBlocklist).apply() }
    }

    override fun getTokenBlocklist(): List<TokenBalance> {
        val json = sharedPreferences.getString("TOKEN_BLOCKLIST", null)

        return if (!json.isNullOrEmpty()) {
            try {
                val jsonReturn = Json.decodeFromString<List<TokenBalance>>(json)
                jsonReturn
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }

    }

    private fun getGasPrices(): Pair<BigInteger, BigInteger> {
        val endpoint = "https://gasstation-mainnet.matic.network/v2"
        val url = URL(endpoint)
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"

        val responseCode = connection.responseCode
        if (responseCode == HttpURLConnection.HTTP_OK) {
            val responseString = connection.inputStream.bufferedReader().use { it.readText() }
            val jsonResponse = JSONObject(responseString)
            val standard = jsonResponse.getJSONObject("standard")
            val maxPriorityFee = standard.getDouble("maxPriorityFee")
            val maxFee = standard.getDouble("maxFee")
            val maxFeeWei = Convert.toWei(BigDecimal(maxFee), Convert.Unit.GWEI).toBigInteger()
            val maxPriorityFeeWei =
                Convert.toWei(BigDecimal(maxPriorityFee), Convert.Unit.GWEI).toBigInteger()
            return Pair(maxPriorityFeeWei, maxFeeWei)
        } else {
            throw Exception("Failed to retrieve data from $endpoint. Response code: $responseCode")
        }
    }

    private fun getEthPriceInUsd(): Double {
        return try {
            val client = OkHttpClient()
            val request = Request.Builder()
                .url("https://api.coingecko.com/api/v3/simple/price?ids=ethereum&vs_currencies=usd")
                .build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return 0.0

            val json = JSONObject(body)
            json.getJSONObject("ethereum").getDouble("usd")
        } catch (e: Exception) {
            Log.e("PriceFetch", "Failed to fetch ETH price", e)
            0.0
        }
    }


    override fun fetchBalances(
        addresses: String,
        first: Int,
    ): Pair<Double, List<TokenBalance>> {
        return try {
            val web3j = Web3jService.build(selectedNetwork.value)

            // Get native ETH balance
            val balanceInWei = web3j.ethGetBalance(addresses, DefaultBlockParameterName.LATEST)
                .send()
                .balance

            val balanceInEth = Convert.fromWei(balanceInWei.toString(), Convert.Unit.ETHER)
                .toDouble()

            // Fetch ETH price in USD from CoinGecko
            val priceUSD = getEthPriceInUsd()

            val balanceUSD = balanceInEth * priceUSD


            // Create a basic TokenBalance entry for ETH/SepoliaETH
            val tokenBalance = TokenBalance(
                contractAddress = "native", // Placeholder
                balance = balanceInWei.toString(),
                name = selectedNetwork.value.displayName,
                symbol = "ETH",
                decimals = 18,
                tokenIcon = "", // Optional: add your own logo URL
                balanceUSD = balanceUSD, // Set to 0 or fetch from price API
                networkName = selectedNetwork.value.displayName
            )

            // Optionally cache this balance like before
            cacheUserBalance(listOf(tokenBalance), application)
            cacheTotalBalanceUSD(balanceInEth, application)

            Pair(balanceInEth, listOf(tokenBalance))
        } catch (e: Exception) {
            Log.e("fetchTokenBalances", "Error fetching native balance", e)
            Pair(0.0, emptyList())
        }
    }




    //TODO: Add functions that interact with the healthyWallet contract
    private val medskyAdress = "0xBca0fDc68d9b21b5bfB16D784389807017B2bbbc"
    private lateinit var medskyContract: MedskyContract

    private val healthyWalletAdressOld = "0x9A8ea6736DF00Af70D1cD70b1Daf3619C8c0D7F4"
    //private val healthyWalletAdress = "0x257F027faAc9eA80F8269a7024FE33a8730223D5" //"0x503Adf07dE6a7B1C23F793aa6b422A0C59Fa219e" //"0x6410E8e6321f46B7A34B9Ea9649a4c84563d8045"
    //0x8d91fa1054f8f53e01661f4147e450edd090336d

    private val healthyWalletAddresses = mapOf(
        Network.ARBITRUM_SEPOLIA_TESTNET to "0x5461ab0D73551Bb44b826A69Baa86c73972d78bE",//"0x8233bf41E05f518c927AFfc4154298A7EB63F336",//""0x07c60bA6F2e58c5E7C5123bFc0F16399606F202f",//"0x94c35e1Ca0B33dAFB7e13a3a951791a3E384377c",//""0xD18CcEEC300d7f81ad2A69175DA510A97184B5A0",//""0xE75A51E1dD78fddc3a24c351Ea160eD6aa7d01a2",
        Network.SEPOLIA to "0xf5f80D411aE97cB4aC8e5DA9Cab6f7a3f74A06B5",//"0x257F027faAc9eA80F8269a7024FE33a8730223D5",
    )
    // WITH REVOKE ACCESS AND DELETE ON DENY: 0x5461ab0D73551Bb44b826A69Baa86c73972d78bE

    private val healthyWalletAdress: String
        get() = healthyWalletAddresses[selectedNetwork.value] ?: throw IllegalStateException("No address for that network.")

    private lateinit var healthyContract: MedicalRecordAccess2


    fun loadMedSkyContract(credentials: Credentials) {
        medskyContract = MedskyContract.load(medskyAdress, web3jService, credentials, DefaultGasProvider())
    }

    /*suspend fun createRecord(recordId: String, hash: String): TransactionReceipt {
        return medskyContract.createRecord(recordId, hash).send()
    }*/

    suspend fun deleteRecord(recordId: String, actionId: String): TransactionReceipt {
        return medskyContract.deleteRecord(recordId, actionId).send()
    }

    suspend fun recordExists(recordId: String): Boolean {
        return medskyContract.recordExists(recordId).send()
    }

    suspend fun readRecords(recordIds: List<String>): List<MedskyContract.Record> {
        return medskyContract.readRecords(recordIds).send() as List<MedskyContract.Record>
    }

    fun accessExists(accessId: String): Boolean {
        return medskyContract.accessExists(accessId).send()
    }



    fun loadHealthyContract(credentials: Credentials) {
        healthyContract = MedicalRecordAccess2.load(healthyWalletAdress, web3jService, credentials, DefaultGasProvider())
    }

    // Fetch next 3 logs (sync batch)
    /*suspend fun getNextAccessLogs(): TransactionReceipt {
        return healthyContract.getNextAccessLogs().send()
    }

    // Preview logs before syncing
    suspend fun previewNextAccessLogs(): List<MedicalRecordAccess.MedicalAccessLog> {
        return healthyContract.previewNextAccessLogs().send() as List<MedicalRecordAccess.MedicalAccessLog>
    }*/

    // Log access to a record (simulate doctor accessing a patient record)
    /*suspend fun logAccess(doctor: String, patient: String, recordType: String): TransactionReceipt {
        return healthyContract.logAccess(doctor, address patient, string recordId, string recordType).send()
    }*/

    // Reset the sync pointer (e.g. for testing)
    suspend fun resetSyncPointer(): TransactionReceipt {
        return healthyContract.resetSyncPointer().send()
    }

    suspend fun denyAccess2(recordId: String, requester: String, credentials: Credentials): TransactionReceipt {
        val nonce = nonceManager.getNextNonce()
        val gasPrice = web3jService.ethGasPrice().send().gasPrice
        val gasLimit = BigInteger.valueOf(3000000) // Adjust

        val function = Function(
            "denyAccess",
            listOf(Address(requester), org.web3j.abi.datatypes.Utf8String(recordId)),
            emptyList()
        )
        val encodedFunction = FunctionEncoder.encode(function)

        val rawTransaction = RawTransaction.createTransaction(
            nonce,
            gasPrice,
            gasLimit,
            healthyWalletAdress,
            encodedFunction
        )

        val signedMessage = TransactionEncoder.signMessage(rawTransaction, selectedNetwork.value.chainId, credentials)
        val hexValue = Numeric.toHexString(signedMessage)

        val transactionResponse = web3jService.ethSendRawTransaction(hexValue).send()

        if (transactionResponse.hasError()) {
            throw RuntimeException("Transaction failed: ${transactionResponse.error.message}")
        }

        Log.d("SyncedLog", "Sent tx deny: ${transactionResponse.transactionHash}")


        // Tentar obter o recibo da transação várias vezes
        val maxWaitMs = 100000L
        val start = System.currentTimeMillis()

        while (System.currentTimeMillis() - start < maxWaitMs) {
            val receipt = web3jService.ethGetTransactionReceipt(transactionResponse.transactionHash).send().transactionReceipt
            if (receipt.isPresent) {
                if (receipt.get().status == "0x0") {
                    Log.e("TxStatus", "Transaction reverted")
                    throw RuntimeException("Transaction reverted")
                }
                return receipt.get()
            }
            // Esperar um pouco antes de tentar novamente
            delay(1000)
        }

        throw RuntimeException("Transaction receipt not generated after sending transaction")
    }

    suspend fun acceptAccess2(recordId: String, requester: String, credentials: Credentials): TransactionReceipt {
        val nonce = nonceManager.getNextNonce()
        val gasPrice = web3jService.ethGasPrice().send().gasPrice
        val gasLimit = BigInteger.valueOf(3000000) // Ajuste conforme necessário
        //val request = healthyContract.getAccessRequest(requester, recordId).send()
        //Log.d("AcceptContract","doctor = $requester, recordId = $recordId, patient = ${request.patientAddress}")
        val function = Function(
            "approveAccess",
            listOf(Address(requester), org.web3j.abi.datatypes.Utf8String(recordId)),
            emptyList()
        )
        val encodedFunction = FunctionEncoder.encode(function)

        val rawTransaction = RawTransaction.createTransaction(
            nonce,
            gasPrice,
            gasLimit,
            healthyWalletAdress,
            encodedFunction
        )

        val signedMessage = TransactionEncoder.signMessage(rawTransaction, selectedNetwork.value.chainId, credentials)
        val hexValue = Numeric.toHexString(signedMessage)

        val transactionResponse = web3jService.ethSendRawTransaction(hexValue).send()

        if (transactionResponse.hasError()) {
            throw RuntimeException("Transaction failed: ${transactionResponse.error.message}")
        }
        Log.d("SyncedLog", "Sent tx accept: ${transactionResponse.transactionHash}")


        val maxWaitMs = 100000L
        val start = System.currentTimeMillis()

        while (System.currentTimeMillis() - start < maxWaitMs) {
            val receipt = web3jService.ethGetTransactionReceipt(transactionResponse.transactionHash).send().transactionReceipt
            if (receipt.isPresent) {
                if (receipt.get().status == "0x0") {
                    Log.e("TxStatus", "Transaction reverted")
                    throw RuntimeException("Transaction reverted")
                }
                return receipt.get()
            }
            // Esperar um pouco antes de tentar novamente
            delay(1000)
        }

        throw RuntimeException("Transaction receipt not generated after sending transaction")
    }

    /*suspend fun syncTransactionWithHealthyContract2(credentials: Credentials): List<com.example.ethktprototype.data.Transaction> {
        val previewLogs = healthyContract.previewNextAccessRequests().send() as List<MedicalRecordAccess2.AccessRequest>

        if (previewLogs.isEmpty()) {
            return emptyList()
        }

        val nonce = web3jService.ethGetTransactionCount(credentials.address, DefaultBlockParameterName.LATEST).send().transactionCount
        val gasPrice = web3jService.ethGasPrice().send().gasPrice
        val gasLimit = BigInteger.valueOf(3000000)

        val function = Function("getNextAccessRequests", emptyList(), emptyList())
        val encodedFunction = FunctionEncoder.encode(function)

        val rawTransaction = RawTransaction.createTransaction(
            nonce,
            gasPrice,
            gasLimit,
            healthyWalletAdress,
            encodedFunction
        )

        val signedMessage = TransactionEncoder.signMessage(rawTransaction, selectedNetwork.value.chainId, credentials)
        val hexValue = Numeric.toHexString(signedMessage)

        val transactionResponse = web3jService.ethSendRawTransaction(hexValue).send()

        if (transactionResponse.hasError()) {
            throw RuntimeException("Transaction failed: ${transactionResponse.error.message}")
        }

        val txHash = transactionResponse.transactionHash

        Log.d("SyncedLog", "Sent tx sync: ${transactionResponse.transactionHash}")


        // Poll for the transaction receipt
        val maxWaitMs = 60000L
        val start = System.currentTimeMillis()

        while (System.currentTimeMillis() - start < maxWaitMs) {
            val receiptResp = web3jService.ethGetTransactionReceipt(txHash).send()
            if (receiptResp.transactionReceipt.isPresent) {
                val receipt = receiptResp.transactionReceipt.get()
                if (receipt.status == "0x0") {
                    Log.e("TxStatus", "Transaction reverted")
                    throw RuntimeException("Transaction reverted")
                }
                return previewLogs.map { log ->
                    val status = if (log.status.toString() == "0") "pending" else if (log.status.toString() == "1") "accepted" else "denied"
                    val examTypes = listOf("MRI", "X-ray", "CT Scan", "Ultrasound", "Blood Test")
                    val randomType = examTypes[Random.nextInt(examTypes.size)]
                    com.example.ethktprototype.data.Transaction(
                        id = "",
                        date = log.timestamp.toString(),
                        status = status,
                        recordId = log.recordId,
                        practitionerId = log.doctorMedplumId,
                        practitionerAddress = log.doctorAddress,
                        type = log.recordId.substringBefore("/"),
                        patientId = "01968b59-76f3-7228-aea9-07db748ee2ca"
                    )
                }
            }
            delay(1000)
        }

        throw RuntimeException("Transaction receipt not generated after sending transaction")
    }*/

    suspend fun requestAccess(doctorAddress: String, patientAddress: String, doctorMedplumId: String, recordId: String, credentials: Credentials): TransactionReceipt {
        val nonce = nonceManager.getNextNonce()
        val gasPrice = web3jService.ethGasPrice().send().gasPrice
        val gasLimit = BigInteger.valueOf(3000000)

        val function = Function(
            "doctorRequestAccess",
            listOf(Address(doctorAddress),Address(patientAddress), Utf8String(doctorMedplumId), Utf8String(recordId)),
            emptyList()
        )
        val encodedFunction = FunctionEncoder.encode(function)

        val rawTransaction = RawTransaction.createTransaction(
            nonce,
            gasPrice,
            gasLimit,
            healthyWalletAdress,
            BigInteger.ZERO,
            encodedFunction
        )

        val signedMessage = TransactionEncoder.signMessage(rawTransaction, selectedNetwork.value.chainId, credentials)
        val hexValue = Numeric.toHexString(signedMessage)

        val transactionResponse = web3jService.ethSendRawTransaction(hexValue).send()
        Log.d("SyncedLog", "Sent tx request: ${transactionResponse.transactionHash}")
        if (transactionResponse.hasError()) {
            throw RuntimeException("Transaction failed: ${transactionResponse.error.message}")
        }

        val maxWaitMs = 60000L
        val start = System.currentTimeMillis()

        while (System.currentTimeMillis() - start < maxWaitMs) {
            val receipt = web3jService.ethGetTransactionReceipt(transactionResponse.transactionHash).send().transactionReceipt
            if (receipt.isPresent) {
                if (receipt.get().status == "0x0") {
                    Log.e("TxStatus", "Transaction reverted")
                    throw RuntimeException("Transaction reverted")
                }
                return receipt.get()
            }
            // Esperar um pouco antes de tentar novamente
            delay(1000)
        }

        throw RuntimeException("Transaction receipt not generated after sending transaction")
    }

    suspend fun createRecord(recordId: String, hash: String, credentials: Credentials): TransactionReceipt {
        val nonce = nonceManager.getNextNonce()

        val gasPrice = web3jService.ethGasPrice().send().gasPrice
        val gasLimit = BigInteger.valueOf(3000000)

        val function = Function(
            "createRecord",
            listOf(Utf8String(recordId), Utf8String(hash)),
            emptyList()
        )
        val encodedFunction = FunctionEncoder.encode(function)

        val rawTransaction = RawTransaction.createTransaction(
            nonce,
            gasPrice,
            gasLimit,
            healthyWalletAdress,
            BigInteger.ZERO,
            encodedFunction
        )

        val signedMessage = TransactionEncoder.signMessage(rawTransaction, selectedNetwork.value.chainId, credentials)
        val hexValue = Numeric.toHexString(signedMessage)

        val transactionResponse = web3jService.ethSendRawTransaction(hexValue).send()
        Log.d("SyncedLog", "Sent createRecord tx: ${transactionResponse.transactionHash}")
        if (transactionResponse.hasError()) {
            throw RuntimeException("Transaction failed: ${transactionResponse.error.message}")
        }

        val maxWaitMs = 60000L
        val start = System.currentTimeMillis()

        while (System.currentTimeMillis() - start < maxWaitMs) {
            val receipt = web3jService.ethGetTransactionReceipt(transactionResponse.transactionHash).send().transactionReceipt
            if (receipt.isPresent) {
                if (receipt.get().status == "0x0") {
                    Log.e("TxStatus", "Transaction reverted")
                    throw RuntimeException("Transaction reverted")
                }
                return receipt.get()
            }
            delay(1000)
        }

        throw RuntimeException("Transaction receipt not generated after sending transaction")
    }

    fun getPatientAccessRequests(): List<com.example.ethktprototype.data.Transaction> {

        val accessRequests = healthyContract.getPatientAccessRequests().send() as List<MedicalRecordAccess2.AccessRequest>

        if (accessRequests.isEmpty()) {
            return emptyList()
        }

        return accessRequests.map { access ->
            com.example.ethktprototype.data.Transaction(
                id = UUID.randomUUID().toString(), // Unique app-level ID
                date = access.timestamp.toString(),
                status = when (access.status.toInt()) {
                    0 -> "pending"
                    1 -> "accepted"
                    2 -> "denied"
                    else -> "Unknown"
                },
                type = access.recordId.substringBefore("/"),
                recordId = access.recordId,
                patientId = access.patientAddress,
                practitionerId = access.doctorMedplumId,
                practitionerAddress = access.doctorAddress,
            )
        }
    }





    //////////////////// Accesse Con Functions ////////////////////

    // val accessesContractAddress = "0xe9354B6CfEAaC38636AcACB397F8E5566dc559fD"//"0xcCbB217F782bBa59aAD9BdF0291E1B325461E146"
    private lateinit var accessesContract: RecordAccessContract

    private val accessesContractAddresses = mapOf(
    Network.ARBITRUM_SEPOLIA_TESTNET to "0xF443c9B544E4020777cf751E344C34754e1A0F40",//"0xF62421b05A67344AC8aAC01Fe93add614A995dd7", //O contrato comentado é com o getLastAccess
    Network.SEPOLIA to "0xcCe7E42DeDE823C42a95d62a9270BD9137510161",//"0xe9354B6CfEAaC38636AcACB397F8E5566dc559fD",
    )

    private val accessesContractAddress: String
        get() = accessesContractAddresses[selectedNetwork.value] ?: throw IllegalStateException("No address for that network.")


    fun loadAccessesContract(credentials: Credentials) {
        accessesContract = RecordAccessContract.load(accessesContractAddress, web3jService, credentials, DefaultGasProvider())
    }

    suspend fun logAccess(recordIds: List<String>, credentials: Credentials): TransactionReceipt {
        val nonce = nonceManager.getNextNonce()

        val baseGasPrice = web3jService.ethGasPrice().send().gasPrice
        val gasPrice = baseGasPrice + (baseGasPrice / BigInteger.TEN) // +10%

        val gasLimit = BigInteger.valueOf(3000000)

        val accessId = UUID.randomUUID().toString()

        Log.d("SyncedLog", "accessId: $accessId, recordIds: $recordIds")
        val function = Function(
            "logAccess",
            listOf(
                DynamicArray(Utf8String::class.java, recordIds.map { Utf8String(it) }),
                Utf8String(accessId)
            ),
            emptyList()
        )
        Log.d("SyncedLog", "function input: ${function.inputParameters[0].value}")

        val encodedFunction = FunctionEncoder.encode(function)

        val rawTransaction = RawTransaction.createTransaction(
            nonce,
            gasPrice,
            gasLimit,
            accessesContractAddress,
            BigInteger.ZERO,
            encodedFunction
        )

        val signedMessage = TransactionEncoder.signMessage(rawTransaction, selectedNetwork.value.chainId, credentials)
        val hexValue = Numeric.toHexString(signedMessage)

        val transactionResponse = web3jService.ethSendRawTransaction(hexValue).send()
        Log.d("SyncedLog", "Sent logAccess tx: ${transactionResponse.transactionHash}")
        if (transactionResponse.hasError()) {
            throw RuntimeException("Transaction failed: ${transactionResponse.error.message}")
        }

        val maxWaitMs = 60000L
        val start = System.currentTimeMillis()

        while (System.currentTimeMillis() - start < maxWaitMs) {
            val receipt = web3jService.ethGetTransactionReceipt(transactionResponse.transactionHash).send().transactionReceipt
            if (receipt.isPresent) {
                if (receipt.get().status == "0x0") {
                    Log.e("TxStatus", "Transaction reverted")
                    throw RuntimeException("Transaction reverted")
                }
                return receipt.get()
            }
            delay(1000)
        }

        throw RuntimeException("Transaction receipt not generated after sending transaction")
    }

}