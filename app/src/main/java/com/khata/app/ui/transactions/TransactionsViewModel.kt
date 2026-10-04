package com.khata.app.ui.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.khata.app.data.repository.CustomerRepository
import com.khata.app.data.repository.TransactionRepository
import com.khata.app.domain.model.Customer
import com.khata.app.domain.model.PaymentMethod
import com.khata.app.domain.model.TransactionFilter
import com.khata.app.domain.model.TransactionType
import com.khata.app.domain.model.TransactionWithCustomer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.LocalDate

enum class DateRangePreset(val displayName: String) {
    ALL_TIME("All Time"),
    TODAY("Today"),
    THIS_WEEK("This Week"),
    THIS_MONTH("This Month");

    fun filterDates(today: LocalDate = LocalDate.now()): Pair<LocalDate?, LocalDate?> = when (this) {
        ALL_TIME -> Pair(null, null)
        TODAY -> Pair(today, today)
        THIS_WEEK -> Pair(today.minusDays(today.dayOfWeek.value.toLong() - 1), today)
        THIS_MONTH -> Pair(today.withDayOfMonth(1), today)
    }
}

data class TransactionsUiState(
    val isLoading: Boolean = true,
    val filter: TransactionFilter = TransactionFilter(),
    val datePreset: DateRangePreset = DateRangePreset.ALL_TIME,
    val customers: List<Customer> = emptyList(),
    val transactions: List<TransactionWithCustomer> = emptyList(),
    val filteredCreditTotal: Long = 0,
    val filteredPaidTotal: Long = 0,
    val hasError: Boolean = false,
)

class TransactionsViewModel(
    private val transactionRepository: TransactionRepository,
    customerRepository: CustomerRepository,
) : ViewModel() {

    private val filterState = MutableStateFlow(TransactionFilter())
    private val datePresetState = MutableStateFlow(DateRangePreset.ALL_TIME)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val transactionsFlow = filterState.flatMapLatest { filter ->
        transactionRepository.observeTransactions(filter)
    }

    val uiState: StateFlow<TransactionsUiState> = combine(
        filterState,
        datePresetState,
        customerRepository.observeCustomers(""),
        transactionsFlow,
    ) { filter, preset, customersWithBalance, transactions ->
        val customers = customersWithBalance.map { it.customer }
        val totalCredit = transactions.filter { it.transaction.type == TransactionType.CREDIT }.sumOf { it.transaction.amount }
        val totalPaid = transactions.filter { it.transaction.type == TransactionType.PAYMENT }.sumOf { it.transaction.amount }
        TransactionsUiState(
            isLoading = false,
            filter = filter,
            datePreset = preset,
            customers = customers,
            transactions = transactions,
            filteredCreditTotal = totalCredit,
            filteredPaidTotal = totalPaid,
        )
    }
        .catch { emit(TransactionsUiState(isLoading = false, hasError = true)) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = TransactionsUiState(),
        )

    fun setTypeFilter(type: TransactionType?) {
        filterState.update { it.copy(type = type) }
    }

    fun setCustomerFilter(customerId: String?) {
        filterState.update { it.copy(customerId = customerId) }
    }

    fun setPaymentMethodFilter(method: PaymentMethod?) {
        filterState.update { it.copy(paymentMethod = method) }
    }

    fun setDatePreset(preset: DateRangePreset) {
        val (from, to) = preset.filterDates()
        datePresetState.value = preset
        filterState.update { it.copy(fromDate = from, toDate = to) }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
