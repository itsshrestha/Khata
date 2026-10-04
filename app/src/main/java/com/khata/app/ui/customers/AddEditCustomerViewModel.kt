package com.khata.app.ui.customers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.khata.app.data.repository.CustomerRepository
import com.khata.app.domain.model.KhataError
import com.khata.app.domain.model.Outcome
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CustomerFormState(
    val name: String = "",
    val phone: String = "",
    val address: String = "",
    val notes: String = "",
    val nameError: KhataError? = null,
    val phoneError: KhataError? = null,
    /** Errors not tied to a field (customer missing, database trouble). */
    val formError: KhataError? = null,
    /** True while an existing customer is being loaded for editing. */
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    /** Set once saved; the screen reacts by navigating away. */
    val savedCustomerId: String? = null,
)

/**
 * Drives both "Add Customer" (customerId == null) and "Edit Customer".
 * Validation lives in the repository/use case, so the rules exist in exactly one place.
 */
class AddEditCustomerViewModel(
    private val customerId: String?,
    private val customerRepository: CustomerRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(CustomerFormState(isLoading = customerId != null))
    val state: StateFlow<CustomerFormState> = _state.asStateFlow()

    val isEditing: Boolean get() = customerId != null

    init {
        if (customerId != null) loadExisting(customerId)
    }

    fun onNameChange(value: String) = _state.update { it.copy(name = value, nameError = null, formError = null) }

    fun onPhoneChange(value: String) = _state.update { it.copy(phone = value, phoneError = null, formError = null) }

    fun onAddressChange(value: String) = _state.update { it.copy(address = value, formError = null) }

    fun onNotesChange(value: String) = _state.update { it.copy(notes = value, formError = null) }

    fun save() {
        val current = _state.value
        // Guards against accidental double taps and saving while still loading.
        if (current.isSaving || current.isLoading || current.savedCustomerId != null) return

        _state.update { it.copy(isSaving = true, nameError = null, phoneError = null, formError = null) }
        viewModelScope.launch {
            val outcome: Outcome<String> = if (customerId == null) {
                customerRepository.addCustomer(current.name, current.phone, current.address, current.notes)
            } else {
                when (
                    val updated = customerRepository.updateCustomer(
                        id = customerId,
                        name = current.name,
                        phone = current.phone,
                        address = current.address,
                        notes = current.notes,
                    )
                ) {
                    is Outcome.Success -> Outcome.Success(customerId)
                    is Outcome.Failure -> updated
                }
            }
            when (outcome) {
                is Outcome.Success -> _state.update { it.copy(isSaving = false, savedCustomerId = outcome.value) }
                is Outcome.Failure -> _state.update { it.withError(outcome.error) }
            }
        }
    }

    private fun loadExisting(id: String) {
        viewModelScope.launch {
            try {
                val customer = customerRepository.getCustomer(id)
                if (customer == null) {
                    _state.update { it.copy(isLoading = false, formError = KhataError.CustomerNotFound) }
                } else {
                    _state.update {
                        it.copy(
                            name = customer.name,
                            phone = customer.phone.orEmpty(),
                            address = customer.address.orEmpty(),
                            notes = customer.notes.orEmpty(),
                            isLoading = false,
                        )
                    }
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                _state.update { it.copy(isLoading = false, formError = KhataError.DatabaseError) }
            }
        }
    }

    private fun CustomerFormState.withError(error: KhataError): CustomerFormState = when (error) {
        KhataError.EmptyCustomerName, KhataError.CustomerNameTooLong ->
            copy(isSaving = false, nameError = error)
        KhataError.InvalidPhone -> copy(isSaving = false, phoneError = error)
        else -> copy(isSaving = false, formError = error)
    }
}
