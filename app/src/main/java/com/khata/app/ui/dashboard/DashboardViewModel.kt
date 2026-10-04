package com.khata.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.khata.app.data.repository.CustomerRepository
import com.khata.app.data.repository.TransactionRepository
import com.khata.app.domain.model.TransactionType
import com.khata.app.domain.model.TransactionWithCustomer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

data class DashboardUiState(
    val isLoading: Boolean = true,
    /** Total outstanding credit in minor units, derived from all transactions. */
    val outstanding: Long = 0,
    val customerCount: Int = 0,
    val customersWithOutstanding: Int = 0,
    /** Today's total credit sales in minor units. */
    val todayCredit: Long = 0,
    /** Today's total collected payments in minor units. */
    val todayCollection: Long = 0,
    /** Top 5 recent transactions across all customers. */
    val recentTransactions: List<TransactionWithCustomer> = emptyList(),
    val hasError: Boolean = false,
)

class DashboardViewModel(
    transactionRepository: TransactionRepository,
    customerRepository: CustomerRepository,
) : ViewModel() {

    private val today = LocalDate.now()

    private val todayTotalsFlow = combine(
        transactionRepository.observeTotal(TransactionType.CREDIT, today, today),
        transactionRepository.observeTotal(TransactionType.PAYMENT, today, today),
        transactionRepository.observeRecentTransactions(limit = 5),
    ) { credit, payment, recent ->
        Triple(credit, payment, recent)
    }

    val uiState: StateFlow<DashboardUiState> = combine(
        transactionRepository.observeOverallBalance(),
        customerRepository.observeActiveCustomerCount(),
        customerRepository.observeCustomersWithOutstandingCount(),
        todayTotalsFlow,
    ) { balance, customerCount, withOutstanding, (todayCredit, todayCollection, recent) ->
        DashboardUiState(
            isLoading = false,
            outstanding = balance.outstanding,
            customerCount = customerCount,
            customersWithOutstanding = withOutstanding,
            todayCredit = todayCredit,
            todayCollection = todayCollection,
            recentTransactions = recent,
        )
    }
        .catch { emit(DashboardUiState(isLoading = false, hasError = true)) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = DashboardUiState(),
        )

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
