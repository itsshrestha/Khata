package com.khata.app.ui.customer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.khata.app.domain.model.Balance
import com.khata.app.domain.model.BalanceStatus
import com.khata.app.domain.model.Customer
import com.khata.app.domain.model.Outcome
import com.khata.app.domain.model.Transaction
import com.khata.app.domain.model.TransactionType
import com.khata.app.domain.usecase.ValidateTransactionChangeUseCase
import com.khata.app.ui.LocalAppContainer
import com.khata.app.ui.components.CustomerAvatar
import com.khata.app.ui.theme.ledgerColors
import com.khata.app.utils.CurrencyFormatter
import com.khata.app.utils.DateFormatter
import com.khata.app.utils.toUserMessage
import java.time.LocalDate

@Composable
fun CustomerDetailsRoute(
    customerId: String,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onAddCredit: () -> Unit,
    onRecordPayment: () -> Unit,
    onViewStatement: () -> Unit = {},
    onNavigateToDashboard: () -> Unit = {},
    feedbackMessage: String? = null,
    onFeedbackShown: () -> Unit = {},
) {
    val container = LocalAppContainer.current
    val viewModel: CustomerDetailsViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                CustomerDetailsViewModel(
                    customerId = customerId,
                    customerRepository = container.customerRepository,
                    transactionRepository = container.transactionRepository,
                )
            }
        },
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Leave the screen once the customer has been archived or deleted.
    LaunchedEffect(state.archived, state.deleted) {
        if (state.archived || state.deleted) onBack()
    }

    CustomerDetailsScreen(
        state = state,
        currency = container.currencyFormatter,
        onBack = onBack,
        onNavigateToDashboard = onNavigateToDashboard,
        onEdit = onEdit,
        onAddCredit = onAddCredit,
        onRecordPayment = onRecordPayment,
        onViewStatement = onViewStatement,
        onArchive = viewModel::archiveCustomer,
        onDeleteCustomer = viewModel::deleteCustomer,
        onDeleteTransaction = viewModel::deleteTransaction,
        onActionErrorShown = viewModel::onActionErrorShown,
        feedbackMessage = feedbackMessage,
        onFeedbackShown = onFeedbackShown,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDetailsScreen(
    state: CustomerDetailsUiState,
    currency: CurrencyFormatter,
    onBack: () -> Unit,
    onNavigateToDashboard: () -> Unit = {},
    onEdit: () -> Unit,
    onAddCredit: () -> Unit,
    onRecordPayment: () -> Unit,
    onViewStatement: () -> Unit,
    onArchive: () -> Unit,
    onDeleteCustomer: () -> Unit,
    onDeleteTransaction: (String) -> Unit,
    onActionErrorShown: () -> Unit,
    modifier: Modifier = Modifier,
    feedbackMessage: String? = null,
    onFeedbackShown: () -> Unit = {},
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var menuExpanded by remember { mutableStateOf(false) }
    var showArchiveDialog by rememberSaveable { mutableStateOf(false) }
    var showDeleteCustomerDialog by rememberSaveable { mutableStateOf(false) }
    var transactionToDelete by remember { mutableStateOf<Transaction?>(null) }
    var selectedTransactionForDetail by remember { mutableStateOf<Transaction?>(null) }

    // Success messages sent back by other screens, e.g. "✓ Credit added successfully".
    LaunchedEffect(feedbackMessage) {
        feedbackMessage?.let { message ->
            // Show first, clear afterwards: clearing changes the effect key and would cancel the snackbar.
            snackbarHostState.showSnackbar(message)
            onFeedbackShown()
        }
    }

    LaunchedEffect(state.actionError) {
        state.actionError?.let { error ->
            snackbarHostState.showSnackbar(error.toUserMessage(currency))
            onActionErrorShown()
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = state.customer?.customer?.name.orEmpty(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToDashboard) {
                        Icon(Icons.Filled.Home, contentDescription = "Dashboard")
                    }
                    if (state.customer != null) {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "More options")
                        }
                        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                            DropdownMenuItem(
                                text = { Text("Go to Dashboard") },
                                onClick = {
                                    menuExpanded = false
                                    onNavigateToDashboard()
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("View Statement") },
                                onClick = {
                                    menuExpanded = false
                                    onViewStatement()
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Edit customer") },
                                onClick = {
                                    menuExpanded = false
                                    onEdit()
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Archive customer") },
                                onClick = {
                                    menuExpanded = false
                                    showArchiveDialog = true
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Delete customer", color = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    menuExpanded = false
                                    showDeleteCustomerDialog = true
                                },
                            )
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        val customerWithBalance = state.customer
        when {
            state.isLoading -> Box(Modifier.fillMaxSize().padding(innerPadding), Alignment.Center) {
                CircularProgressIndicator()
            }
            state.hasError -> CenteredMessage(
                text = "We couldn't load this customer. Please go back and try again.",
                modifier = Modifier.padding(innerPadding),
            )
            customerWithBalance == null -> CenteredMessage(
                text = "This customer could not be found.",
                modifier = Modifier.padding(innerPadding),
            )
            else -> CustomerDetailsContent(
                customer = customerWithBalance.customer,
                balance = customerWithBalance.balance,
                transactions = state.transactions,
                currency = currency,
                onAddCredit = onAddCredit,
                onRecordPayment = onRecordPayment,
                onDeleteTransactionClick = { transactionToDelete = it },
                onTransactionClick = { selectedTransactionForDetail = it },
                modifier = Modifier.padding(innerPadding),
            )
        }

        if (selectedTransactionForDetail != null) {
            com.khata.app.ui.components.TransactionDetailDialog(
                transaction = selectedTransactionForDetail!!,
                customerName = customerWithBalance?.customer?.name,
                currency = currency,
                onDismiss = { selectedTransactionForDetail = null },
            )
        }

        if (showArchiveDialog && customerWithBalance != null) {
            ArchiveDialog(
                customer = customerWithBalance.customer,
                balance = customerWithBalance.balance,
                currency = currency,
                isArchiving = state.isArchiving,
                onConfirm = {
                    showArchiveDialog = false
                    onArchive()
                },
                onDismiss = { showArchiveDialog = false },
            )
        }

        if (showDeleteCustomerDialog && customerWithBalance != null) {
            DeleteCustomerDialog(
                customer = customerWithBalance.customer,
                balance = customerWithBalance.balance,
                currency = currency,
                isDeleting = state.isDeleting,
                onConfirm = {
                    showDeleteCustomerDialog = false
                    onDeleteCustomer()
                },
                onDismiss = { showDeleteCustomerDialog = false },
            )
        }

        if (transactionToDelete != null && customerWithBalance != null) {
            DeleteTransactionDialog(
                transaction = transactionToDelete!!,
                currentOutstanding = customerWithBalance.balance.outstanding,
                currency = currency,
                onConfirm = {
                    val id = transactionToDelete!!.id
                    transactionToDelete = null
                    onDeleteTransaction(id)
                },
                onDismiss = { transactionToDelete = null },
            )
        }
    }
}

@Composable
private fun CustomerDetailsContent(
    customer: Customer,
    balance: Balance,
    transactions: List<Transaction>,
    currency: CurrencyFormatter,
    onAddCredit: () -> Unit,
    onRecordPayment: () -> Unit,
    onDeleteTransactionClick: (Transaction) -> Unit,
    onTransactionClick: (Transaction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val today = remember { LocalDate.now() }
    // Group by business date; LinkedHashMap keeps the oldest-first order from the database.
    val groupedByDate = remember(transactions) { transactions.groupBy { it.date } }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = 8.dp,
            bottom = 32.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(key = "header") {
            CustomerHeader(customer = customer, balance = balance, currency = currency)
        }
        item(key = "actions") {
            ActionButtons(
                canRecordPayment = balance.status == BalanceStatus.OWES,
                onAddCredit = onAddCredit,
                onRecordPayment = onRecordPayment,
            )
        }
        item(key = "history-title") {
            Text(
                text = "Transaction History",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 12.dp),
            )
        }

        if (transactions.isEmpty()) {
            item(key = "history-empty") {
                Text(
                    text = "No transactions yet. Add a credit when this customer buys on credit.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }
        } else {
            groupedByDate.forEach { (date, dayTransactions) ->
                item(key = "date-$date") {
                    Text(
                        text = DateFormatter.friendly(date, today),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                items(items = dayTransactions, key = { "tx-${it.id}" }) { transaction ->
                    TransactionRow(
                        transaction = transaction,
                        currency = currency,
                        onDeleteClick = { onDeleteTransactionClick(transaction) },
                        onClick = { onTransactionClick(transaction) },
                    )
                }
            }
        }

        item(key = "summary") {
            SummaryCard(balance = balance, currency = currency)
        }
    }
}

@Composable
private fun CustomerHeader(customer: Customer, balance: Balance, currency: CurrencyFormatter) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                CustomerAvatar(name = customer.name, size = 56.dp)
                Column {
                    Text(
                        text = customer.name,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    InfoLine(label = "Phone", value = customer.phone ?: "Not provided")
                }
            }

            customer.address?.let { InfoLine(label = "Address", value = it) }
            customer.notes?.let { InfoLine(label = "Notes", value = it) }

            HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))

            Text(text = "Outstanding", style = MaterialTheme.typography.titleMedium)
            when (balance.status) {
                BalanceStatus.OWES -> Text(
                    text = currency.format(balance.outstanding),
                    style = MaterialTheme.typography.headlineLarge,
                )
                BalanceStatus.SETTLED -> {
                    Text(text = currency.format(0), style = MaterialTheme.typography.headlineLarge)
                    Text(
                        text = "✓ Paid · No outstanding balance",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                BalanceStatus.NO_CREDIT -> {
                    Text(text = currency.format(0), style = MaterialTheme.typography.headlineLarge)
                    Text(text = "No outstanding balance", style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
        )
        Text(text = value, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun ActionButtons(
    canRecordPayment: Boolean,
    onAddCredit: () -> Unit,
    onRecordPayment: () -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(
            onClick = onAddCredit,
            modifier = Modifier.weight(1f).height(56.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Text(text = " Add Credit", style = MaterialTheme.typography.titleMedium)
        }
        OutlinedButton(
            onClick = onRecordPayment,
            // A customer with nothing outstanding cannot make a payment.
            enabled = canRecordPayment,
            modifier = Modifier.weight(1f).height(56.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Text(text = "\uD83D\uDCB0 Record Payment", style = MaterialTheme.typography.titleMedium, maxLines = 1)
        }
    }
}

@Composable
private fun TransactionRow(
    transaction: Transaction,
    currency: CurrencyFormatter,
    onDeleteClick: () -> Unit,
    onClick: () -> Unit,
) {
    val isCredit = transaction.type == TransactionType.CREDIT
    val amountColor = if (isCredit) MaterialTheme.ledgerColors.credit else MaterialTheme.ledgerColors.payment
    val title = if (isCredit) "Credit" else "Payment"
    val detail = if (isCredit) {
        transaction.description ?: "Credit sale"
    } else {
        listOfNotNull(transaction.paymentMethod?.displayName, transaction.description).joinToString(" · ")
    }
    val timeFormatted = DateFormatter.dateTimeFull(transaction.createdAt.toEpochMilli())

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium, color = amountColor)
                if (detail.isNotEmpty()) {
                    Text(
                        text = detail,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = timeFormatted + (transaction.deviceName?.let { " · $it" } ?: ""),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                )
            }
            Text(
                text = currency.formatSigned(transaction.amount, transaction.type),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = amountColor,
            )
            IconButton(onClick = onDeleteClick) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Delete transaction",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                )
            }
        }
    }
}

@Composable
private fun SummaryCard(balance: Balance, currency: CurrencyFormatter) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SummaryRow(label = "Total Credit", value = currency.format(balance.totalCredit))
            SummaryRow(label = "Total Paid", value = currency.format(balance.totalPaid))
            HorizontalDivider()
            SummaryRow(label = "Outstanding", value = currency.format(balance.outstanding), emphasized = true)
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
private fun ArchiveDialog(
    customer: Customer,
    balance: Balance,
    currency: CurrencyFormatter,
    isArchiving: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val balanceNote = if (balance.status == BalanceStatus.OWES) {
        " They still owe ${currency.format(balance.outstanding)}, which stays in your outstanding total."
    } else {
        ""
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Archive ${customer.name}?") },
        text = {
            Text(
                "They will be hidden from the customer list. " +
                    "Their transaction history is kept safe." + balanceNote,
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !isArchiving) { Text("Archive") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun CenteredMessage(text: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun DeleteTransactionDialog(
    transaction: Transaction,
    currentOutstanding: Long,
    currency: CurrencyFormatter,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val validator = remember { ValidateTransactionChangeUseCase() }
    val outcome = remember(transaction, currentOutstanding) {
        validator.validateDelete(transaction, currentOutstanding)
    }
    val isCredit = transaction.type == TransactionType.CREDIT
    val typeName = if (isCredit) "credit" else "payment"

    val impactText = when (outcome) {
        is Outcome.Success -> {
            val newOutstanding = outcome.value
            val changeDesc = if (isCredit) {
                "reduce the outstanding balance from ${currency.format(currentOutstanding)} to ${currency.format(newOutstanding)}"
            } else {
                "increase the outstanding balance from ${currency.format(currentOutstanding)} to ${currency.format(newOutstanding)}"
            }
            "Deleting this $typeName of ${currency.format(transaction.amount)} will $changeDesc."
        }
        is Outcome.Failure -> {
            "Deleting this credit is not allowed because remaining credits would not cover payments already made by this customer."
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete ${if (isCredit) "Credit" else "Payment"}?") },
        text = {
            Text(text = impactText)
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = outcome is Outcome.Success,
            ) {
                Text(
                    text = "Delete",
                    color = if (outcome is Outcome.Success) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun DeleteCustomerDialog(
    customer: Customer,
    balance: Balance,
    currency: CurrencyFormatter,
    isDeleting: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val warningText = if (balance.outstanding > 0) {
        "Warning: ${customer.name} still owes ${currency.format(balance.outstanding)}. Deleting this customer will permanently delete them and all their transaction history. This action cannot be undone."
    } else {
        "Are you sure you want to permanently delete ${customer.name} and all their transaction history? This action cannot be undone."
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete ${customer.name}?") },
        text = { Text(text = warningText) },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !isDeleting) {
                Text("Delete Permanently", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}


