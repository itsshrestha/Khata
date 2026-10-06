package com.khata.app.ui.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.khata.app.data.sync.SyncState
import com.khata.app.domain.model.TransactionType
import com.khata.app.domain.model.TransactionWithCustomer
import com.khata.app.ui.LocalAppContainer
import com.khata.app.ui.components.CustomerAvatar
import com.khata.app.ui.theme.ledgerColors
import com.khata.app.utils.CurrencyFormatter
import com.khata.app.utils.DateFormatter
import java.time.LocalDate
import java.time.LocalTime

/** Wires the ViewModel to the screen. Composables below this never touch repositories. */
@Composable
fun DashboardRoute(
    onNavigateToCustomerDetails: (String) -> Unit = {},
    onNavigateToCustomers: () -> Unit = {},
    onAddCustomer: () -> Unit = {},
) {
    val container = LocalAppContainer.current
    val viewModel: DashboardViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                DashboardViewModel(
                    transactionRepository = container.transactionRepository,
                    customerRepository = container.customerRepository,
                    syncManager = container.syncManager,
                )
            }
        },
    )
    val state = viewModel.uiState.collectAsStateWithLifecycle().value
    DashboardScreen(
        state = state,
        currency = container.currencyFormatter,
        onNavigateToCustomerDetails = onNavigateToCustomerDetails,
        onNavigateToCustomers = onNavigateToCustomers,
        onAddCustomer = onAddCustomer,
        onSyncNow = viewModel::triggerSync,
    )
}

@Composable
fun DashboardScreen(
    state: DashboardUiState,
    currency: CurrencyFormatter,
    onNavigateToCustomerDetails: (String) -> Unit,
    onNavigateToCustomers: () -> Unit,
    onAddCustomer: () -> Unit,
    onSyncNow: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val greeting = remember { DateFormatter.greeting(LocalTime.now().hour) }
    val today = remember { LocalDate.now() }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = "Khata",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
                Text(text = "$greeting \uD83D\uDC4B", style = MaterialTheme.typography.headlineMedium)
            }
        }

        // Outstanding Credit Banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
        ) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Total Outstanding Credit",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                )
                Text(
                    text = if (state.isLoading) "…" else currency.format(state.outstanding),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        // Today's Sales & Collection Summary
        Text(text = "Today's Summary", style = MaterialTheme.typography.titleLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TodayCard(
                label = "Today's Credit",
                value = if (state.isLoading) "…" else currency.format(state.todayCredit),
                color = MaterialTheme.ledgerColors.credit,
                modifier = Modifier.weight(1f),
            )
            TodayCard(
                label = "Today's Collection",
                value = if (state.isLoading) "…" else currency.format(state.todayCollection),
                color = MaterialTheme.ledgerColors.payment,
                modifier = Modifier.weight(1f),
            )
        }

        // Customer Quick Stats
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                label = "Active Customers",
                value = if (state.isLoading) "…" else state.customerCount.toString(),
                modifier = Modifier.weight(1f),
            )
            StatCard(
                label = "With Balance Due",
                value = if (state.isLoading) "…" else state.customersWithOutstanding.toString(),
                modifier = Modifier.weight(1f),
            )
        }

        // Quick Actions
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = onAddCustomer,
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text(text = " Add Customer", style = MaterialTheme.typography.titleMedium)
            }
            OutlinedButton(
                onClick = onNavigateToCustomers,
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Icon(Icons.Filled.AccountCircle, contentDescription = null)
                Text(text = " View Customers", style = MaterialTheme.typography.titleMedium)
            }
        }

        // Recent Activity
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = "Recent Activity", style = MaterialTheme.typography.titleLarge)
        }

        if (state.recentTransactions.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Text(
                    text = "No recent transactions.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(20.dp),
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                state.recentTransactions.forEach { tx ->
                    RecentTransactionRow(
                        transaction = tx,
                        currency = currency,
                        today = today,
                        onClick = { onNavigateToCustomerDetails(tx.transaction.customerId) },
                    )
                }
            }
        }

        if (state.hasError) {
            Text(
                text = "We couldn't load your data. Please restart the app.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }

        ExtendedFloatingActionButton(
            onClick = onSyncNow,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 20.dp),
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            icon = {
                when (state.syncState) {
                    is SyncState.Syncing -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.5.dp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                    is SyncState.Offline -> {
                        Icon(Icons.Filled.Warning, contentDescription = "Offline")
                    }
                    else -> {
                        Icon(Icons.Filled.Refresh, contentDescription = "Sync Now")
                    }
                }
            },
            text = {
                Text(
                    text = when (state.syncState) {
                        is SyncState.Syncing -> "Syncing…"
                        is SyncState.Offline -> "Offline"
                        else -> "Sync Now"
                    },
                    fontWeight = FontWeight.SemiBold,
                )
            },
        )
    }
}

@Composable
private fun TodayCard(
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = color,
            )
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(text = value, style = MaterialTheme.typography.headlineMedium)
        }
    }
}

@Composable
private fun RecentTransactionRow(
    transaction: TransactionWithCustomer,
    currency: CurrencyFormatter,
    today: LocalDate,
    onClick: () -> Unit,
) {
    val isCredit = transaction.transaction.type == TransactionType.CREDIT
    val amountColor = if (isCredit) MaterialTheme.ledgerColors.credit else MaterialTheme.ledgerColors.payment
    val detail = if (isCredit) {
        transaction.transaction.description ?: "Credit sale"
    } else {
        listOfNotNull(transaction.transaction.paymentMethod?.displayName, transaction.transaction.description).joinToString(" · ")
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CustomerAvatar(name = transaction.customerName, size = 40.dp)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.customerName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${DateFormatter.friendly(transaction.transaction.date, today)} · $detail",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = currency.formatSigned(transaction.transaction.amount, transaction.transaction.type),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = amountColor,
            )
        }
    }
}
