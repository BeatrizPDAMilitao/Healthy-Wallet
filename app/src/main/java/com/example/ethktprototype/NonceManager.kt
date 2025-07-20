package com.example.ethktprototype

import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import java.math.BigInteger

class NonceManager(private val web3jService: Web3j, private val address: String) {
    private var currentNonce: BigInteger? = null

    @Synchronized
    fun getNextNonce(): BigInteger {
        if (currentNonce == null) {
            // get the current nonce from the blockchain
            currentNonce = web3jService.ethGetTransactionCount(
                address,
                DefaultBlockParameterName.LATEST
            ).send().transactionCount
        }
        // Return the current nonce and increment it for the next call
        return currentNonce!!.also { currentNonce = it.add(BigInteger.ONE) }
    }
}