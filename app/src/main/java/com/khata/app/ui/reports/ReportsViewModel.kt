package com.khata.app.ui.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.khata.app.data.repository.CustomerRepository
import com.khata.app.data.repository.TransactionRepository
import com.khata.app.domain.model.CustomerWithBalance
import com.khata.app.domain.model.TransactionType
import com.khata.app.ui.transactions.DateRangePreset
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

data class DailySummary(
    val date: LocalDate,
    val creditAmount: Long,
    val paymentAmount: Long,
)

data class ReportsUiState(
    val isLoading: Boolean = true,
    val period: DateRangePreset = DateRangePreset.THIS_MONTH,
    val totalOutstanding: Long = 0,
    val periodCredit: Long = 0,
    val periodCollection: Long = 0,
    val netChange: Long = 0,
    val topDebtors: List<CustomerWithBalance> = emptyList(),
    val dailyBreakdown: List<DailySummary> = emptyList(),
    val hasError: Boolean = false,
)

class ReportsViewModel(
    private val transactionRepository: TransactionRepository,
    customerRepository: CustomerRepository,
) : ViewModel() {

    private val periodState = MutableStateFlow(DateRangePreset.THIS_MONTH)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val periodTotalsFlow = periodState.flatMapLatest { preset ->
        val (from, to) = preset.filterDates()
        val fromDate = from ?: LocalDate.now().minusDays(30)
        val toDate = to ?: LocalDate.now()

        combine(
            transactionRepository.observeTotal(TransactionType.CREDIT, fromDate, toDate),
            transactionRepository.observeTotal(TransactionType.PAYMENT, fromDate, toDate),
            transactionRepository.observeTransactions(),
        ) { credit, payment, allTx ->
            Triple(credit, payment, allTx)
        }
    }

    val uiState: StateFlow<ReportsUiState> = combine(
        periodState,
        transactionRepository.observeOverallBalance(),
        customerRepository.observeCustomers(),
        periodTotalsFlow,
    ) { period, overallBalance, customers, (periodCredit, periodCollection, allTx) ->
        val topDebtors = customers
            .filter { it.balance.outstanding > 0 }
            .sortedByDescending { it.balance.outstanding }
            .take(5)

        val netChange = periodCredit - periodCollection

        // Daily breakdown for visual chart (last 7 days)
        val today = LocalDate.now()
        val daysList = (0..6).map { today.minusDays(it.toLong()) }.reversed()
        val dailyBreakdown = daysList.map { date ->
            val dayCredits = allTx
                .filter { it.transaction.date == date && it.transaction.type == TransactionType.CREDIT }
                .sumOf { it.transaction.amount }
            val dayPayments = allTx
                .filter { it.transaction.date == date && it.transaction.type == TransactionType.PAYMENT }
                .sumOf { it.transaction.amount }
            DailySummary(date = date, creditAmount = dayCredits, paymentAmount = dayPayments)
        }

        ReportsUiState(
            isLoading = false,
            period = period,
            totalOutstanding = overallBalance.outstanding,
            periodCredit = periodCredit,
            periodCollection = periodCollection,
            netChange = netChange,
            topDebtors = topDebtors,
            dailyBreakdown = dailyBreakdown,
        )
    }
        .catch { emit(ReportsUiState(isLoading = false, hasError = true)) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = ReportsUiState(),
        )

    fun setPeriod(period: DateRangePreset) {
        periodState.value = period
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
