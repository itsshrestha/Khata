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

data class CustomerStatementUiState(
    val isLoading: Boolean = true,
    val customer: CustomerWithBalance? = null,
    val statementRows: List<StatementRow> = emptyList(),
    val totalCredit: Long = 0,
    val totalPaid: Long = 0,
    val hasError: Boolean = false,
)

class CustomerStatementViewModel(
    private val customerId: String,
    customerRepository: CustomerRepository,
    transactionRepository: TransactionRepository,
) : ViewModel() {

    val uiState: StateFlow<CustomerStatementUiState> = combine(
        customerRepository.observeCustomer(customerId),
        transactionRepository.observeCustomerTransactions(customerId),
    ) { customer, transactions ->
        var running = 0L
        val rows = transactions.map { tx ->
            when (tx.type) {
                TransactionType.CREDIT -> running += tx.amount
                TransactionType.PAYMENT -> running -= tx.amount
            }
            StatementRow(transaction = tx, runningBalance = running)
        }
        val totalCredit = transactions.filter { it.type == TransactionType.CREDIT }.sumOf { it.amount }
        val totalPaid = transactions.filter { it.type == TransactionType.PAYMENT }.sumOf { it.amount }

        CustomerStatementUiState(
            isLoading = false,
            customer = customer,
            statementRows = rows,
            totalCredit = totalCredit,
            totalPaid = totalPaid,
        )
    }
        .catch { emit(CustomerStatementUiState(isLoading = false, hasError = true)) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = CustomerStatementUiState(),
        )

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
