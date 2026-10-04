package com.khata.app.domain.usecase

import com.khata.app.domain.model.KhataError
import com.khata.app.domain.model.Outcome

/** Cleaned-up customer form values: trimmed, with blank optional fields turned into `null`. */
data class CustomerInput(
    val name: String,
    val phone: String?,
    val address: String?,
    val notes: String?,
)

class ValidateCustomerUseCase {

    operator fun invoke(
        name: String,
        phone: String?,
        address: String?,
        notes: String?,
    ): Outcome<CustomerInput> {
        val cleanName = name.trim()
        if (cleanName.isEmpty()) return Outcome.Failure(KhataError.EmptyCustomerName)
        if (cleanName.length > MAX_NAME_LENGTH) return Outcome.Failure(KhataError.CustomerNameTooLong)

        val cleanPhone = phone.blankToNull()
        if (cleanPhone != null && !isValidPhone(cleanPhone)) {
            return Outcome.Failure(KhataError.InvalidPhone)
        }

        return Outcome.Success(
            CustomerInput(
                name = cleanName,
                phone = cleanPhone,
                address = address.blankToNull(),
                notes = notes.blankToNull(),
            ),
        )
    }

    /** Lenient on purpose: optional leading +, digits, spaces and dashes; 7 to 15 digits. */
    private fun isValidPhone(phone: String): Boolean {
        if (!phone.matches(Regex("""\+?[0-9 \-]+"""))) return false
        return phone.count { it.isDigit() } in MIN_PHONE_DIGITS..MAX_PHONE_DIGITS
    }

    private fun String?.blankToNull(): String? = this?.trim()?.takeIf { it.isNotEmpty() }

    companion object {
        const val MAX_NAME_LENGTH = 100
        const val MIN_PHONE_DIGITS = 7
        const val MAX_PHONE_DIGITS = 15
    }
}
