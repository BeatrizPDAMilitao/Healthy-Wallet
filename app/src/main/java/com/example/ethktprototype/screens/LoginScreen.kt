package com.example.ethktprototype.screens

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ethktprototype.HealthyWalletApplication
import com.example.ethktprototype.MedPlumAPI
import com.example.ethktprototype.WalletViewModel

@Composable
fun LoginScreen(context: Context, viewModel: WalletViewModel) {
    val application = context.applicationContext as HealthyWalletApplication

    val authManager = MedPlumAPI(application)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Login to MedPlum",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Button(
            onClick = {
                authManager.launchLogin(context as ComponentActivity)
            },
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text("Login")
        }
    }
}