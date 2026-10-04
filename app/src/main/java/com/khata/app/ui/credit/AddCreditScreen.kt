package com.khata.app.ui.credit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.khata.app.ui.LocalAppContainer
import com.khata.app.ui.components.AmountField
import com.khata.app.ui.components.DateField
import com.khata.app.utils.CurrencyFormatter
import com.khata.app.utils.MoneyParser
import com.khata.app.utils.toUserMessage

@Composable
fun AddCreditRoute(
    customerId: String,
    onBack: () -> Unit,
    onSaved: () -> Unit,
) {
    val container = LocalAppContainer.current
    val viewModel: AddCreditViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                AddCreditViewModel(
                    customerId = customerId,
                    customerRepository = container.customerRepository,
                    transactionRepository = container.transactionRepository,
                )
            }
        },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.saved) {
        if (state.saved) onSaved()
    }

    AddCreditScreen(
        state = state,
        currency = container.currencyFormatter,
        onBack = onBack,
        onModeChange = viewModel::onModeChange,
        onAmountChange = viewModel::onAmountChange,
        onDescriptionChange = viewModel::onDescriptionChange,
        onDateChange = viewModel::onDateChange,
        onAddItem = viewModel::onAddItem,
        onRemoveItem = viewModel::onRemoveItem,
        onItemNameChange = viewModel::onItemNameChange,
        onItemQuantityChange = viewModel::onItemQuantityChange,
        onItemPriceChange = viewModel::onItemPriceChange,
        onSave = viewModel::save,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCreditScreen(
    state: AddCreditUiState,
    currency: CurrencyFormatter,
    onBack: () -> Unit,
    onModeChange: (CreditMode) -> Unit,
    onAmountChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onDateChange: (java.time.LocalDate) -> Unit,
    onAddItem: () -> Unit,
    onRemoveItem: (Long) -> Unit,
    onItemNameChange: (Long, String) -> Unit,
    onItemQuantityChange: (Long, String) -> Unit,
    onItemPriceChange: (Long, String) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Add Credit") },
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
            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                CustomerBanner(name = state.customerName.orEmpty())
                ModeToggle(mode = state.mode, onModeChange = onModeChange)

                when (state.mode) {
                    CreditMode.QUICK -> AmountField(
                        value = state.amount,
                        onValueChange = onAmountChange,
                        currencySymbol = currency.symbol,
                        errorMessage = state.amountError?.toUserMessage(currency),
                    )
                    CreditMode.DETAILED -> DetailedItems(
                        state = state,
                        currency = currency,
                        onAddItem = onAddItem,
                        onRemoveItem = onRemoveItem,
                        onItemNameChange = onItemNameChange,
                        onItemQuantityChange = onItemQuantityChange,
                        onItemPriceChange = onItemPriceChange,
                    )
                }

                OutlinedTextField(
                    value = state.description,
                    onValueChange = onDescriptionChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Description (optional)") },
                    minLines = 2,
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Default,
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
                    onClick = onSave,
                    enabled = !state.isSaving,
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
                        Text("Save Credit", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomerBanner(name: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(
                text = "Customer",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
            )
            Text(
                text = name,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModeToggle(mode: CreditMode, onModeChange: (CreditMode) -> Unit) {
    val options = listOf(CreditMode.QUICK to "Quick Credit", CreditMode.DETAILED to "Detailed Credit")
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, (option, label) ->
            SegmentedButton(
                selected = mode == option,
                onClick = { onModeChange(option) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                modifier = Modifier.height(48.dp),
            ) {
                Text(label)
            }
        }
    }
}

@Composable
private fun DetailedItems(
    state: AddCreditUiState,
    currency: CurrencyFormatter,
    onAddItem: () -> Unit,
    onRemoveItem: (Long) -> Unit,
    onItemNameChange: (Long, String) -> Unit,
    onItemQuantityChange: (Long, String) -> Unit,
    onItemPriceChange: (Long, String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(text = "Items", style = MaterialTheme.typography.titleLarge)

        state.items.forEachIndexed { index, item ->
            ItemCard(
                number = index + 1,
                item = item,
                currency = currency,
                canRemove = state.items.size > 1,
                onRemove = { onRemoveItem(item.id) },
                onNameChange = { onItemNameChange(item.id, it) },
                onQuantityChange = { onItemQuantityChange(item.id, it) },
                onPriceChange = { onItemPriceChange(item.id, it) },
            )
        }

        OutlinedButton(
            onClick = onAddItem,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Text(" Add Item", style = MaterialTheme.typography.titleMedium)
        }

        state.itemsError?.let { error ->
            Text(
                text = error.toUserMessage(currency),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Total", style = MaterialTheme.typography.titleLarge)
                Text(
                    text = currency.format(state.detailedTotal),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun ItemCard(
    number: Int,
    item: CreditItemForm,
    currency: CurrencyFormatter,
    canRemove: Boolean,
    onRemove: () -> Unit,
    onNameChange: (String) -> Unit,
    onQuantityChange: (String) -> Unit,
    onPriceChange: (String) -> Unit,
) {
    val lineTotal = com.khata.app.domain.usecase.CreditFormMapper.liveTotal(listOf(item.toDraft()))

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
                Text("Item $number", style = MaterialTheme.typography.titleMedium)
                if (canRemove) {
                    IconButton(onClick = onRemove) {
                        Icon(Icons.Filled.Delete, contentDescription = "Remove item $number")
                    }
                }
            }
            OutlinedTextField(
                value = item.name,
                onValueChange = onNameChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Item name") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next,
                ),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = item.quantity,
                    onValueChange = { raw ->
                        val text = raw.replace(',', '.')
                        if (MoneyParser.isValidQuantityInput(text)) onQuantityChange(text)
                    },
                    modifier = Modifier.weight(1f),
                    label = { Text("Quantity") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                )
                OutlinedTextField(
                    value = item.unitPrice,
                    onValueChange = { raw ->
                        val text = raw.replace(',', '.')
                        if (MoneyParser.isValidAmountInput(text)) onPriceChange(text)
                    },
                    modifier = Modifier.weight(1f),
                    label = { Text("Unit price") },
                    prefix = { Text("${currency.symbol} ") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                )
            }
            Text(
                text = "Line total: ${currency.format(lineTotal)}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.align(Alignment.End),
            )
        }
    }
}
