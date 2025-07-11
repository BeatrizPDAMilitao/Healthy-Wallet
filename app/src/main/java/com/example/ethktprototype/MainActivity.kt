package com.example.ethktprototype

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ethktprototype.screens.ActivityScreen
import com.example.ethktprototype.screens.ImportWalletScreen
import com.example.ethktprototype.screens.SettingsScreen
import com.example.ethktprototype.screens.TokenListScreen
import com.example.ethktprototype.ui.theme.EthKtPrototypeTheme
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.core.content.ContextCompat
import com.example.ethktprototype.screens.CreateEHRScreen
import com.example.ethktprototype.screens.EHRsScreen
import com.example.ethktprototype.screens.ExamsScreen
import com.example.ethktprototype.screens.HealthSummaryScreen
import com.example.ethktprototype.screens.LoginScreen
import com.example.ethktprototype.screens.MedicationScreen
import com.example.ethktprototype.screens.PatientDetailsScreen
import com.example.ethktprototype.screens.PatientsListScreen
import com.example.ethktprototype.screens.PrescriptionsScreen
import com.example.ethktprototype.screens.SharedWithDoctorScreen
import com.example.ethktprototype.screens.TransactionScreen
import com.example.ethktprototype.screens.VaccinationsScreen
import kotlinx.coroutines.runBlocking

/**
 * MainActivity.kt
 * This is the main activity of the application. It sets up the navigation and handles the
 * permission request for notifications.
 *
 * @author Beatriz Militão
 * @version 1.0
 */
class MainActivity : ComponentActivity() {
    private lateinit var viewModel: WalletViewModel

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permission granted, you can send notifications
        } else {
            // Permission denied, notify the user
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val application = applicationContext as HealthyWalletApplication
        val factory = WalletViewModelFactory(application)
        viewModel = ViewModelProvider(this, factory)[WalletViewModel::class.java]
        runBlocking {
            viewModel.updateMedPlumToken()
        }
        viewModel.updateIsAppLoading(true)
        //Log.d("MedplumAuth", "token: ${viewModel.uiState.value.medPlumToken}")

        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED -> {
                // Permission granted, you can send notifications
            }
            else -> {
                // Request permission
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            val navController = rememberNavController()
            val uiState by viewModel.uiState.collectAsState()
            val startPoint = if (!uiState.mnemonicLoaded) "importWallet" else if (!uiState.medPlumToken) "loginScreen" else "EHRs"
            Log.d("MainActivityStartPoint", "Start Point: $startPoint")
            EthKtPrototypeTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    NavHost(navController = navController, startDestination = startPoint) {
                        composable("importWallet") {
                            ImportWalletScreen(
                                navController = navController,
                                viewModel = viewModel
                            )
                        }
                        composable("tokenList") {
                            TokenListScreen(navController, viewModel, application)
                        }
                        composable("settingsScreen") {
                            SettingsScreen(navController = navController, viewModel = viewModel)
                        }
                        composable("activity") {
                            ActivityScreen(navController = navController, viewModel = viewModel)
                        }
                        composable("EHRs") {
                            EHRsScreen(navController = navController, viewModel = viewModel)
                        }
                        composable("transaction/{transactionId}") { backStackEntry ->
                            val transactionId = backStackEntry.arguments?.getString("transactionId")
                            TransactionScreen(
                                navController = navController,
                                viewModel = viewModel,
                                transactionId = transactionId
                            )
                        }
                        composable("healthSummaryScreen") {
                            HealthSummaryScreen(navController = navController, viewModel = viewModel)
                        }
                        composable("examsScreen") {
                            ExamsScreen(navController = navController, viewModel = viewModel)
                        }
                        composable("prescriptionsScreen") {
                            PrescriptionsScreen(navController = navController, viewModel = viewModel)
                        }
                        composable("vaccinationsScreen") {
                            VaccinationsScreen(navController = navController, viewModel = viewModel)
                        }
                        composable("medicationScreen") {
                            MedicationScreen(navController = navController, viewModel = viewModel)
                        }
                        composable("loginScreen") {
                            LoginScreen(context = this@MainActivity, viewModel = viewModel)
                            //startActivity(Intent(this@MainActivity, LoginActivity::class.java))
                        }
                        composable("patientsListScreen") {
                            PatientsListScreen(navController = navController, viewModel = viewModel)
                        }
                        composable("patientDetails/{patientId}") { backStackEntry ->
                            val patientId = backStackEntry.arguments?.getString("patientId")
                            PatientDetailsScreen(navController = navController, viewModel = viewModel, patientId = patientId.toString())
                        }
                        composable("patientsListScreen"){
                            SharedWithDoctorScreen(navController = navController, viewModel = viewModel)
                        }
                        composable("createEHR/{patientId}") { backStackEntry ->
                            val patientId = backStackEntry.arguments?.getString("patientId")
                            CreateEHRScreen(navController = navController, viewModel = viewModel, patientId = patientId.toString())
                        }
                    }
                }
            }

            LaunchedEffect(navController) {
                intent?.getStringExtra("transactionId")?.let { transactionId ->
                    Log.d("MainActivityTransactionID", "Transaction ID: $transactionId")
                    navController.navigate("transaction/$transactionId")
                }
            }
        }
    }
}