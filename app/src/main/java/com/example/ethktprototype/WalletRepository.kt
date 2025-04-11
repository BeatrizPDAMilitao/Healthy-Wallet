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
import com.example.ethktprototype.contracts.MedicalRecordAccess
import com.example.ethktprototype.contracts.MedicalRecordAccess2
import com.example.ethktprototype.contracts.MedskyContract
import com.example.ethktprototype.data.GraphQLData
import com.example.ethktprototype.data.GraphQLQueries
import com.example.ethktprototype.data.GraphQLResponse
import com.example.ethktprototype.data.NftValue
import com.example.ethktprototype.data.PortfolioData
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
    private val _selectedNetwork = mutableStateOf(Network.SEPOLIA)
    private val selectedNetwork: MutableState<Network> = _selectedNetwork
    private val mnemonicLoaded = MutableLiveData<Boolean>()

    override fun storeMnemonic(mnemonic: String) {
        encryptMnemonic(context, mnemonic)

        val sharedPreferences = context.getSharedPreferences("WalletPrefs", Context.MODE_PRIVATE)
        val encryptedMnemonic = getEncryptedMnemonic(context)
        val encodedMnemonic = Base64.encodeToString(encryptedMnemonic, Base64.DEFAULT)
        sharedPreferences.edit().putString("encrypted_mnemonic", encodedMnemonic).apply()

        mnemonicLoaded.value = true
    }

    override fun loadMnemonicFromPrefs(): String? {
        val prefs = context.getSharedPreferences("WalletPrefs", Context.MODE_PRIVATE)
        val storedMnemonic = prefs.getString("encrypted_mnemonic", null)
        mnemonicLoaded.value = storedMnemonic != null
        return storedMnemonic
    }

    fun updateSelectedNetwork(network: Network): Network {
        selectedNetwork.value = network
        val jsonNetwork = Json.encodeToString(network)
        sharedPreferences.edit().putString("SELECTED_NETWORK_NAME", jsonNetwork).apply()
        return network
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

    override fun removeAllWalletData() {
        sharedPreferences.edit().clear().apply()
    }

    override fun getMnemonic(): String? {
        // Decrypt the mnemonic
        return getDecryptedMnemonic(context)
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

    override fun fetchNfts(
        walletAddress: String,
        selectedNetwork: Network
    ): List<NftValue> {
        //val envVars = EnvVars()
        val zapperApiKey = "MY_API_KEY"
        val currentTime = System.currentTimeMillis() / 1000
        val sharedPreferences = getBalancesSharedPreferences(application)
        val cacheExpirationTime = getNftCacheExpirationTime(sharedPreferences)
        val cachedBalances = getUserNftBalances(application, selectedNetwork.displayName)

        return if (cachedBalances.isNotEmpty() && cacheExpirationTime > currentTime) {
            cachedBalances
        } else {
            val client = OkHttpClient()

            val requestBody = GraphQLQueries.getNftUsersTokensQuery(
                owners = listOf(walletAddress),
                network = selectedNetwork.name,
                first = 10
            )

            val request = Request.Builder()
                .url("https://public.zapper.xyz/graphql")
                .header("x-zapper-api-key", zapperApiKey)
                .post(requestBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()

            return try {
                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                val jsonResponse = JsonUtils.json.decodeFromString<GraphQLResponse<GraphQLData>>(responseBody ?: "")
                val nftNodes = jsonResponse.data?.nftUsersTokens?.edges

                val nftList = nftNodes?.map { edge ->
                    val node = edge.node
                    val collection = node.collection
                    val image = node.mediasV3?.images?.edges?.firstOrNull()?.node?.original

                    NftValue(
                        contractAddress = collection.address,
                        contractName = collection.name ?: "Unknown",
                        image = image ?: ""
                    )
                } ?: emptyList()

                sharedPreferences.edit().putLong("CACHE_EXPIRATION_TIME_NFT", currentTime).apply()
                cacheUserNftBalance(nftList, application, selectedNetwork.displayName)

                nftList
            } catch (e: Exception) {
                Log.e("fetchNfts", "Error fetching NFT data", e)
                emptyList()  // Return empty list in case of error
            }
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


    override suspend fun sendTokens(
        credentials: Credentials,
        contractAddress: String,
        toAddress: String,
        value: BigDecimal
    ): String {
        val web3jService = Web3jService.build(selectedNetwork.value)
        val contract = ERC20.load(contractAddress, web3jService, credentials, DefaultGasProvider())
        val decimals = contract.decimals()
        val gasPrice = web3jService.ethGasPrice().send().gasPrice

        val function = Function(
            "transfer",
            listOf(
                Address(toAddress),
                Uint256(
                    Convert.toWei(
                        value.multiply(BigDecimal.TEN.pow(decimals.toInt())),
                        Convert.Unit.WEI
                    ).toBigInteger()
                )
            ),
            emptyList()
        )
        val nonce: BigInteger = web3jService.ethGetTransactionCount(
            credentials.address,
            DefaultBlockParameterName.LATEST
        )
            .send().transactionCount

        // Encode the function call to get the data that needs to be sent in the transaction
        val encodedFunction = FunctionEncoder.encode(function)

        val ethEstimateGas = web3jService.ethEstimateGas(
            Transaction.createFunctionCallTransaction(
                //TODO: Fix this line causing errors - testing using burn address here for now
                "0x000000000000000000000000000000000000dEaD",
                nonce,
                gasPrice,
                null,
                "0x000000000000000000000000000000000000dEaD",
                encodedFunction
            )
        ).send()

        val gasLimit = ethEstimateGas.amountUsed.plus(BigInteger.valueOf(40000))

        val (maxPriorityFeeWei, maxFeeWei) = getGasPrices()


        return withContext(Dispatchers.IO) {
            try {

                if (contractAddress == "0x0000000000000000000000000000000000001010") {
                    val transfer = Transfer.sendFundsEIP1559(
                        web3jService,
                        credentials,
                        toAddress,
                        Convert.toWei(
                            value.multiply(BigDecimal.TEN.pow(decimals.toInt())),
                            Convert.Unit.WEI
                        ),
                        Convert.Unit.WEI,
                        gasLimit,
                        maxPriorityFeeWei,
                        maxFeeWei
                    ).sendAsync().get()

                    if (transfer.isStatusOK) {
                        transfer.transactionHash
                    } else {
                        throw RuntimeException("EIP1559 Transaction failed: ${transfer.logs}")
                    }
                } else {
                    // Create a raw transaction object
                    val transaction = RawTransaction.createTransaction(
                        nonce,
                        gasPrice,
                        gasLimit,
                        contractAddress,
                        encodedFunction
                    )

                    // my attempt to fix only EIP allowed over RPC
                    val signedT = TransactionEncoder.signMessage(
                        transaction,
                        selectedNetwork.value.chainId,
                        credentials
                    )

                    // Convert the signed transaction to hex format
                    val hexValue = Numeric.toHexString(signedT)

                    // Send the signed transaction to the network
                    val transactionResponse =
                        web3jService.ethSendRawTransaction(hexValue).sendAsync().get()

                    // Check if the transaction was successful or not
                    if (transactionResponse.hasError()) {
                        throw RuntimeException("Transaction failed: ${transactionResponse.error.message}")
                    } else {
                        transactionResponse.transactionHash
                    }
                }

            } catch (e: Exception) {
                Log.e("send", "transaction failed: ${e.message}")
                //Sentry.captureException(e)
                throw e
            }
        }
    }
    //TODO: Add functions that interact with the healthyWallet contract
    private val medskyAdress = "0xBca0fDc68d9b21b5bfB16D784389807017B2bbbc"
    private lateinit var medskyContract: MedskyContract

    private val healthyWalletAdressOld = "0x9A8ea6736DF00Af70D1cD70b1Daf3619C8c0D7F4"
    private val healthyWalletAdress = "0x3069FD69c7C178414de9b1761084c984B4d9A5ba" //"0x6410E8e6321f46B7A34B9Ea9649a4c84563d8045"
    private lateinit var healthyContract: MedicalRecordAccess2

    val web3jService = Web3jService.build(selectedNetwork.value)

    fun loadMedSkyContract(credentials: Credentials) {
        medskyContract = MedskyContract.load(medskyAdress, web3jService, credentials, DefaultGasProvider())
    }

    suspend fun createRecord(recordId: String, hash: String): TransactionReceipt {
        return medskyContract.createRecord(recordId, hash).send()
    }

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
    suspend fun getNextAccessLogs(): TransactionReceipt {
        return healthyContract.getNextAccessLogs().send()
    }

    // Preview logs before syncing
    suspend fun previewNextAccessLogs(): List<MedicalRecordAccess.MedicalAccessLog> {
        return healthyContract.previewNextAccessLogs().send() as List<MedicalRecordAccess.MedicalAccessLog>
    }

    // Log access to a record (simulate doctor accessing a patient record)
    /*suspend fun logAccess(doctor: String, patient: String, recordType: String): TransactionReceipt {
        return healthyContract.logAccess(doctor, address patient, string recordId, string recordType).send()
    }*/

    // Reset the sync pointer (e.g. for testing)
    suspend fun resetSyncPointer(): TransactionReceipt {
        return healthyContract.resetSyncPointer().send()
    }

    suspend fun denyAccess2(recordId: String, requester: String, credentials: Credentials): TransactionReceipt {
        val nonce = web3jService.ethGetTransactionCount(credentials.address, DefaultBlockParameterName.LATEST).send().transactionCount
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

        Log.d("SyncedLog", "Sent tx: ${transactionResponse.transactionHash}")


        // Tentar obter o recibo da transação várias vezes
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

    suspend fun acceptAccess2(recordId: String, requester: String, credentials: Credentials): TransactionReceipt {
        val nonce = web3jService.ethGetTransactionCount(credentials.address, DefaultBlockParameterName.LATEST).send().transactionCount
        val gasPrice = web3jService.ethGasPrice().send().gasPrice
        val gasLimit = BigInteger.valueOf(3000000) // Ajuste conforme necessário
        val request = healthyContract.getAccessRequest(requester, recordId).send()
        Log.d("AcceptContract","doctor = $requester, recordId = $recordId, patient = ${request.patient}")
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
        Log.d("SyncedLog", "Sent tx: ${transactionResponse.transactionHash}")


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

    suspend fun syncTransactionWithHealthyContract2(credentials: Credentials): List<com.example.ethktprototype.data.Transaction> {
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

        Log.d("SyncedLog", "Sent tx: ${transactionResponse.transactionHash}")


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
                    com.example.ethktprototype.data.Transaction(
                        id = log.recordId,
                        date = log.timestamp.toString(),
                        status = status,
                        practitionerId = log.doctor,
                        type = log.recordType,
                        patientId = log.patient
                    )
                }
            }
            delay(1000)
        }

        throw RuntimeException("Transaction receipt not generated after sending transaction")
    }

    suspend fun requestAccess(doctor: String, patient: String, recordId: String, recordType: String, credentials: Credentials): TransactionReceipt {
        val nonce = web3jService.ethGetTransactionCount(credentials.address, DefaultBlockParameterName.LATEST).send().transactionCount
        val gasPrice = web3jService.ethGasPrice().send().gasPrice
        val gasLimit = BigInteger.valueOf(3000000) // Ajuste conforme necessário

        val function = Function(
            "doctorRequestAccess",
            listOf(Address(doctor),Address(patient), org.web3j.abi.datatypes.Utf8String(recordId), org.web3j.abi.datatypes.Utf8String(recordType)),
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

}