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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
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
import androidx.compose.runtime.setValue
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
fun HealthSummaryScreen(
    navController: NavHostController,
    viewModel: WalletViewModel,
) {
    val patientId = "Patient/019706de-81bf-77d0-a864-2db46cad1d8c"
    val patientIdWitoutPrefix = patientId.split("/").last()

    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Collecting patient and health data from the ViewModel
    val patient by viewModel.patient.collectAsState()
    val conditions by viewModel.conditions.collectAsState()
    val diagnosticReports by viewModel.diagnosticReports.collectAsState()
    val allergies by viewModel.allergies.collectAsState()
    val medications by viewModel.medicationStatements.collectAsState()
    val procedures by viewModel.procedures.collectAsState()
    val devices by viewModel.devices.collectAsState()
    val immunizations by viewModel.immunizations.collectAsState()

    var showPatient by remember { mutableStateOf(false) }
    var showAllergies by remember { mutableStateOf(false) }
    var showDiagnosticReports by remember { mutableStateOf(false) }
    var showMedications by remember { mutableStateOf(false) }
    var showProcedures by remember { mutableStateOf(false) }
    var showDevices by remember { mutableStateOf(false) }
    var showImmunizations by remember { mutableStateOf(false) }


    LaunchedEffect(true) {
        //viewModel.getConditions("019706de-81c4-729c-8ad0-efc79193a8a8")
        //viewModel.getPatientComplete(patientIdWitoutPrefix)
        if (!viewModel.uiState.value.hasFetched.getOrDefault("HealthSummary", false)) {
            viewModel.getHealthSummaryData()
        }
        /* (!viewModel.uiState.value.hasFetchedDiagnosticReports) {
            viewModel.getDiagnosticReports()
        }
        if (!viewModel.uiState.value.hasFetchedAllergies) {
            viewModel.getAllergies()
        }
        if (!viewModel.uiState.value.hasFetchedMedicationStatements) {
            viewModel.getMedicationStatements()
        }
        if (!viewModel.uiState.value.hasFetchedProcedures) {
            viewModel.getProcedures()
        }
        if (!viewModel.uiState.value.hasFetchedDevices) {
            viewModel.getDevices()
        }
        if (!viewModel.uiState.value.hasFetchedImmunizations) {
            viewModel.getImmunizations()
        }*/
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
                        text = "Health Summary",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp,
                            color = Color.White
                        )
                    )
                    IconButton(onClick = {
                        /*viewModel.getPatientComplete()
                        viewModel.getDiagnosticReports()
                        viewModel.getAllergies()
                        viewModel.getMedicationStatements()
                        viewModel.getProcedures()
                        viewModel.getDevices()
                        viewModel.getImmunizations()*/
                        viewModel.getHealthSummaryData()
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

            if (uiState.isHealthSummaryLoading) {
                androidx.compose.material3.CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(horizontal = 16.dp),
                    color = Color.White
                )
            }

            Column(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Health Summary Content
                HealthSection(
                    title = "Identification",
                    isExpanded = showPatient,
                    isLoading = uiState.isPatientLoading,
                    onToggle = { showPatient = !showPatient }
                ) {
                    if (patient == null) {
                        Text("No information about the patient.")
                    } else {
                        Text("Name: ${patient!!.name}")
                        Text("Birth Date: ${patient!!.birthDate}")
                        Text("Gender: ${patient!!.gender}")
                        Text("Patient Number (ID): ${patient!!.identifier}")
                        if (patient!!.healthUnit.isEmpty() || patient!!.healthUnit == "") {
                            Text("Health Unit: Not specified")
                        }
                        else {
                            Text("Health Unit: ${patient!!.healthUnit}")
                        }
                        if (patient!!.doctor.isEmpty() || patient!!.doctor == "") {
                            Text("Responsible Doctor: Not specified")
                        }
                        else {
                            Text("Responsible Doctor: ${patient!!.doctor}")
                        }
                        Text("Address: ${patient!!.address}")
                    }
                }
                HealthSection(
                    title = "Allergies",
                    isExpanded = showAllergies,
                    isLoading = uiState.isAllergiesLoading,
                    onToggle = { showAllergies = !showAllergies }
                ) {
                    if (allergies.isEmpty()) {
                        Text("No registered allergies.")
                    } else {
                        allergies.forEach { allergy ->
                            Text("• ${allergy.code}")
                        }
                    }
                }
                HealthSection(
                    title = "Diagnostic Reports",
                    isExpanded = showDiagnosticReports,
                    isLoading = uiState.isDiagnosticReportsLoading,
                    onToggle = { showDiagnosticReports = !showDiagnosticReports }
                ) {
                    if (diagnosticReports.isEmpty()) {
                        Text("No exams.")
                    } else {
                        diagnosticReports.forEach { diagnostic ->
                            Text("• ${diagnostic.code}, ${diagnostic.effectiveDateTime}")
                        }
                    }
                }
                HealthSection(
                    title = "Medications",
                    isExpanded = showMedications,
                    isLoading = uiState.isMedicationStatementsLoading,
                    onToggle = { showMedications = !showMedications }
                ) {
                    if (medications.isEmpty()) {
                        Text("No recent medications.")
                    } else {
                        medications.forEach { medication ->
                            Text("• ${medication.medication}, ${medication.start} - ${medication.end}")
                        }
                    }
                }
                HealthSection(
                    title = "Medical Devices",
                    isExpanded = showDevices,
                    isLoading = uiState.isDevicesLoading,
                    onToggle = { showDevices = !showDevices }
                ) {
                    if (devices.isEmpty()) {
                        Text("No registered medical device.")
                    } else {
                        devices.forEach { device ->
                            Text("• ${device.type} (${device.status})")
                        }
                    }
                }
                HealthSection(
                    title = "Procedures",
                    isExpanded = showProcedures,
                    isLoading = uiState.isProceduresLoading,
                    onToggle = { showProcedures = !showProcedures }
                ) {
                    if (procedures.isEmpty()) {
                        Text("No recent procedures.")
                    } else {
                        procedures.forEach { procedure ->
                            Text("• ${procedure.code}, ${procedure.status}")
                        }
                    }
                }
                HealthSection(
                    title = "Immunizations",
                    isExpanded = showImmunizations,
                    isLoading = uiState.isImmunizationsLoading,
                    onToggle = { showImmunizations = !showImmunizations }
                ) {
                    if (procedures.isEmpty()) {
                        Text("No recent vaccines.")
                    } else {
                        immunizations.forEach { immunization ->
                            Text("• ${immunization.vaccine}, ${immunization.status}, ${immunization.occurrenceDateTime}")
                        }
                    }
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

@Composable
fun HealthSection(
    title: String,
    isExpanded: Boolean,
    isLoading: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggle() },
            shape = RoundedCornerShape(12.dp)
        ) {
            //TODO: Add column here
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(title, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Icon(
                        imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = "Toggle"
                    )
                }
                if (isExpanded) {
                    if (isLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            androidx.compose.material3.CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else {
                        content()
                    }
                }
            }
        }
    }
}
