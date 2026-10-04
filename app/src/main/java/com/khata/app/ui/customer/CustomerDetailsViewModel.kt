package com.khata.app.ui.customer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.khata.app.data.repository.CustomerRepository
import com.khata.app.data.repository.TransactionRepository
import com.khata.app.domain.model.CustomerWithBalance
import com.khata.app.domain.model.KhataError
import com.khata.app.domain.model.Outcome
import com.khata.app.domain.model.Transaction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CustomerDetailsUiState(
    val isLoading: Boolean = true,
    val customer: CustomerWithBalance? = null,
    /** Full history, oldest first. */
    val transactions: List<Transaction> = emptyList(),
    val notFound: Boolean = false,
    val hasError: Boolean = false,
    val isArchiving: Boolean = false,
    /** True after a successful archive; the screen navigates back. */
    val archived: Boolean = false,
    val isDeleting: Boolean = false,
    /** True after a successful permanent delete; the screen navigates back. */
    val deleted: Boolean = false,
    /** One-shot error for a snackbar; cleared via [CustomerDetailsViewModel.onActionErrorShown]. */
    val actionError: KhataError? = null,
)

class CustomerDetailsViewModel(
    private val customerId: String,
    private val customerRepository: CustomerRepository,
    private val transactionRepository: TransactionRepository,
) : ViewModel() {

    private data class ActionState(
        val isArchiving: Boolean = false,
        val archived: Boolean = false,
        val isDeleting: Boolean = false,
        val deleted: Boolean = false,
        val actionError: KhataError? = null,
    )

    private val actionState = MutableStateFlow(ActionState())

    /** Customer, balance and history are all observed, so the screen updates the moment a transaction is saved. */
    private val dataState = combine(
        customerRepository.observeCustomer(customerId),
        transactionRepository.observeCustomerTransactions(customerId),
    ) { customer, transactions ->
        CustomerDetailsUiState(
            isLoading = false,
            customer = customer,
            transactions = transactions,
            notFound = customer == null,
        )
    }.catch { emit(CustomerDetailsUiState(isLoading = false, hasError = true)) }

    val uiState: StateFlow<CustomerDetailsUiState> = combine(dataState, actionState) { data, action ->
        data.copy(
            isArchiving = action.isArchiving,
            archived = action.archived,
            isDeleting = action.isDeleting,
            deleted = action.deleted,
            actionError = action.actionError,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = CustomerDetailsUiState(),
    )

    /** Soft-deletes the customer. History is kept; the customer just leaves the active list. */
    fun archiveCustomer() {
        if (actionState.value.isArchiving || actionState.value.archived) return
        actionState.update { it.copy(isArchiving = true, actionError = null) }
        viewModelScope.launch {
            when (val outcome = customerRepository.archiveCustomer(customerId)) {
                is Outcome.Success<*> -> actionState.update { it.copy(isArchiving = false, archived = true) }
                is Outcome.Failure ->
                    actionState.update { it.copy(isArchiving = false, actionError = outcome.error) }
            }
        }
    }

    /** Permanently deletes the customer and all associated transactions. */
    fun deleteCustomer() {
        if (actionState.value.isDeleting || actionState.value.deleted) return
        actionState.update { it.copy(isDeleting = true, actionError = null) }
        viewModelScope.launch {
            when (val outcome = customerRepository.deleteCustomer(customerId)) {
                is Outcome.Success<*> -> actionState.update { it.copy(isDeleting = false, deleted = true) }
                is Outcome.Failure ->
                    actionState.update { it.copy(isDeleting = false, actionError = outcome.error) }
            }
        }
    }

    fun deleteTransaction(transactionId: String) {
        viewModelScope.launch {
            when (val outcome = transactionRepository.deleteTransaction(transactionId)) {
                is Outcome.Success<*> -> { /* State updates automatically via Flow */ }
                is Outcome.Failure -> actionState.update { it.copy(actionError = outcome.error) }
            }
        }
    }

    fun onActionErrorShown() {
        actionState.update { it.copy(actionError = null) }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
