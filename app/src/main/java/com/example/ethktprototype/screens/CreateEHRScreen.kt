package com.example.ethktprototype.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.ethktprototype.data.AllergyIntoleranceEntity
import com.example.ethktprototype.data.ConditionEntity
import com.example.ethktprototype.data.DiagnosticReportEntity
import com.example.ethktprototype.data.ImmunizationEntity
import com.example.ethktprototype.data.MedicationRequestEntity
import com.example.ethktprototype.data.ObservationEntity
import com.example.ethktprototype.data.ProcedureEntity
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEHRScreen(
    navController: NavHostController,
    viewModel: WalletViewModel,
    patientId: String
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val ehrTypes = listOf("DiagnosticReport", "Immunization", "Allergy", "Condition", "Observation", "MedicationRequest", "Procedure")
    var selectedEHRType by remember { mutableStateOf("") }
    var formFields by remember { mutableStateOf(mapOf<String, String>()) }
    var errorMessage by remember { mutableStateOf("") }

    val statusOptions = listOf(
        "registered", "partial", "preliminary", "final",
        "amended", "corrected", "appended", "cancelled",
        "entered-in-error", "unknown"
    )
    var statusExpanded by remember { mutableStateOf(false) }

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
                Text(
                    text = "Create EHR",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        color = Color.White
                    )
                )
            }

            Spacer(modifier = Modifier.height(24.dp))


            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                EHRDropdownMenu(
                    ehrTypes = ehrTypes,
                    selectedEHRType = selectedEHRType,
                    onEHRTypeSelected = { selectedEHRType = it }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Campos dinâmicos baseados no tipo de EHR selecionado
                if (selectedEHRType.isNotEmpty()) {
                    when (selectedEHRType) {
                        "DiagnosticReport" -> {
                            TextFieldWithValidation(
                                label = "Code",
                                value = formFields["code"] ?: "",
                                onValueChange = { formFields = formFields + ("code" to it) }
                            )
                            val statusOptions = listOf(
                                "registered", "partial", "preliminary", "final",
                                "amended", "corrected", "appended", "cancelled",
                                "entered-in-error", "unknown"
                            )

                            var statusExpanded by remember { mutableStateOf(false) }

                            ExposedDropdownMenuBox(
                                expanded = statusExpanded,
                                onExpandedChange = { statusExpanded = !statusExpanded }
                            ) {
                                androidx.compose.material3.TextField(
                                    readOnly = true,
                                    value = formFields["status"] ?: "",
                                    onValueChange = {},
                                    label = { Text("Status") },
                                    trailingIcon = {
                                        Icon(
                                            imageVector = if (statusExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                            contentDescription = null
                                        )
                                    },
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth()
                                )

                                ExposedDropdownMenu(
                                    expanded = statusExpanded,
                                    onDismissRequest = { statusExpanded = false }
                                ) {
                                    statusOptions.forEach { status ->
                                        DropdownMenuItem(
                                            text = { Text(status) },
                                            onClick = {
                                                formFields = formFields + ("status" to status)
                                                statusExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            TextFieldWithValidation(
                                label = "Effective DateTime",
                                value = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date()),
                                onValueChange = {}
                            )
                            TextFieldWithValidation(
                                label = "Result",
                                value = formFields["result"] ?: "",
                                onValueChange = { formFields = formFields + ("result" to it) }
                            )
                            formFields = formFields + ("effectiveDateTime" to SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault()).format(Date()))
                            formFields = formFields + ("subject" to patientId)
                            formFields = formFields + ("performer" to viewModel.getLoggedInUsertId())
                        }
                        /*"Immunization" -> {
                            TextFieldWithValidation(
                                label = "Vaccine",
                                value = formFields["vaccine"] ?: "",
                                onValueChange = { formFields = formFields + ("vaccine" to it) }
                            )
                            TextFieldWithValidation(
                                label = "Occurrence DateTime",
                                value = formFields["occurrenceDateTime"] ?: "",
                                onValueChange = { formFields = formFields + ("occurrenceDateTime" to it) }
                            )
                            TextFieldWithValidation(
                                label = "Status",
                                value = formFields["status"] ?: "",
                                onValueChange = { formFields = formFields + ("status" to it) }
                            )
                            TextFieldWithValidation(
                                label = "Lot Number",
                                value = formFields["lotNumber"] ?: "",
                                onValueChange = { formFields = formFields + ("lotNumber" to it) }
                            )
                        }
                        "Allergy" -> {
                            TextFieldWithValidation(
                                label = "Code",
                                value = formFields["code"] ?: "",
                                onValueChange = { formFields = formFields + ("code" to it) }
                            )
                            TextFieldWithValidation(
                                label = "Status",
                                value = formFields["status"] ?: "",
                                onValueChange = { formFields = formFields + ("status" to it) }
                            )
                            TextFieldWithValidation(
                                label = "Onset",
                                value = formFields["onset"] ?: "",
                                onValueChange = { formFields = formFields + ("onset" to it) }
                            )
                            TextFieldWithValidation(
                                label = "Recorded Date",
                                value = formFields["recordedDate"] ?: "",
                                onValueChange = { formFields = formFields + ("recordedDate" to it) }
                            )
                        }
                        "Condition" -> {
                            TextFieldWithValidation(
                                label = "Code",
                                value = formFields["code"] ?: "",
                                onValueChange = { formFields = formFields + ("code" to it) }
                            )
                            TextFieldWithValidation(
                                label = "Subject ID",
                                value = formFields["subjectId"] ?: "",
                                onValueChange = { formFields = formFields + ("subjectId" to it) }
                            )
                            TextFieldWithValidation(
                                label = "Onset DateTime",
                                value = formFields["onsetDateTime"] ?: "",
                                onValueChange = { formFields = formFields + ("onsetDateTime" to it) }
                            )
                            TextFieldWithValidation(
                                label = "Clinical Status",
                                value = formFields["clinicalStatus"] ?: "",
                                onValueChange = { formFields = formFields + ("clinicalStatus" to it) }
                            )
                            TextFieldWithValidation(
                                label = "Recorder ID",
                                value = formFields["recorderId"] ?: "",
                                onValueChange = { formFields = formFields + ("recorderId" to it) }
                            )
                        }
                        "Observation" -> {
                            TextFieldWithValidation(
                                label = "Status",
                                value = formFields["status"] ?: "",
                                onValueChange = { formFields = formFields + ("status" to it) }
                            )
                            TextFieldWithValidation(
                                label = "Code",
                                value = formFields["code"] ?: "",
                                onValueChange = { formFields = formFields + ("code" to it) }
                            )
                            TextFieldWithValidation(
                                label = "Subject ID",
                                value = formFields["subjectId"] ?: "",
                                onValueChange = { formFields = formFields + ("subjectId" to it) }
                            )
                            TextFieldWithValidation(
                                label = "Effective DateTime",
                                value = formFields["effectiveDateTime"] ?: "",
                                onValueChange = { formFields = formFields + ("effectiveDateTime" to it) }
                            )
                            TextFieldWithValidation(
                                label = "Value Quantity",
                                value = formFields["valueQuantity"] ?: "",
                                onValueChange = { formFields = formFields + ("valueQuantity" to it) }
                            )
                            TextFieldWithValidation(
                                label = "Unit",
                                value = formFields["unit"] ?: "",
                                onValueChange = { formFields = formFields + ("unit" to it) }
                            )
                        }
                        "MedicationRequest" -> {
                            TextFieldWithValidation(
                                label = "Medication",
                                value = formFields["medication"] ?: "",
                                onValueChange = { formFields = formFields + ("medication" to it) }
                            )
                            TextFieldWithValidation(
                                label = "Authored On",
                                value = formFields["authoredOn"] ?: "",
                                onValueChange = { formFields = formFields + ("authoredOn" to it) }
                            )
                            TextFieldWithValidation(
                                label = "Status",
                                value = formFields["status"] ?: "",
                                onValueChange = { formFields = formFields + ("status" to it) }
                            )
                            TextFieldWithValidation(
                                label = "Dosage",
                                value = formFields["dosage"] ?: "",
                                onValueChange = { formFields = formFields + ("dosage" to it) }
                            )
                            formFields = formFields + ("subject" to patientId)
                        }
                        "Procedure" -> {
                            TextFieldWithValidation(
                                label = "Procedure Code",
                                value = formFields["procedureCode"] ?: "",
                                onValueChange = { formFields = formFields + ("procedureCode" to it) }
                            )
                            TextFieldWithValidation(
                                label = "Status",
                                value = formFields["status"] ?: "",
                                onValueChange = { formFields = formFields + ("status" to it) }
                            )
                            TextFieldWithValidation(
                                label = "Performed DateTime",
                                value = formFields["performedDateTime"] ?: "",
                                onValueChange = { formFields = formFields + ("performedDateTime" to it) }
                            )
                        }*/
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Botão para criar o EHR
                androidx.compose.material3.Button(
                    onClick = {
                        val isValid = validateForm(selectedEHRType, formFields)
                        Log.d("CreateEHRScreen", "Validation: $isValid, Selected EHR Type: $selectedEHRType, Form Fields: $formFields")
                        if (validateForm(selectedEHRType, formFields)) {
                            val record = when (selectedEHRType) {
                                "DiagnosticReport" -> DiagnosticReportEntity(
                                    id = formFields["id"] ?: "",
                                    code = formFields["code"] ?: "",
                                    status = formFields["status"] ?: "",
                                    effectiveDateTime = formFields["effectiveDateTime"] ?: "",
                                    result = formFields["result"] ?: ""
                                )
                                "Immunization" -> ImmunizationEntity(
                                    id = formFields["id"] ?: "",
                                    vaccine = formFields["vaccine"] ?: "",
                                    occurrenceDateTime = formFields["occurrenceDateTime"] ?: "",
                                    status = formFields["status"] ?: "",
                                    lotNumber = formFields["lotNumber"] ?: ""
                                )
                                "Allergy" -> AllergyIntoleranceEntity(
                                    id = formFields["id"] ?: "",
                                    code = formFields["code"] ?: "",
                                    status = formFields["status"] ?: "",
                                    onset = formFields["onset"] ?: "",
                                    recordedDate = formFields["recordedDate"] ?: ""
                                )
                                "Condition" -> ConditionEntity(
                                    id = formFields["id"] ?: "",
                                    code = formFields["code"] ?: "",
                                    subjectId = formFields["subjectId"] ?: "",
                                    onsetDateTime = formFields["onsetDateTime"] ?: "",
                                    clinicalStatus = formFields["clinicalStatus"] ?: "",
                                    recorderId = formFields["recorderId"] ?: ""
                                )
                                "Observation" -> ObservationEntity(
                                    id = formFields["id"] ?: "",
                                    status = formFields["status"] ?: "",
                                    code = formFields["code"] ?: "",
                                    subjectId = formFields["subjectId"] ?: "",
                                    effectiveDateTime = formFields["effectiveDateTime"] ?: "",
                                    valueQuantity = formFields["valueQuantity"] ?: "",
                                    unit = formFields["unit"]
                                )
                                "MedicationRequest" -> MedicationRequestEntity(
                                    id = formFields["id"] ?: "",
                                    medication = formFields["medication"] ?: "",
                                    authoredOn = formFields["authoredOn"] ?: "",
                                    status = formFields["status"] ?: "",
                                    dosage = formFields["dosage"] ?: ""
                                )
                                "Procedure" -> ProcedureEntity(
                                    id = formFields["id"] ?: "",
                                    code = formFields["procedureCode"] ?: "",
                                    status = formFields["status"] ?: "",
                                    performedDateTime = formFields["performedDateTime"] ?: ""
                                )
                                else -> null
                            }

                            if (record != null) {
                                val recordHash = viewModel.calculateRecordHash(formFields)
                                viewModel.viewModelScope.launch {
                                    viewModel.callCreateRecordContract(selectedEHRType, record, recordHash, patientId)
                                }
                            }
                            navController.popBackStack()
                        } else {
                            errorMessage = "Please fill in all required fields."
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Create EHR")
                }

                if (errorMessage.isNotEmpty()) {
                    Text(
                        text = errorMessage,
                        color = Color.Red,
                        style = MaterialTheme.typography.bodyMedium
                    )
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

fun validateForm(ehrType: String, fields: Map<String, String>): Boolean {
    return when (ehrType) {
        "DiagnosticReport" -> fields["code"].isNullOrBlank().not() && fields["status"].isNullOrBlank().not() && fields["result"].isNullOrBlank().not()
        "MedicationRequest" -> fields["medication"].isNullOrBlank().not() && fields["authoredOn"].isNullOrBlank().not() && fields["status"].isNullOrBlank().not() && fields["dosage"].isNullOrBlank().not()
        "Immunization" -> fields["vaccine"].isNullOrBlank().not() && fields["occurrenceDateTime"].isNullOrBlank().not() && fields["status"].isNullOrBlank().not() && fields["lotNumber"].isNullOrBlank().not()
        "Allergy" -> fields["code"].isNullOrBlank().not() && fields["status"].isNullOrBlank().not() && fields["onset"].isNullOrBlank().not() && fields["recordedDate"].isNullOrBlank().not()
        "Condition" -> fields["code"].isNullOrBlank().not() && fields["subjectId"].isNullOrBlank().not() && fields["onsetDateTime"].isNullOrBlank().not() && fields["clinicalStatus"].isNullOrBlank().not() && fields["recorderId"].isNullOrBlank().not()
        "Observation" -> fields["status"].isNullOrBlank().not() && fields["code"].isNullOrBlank().not() && fields["subjectId"].isNullOrBlank().not() && fields["effectiveDateTime"].isNullOrBlank().not() && fields["valueQuantity"].isNullOrBlank().not()
        "Procedure" -> fields["procedureCode"].isNullOrBlank().not() && fields["status"].isNullOrBlank().not() && fields["performedDateTime"].isNullOrBlank().not()
        else -> false
    }
}

// Composable para TextField com validação
@Composable
fun TextFieldWithValidation(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    androidx.compose.material3.TextField(
        value = value,
        onValueChange = { input ->
            if (input.matches(Regex("^[a-zA-Z0-9\\s]*$"))) { // Evitar caracteres perigosos
                onValueChange(input)
            }
        },
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth()
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EHRDropdownMenu(
    ehrTypes: List<String>,
    selectedEHRType: String,
    onEHRTypeSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        androidx.compose.material3.TextField(
            value = selectedEHRType,
            onValueChange = {},
            readOnly = true,
            label = { Text("Select EHR Type") },
            trailingIcon = {
                Icon(
                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = null
                )
            },
            modifier = Modifier
                .menuAnchor() // Required for positioning the dropdown
                .fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            ehrTypes.forEach { type ->
                DropdownMenuItem(
                    text = { Text(type) },
                    onClick = {
                        onEHRTypeSelected(type)
                        expanded = false
                    }
                )
            }
        }
    }
}

