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
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class CustomerListUiState(
    val isLoading: Boolean = true,
    val customers: List<CustomerWithBalance> = emptyList(),
    /** The query these results belong to, so the UI can tell "no customers" from "no matches". */
    val resultsForQuery: String = "",
    val hasError: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
class CustomerListViewModel(
    customerRepository: CustomerRepository,
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")

    /** Bound to the search field. Kept separate from results so typing is never delayed by the database. */
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val uiState: StateFlow<CustomerListUiState> = _searchQuery
        .flatMapLatest { query ->
            customerRepository.observeCustomers(query)
                .map { customers ->
                    CustomerListUiState(
                        isLoading = false,
                        customers = customers,
                        resultsForQuery = query,
                    )
                }
                .catch { emit(CustomerListUiState(isLoading = false, hasError = true, resultsForQuery = query)) }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = CustomerListUiState(),
        )

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
