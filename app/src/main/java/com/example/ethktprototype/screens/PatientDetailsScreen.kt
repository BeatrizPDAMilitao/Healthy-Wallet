package com.example.ethktprototype.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.ethktprototype.WalletViewModel
import com.example.ethktprototype.composables.BottomNavigationBar
import com.example.ethktprototype.data.PatientEntity

@Composable
fun PatientDetailsScreen(
    navController: NavHostController,
    viewModel: WalletViewModel,
    patientId: String
) {
    val uiState by viewModel.uiState.collectAsState()
    val patient = remember { mutableStateOf<PatientEntity?>(null) }

    val diagnosticReportsMap by viewModel.diagnosticReports.collectAsState()
    val diagnosticReports = diagnosticReportsMap[patientId] ?: emptyList()


    LaunchedEffect(patientId) {
        patient.value = viewModel.getPatientById(patientId)
        if (!viewModel.uiState.value.hasFetched.getOrDefault("Patient", false)) {
            viewModel.getPatientListForPractitioner() //Isto está bem??
        }
        if (!viewModel.uiState.value.hasFetched.getOrDefault("DiagnosticReports", false)) {
            viewModel.getDiagnosticReports(patientId, isPractitioner= true)
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Patient Details",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                IconButton(onClick = {
                    viewModel.getDiagnosticReports(patientId, isPractitioner= true)
                }) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = Color.White
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))

            if (uiState.isDiagnosticReportsLoading) {
                androidx.compose.material3.CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(horizontal = 16.dp),
                    color = Color.White
                )
            }
            patient.let {
                Text("Name: ${it.value?.name}")
                Text("Gender: ${it.value?.gender}")
                Text("Birth Date: ${it.value?.birthDate}")
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                Button(
                    onClick = {
                        navController.navigate("createEHR/${patientId}")
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Create New EHR")
                }
                if (diagnosticReports.isEmpty()) {
                    Text("No exams found.")
                } else {
                    ExamList(diagnosticReports, false, viewModel)
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