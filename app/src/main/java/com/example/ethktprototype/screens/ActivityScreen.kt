package com.example.ethktprototype.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.Divider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.ethktprototype.WalletViewModel
import com.example.ethktprototype.composables.ReceiveBottomSheet
import com.example.ethktprototype.composables.SuccessDialogModal
import java.text.DecimalFormat
import androidx.compose.ui.graphics.Color
import com.example.ethktprototype.data.Transaction
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewModelScope
import com.example.ethktprototype.simulateTransactionReceived
import kotlinx.coroutines.launch

/**
 * ActivityScreen is a Composable function that displays the activity screen of the wallet application.
 *
 * @param navController The NavHostController used for navigation.
 * @param viewModel The WalletViewModel that holds the UI state and business logic.
 */
@Composable
fun ActivityScreen(
    navController: NavHostController,
    viewModel: WalletViewModel,
) {
    Log.d("ViewModel", "Act: $viewModel")
    val uiState by viewModel.uiState.collectAsState()
    Log.d("Notifications", "Recomposing with transactions: ${viewModel.getTransactions()}")
    val context = LocalContext.current
    val decimalFormatBalance = DecimalFormat("#.##")
    val showDialog = remember { mutableStateOf(false) }
    val selectedTransaction = remember { mutableStateOf<Transaction?>(null) }

    LaunchedEffect(uiState.transactionHash) {
        if (uiState.transactionHash.isNotEmpty()) {
            viewModel.setShowSuccessModal(true)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.getBalances()
        viewModel.getNftBalances()
    }

    LaunchedEffect(uiState.selectedNetwork) {
        viewModel.getBalances()
        viewModel.getNftBalances()
    }
    LaunchedEffect(uiState.transactions) {
        viewModel.getTransactions()
    }

    Box(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .fillMaxHeight()
                .padding(vertical = 8.dp, horizontal = 16.dp)
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp, 0.dp)
                    .wrapContentHeight(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = uiState.ens.ifEmpty {
                        uiState.walletAddress.take(5) + "..." + uiState.walletAddress.takeLast(
                            4
                        )
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium,
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.weight(1f))
                IconButton(
                    modifier = Modifier.size(30.dp),
                    onClick = { navController.navigate("settingsScreen") }
                ) {
                    Icon(
                        Icons.Filled.Settings,
                        "contentDescription",
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 56.dp)
            ) {
                Text(
                    text = "Activity",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Spacer(modifier = Modifier.height(2.dp))

                Button(
                    onClick = {
                        viewModel.viewModelScope.launch {
                            viewModel.syncTransactionWithHealthyContract()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .background(MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(8.dp)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Sync Transactions")
                }

                Spacer(modifier = Modifier.height(2.dp))

                /*Button(
                    onClick = {
                        viewModel.viewModelScope.launch {
                            viewModel.callMedSkyContract()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .background(MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(8.dp)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Call contract")
                }
                Spacer(modifier = Modifier.height(2.dp))*/

                Button(
                    onClick = {
                        viewModel.viewModelScope.launch {
                            simulateTransactionReceived(context, viewModel)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .background(MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(8.dp)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Simulate New Transaction")
                }
                Divider(color = Color.Gray, thickness = 1.dp)
                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(viewModel.getTransactions()) { transaction ->
                        TransactionItem(transaction, navController)
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            }
        }

        BottomNavigation(
            modifier = Modifier.align(Alignment.BottomCenter)
                .fillMaxWidth(),
            backgroundColor = MaterialTheme.colorScheme.inverseOnSurface,
        ) {
            BottomNavigationItem(
                icon = { Icon(Icons.Filled.Wallet, contentDescription = "Wallet") },
                label = { Text("Wallet") },
                selected = false,
                onClick = {
                    navController.navigate("tokenList")
                }
            )
            BottomNavigationItem(
                icon = { Icon(Icons.Filled.History, contentDescription = "Activity") },
                label = { Text("Activity") },
                selected = true,
                onClick = {
                    // Do nothing. This is the current screen.
                }
            )
        }
    }
    if (uiState.showRecordDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.setShowRecordDialog(false) },
            title = { Text("Transaction Details") },
            text = {
                Column {
                    Text("ID: ${uiState.transactionHash}")
                    //Text("Date: ${uiState.transactionHash}")
                    //Text("Status: ${uiState.transactionHash}")
                }
            },
            confirmButton = {
                Button(onClick = { viewModel.setShowRecordDialog(false) }) {
                    Text("OK")
                }
            },
            containerColor = MaterialTheme.colorScheme.background,
        )
    }

    if (uiState.showWalletModal) {
        ReceiveBottomSheet(
            walletAddress = uiState.walletAddress,
            onDismiss = { viewModel.setShowWalletModal(false) }
        )
    }

    /*if (uiState.showSuccessModal) {
        SuccessDialogModal(
            value = decimalFormatBalance.format(uiState.sentAmount).toString(),
            network = uiState.selectedNetwork,
            hash = uiState.hash,
            address = uiState.toAddress,
            sentCurrency = uiState.sentCurrency,
            onDismiss = { viewModel.setShowSuccessModal(false); viewModel.setHashValue("") }
        )
    }*/
    if (uiState.showSyncSuccessDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.setShowSyncSuccessDialog(false) },
            title = { Text("Sync successful") },
            text = { Text("Transactions were successfully synchronized.") },
            confirmButton = {
                Button(onClick = { viewModel.setShowSyncSuccessDialog(false) }) {
                    Text("OK")
                }
            },
            containerColor = MaterialTheme.colorScheme.background,
        )
    }

    if (uiState.showSyncErrorDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.setShowSyncErrorDialog(false) },
            title = { Text("Not able to sync") },
            text = { Text("An error occurred while doing sync.") },
            confirmButton = {
                Button(onClick = { viewModel.setShowSyncErrorDialog(false) }) {
                    Text("OK")
                }
            },
            containerColor = MaterialTheme.colorScheme.background,
        )
    }
}

/**
 * TransactionItem is a Composable function that displays a single transaction item.
 *
 * @param transaction The transaction data to display.
 * @param onClick The callback to invoke when the item is clicked.
 */
@Composable
fun TransactionItem(transaction: Transaction, navController: NavHostController) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .background(Color.DarkGray, shape = RoundedCornerShape(8.dp))
            .padding(8.dp)
            .clickable {
                navController.navigate("transaction/${transaction.id}")
            }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Request to access ${transaction.type}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Date: ${transaction.date}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            Text(
                text = transaction.status,
                style = MaterialTheme.typography.bodyMedium,
                color = when (transaction.status) {
                    "denied" -> Color(0xFFD32F2F)
                    "accepted" -> Color(0xFF388E3C)
                    "pending" -> Color(0xFF1976D2)
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )
        }
    }
}