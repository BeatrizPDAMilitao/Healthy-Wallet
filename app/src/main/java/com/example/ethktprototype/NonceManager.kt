package com.example.ethktprototype

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import java.math.BigInteger

/**
 * NonceManager is responsible for managing the nonce for a given Ethereum address.
 * It retrieves the current nonce from the blockchain and increments it for each transaction.
 */
//TODO: Consider using a more robust nonce management strategy, such as handling nonce gaps or reorgs.
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

class RobustNonceManager(
    private val web3j: Web3j,
    private val address: String,
    private val clock: () -> Long = { System.currentTimeMillis() }
) {
    private val mutex = Mutex()

    // Next candidate nonce (may be >= chain pending if we have in-flight txs)
    private var nextLocal: BigInteger? = null

    // Nonces that have been handed out but not yet mined/abandoned.
    // You may also persist these (e.g., Room) with createdAt timestamps.
    private val inFlight = sortedSetOf<BigInteger>()

    private suspend fun chainPendingNonce(): BigInteger =
        withContext(Dispatchers.IO) {
            web3j.ethGetTransactionCount(
                address,
                DefaultBlockParameterName.PENDING // IMPORTANT
            ).send().transactionCount
        }

    /**
     * Allocate (reserve) the next available nonce.
     * Safe for concurrent callers.
     */
    suspend fun allocate(): BigInteger = mutex.withLock {
        if (nextLocal == null) {
            nextLocal = chainPendingNonce()
        }

        var candidate = nextLocal!!
        while (inFlight.contains(candidate)) {
            candidate = candidate + BigInteger.ONE
        }

        inFlight.add(candidate)
        nextLocal = candidate + BigInteger.ONE
        candidate
    }

    /**
     * Release a nonce reservation when a tx failed to broadcast (no tx hash),
     * or when we explicitly decide to abandon it.
     */
    suspend fun release(nonce: BigInteger) = mutex.withLock {
        inFlight.remove(nonce)
        // We do NOT decrement nextLocal; we’ll naturally reuse any gaps.
    }

    /**
     * Mark a nonce as mined/confirmed (or known-broadcast and mined later).
     */
    suspend fun markMined(nonce: BigInteger) = mutex.withLock {
        inFlight.remove(nonce)
        // Optionally: if nonce is far below chain pending, consider a reconcile.
    }

    /**
     * Reconcile local view with chain (e.g., after "nonce too low" errors).
     */
    suspend fun reconcileFromChain() = mutex.withLock {
        val pending = chainPendingNonce()
        // Drop in-flight nonces lower than pending (they are mined or at least accepted).
        inFlight.removeIf { it < pending }
        // Move nextLocal forward if behind the chain’s pending count.
        if (nextLocal == null || nextLocal!! < pending) nextLocal = pending
    }
}
