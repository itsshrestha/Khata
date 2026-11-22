package com.khata.app.ui.customers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.khata.app.data.repository.CustomerRepository
import com.khata.app.domain.model.CustomerWithBalance
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Sort mode for the customer list. */
enum class SortOrder {
    /** Highest outstanding first (default). */
    DESCENDING,
    /** Alphabetical A-Z by customer name. */
    NAME_AZ,
}

data class CustomerListUiState(
    val isLoading: Boolean = true,
    val customers: List<CustomerWithBalance> = emptyList(),
    /** The query these results belong to, so the UI can tell "no customers" from "no matches". */
    val resultsForQuery: String = "",
    val hasError: Boolean = false,
    val sortOrder: SortOrder = SortOrder.DESCENDING,
)

@OptIn(ExperimentalCoroutinesApi::class)
class CustomerListViewModel(
    customerRepository: CustomerRepository,
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _sortOrder = MutableStateFlow(SortOrder.DESCENDING)

    /** Bound to the search field. Kept separate from results so typing is never delayed by the database. */
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    /** Exposed so the UI can read the current sort direction for icon state. */
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()

    val uiState: StateFlow<CustomerListUiState> = combine(
        _searchQuery.flatMapLatest { query ->
            customerRepository.observeCustomers(query)
                .map<List<CustomerWithBalance>, Pair<String, List<CustomerWithBalance>?>> { customers -> query to customers }
                .catch { emit(query to null) }
        },
        _sortOrder,
    ) { (query, customers), order ->
        if (customers == null) {
            CustomerListUiState(
                isLoading = false,
                hasError = true,
                resultsForQuery = query,
                sortOrder = order,
            )
        } else {
            val sorted = when (order) {
                SortOrder.DESCENDING -> customers.sortedByDescending { it.balance.outstanding }
                SortOrder.NAME_AZ -> customers.sortedBy { it.customer.name.lowercase() }
            }
            CustomerListUiState(
                isLoading = false,
                customers = sorted,
                resultsForQuery = query,
                sortOrder = order,
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = CustomerListUiState(),
    )

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onToggleSortOrder() {
        _sortOrder.value = when (_sortOrder.value) {
            SortOrder.DESCENDING -> SortOrder.NAME_AZ
            SortOrder.NAME_AZ -> SortOrder.DESCENDING
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
