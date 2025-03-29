package com.example.ethktprototype

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.example.ethktprototype.data.Transaction

/**
 * This class is a BroadcastReceiver that listens for incoming transactions.
 */
class TransactionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val application = context.applicationContext as HealthyWalletApplication
        val factory = WalletViewModelFactory(application)
        val viewModel = ViewModelProvider(application, factory)[WalletViewModel::class.java]

        val transactionId = intent.getStringExtra("transactionId") ?: return
        val transactionDate = intent.getStringExtra("transactionDate") ?: return
        val transactionStatus = intent.getStringExtra("transactionStatus") ?: return

        val transaction = Transaction(
            id = transactionId,
            date = transactionDate,
            status = transactionStatus
        )
        Log.d("ViewModel", "After: $viewModel")

        //Should be done here
        //view model is changing
        //viewModel.onNotificationReceived(transaction)
    }
}