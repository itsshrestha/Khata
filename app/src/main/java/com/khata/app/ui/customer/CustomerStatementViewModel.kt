package com.khata.app.ui.customer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.khata.app.data.repository.CustomerRepository
import com.khata.app.data.repository.TransactionRepository
import com.khata.app.domain.model.CustomerWithBalance
import com.khata.app.domain.model.Transaction
import com.khata.app.domain.model.TransactionType
import com.khata.app.utils.CurrencyFormatter
import com.khata.app.utils.DateFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

data class StatementRow(
    val transaction: Transaction,
    val runningBalance: Long,
)

data class DateFilterRange(
    val fromDate: LocalDate? = null,
    val toDate: LocalDate? = null,
)

data class CustomerStatementUiState(
    val isLoading: Boolean = true,
    val customer: CustomerWithBalance? = null,
    val statementRows: List<StatementRow> = emptyList(),
    val totalCredit: Long = 0,
    val totalPaid: Long = 0,
    val fromDate: LocalDate? = null,
    val toDate: LocalDate? = null,
    val hasError: Boolean = false,
)

class CustomerStatementViewModel(
    private val customerId: String,
    customerRepository: CustomerRepository,
    transactionRepository: TransactionRepository,
) : ViewModel() {

    private val dateFilterState = MutableStateFlow(DateFilterRange())

    val uiState: StateFlow<CustomerStatementUiState> = combine(
        customerRepository.observeCustomer(customerId),
        transactionRepository.observeCustomerTransactions(customerId),
        dateFilterState,
    ) { customer: CustomerWithBalance?, transactions: List<Transaction>, dateFilter: DateFilterRange ->
        val fromDate = dateFilter.fromDate
        val toDate = dateFilter.toDate
        var running = 0L
        val allRows = transactions.map { tx ->
            when (tx.type) {
                TransactionType.CREDIT -> running += tx.amount
                TransactionType.PAYMENT -> running -= tx.amount
            }
            StatementRow(transaction = tx, runningBalance = running)
        }

        val filteredRows = if (fromDate != null && toDate != null) {
            allRows.filter { row ->
                val txDate = row.transaction.date
                (txDate.isEqual(fromDate) || txDate.isAfter(fromDate)) &&
                    (txDate.isEqual(toDate) || txDate.isBefore(toDate))
            }
        } else {
            allRows
        }

        val totalCredit = filteredRows.filter { it.transaction.type == TransactionType.CREDIT }.sumOf { it.transaction.amount }
        val totalPaid = filteredRows.filter { it.transaction.type == TransactionType.PAYMENT }.sumOf { it.transaction.amount }

        CustomerStatementUiState(
            isLoading = false,
            customer = customer,
            statementRows = filteredRows,
            totalCredit = totalCredit,
            totalPaid = totalPaid,
            fromDate = fromDate,
            toDate = toDate,
        )
    }
        .catch { emit(CustomerStatementUiState(isLoading = false, hasError = true)) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = CustomerStatementUiState(),
        )

    fun setDateFilter(fromDate: LocalDate?, toDate: LocalDate?) {
        dateFilterState.value = DateFilterRange(fromDate, toDate)
    }

    fun generateShareableText(currency: CurrencyFormatter): String {
        val state = uiState.value
        val customer = state.customer?.customer ?: return ""
        val balance = state.customer.balance

        val sb = StringBuilder()
        sb.appendLine("📋 KHATA CUSTOMER STATEMENT")
        sb.appendLine("---------------------------")
        sb.appendLine("Customer: ${customer.name}")
        customer.phone?.let { sb.appendLine("Phone: $it") }
        sb.appendLine("Date: ${DateFormatter.full(LocalDate.now())}")
        sb.appendLine("---------------------------")
        sb.appendLine("TRANSACTION HISTORY:")

        state.statementRows.forEach { row ->
            val dateStr = DateFormatter.full(row.transaction.date)
            val typeStr = if (row.transaction.type == TransactionType.CREDIT) "CREDIT" else "PAYMENT"
            val desc = row.transaction.description ?: ""
            val amt = currency.format(row.transaction.amount)
            val bal = currency.format(row.runningBalance)
            sb.appendLine("$dateStr | $typeStr | $amt | Bal: $bal")
            if (desc.isNotEmpty()) {
                sb.appendLine("   Note: $desc")
            }
        }

        sb.appendLine("---------------------------")
        sb.appendLine("Total Credit: ${currency.format(state.totalCredit)}")
        sb.appendLine("Total Paid:   ${currency.format(state.totalPaid)}")
        sb.appendLine("OUTSTANDING:  ${currency.format(balance.outstanding)}")
        sb.appendLine("---------------------------")
        sb.appendLine("Thank you for doing business with us!")

        return sb.toString()
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
