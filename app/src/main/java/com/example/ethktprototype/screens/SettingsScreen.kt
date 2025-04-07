package com.example.ethktprototype.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.ethktprototype.WalletViewModel
import com.example.ethktprototype.composables.ClearBlocklist
import com.example.ethktprototype.composables.RemoveTransactions
import com.example.ethktprototype.composables.RemoveWallet
import com.example.ethktprototype.composables.ResetContractCounter
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items

@Composable
fun SettingsScreen(navController: NavHostController, viewModel: WalletViewModel) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(22.dp)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        item {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
        item { RemoveWallet(viewModel = viewModel) }
        item { Spacer(modifier = Modifier.height(8.dp)) }
        item { ClearBlocklist(viewModel = viewModel) }
        item { Spacer(modifier = Modifier.height(8.dp)) }
        item { RemoveTransactions(viewModel = viewModel) }
        item { Spacer(modifier = Modifier.height(8.dp)) }
        item { ResetContractCounter(viewModel = viewModel) }
    }
}