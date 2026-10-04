package com.khata.app.ui.payment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.khata.app.data.repository.CustomerRepository
import com.khata.app.data.repository.TransactionRepository
import com.khata.app.domain.model.KhataError
import com.khata.app.domain.model.Outcome
import com.khata.app.domain.model.PaymentMethod
import com.khata.app.domain.model.PaymentReceipt
import com.khata.app.domain.usecase.ValidatePaymentUseCase
import com.khata.app.utils.MoneyParser
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class RecordPaymentUiState(
    val isLoading: Boolean = true,
    val customerName: String? = null,
    val outstanding: Long = 0L,
    val amountText: String = "",
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val note: String = "",
    val date: LocalDate = LocalDate.now(),
    val amountError: KhataError? = null,
    val formError: KhataError? = null,
    val showConfirmation: Boolean = false,
    val pendingAmountMinor: Long = 0L,
    val isSaving: Boolean = false,
    val receipt: PaymentReceipt? = null,
) {
    val customerMissing: Boolean get() = !isLoading && customerName == null
    val canSubmit: Boolean get() = !isLoading && !isSaving && outstanding > 0L
}

class RecordPaymentViewModel(
    private val customerId: String,
    private val customerRepository: CustomerRepository,
    private val transactionRepository: TransactionRepository,
    private val validatePayment: ValidatePaymentUseCase = ValidatePaymentUseCase(),
) : ViewModel() {

    private val _state = MutableStateFlow(RecordPaymentUiState())
    val state: StateFlow<RecordPaymentUiState> = _state.asStateFlow()

    init {
        loadCustomerData()
    }

    fun onAmountChange(value: String) = _state.update {
        it.copy(amountText = value, amountError = null, formError = null)
    }

    fun onPaymentMethodChange(method: PaymentMethod) = _state.update {
        it.copy(paymentMethod = method)
    }

    fun onNoteChange(note: String) = _state.update {
        it.copy(note = note)
    }

    fun onDateChange(date: LocalDate) = _state.update {
        it.copy(date = date)
    }

    /** Triggers validation and opens the confirmation dialog if valid. */
    fun onRequestRecord() {
        val current = _state.value
        if (!current.canSubmit) return

        val parsedAmount = MoneyParser.parseToMinorUnits(current.amountText)
        when (val result = validatePayment(parsedAmount, current.outstanding)) {
            is Outcome.Failure -> {
                _state.update { it.copy(amountError = result.error) }
            }
            is Outcome.Success -> {
                _state.update {
                    it.copy(
                        amountError = null,
                        pendingAmountMinor = result.value,
                        showConfirmation = true,
                    )
                }
            }
        }
    }

    fun onDismissConfirmation() {
        _state.update { it.copy(showConfirmation = false) }
    }

    fun onConfirmRecord() {
        val current = _state.value
        if (current.isSaving || current.receipt != null) return

        _state.update { it.copy(isSaving = true, showConfirmation = false) }

        viewModelScope.launch {
            val outcome = transactionRepository.recordPayment(
                customerId = customerId,
                amount = current.pendingAmountMinor,
                method = current.paymentMethod,
                note = current.note,
                date = current.date,
            )
            when (outcome) {
                is Outcome.Success -> {
                    _state.update { it.copy(isSaving = false, receipt = outcome.value) }
                }
                is Outcome.Failure -> {
                    _state.update { it.copy(isSaving = false, formError = outcome.error) }
                }
            }
        }
    }

    private fun loadCustomerData() {
        viewModelScope.launch {
            try {
                val customerWithBalance = customerRepository.getCustomer(customerId)
                if (customerWithBalance == null || customerWithBalance.isArchived) {
                    _state.update { it.copy(isLoading = false, customerName = null) }
                } else {
                    transactionRepository.observeCustomerBalance(customerId).collect { balance ->
                        _state.update {
                            it.copy(
                                isLoading = false,
                                customerName = customerWithBalance.name,
                                outstanding = balance.outstanding,
                            )
                        }
                    }
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                _state.update { it.copy(isLoading = false, formError = KhataError.DatabaseError) }
            }
        }
    }
}
