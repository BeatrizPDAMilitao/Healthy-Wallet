package com.example.ethktprototype.screens

import android.util.Log
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import com.example.ethktprototype.WalletViewModel
import com.example.ethktprototype.composables.BottomNavigationBar
import com.example.ethktprototype.data.DiagnosticReportEntity
import kotlinx.coroutines.launch
import kotlin.code

@Composable
fun ExamsScreen(
    navController: NavHostController,
    viewModel: WalletViewModel,
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val patientId = viewModel.getLoggedInUsertId()
    val conditions by viewModel.conditions.collectAsState()
    val diagnosticReportsMap by viewModel.diagnosticReports.collectAsState()
    val diagnosticReports = diagnosticReportsMap[patientId] ?: emptyList()

    LaunchedEffect(true) {
        if (!viewModel.uiState.value.hasFetched.getOrDefault("DiagnosticReports", false)) {
            viewModel.getDiagnosticReports()
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
                        text = "Exams",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp,
                            color = Color.White
                        )
                    )
                    IconButton(onClick = {
                        viewModel.getDiagnosticReports()
                        viewModel.getPractitionersData()
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

            if (uiState.isDiagnosticReportsLoading) {
                androidx.compose.material3.CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(horizontal = 16.dp),
                    color = Color.White
                )
            }

            // Health Summary Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                if (diagnosticReports.isEmpty()) {
                    Text("No exams found.")
                } else {
                    ExamList(diagnosticReports, true, viewModel)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamList(diagnosticReports: List<DiagnosticReportEntity>, isPatient: Boolean, viewModel: WalletViewModel, showName: Boolean= false) {
    val practitioners by viewModel.practitioners.collectAsState()
    val subjectNames = remember { mutableStateOf<Map< String,String?>>(emptyMap()) }
    LaunchedEffect(diagnosticReports.map { it.subjectId }) {
        val subjectsId = diagnosticReports.map { it.subjectId }.distinct()
        Log.e("ExamList", "subjectsId: $subjectsId")
        subjectNames.value = viewModel.getSubjectsName(subjectsId)
        Log.e("ExamList", "subjectNames: $subjectNames")
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        items(diagnosticReports) { diagnostic ->
            var isExpanded by remember { mutableStateOf(false) }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clickable { isExpanded = !isExpanded },
                shape = RoundedCornerShape(12.dp),
                //elevation = 4.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            if (showName) {
                                Text(
                                    text = "Subject: ${subjectNames.value[diagnostic.subjectId] ?: "Unknown"}",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                            Text(
                                text = "Date: ${diagnostic.effectiveDateTime}",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Code: ${diagnostic.code}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Icon(
                            imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = if (isExpanded) "Collapse" else "Expand"
                        )
                    }

                    // Expandable content
                    if (isExpanded) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("ID: ${diagnostic.id}")
                        Text("Status: ${diagnostic.status}")
                        Text("Results: ${diagnostic.result}")

                        if (!isPatient) {
                            if (diagnostic.status == "NO_CONSENT") {
                                Spacer(modifier = Modifier.height(16.dp))
                                androidx.compose.material3.Button(
                                    onClick = {
                                        viewModel.viewModelScope.launch {
                                            viewModel.requestAccess(
                                                "DiagnosticReport/" + diagnostic.id
                                            )
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Request access")
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.height(16.dp))

                            var showModal by remember { mutableStateOf(false) }
                            var searchQuery by remember { mutableStateOf("") }

                            IconButton(
                                onClick = { showModal = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Share"
                                )
                            }

                            if (showModal) {
                                ModalBottomSheet(
                                    onDismissRequest = { showModal = false }
                                ) {
                                    LazyColumn(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp)
                                    ) {
                                        item {
                                            Text(
                                                text = "Share with Practitioner",
                                                style = MaterialTheme.typography.titleLarge,
                                                modifier = Modifier.padding(bottom = 16.dp)
                                            )
                                        }

                                        item {
                                            // Campo de pesquisa
                                            androidx.compose.material3.TextField(
                                                value = searchQuery,
                                                onValueChange = { searchQuery = it },
                                                label = { Text("Search by name") },
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }

                                        item {
                                            Spacer(modifier = Modifier.height(16.dp))
                                        }

                                        // Lista de practitioners
                                        val filteredPractitioners = practitioners.filter {
                                            it.name.contains(searchQuery, ignoreCase = true)
                                        }

                                        items(filteredPractitioners) { practitioner ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        viewModel.shareAccessWithPractitioner(practitioner.id, "DiagnosticReport/"+diagnostic.id)
                                                        showModal = false
                                                    }
                                                    .padding(vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "Dr." + practitioner.name,
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(8.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}