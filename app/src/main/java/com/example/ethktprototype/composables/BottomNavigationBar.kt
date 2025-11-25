package com.example.ethktprototype.composables

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController

@Composable
fun BottomNavigationBar(
    navController: NavController,
    currentRoute: String
) {
   BottomNavigation(
       modifier = Modifier.fillMaxWidth(),
       backgroundColor = MaterialTheme.colorScheme.inverseOnSurface,
   ) {
       BottomNavigationItem(
           icon = { Icon(Icons.Filled.Wallet, contentDescription = "Wallet") },
           label = { Text("Wallet") },
           selected = currentRoute == "tokenList",
           onClick = { navController.navigate("tokenList") }
       )
       BottomNavigationItem(
           icon = { Icon(Icons.Filled.MedicalServices, contentDescription = "Home") },
           label = { Text("Home") },
           selected = currentRoute == "EHRs",
           onClick = { navController.navigate("EHRs") }
       )
       BottomNavigationItem(
           icon = { Icon(Icons.Filled.History, contentDescription = "Activity") },
           label = { Text("Activity") },
           selected = currentRoute == "activity",
           onClick = { navController.navigate("activity") }
       )
   }
}