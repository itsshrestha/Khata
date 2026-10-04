package com.khata.app.ui.settings

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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.khata.app.data.sync.AuthState
import com.khata.app.data.sync.SyncState
import com.khata.app.ui.LocalAppContainer
import com.khata.app.utils.CurrencyFormatter
import com.khata.app.utils.DateFormatter
import java.time.Instant
import java.time.ZoneId

@Composable
fun SettingsRoute() {
    val container = LocalAppContainer.current
    val viewModel: SettingsViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                SettingsViewModel(
                    customerRepository = container.customerRepository,
                    transactionRepository = container.transactionRepository,
                    authManager = container.authManager,
                    syncManager = container.syncManager,
                )
            }
        },
    )
    val state = viewModel.uiState.collectAsStateWithLifecycle().value
    SettingsScreen(
        state = state,
        currency = container.currencyFormatter,
        onSyncNow = viewModel::triggerSyncNow,
        onSignIn = viewModel::signIn,
        onSignUp = viewModel::signUp,
        onSignOut = viewModel::signOut,
        onClearAuthError = viewModel::clearAuthError,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    currency: CurrencyFormatter,
    onSyncNow: () -> Unit = {},
    onSignIn: (String, String) -> Unit = { _, _ -> },
    onSignUp: (String, String) -> Unit = { _, _ -> },
    onSignOut: () -> Unit = {},
    onClearAuthError: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var showAuthDialog by remember { mutableStateOf(false) }
    var isRegisterMode by remember { mutableStateOf(false) }
    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }

    LaunchedEffect(state.authError) {
        state.authError?.let { err ->
            snackbarHostState.showSnackbar(err)
            onClearAuthError()
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { TopAppBar(title = { Text("Settings & Cloud Sync") }) },
    ) { innerPadding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(innerPadding), Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Shop Header Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Icon(
                            Icons.Filled.Home,
                            contentDescription = null,
                            modifier = Modifier.padding(4.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Column {
                            Text(
                                text = state.shopName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = "Currency: Nepalese / Indian Rupee (Rs.)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                            )
                        }
                    }
                }

                // Cloud Sync Section
                Text(text = "Cloud Synchronization", style = MaterialTheme.typography.titleLarge)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(Icons.Filled.Refresh, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Text(text = "Sync Status", style = MaterialTheme.typography.titleMedium)
                            }
                            SyncStatusBadge(state.syncState)
                        }

                        val accountText = when (val auth = state.authState) {
                            is AuthState.Authenticated -> auth.email ?: "Signed In"
                            is AuthState.Loading -> "Checking authentication..."
                            AuthState.SignedOut -> "Offline (Signed Out)"
                        }
                        SettingInfoRow(label = "Account", value = accountText)

                        val lastSyncText = state.lastSyncTimestamp?.let {
                            DateFormatter.full(Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate())
                        } ?: "Never"
                        SettingInfoRow(label = "Last Synchronized", value = lastSyncText)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Button(
                                onClick = onSyncNow,
                                modifier = Modifier.weight(1f),
                                enabled = state.syncState !is SyncState.Syncing,
                            ) {
                                Icon(Icons.Filled.Refresh, contentDescription = null)
                                Spacer(Modifier.padding(4.dp))
                                Text("Sync Now")
                            }

                            if (state.authState is AuthState.Authenticated) {
                                OutlinedButton(
                                    onClick = onSignOut,
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text("Sign Out")
                                }
                            } else {
                                OutlinedButton(
                                    onClick = { showAuthDialog = true },
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text("Sign In / Register")
                                }
                            }
                        }
                    }
                }

                // Ledger Stats Summary
                Text(text = "Ledger Summary", style = MaterialTheme.typography.titleLarge)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SettingInfoRow(label = "Active Customers", value = state.activeCustomerCount.toString())
                        SettingInfoRow(label = "Total Outstanding Credit", value = currency.format(state.totalOutstanding))
                        SettingInfoRow(label = "Total Ledger Entries", value = state.totalTransactionsCount.toString())
                    }
                }

                // Privacy & Security
                Text(text = "Data Safety & Security", style = MaterialTheme.typography.titleLarge)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(Icons.Filled.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Column {
                                Text(text = "Offline-First Local Storage", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = "All shop credit data is saved locally in Room SQLite first. When internet is available, changes automatically sync with Supabase PostgreSQL under your account.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }

                // App Info
                Text(text = "App Info", style = MaterialTheme.typography.titleLarge)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SettingInfoRow(label = "App Name", value = "Khata - Shop Credit Manager")
                        SettingInfoRow(label = "Version", value = "2.0.0 (Cloud Sync Build)")
                        SettingInfoRow(label = "UI Framework", value = "Jetpack Compose + Material 3")
                        SettingInfoRow(label = "Database", value = state.databaseVersion)
                    }
                }
            }
        }
    }

    if (showAuthDialog) {
        AlertDialog(
            onDismissRequest = { showAuthDialog = false },
            title = { Text(if (isRegisterMode) "Register Khata Account" else "Sign In to Khata Account") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = { emailInput = it },
                        label = { Text("Email") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it },
                        label = { Text("Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    TextButton(onClick = { isRegisterMode = !isRegisterMode }) {
                        Text(if (isRegisterMode) "Already have an account? Sign In" else "Need an account? Register")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (emailInput.isNotBlank() && passwordInput.isNotBlank()) {
                            showAuthDialog = false
                            if (isRegisterMode) {
                                onSignUp(emailInput.trim(), passwordInput.trim())
                            } else {
                                onSignIn(emailInput.trim(), passwordInput.trim())
                            }
                        }
                    },
                    enabled = !state.isAuthActionLoading,
                ) {
                    Text(if (isRegisterMode) "Register" else "Sign In")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAuthDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
private fun SyncStatusBadge(syncState: SyncState) {
    val (label, color, icon) = when (syncState) {
        is SyncState.Synced -> Triple("✓ Synced", Color(0xFF2E7D32), Icons.Filled.CheckCircle)
        is SyncState.Syncing -> Triple("⟳ Syncing...", MaterialTheme.colorScheme.primary, Icons.Filled.Refresh)
        is SyncState.Offline -> Triple("⚠ Offline", Color(0xFFE65100), Icons.Filled.Warning)
        is SyncState.Error -> Triple("! Sync Failed", MaterialTheme.colorScheme.error, Icons.Filled.Warning)
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(icon, contentDescription = null, tint = color)
        Text(text = label, color = color, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun SettingInfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
        Text(text = value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
    }
}
