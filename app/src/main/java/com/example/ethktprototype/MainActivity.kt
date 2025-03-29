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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

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

        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED -> {
                // Permissão já concedida, você pode enviar notificações
            }
            else -> {
                // Solicite a permissão
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            val navController = rememberNavController()
            val uiState by viewModel.uiState.collectAsState()
            val startPoint = if (!uiState.mnemonicLoaded) "importWallet" else "tokenList"

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
                    }
                }
            }
        }
    }
}