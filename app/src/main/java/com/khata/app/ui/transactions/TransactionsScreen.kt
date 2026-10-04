package com.khata.app.ui.transactions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.khata.app.domain.model.PaymentMethod
import com.khata.app.domain.model.TransactionType
import com.khata.app.domain.model.TransactionWithCustomer
import com.khata.app.ui.LocalAppContainer
import com.khata.app.ui.components.CustomerAvatar
import com.khata.app.ui.theme.ledgerColors
import com.khata.app.utils.CurrencyFormatter
import com.khata.app.utils.DateFormatter
import java.time.LocalDate

@Composable
fun TransactionsRoute(
    onTransactionClick: (String) -> Unit,
) {
    val container = LocalAppContainer.current
    val viewModel: TransactionsViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                TransactionsViewModel(
                    transactionRepository = container.transactionRepository,
                    customerRepository = container.customerRepository,
                )
            }
        },
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    TransactionsScreen(
        state = state,
        currency = container.currencyFormatter,
        onTransactionClick = onTransactionClick,
        onSetTypeFilter = viewModel::setTypeFilter,
        onSetDatePreset = viewModel::setDatePreset,
        onSetCustomerFilter = viewModel::setCustomerFilter,
        onSetPaymentMethodFilter = viewModel::setPaymentMethodFilter,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    state: TransactionsUiState,
    currency: CurrencyFormatter,
    onTransactionClick: (String) -> Unit,
    onSetTypeFilter: (TransactionType?) -> Unit,
    onSetDatePreset: (DateRangePreset) -> Unit,
    onSetCustomerFilter: (String?) -> Unit,
    onSetPaymentMethodFilter: (PaymentMethod?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val today = remember { LocalDate.now() }
    var customerMenuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Transactions Ledger") },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // Filter Bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Row 1: Type Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = state.filter.type == null,
                        onClick = { onSetTypeFilter(null) },
                        label = { Text("All Types") },
                    )
                    FilterChip(
                        selected = state.filter.type == TransactionType.CREDIT,
                        onClick = { onSetTypeFilter(TransactionType.CREDIT) },
                        label = { Text("Credits") },
                    )
                    FilterChip(
                        selected = state.filter.type == TransactionType.PAYMENT,
                        onClick = { onSetTypeFilter(TransactionType.PAYMENT) },
                        label = { Text("Payments") },
                    )

                    // Customer Filter Dropdown Chip
                    val selectedCustomer = state.customers.find { it.id == state.filter.customerId }
                    Box {
                        FilterChip(
                            selected = state.filter.customerId != null,
                            onClick = { customerMenuExpanded = true },
                            label = { Text(selectedCustomer?.name ?: "All Customers") },
                        )
                        DropdownMenu(
                            expanded = customerMenuExpanded,
                            onDismissRequest = { customerMenuExpanded = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text("All Customers") },
                                onClick = {
                                    customerMenuExpanded = false
                                    onSetCustomerFilter(null)
                                },
                            )
                            state.customers.forEach { customer ->
                                DropdownMenuItem(
                                    text = { Text(customer.name) },
                                    onClick = {
                                        customerMenuExpanded = false
                                        onSetCustomerFilter(customer.id)
                                    },
                                )
                            }
                        }
                    }
                }

                // Row 2: Date Range Presets
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    DateRangePreset.entries.forEach { preset ->
                        FilterChip(
                            selected = state.datePreset == preset,
                            onClick = { onSetDatePreset(preset) },
                            label = { Text(preset.displayName) },
                        )
                    }
                }
            }

            // Summary Totals Bar
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Text(
                            text = "Credits Total",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = currency.format(state.filteredCreditTotal),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.ledgerColors.credit,
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Payments Total",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = currency.format(state.filteredPaidTotal),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.ledgerColors.payment,
                        )
                    }
                }
            }

            // Transaction List
            var selectedTxForDialog by remember { mutableStateOf<TransactionWithCustomer?>(null) }

            when {
                state.isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator()
                }
                state.transactions.isEmpty() -> Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "No transactions found matching your filters.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(items = state.transactions, key = { "tx-${it.transaction.id}" }) { tx ->
                        LedgerRow(
                            transaction = tx,
                            currency = currency,
                            today = today,
                            onClick = { selectedTxForDialog = tx },
                        )
                    }
                }
            }

            if (selectedTxForDialog != null) {
                com.khata.app.ui.components.TransactionDetailDialog(
                    transaction = selectedTxForDialog!!.transaction,
                    customerName = selectedTxForDialog!!.customerName,
                    currency = currency,
                    onDismiss = { selectedTxForDialog = null },
                )
            }
        }
    }
}

@Composable
private fun LedgerRow(
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CustomerAvatar(name = transaction.customerName, size = 44.dp)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.customerName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${DateFormatter.friendly(transaction.transaction.date, today)} · $detail",
                    style = MaterialTheme.typography.bodyMedium,
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
