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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.ethktprototype.WalletViewModel
import com.example.ethktprototype.composables.BottomNavigationBar

@Composable
fun SharedWithDoctorScreen(
    navController: NavHostController,
    viewModel: WalletViewModel,
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val sharedEHRs by viewModel.sharedEHR.collectAsState()

    LaunchedEffect(true) {
        if (!viewModel.uiState.value.hasFetched.getOrDefault("SharedEHR", false)) {
            viewModel.getSharedEHRs()
        }
    }

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
                        text = "Shared with Me",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp,
                            color = Color.White
                        )
                    )
                    IconButton(onClick = {
                        viewModel.getSharedEHRs()
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

            if (uiState.isPatientLoading) {
                androidx.compose.material3.CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(horizontal = 16.dp),
                    color = Color.White
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                if (sharedEHRs.diagnosticReports.isEmpty()) {
                    Text("No diagnostic reports found.")
                } else {
                    ExamList(sharedEHRs.diagnosticReports, isPatient = false, viewModel, showName = true)
                }
            }
        }

        if (uiState.showSuccessModal) {
            AlertDialog(
                onDismissRequest = {
                    viewModel.setShowSuccessModal(false)
                    viewModel.setSuccessMessage("")
                },
                title = { Text("Success") },
                text = { Text(uiState.successMessage) },
                confirmButton = {
                    Button(onClick = {
                        viewModel.setShowSuccessModal(false)
                        viewModel.setSuccessMessage("")
                    }) {
                        Text("OK")
                    }
                },
                containerColor = MaterialTheme.colorScheme.background,
            )
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
                modifier = Modifier.align(Alignment.TopCenter)
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