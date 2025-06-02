package com.example.ethktprototype

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import android.util.Log
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

class LoginActivity : ComponentActivity() {
    private var isTokenReceived = false
    private lateinit var viewModel: WalletViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val application = applicationContext as HealthyWalletApplication
        val factory = WalletViewModelFactory(application)
        viewModel = ViewModelProvider(this, factory)[WalletViewModel::class.java]

        val authManager = MedPlumAPI(application)

        lifecycleScope.launch {
            authManager.handleRedirectAndExchange(intent) { accessToken ->
                isTokenReceived = true
                viewModel.storeMedPlumToken(accessToken)
                Log.d("LoginActivity", "Token received: $accessToken")
                navigateToMainActivity()
            }
        }
        setContent {
            Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Text("Logging in...")
            }
        }

    }

    override fun onResume() {
        super.onResume()
        if (isTokenReceived) {
            navigateToMainActivity()
        }
    }

    private fun navigateToMainActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}