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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button

import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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

/**
 * EHRsScreen.kt
 */
@Composable
fun EHRsScreen(
    navController: NavHostController,
    viewModel: WalletViewModel,
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val patientId = "Patient/019706de-81bf-77d0-a864-2db46cad1d8c"

    val patient = viewModel.patient.collectAsState()

    LaunchedEffect(true) {
        if (!viewModel.uiState.value.hasFetched.getOrDefault("Patient", false)) {
            viewModel.getPatientComplete()
        }
    }

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
                        Text(
                            text = "Welcome ${patient.value?.name ?: "Patient"}!",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 24.sp,
                                color = Color.White
                            )
                        )
                        IconButton(onClick = {
                            viewModel.getPatientComplete()
                        }) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh",
                                tint = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
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
                            Text("🏥 Health Unit: Unknown", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 18.sp))
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

                /// Test Button: Uncomment to test
                /*Column (modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = { viewModel.testFetchPrescriptions(patientId) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .height(52.dp)
                    ) {
                        Text(
                            text = "Test fetch prescriptions 30 times.",
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