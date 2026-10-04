package com.khata.app.ui.credit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.khata.app.data.repository.CustomerRepository
import com.khata.app.data.repository.TransactionRepository
import com.khata.app.domain.model.KhataError
import com.khata.app.domain.model.Outcome
import com.khata.app.domain.usecase.CreditFormMapper
import com.khata.app.domain.usecase.CreditItemDraft
import com.khata.app.utils.MoneyParser
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

enum class CreditMode { QUICK, DETAILED }

/** One editable row of the detailed form. Everything is text until it is validated on save. */
data class CreditItemForm(
    val id: Long,
    val name: String = "",
    val quantity: String = "1",
    val unitPrice: String = "",
) {
    fun toDraft(): CreditItemDraft = CreditFormMapper.toDraft(name, quantity, unitPrice)
}

data class AddCreditUiState(
    val isLoading: Boolean = true,
    val customerName: String? = null,
    val mode: CreditMode = CreditMode.QUICK,
    val amount: String = "",
    val description: String = "",
    val date: LocalDate = LocalDate.now(),
    val items: List<CreditItemForm> = emptyList(),
    val amountError: KhataError? = null,
    val itemsError: KhataError? = null,
    val formError: KhataError? = null,
    val isSaving: Boolean = false,
    /** True after a successful save; the screen navigates away. */
    val saved: Boolean = false,
) {
    /** Live total for the detailed form, in minor units. */
    val detailedTotal: Long get() = CreditFormMapper.liveTotal(items.map { it.toDraft() })

    val customerMissing: Boolean get() = !isLoading && customerName == null
}

class AddCreditViewModel(
    private val customerId: String,
    private val customerRepository: CustomerRepository,
    private val transactionRepository: TransactionRepository,
) : ViewModel() {

    private var nextItemId = 1L

    private val _state = MutableStateFlow(AddCreditUiState(items = listOf(CreditItemForm(id = nextItemId++))))
    val state: StateFlow<AddCreditUiState> = _state.asStateFlow()

    init {
        loadCustomer()
    }

    fun onModeChange(mode: CreditMode) = _state.update {
        it.copy(mode = mode, amountError = null, itemsError = null, formError = null)
    }

    fun onAmountChange(value: String) = _state.update { it.copy(amount = value, amountError = null, formError = null) }

    fun onDescriptionChange(value: String) = _state.update { it.copy(description = value, formError = null) }

    fun onDateChange(date: LocalDate) = _state.update { it.copy(date = date) }

    fun onAddItem() = _state.update {
        it.copy(items = it.items + CreditItemForm(id = nextItemId++), itemsError = null)
    }

    fun onRemoveItem(itemId: Long) = _state.update { current ->
        // Always keep at least one row so the form never becomes empty.
        val remaining = current.items.filterNot { it.id == itemId }
        current.copy(
            items = remaining.ifEmpty { listOf(CreditItemForm(id = nextItemId++)) },
            itemsError = null,
        )
    }

    fun onItemChange(itemId: Long, transform: (CreditItemForm) -> CreditItemForm) = _state.update { current ->
        current.copy(
            items = current.items.map { if (it.id == itemId) transform(it) else it },
            itemsError = null,
            formError = null,
        )
    }

    fun onItemNameChange(itemId: Long, value: String) = onItemChange(itemId) { it.copy(name = value) }

    fun onItemQuantityChange(itemId: Long, value: String) = onItemChange(itemId) { it.copy(quantity = value) }

    fun onItemPriceChange(itemId: Long, value: String) = onItemChange(itemId) { it.copy(unitPrice = value) }

    fun save() {
        val current = _state.value
        // Duplicate-submission guard: ignore taps while saving or after success.
        if (current.isSaving || current.isLoading || current.saved || current.customerMissing) return

        _state.update { it.copy(isSaving = true, amountError = null, itemsError = null, formError = null) }
        viewModelScope.launch {
            val outcome: Outcome<String> = when (current.mode) {
                CreditMode.QUICK -> transactionRepository.addQuickCredit(
                    customerId = customerId,
                    amount = MoneyParser.parseToMinorUnits(current.amount),
                    description = current.description,
                    date = current.date,
                )
                CreditMode.DETAILED -> {
                    val drafts = current.items.map { it.toDraft() }
                    transactionRepository.addDetailedCredit(
                        customerId = customerId,
                        items = drafts,
                        // With no typed description, summarise the items so history stays readable.
                        description = current.description.ifBlank { CreditFormMapper.summarizeItems(drafts) },
                        date = current.date,
                    )
                }
            }
            when (outcome) {
                is Outcome.Success -> _state.update { it.copy(isSaving = false, saved = true) }
                is Outcome.Failure -> _state.update { it.withError(outcome.error) }
            }
        }
    }

    private fun loadCustomer() {
        viewModelScope.launch {
            try {
                val customer = customerRepository.getCustomer(customerId)
                _state.update {
                    it.copy(
                        isLoading = false,
                        customerName = customer?.takeUnless { c -> c.isArchived }?.name,
                    )
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                _state.update { it.copy(isLoading = false, formError = KhataError.DatabaseError) }
            }
        }
    }

    private fun AddCreditUiState.withError(error: KhataError): AddCreditUiState = when (error) {
        KhataError.InvalidAmount, KhataError.AmountTooLarge ->
            if (mode == CreditMode.QUICK) {
                copy(isSaving = false, amountError = error)
            } else {
                copy(isSaving = false, itemsError = error)
            }
        KhataError.NoItems, KhataError.EmptyItemName, KhataError.InvalidItemQuantity, KhataError.InvalidItemPrice ->
            copy(isSaving = false, itemsError = error)
        else -> copy(isSaving = false, formError = error)
    }
}
