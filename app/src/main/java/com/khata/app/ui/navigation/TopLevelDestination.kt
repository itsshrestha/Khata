package com.khata.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

/** The five bottom-navigation sections. Detail screens get their own routes in later phases. */
enum class TopLevelDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    Dashboard("dashboard", "Dashboard", Icons.Filled.Home),
    Customers("customers", "Customers", Icons.Filled.Person),
    Transactions("transactions", "Transactions", Icons.AutoMirrored.Filled.List),
    Reports("reports", "Reports", Icons.Filled.DateRange),
    Settings("settings", "Settings", Icons.Filled.Settings),
}
