package com.example.ethktprototype.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button

import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.ethktprototype.WalletViewModel
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import com.example.ethktprototype.HealthyWalletApplication
import com.example.ethktprototype.MedPlumAPI
import com.example.ethktprototype.composables.BottomNavigationBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.runtime.remember


/**
 * EHRsScreen.kt
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EHRsScreen(
    navController: NavHostController,
    viewModel: WalletViewModel,
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val patient = viewModel.patient.collectAsState()

    val practitioner = viewModel.practitioner.collectAsState()

    val isRefreshing = uiState.isAppLoading
    val pullRefreshState = remember { PullToRefreshState() }


    LaunchedEffect(true) {
        if (!viewModel.uiState.value.hasFetched.getOrDefault("Patient", false)) {
            viewModel.getUser()
        }
    }

    /*LaunchedEffect(pullRefreshState.isAnimating) {
        if (pullRefreshState.isAnimating) {
            viewModel.getUser()
            pullRefreshState.() // Important: End the refresh animation
        }
    }*/

    if (uiState.isAppLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Loading...",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                )
            }
        }
    }
    else {
        Box(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(bottom = 56.dp)
            ) {
                // Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.9f))
                        .padding(vertical = 24.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        if (patient.value != null) {
                            Text(
                                text = "Welcome!",// ${patient.value?.name ?: "Patient"}!",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 24.sp,
                                    color = Color.White
                                )
                            )
                        }
                        else if (practitioner.value != null) {
                            Text(
                                text = "Practitioner: ${practitioner.value?.name ?: "Practitioner"}!",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 16.sp,
                                    color = Color.White
                                )
                            )
                        }
                        else {
                            Text(
                                text = "User: Unknown",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 16.sp,
                                    color = Color.White
                                )
                            )
                        }
                        IconButton(onClick = {
                            viewModel.getUser()
                        }) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh",
                                tint = Color.White
                            )
                        }
                        IconButton(onClick = {
                            navController.navigate("profile")
                        }) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Profile",
                                tint = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (patient.value != null) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .padding(horizontal = 20.dp)
                            .fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            if (patient.value?.doctor == null || patient.value?.doctor == "") {
                                Text("👨‍⚕️ Doctor: Unknown", style = MaterialTheme.typography.bodyMedium.copy(fontSize = 18.sp))
                            }
                            else {
                                Text(
                                    "👨‍⚕️ Doctor: ${patient.value?.doctor}",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            if (patient.value?.healthUnit == null || patient.value?.healthUnit == "") {
                                Text("🏥 Health Unit: Unknown", style = MaterialTheme.typography.bodyMedium.copy(fontSize = 18.sp))
                            }
                            else {
                                Text("🏥 Health Unit: ${patient.value?.healthUnit}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 18.sp))
                            }
                        }
                    }
                    // Feature Buttons
                    Spacer(modifier = Modifier.height(24.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                    ) {
                        val features = listOf(
                            "Health Summary" to "healthSummaryScreen",
                            "Exams" to "examsScreen",
                            "Prescriptions" to "prescriptionsScreen",
                            "Regular Medication" to "medicationScreen",
                            "Vaccination Record" to "vaccinationsScreen"
                        )

                        features.chunked(2).forEach { row ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                row.forEach { (label, route) ->
                                    Card(
                                        shape = RoundedCornerShape(18.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(100.dp)
                                            .clickable { navController.navigate(route) },
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                                        elevation = CardDefaults.cardElevation(2.dp)
                                    ) {
                                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            Text(
                                                text = label,
                                                style = MaterialTheme.typography.bodyLarge.copy(
                                                    fontWeight = FontWeight.Medium
                                                ),
                                                modifier = Modifier.padding(8.dp),
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                                if (row.size == 1) Spacer(modifier = Modifier.weight(1f)) // Fill space in odd row
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }

                if (practitioner.value != null) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .padding(horizontal = 20.dp)
                            .fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Practitioner: ${practitioner.value?.name ?: "Unknown"}",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Qualification: ${practitioner.value?.qualification ?: "Unknown"}",
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 18.sp)
                            )
                        }
                    }
                    // Feature Buttons
                    Spacer(modifier = Modifier.height(24.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                    ) {
                        val features = listOf(
                            "My Patients" to "patientsListScreen",
                            "Shared With Me" to "sharedWithDoctor",
                        )

                        features.chunked(2).forEach { row ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                row.forEach { (label, route) ->
                                    Card(
                                        shape = RoundedCornerShape(18.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(100.dp)
                                            .clickable { navController.navigate(route) },
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                                        elevation = CardDefaults.cardElevation(2.dp)
                                    ) {
                                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            Text(
                                                text = label,
                                                style = MaterialTheme.typography.bodyLarge.copy(
                                                    fontWeight = FontWeight.Medium
                                                ),
                                                modifier = Modifier.padding(8.dp),
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                                if (row.size == 1) Spacer(modifier = Modifier.weight(1f)) // Fill space in odd row
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }

                /// Test Button: Uncomment to test
                /*Column (modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = { viewModel.testFetchPrescriptions(patient.value?.id ?: "") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .height(52.dp)
                    ) {
                        Text(
                            text = "Test Read.",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
                        )
                    }
                    Button(
                        onClick = { viewModel.getSimulateTransactionFees() },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .height(52.dp)
                    ) {
                        Text(
                            text = "Get gas fees",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
                        )
                    }
                    Button(
                        onClick = { viewModel.showSimulateTransactionTimes() },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .height(52.dp)
                    ) {
                        Text(
                            text = "Get times",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
                        )
                    }
                }*/

                val application = context.applicationContext as HealthyWalletApplication
                val authManager = MedPlumAPI(application, viewModel)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Button(
                        onClick = {
                            authManager.logout(context)
                            navController.navigate("loginScreen") {
                                popUpTo("healthSummaryScreen") { inclusive = true }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                    ) {
                        Text("Logout")
                    }
                }
            }
            if (uiState.showErrorModal) {
                val snackbarHostState = remember { SnackbarHostState() }

                LaunchedEffect(uiState.showErrorModal) {
                    snackbarHostState.showSnackbar(uiState.errorMessage)
                    viewModel.setShowErrorModal(false)
                }

                SnackbarHost(
                    hostState = snackbarHostState,
                    snackbar = { snackbarData ->
                        Snackbar(
                            containerColor = Color.Red,
                            contentColor = Color.White,
                            actionColor = Color.White,
                            snackbarData = snackbarData
                        )
                    },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }

            // Bottom Navigation Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
            ) {
                BottomNavigationBar(
                    navController = navController,
                    currentRoute = "EHRs"
                )
            }
        }
    }
}