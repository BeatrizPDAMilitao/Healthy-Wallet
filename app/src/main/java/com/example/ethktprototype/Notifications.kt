package com.example.ethktprototype

import android.util.Log
import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.ethktprototype.data.Transaction
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.activity.result.ActivityResultLauncher


fun sendNotification(context: Context, viewModel: WalletViewModel, title: String, message: String, requestPermissionLauncher: ActivityResultLauncher<String>) {
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
        val channelId = "transaction_channel"
        val channelName = "Transaction Notifications"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT)
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(context).notify(1, notification)
    } else {
        requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}

/**
 * This function sends a notification to the user when a new transaction is received.
 *
 * @param context The context from which the notification is sent.
 * @param viewModel The ViewModel instance to manage the notification state.
 * @param title The title of the notification.
 * @param message The message of the notification.
 */
fun sendNotification(context: Context, viewModel: WalletViewModel, title: String, message: String) {
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
        val channelId = "transaction_channel"
        val channelName = "Transaction Notifications"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT)
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(context).notify(viewModel.getAndIncrementNotificationId(), notification)
    }
}

/*fun simulateTransactionNotification(context: Context, viewModel: WalletViewModel) {
    val newTransaction = Transaction(
        id = "",
        date = "",
        status = "pending"
    )
    sendNotification(context, "New Transaction", "You have a new transaction pending.")
    viewModel.onNotificationReceived(newTransaction)
}*/

/**
 * This function simulates the reception of a transaction notification.
 * It creates a new transaction and sends a broadcast to the TransactionReceiver.
 *
 * @param context The context from which the notification is sent.
 * @param viewModel The ViewModel instance to manage the notification state.
 */
fun simulateTransactionReceived(context: Context, viewModel: WalletViewModel) {
    Log.d("ViewModel", "Before: $viewModel")
    val newTransaction = Transaction(
        id = viewModel.getAndIncrementTransactionId().toString(),
        date = viewModel.getCurrentDate(),
        status = "pending"
    )

    // Send a broadcast of the new transaction
    val intent = Intent(context, TransactionReceiver::class.java).apply {
        putExtra("transactionId", newTransaction.id)
        putExtra("transactionDate", newTransaction.date)
        putExtra("transactionStatus", newTransaction.status)
    }
    context.sendBroadcast(intent)
    // should not be done here
    viewModel.onNotificationReceived(newTransaction)
}

