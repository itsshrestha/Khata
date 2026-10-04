package com.khata.app.ui.customer

import android.content.Intent
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.khata.app.domain.model.TransactionType
import com.khata.app.ui.LocalAppContainer
import com.khata.app.ui.theme.ledgerColors
import com.khata.app.utils.CurrencyFormatter
import com.khata.app.utils.DateFormatter

@Composable
fun CustomerStatementRoute(
    customerId: String,
    onBack: () -> Unit,
) {
    val container = LocalAppContainer.current
    val viewModel: CustomerStatementViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                CustomerStatementViewModel(
                    customerId = customerId,
                    customerRepository = container.customerRepository,
                    transactionRepository = container.transactionRepository,
                )
            }
        },
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val onShare = {
        val text = viewModel.generateShareableText(container.currencyFormatter)
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, text)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Share Khata Statement")
        context.startActivity(shareIntent)
    }

    CustomerStatementScreen(
        state = state,
        currency = container.currencyFormatter,
        onBack = onBack,
        onShare = onShare,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerStatementScreen(
    state: CustomerStatementUiState,
    currency: CurrencyFormatter,
    onBack: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Customer Statement") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onShare, enabled = state.customer != null) {
                        Icon(Icons.Filled.Share, contentDescription = "Share Statement")
                    }
                },
            )
        },
    ) { innerPadding ->
        when {
            state.isLoading -> Box(Modifier.fillMaxSize().padding(innerPadding), Alignment.Center) {
                CircularProgressIndicator()
            }
            state.customer == null -> Box(Modifier.fillMaxSize().padding(innerPadding), Alignment.Center) {
                Text("Customer not found.")
            }
            else -> {
                val customer = state.customer.customer
                val balance = state.customer.balance

                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // Header Card
                    item(key = "header") {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        ) {
                            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "SHOP STATEMENT",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                                )
                                Text(
                                    text = customer.name,
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                                customer.phone?.let { Text("Phone: $it", style = MaterialTheme.typography.bodyLarge) }
                                customer.address?.let { Text("Address: $it", style = MaterialTheme.typography.bodyLarge) }
                            }
                        }
                    }

                    // Summary Card
                    item(key = "summary") {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                SummaryRow(label = "Total Credit", value = currency.format(state.totalCredit))
                                SummaryRow(label = "Total Paid", value = currency.format(state.totalPaid))
                                HorizontalDivider()
                                SummaryRow(
                                    label = "Outstanding Balance",
                                    value = currency.format(balance.outstanding),
                                    emphasized = true,
                                )
                            }
                        }
                    }

                    // Statement Table Header
                    item(key = "table-header") {
                        Text(
                            text = "Itemized Statement",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }

                    if (state.statementRows.isEmpty()) {
                        item(key = "empty") {
                            Text(
                                text = "No transactions found for this customer.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        items(items = state.statementRows, key = { "stmt-${it.transaction.id}" }) { row ->
                            StatementRowCard(row = row, currency = currency)
                        }
                    }

                    item(key = "share-button") {
                        Button(
                            onClick = onShare,
                            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                            shape = RoundedCornerShape(16.dp),
                        ) {
                            Icon(Icons.Filled.Share, contentDescription = null)
                            Text(text = " Share Statement via Text / Messaging", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String, emphasized: Boolean = false) {
    val style = if (emphasized) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyLarge
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = label, style = style)
        Text(text = value, style = style, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun StatementRowCard(row: StatementRow, currency: CurrencyFormatter) {
    val isCredit = row.transaction.type == TransactionType.CREDIT
    val amountColor = if (isCredit) MaterialTheme.ledgerColors.credit else MaterialTheme.ledgerColors.payment

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = DateFormatter.full(row.transaction.date),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = currency.formatSigned(row.transaction.amount, row.transaction.type),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = amountColor,
                )
            }

            row.transaction.description?.let { desc ->
                if (desc.isNotEmpty()) {
                    Text(text = desc, style = MaterialTheme.typography.bodyMedium)
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Text(
                    text = "Running Bal: ${currency.format(row.runningBalance)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
