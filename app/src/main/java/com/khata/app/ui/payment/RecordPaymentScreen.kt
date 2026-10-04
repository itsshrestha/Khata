package com.khata.app.ui.payment

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.khata.app.domain.model.PaymentMethod
import com.khata.app.domain.model.PaymentReceipt
import com.khata.app.ui.LocalAppContainer
import com.khata.app.ui.components.AmountField
import com.khata.app.ui.components.DateField
import com.khata.app.ui.theme.ledgerColors
import com.khata.app.utils.CurrencyFormatter
import com.khata.app.utils.toUserMessage

@Composable
fun RecordPaymentRoute(
    customerId: String,
    onBack: () -> Unit,
    onPaymentRecorded: (PaymentReceipt) -> Unit,
) {
    val container = LocalAppContainer.current
    val viewModel: RecordPaymentViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                RecordPaymentViewModel(
                    customerId = customerId,
                    customerRepository = container.customerRepository,
                    transactionRepository = container.transactionRepository,
                )
            }
        },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.receipt) {
        state.receipt?.let(onPaymentRecorded)
    }

    RecordPaymentScreen(
        state = state,
        currency = container.currencyFormatter,
        onBack = onBack,
        onAmountChange = viewModel::onAmountChange,
        onPaymentMethodChange = viewModel::onPaymentMethodChange,
        onNoteChange = viewModel::onNoteChange,
        onDateChange = viewModel::onDateChange,
        onRequestRecord = viewModel::onRequestRecord,
        onDismissConfirmation = viewModel::onDismissConfirmation,
        onConfirmRecord = viewModel::onConfirmRecord,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordPaymentScreen(
    state: RecordPaymentUiState,
    currency: CurrencyFormatter,
    onBack: () -> Unit,
    onAmountChange: (String) -> Unit,
    onPaymentMethodChange: (PaymentMethod) -> Unit,
    onNoteChange: (String) -> Unit,
    onDateChange: (java.time.LocalDate) -> Unit,
    onRequestRecord: () -> Unit,
    onDismissConfirmation: () -> Unit,
    onConfirmRecord: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Record Payment") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        when {
            state.isLoading -> Box(Modifier.fillMaxSize().padding(innerPadding), Alignment.Center) {
                CircularProgressIndicator()
            }
            state.customerMissing -> Box(Modifier.fillMaxSize().padding(innerPadding).padding(32.dp), Alignment.Center) {
                Text(
                    text = "This customer could not be found.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                )
            }
            state.outstanding <= 0L -> Box(Modifier.fillMaxSize().padding(innerPadding).padding(32.dp), Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "✓ Paid",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.ledgerColors.payment,
                    )
                    Text(
                        text = "This customer has no outstanding balance to pay.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                CustomerBalanceBanner(
                    name = state.customerName.orEmpty(),
                    outstanding = state.outstanding,
                    currency = currency,
                )

                AmountField(
                    value = state.amountText,
                    onValueChange = onAmountChange,
                    currencySymbol = currency.symbol,
                    label = "Payment Amount",
                    errorMessage = state.amountError?.toUserMessage(currency),
                )

                PaymentMethodSelection(
                    selectedMethod = state.paymentMethod,
                    onMethodSelected = onPaymentMethodChange,
                )

                OutlinedTextField(
                    value = state.note,
                    onValueChange = onNoteChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Note (optional)") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Next,
                    ),
                )

                DateField(date = state.date, onDateChange = onDateChange)

                state.formError?.let { error ->
                    Text(
                        text = error.toUserMessage(currency),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                Button(
                    onClick = onRequestRecord,
                    enabled = !state.isSaving && state.amountText.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    if (state.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Text("Record Payment", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }

        if (state.showConfirmation) {
            PaymentConfirmationDialog(
                paymentAmount = state.pendingAmountMinor,
                previousOutstanding = state.outstanding,
                currency = currency,
                isSaving = state.isSaving,
                onConfirm = onConfirmRecord,
                onDismiss = onDismissConfirmation,
            )
        }
    }
}

@Composable
private fun CustomerBalanceBanner(name: String, outstanding: Long, currency: CurrencyFormatter) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Outstanding Balance",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                )
                Text(
                    text = currency.format(outstanding),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.ledgerColors.credit,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PaymentMethodSelection(
    selectedMethod: PaymentMethod,
    onMethodSelected: (PaymentMethod) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Payment Method", style = MaterialTheme.typography.titleMedium)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            PaymentMethod.entries.forEach { method ->
                FilterChip(
                    selected = selectedMethod == method,
                    onClick = { onMethodSelected(method) },
                    label = { Text(method.displayName) },
                )
            }
        }
    }
}

@Composable
private fun PaymentConfirmationDialog(
    paymentAmount: Long,
    previousOutstanding: Long,
    currency: CurrencyFormatter,
    isSaving: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val newOutstanding = previousOutstanding - paymentAmount

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record payment of ${currency.format(paymentAmount)}?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Previous balance", style = MaterialTheme.typography.bodyMedium)
                    Text(currency.format(previousOutstanding), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("New balance", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                    Text(currency.format(newOutstanding), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.ledgerColors.payment)
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfirm, enabled = !isSaving) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Text("Confirm")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) {
                Text("Cancel")
            }
        },
    )
}
