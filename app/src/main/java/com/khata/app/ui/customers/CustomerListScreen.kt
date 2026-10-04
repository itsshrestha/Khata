package com.khata.app.ui.customers

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.khata.app.domain.model.CustomerWithBalance
import com.khata.app.ui.LocalAppContainer
import com.khata.app.ui.components.BalanceStatusText
import com.khata.app.ui.components.CustomerAvatar
import com.khata.app.utils.CurrencyFormatter

@Composable
fun CustomerListRoute(
    onAddCustomer: () -> Unit,
    onCustomerClick: (String) -> Unit,
) {
    val container = LocalAppContainer.current
    val viewModel: CustomerListViewModel = viewModel(
        factory = viewModelFactory {
            initializer { CustomerListViewModel(container.customerRepository) }
        },
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val query by viewModel.searchQuery.collectAsStateWithLifecycle()

    CustomerListScreen(
        state = state,
        query = query,
        currency = container.currencyFormatter,
        onQueryChange = viewModel::onSearchQueryChange,
        onAddCustomer = onAddCustomer,
        onCustomerClick = onCustomerClick,
    )
}

@Composable
fun CustomerListScreen(
    state: CustomerListUiState,
    query: String,
    currency: CurrencyFormatter,
    onQueryChange: (String) -> Unit,
    onAddCustomer: () -> Unit,
    onCustomerClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddCustomer,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Add Customer") },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
        ) {
            Text(
                text = "Customers",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(top = 16.dp, bottom = 12.dp),
            )

            SearchField(query = query, onQueryChange = onQueryChange)

            Box(modifier = Modifier.fillMaxSize().padding(top = 12.dp)) {
                when {
                    state.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    state.hasError -> MessageState(
                        title = "Something went wrong",
                        message = "We couldn't load your customers. Please try again.",
                    )
                    state.customers.isEmpty() && state.resultsForQuery.isBlank() -> MessageState(
                        title = "No customers yet",
                        message = "Tap “Add Customer” to add your first customer.",
                    )
                    state.customers.isEmpty() -> MessageState(
                        title = "No matches",
                        message = "No customer matches “${state.resultsForQuery.trim()}”. Try a name or phone number.",
                    )
                    else -> CustomerList(
                        customers = state.customers,
                        currency = currency,
                        onCustomerClick = onCustomerClick,
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text("Search by name or phone") },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Filled.Clear, contentDescription = "Clear search")
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
    )
}

@Composable
private fun CustomerList(
    customers: List<CustomerWithBalance>,
    currency: CurrencyFormatter,
    onCustomerClick: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        // Extra bottom space so the floating button never covers the last row.
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(items = customers, key = { it.customer.id }) { item ->
            CustomerRow(item = item, currency = currency, onClick = { onCustomerClick(item.customer.id) })
        }
    }
}

@Composable
private fun CustomerRow(
    item: CustomerWithBalance,
    currency: CurrencyFormatter,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(18.dp)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable(onClick = onClick),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 76.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            CustomerAvatar(name = item.customer.name)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = item.customer.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = item.customer.phone ?: "No phone number",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
                BalanceStatusText(balance = item.balance, currency = currency)
            }
        }
    }
}

@Composable
private fun MessageState(title: String, message: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}
