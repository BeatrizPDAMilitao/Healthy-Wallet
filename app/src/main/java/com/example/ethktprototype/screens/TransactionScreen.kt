package com.example.ethktprototype.screens

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.AlertDialog

import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.ethktprototype.WalletViewModel
import androidx.compose.ui.platform.LocalContext
import com.example.ethktprototype.data.Transaction
import com.example.ethktprototype.nexus.ProofBridge
import com.example.ethktprototype.nexus.extractGuestElf
import com.example.ethktprototype.nexus.generateProof
import net.glxn.qrgen.android.QRCode
import java.io.File
import java.net.InetAddress

/**
 * TransactionScreen is a Composable function that displays the transaction details.
 *
 * @param navController The NavHostController used for navigation.
 * @param viewModel The WalletViewModel instance used to manage the wallet state.
 * @param transactionId The ID of the transaction to be displayed.
 */
@Composable
fun TransactionScreen(
    navController: NavHostController,
    viewModel: WalletViewModel,
    transactionId: String?
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Log.d("MainActivityTransactionID", "Available transactions: ${viewModel.getTransactions()}")
    Log.d("ExampleTestSample", "Looking for transaction ID: $transactionId")

    val transaction = remember { mutableStateOf<Transaction?>(null) }

    LaunchedEffect(transactionId) {
        transaction.value = viewModel.getTransactionById(transactionId.toString())
        Log.d("ExampleTestSample", "Found transaction: ${transaction.value?.id}")
    }

    LaunchedEffect(uiState.transactions) {
        transaction.value = viewModel.getTransactionById(transactionId.toString())
    }

    Box(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(vertical = 8.dp, horizontal = 16.dp)
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
                        uiState.walletAddress.take(5) + "..." + uiState.walletAddress.takeLast(4)
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
                        contentDescription = "Settings",
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (uiState.isTransactionProcessing) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            if (transaction.value != null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .background(MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(8.dp))
                        .padding(16.dp)
                ) {
                    if (!transaction.value!!.conditions.isNullOrEmpty()) {
                        Text(
                            text = "Practitioner ${transaction.value!!.practitionerId} requested proof for ${transaction.value!!.type}",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    else {
                        Text(
                            text = "Practitioner ${transaction.value!!.practitionerId} requested access to your ${transaction.value!!.type}",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Transaction ID: ${transaction.value!!.id}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Date: ${transaction.value!!.date}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = {
                            viewModel.getPractitionerData(transaction.value!!.practitionerId);
                            viewModel.setShowDataDialog(true)
                        },
                    ) {
                        Text(
                            text = "Practitioner ID: ${transaction.value!!.practitionerId}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Patient ID: ${transaction.value!!.patientId}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    /*if (transaction.value!!.practitionerAddress == uiState.walletAddress) {
                        Text(
                            text = "You are the patient",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }*/
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Type: ${transaction.value!!.type}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Status: ${transaction.value!!.status}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = when (transaction.value!!.status) {
                            "denied" -> Color(0xFFD32F2F)
                            "accepted" -> Color(0xFF388E3C)
                            "pending" -> Color(0xFF1976D2)
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                    )
                    Log.d("ZKP", "Conditions: ${transaction.value!!.conditions}")
                    if (!transaction.value!!.conditions.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("ZKP Required for:")
                        transaction.value!!.conditions!!.forEach {
                            Text("- ${it.type}")
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = {
                            val elfFile = extractGuestElf(context)
                            val input = "15"
                            val result = ProofBridge.runProof(elfFile.absolutePath, input)
                            Log.d("ZKP", "Proof Result: $result")
                            //val ip = InetAddress.getLocalHost().hostAddress
                            val proofUrl = "http://localhost:8080/proof" // manually resolve IP

                            val qrBitmap = QRCode.from(proofUrl).bitmap()
                            uiState.qrCode = qrBitmap
                            // Display the QR code

                            Log.d("ZKP", "Simulating ZKP proof generation...")
                        },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                                .background(MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(8.dp)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Generate ZKP Proof")
                        }
                    }
                    else if (transaction.value!!.status == "pending") {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row {
                            Button(
                                onClick = {
                                    // Handle accept
                                    transaction.let { transaction ->
                                        val transactionId = transaction.value!!.id
                                        val recordId = transaction.value!!.recordId
                                        val requester = transaction.value!!.practitionerAddress
                                        viewModel.callAcceptContract(transactionId, recordId, requester)
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF03A9F4))
                            ) {
                                Text("Accept")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    // Handle deny
                                    transaction.let { transaction ->
                                        val transactionId = transaction.value!!.id
                                        val recordId = transaction.value!!.recordId
                                        val requester = transaction.value!!.practitionerAddress
                                        viewModel.callDenyContract(transactionId, recordId, requester)
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF03A9F4))

                            ) {
                                Text("Deny")
                            }
                        }
                    }
                }
            } else {
                Text(
                    text = "Transaction not found",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        BottomNavigation(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
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
        if (uiState.showDenyDialog) {
            // Show the deny dialog
            AlertDialog(
                onDismissRequest = { viewModel.setShowDenyDialog(false) },
                title = { Text("Transaction Details") },
                text = {
                    Column {
                        Text("Hash: ${uiState.transactionHash}")
                        //Text("Date: ${uiState.transactionHash}")
                        //Text("Status: ${uiState.transactionHash}")
                    }
                },
                confirmButton = {
                    Button(onClick = { viewModel.setShowDenyDialog(false) }) {
                        Text("OK")
                    }
                },
                backgroundColor = MaterialTheme.colorScheme.background,
            )
        }
        if (uiState.showDataDialog) {
            // Show the data dialog
            AlertDialog(
                onDismissRequest = { viewModel.setShowDataDialog(false) },
                title = { Text("Practitioner Data") },
                text = {
                    Column {
                        Text("Practitioner ID: ${uiState.practitionerData?.id}")
                        Text("Name: ${uiState.practitionerData?.name}")
                        Text("Telecom: ${uiState.practitionerData?.telecom}")
                    }
                },
                confirmButton = {
                    Button(onClick = { viewModel.setShowDataDialog(false) }) {
                        Text("OK")
                    }
                },
                backgroundColor = MaterialTheme.colorScheme.background,
            )
        }
        if (uiState.qrCode != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Image(
                bitmap = uiState.qrCode!!.asImageBitmap(),
                contentDescription = "QR Code",
                modifier = Modifier
                    .size(300.dp)
                    .align(Alignment.Center),
                contentScale = ContentScale.Fit
            )
        }
    }
}