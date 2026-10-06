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
    val customFromDate: LocalDate? = null,
    val customToDate: LocalDate? = null,
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
    private val customRangeState = MutableStateFlow<Pair<LocalDate, LocalDate>?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val periodTotalsFlow = combine(periodState, customRangeState) { preset, customRange ->
        if (customRange != null) {
            Pair(customRange.first, customRange.second)
        } else {
            val (from, to) = preset.filterDates()
            Pair(from ?: LocalDate.now().minusDays(30), to ?: LocalDate.now())
        }
    }.flatMapLatest { (fromDate, toDate) ->
        combine(
            transactionRepository.observeTotal(TransactionType.CREDIT, fromDate, toDate),
            transactionRepository.observeTotal(TransactionType.PAYMENT, fromDate, toDate),
            transactionRepository.observeTransactions(),
        ) { credit, payment, allTx ->
            Tuple4(credit, payment, allTx, fromDate to toDate)
        }
    }

    private data class Tuple4<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

    val uiState: StateFlow<ReportsUiState> = combine(
        periodState,
        customRangeState,
        transactionRepository.observeOverallBalance(),
        customerRepository.observeCustomers(),
        periodTotalsFlow,
    ) { period, customRange, overallBalance, customers, (periodCredit, periodCollection, allTx, dateRange) ->
        val topDebtors = customers
            .filter { it.balance.outstanding > 0 }
            .sortedByDescending { it.balance.outstanding }
            .take(5)

        val netChange = periodCredit - periodCollection

        // Daily breakdown for visual chart (last 7 days up to end date)
        val endDate = dateRange.second
        val daysList = (0..6).map { endDate.minusDays(it.toLong()) }.reversed()
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
            customFromDate = customRange?.first,
            customToDate = customRange?.second,
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
        customRangeState.value = null
        periodState.value = period
    }

    fun setCustomDateRange(fromDate: LocalDate, toDate: LocalDate) {
        customRangeState.value = Pair(fromDate, toDate)
    }

    fun clearCustomDateRange() {
        customRangeState.value = null
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
