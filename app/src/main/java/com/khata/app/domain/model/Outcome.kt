package com.khata.app.domain.model

/** Result of an operation that can fail for a user-correctable or known reason. */
sealed interface Outcome<out T> {
    data class Success<T>(val value: T) : Outcome<T>
    data class Failure(val error: KhataError) : Outcome<Nothing>
}

inline fun <T, R> Outcome<T>.map(transform: (T) -> R): Outcome<R> = when (this) {
    is Outcome.Success -> Outcome.Success(transform(value))
    is Outcome.Failure -> this
}

/**
 * Every error the app can show the user. Messages are user-friendly: no stack traces and no
 * technical jargon. Amount-bearing messages receive a formatter so the currency stays configurable.
 */
sealed interface KhataError {

    // ---- Customer ----
    data object EmptyCustomerName : KhataError
    data object CustomerNameTooLong : KhataError
    data object InvalidPhone : KhataError
    data object CustomerNotFound : KhataError

    // ---- Amounts ----
    /** Missing, zero, or negative amount. */
    data object InvalidAmount : KhataError
    data object AmountTooLarge : KhataError

    // ---- Credit items ----
    data object NoItems : KhataError
    data object EmptyItemName : KhataError
    data object InvalidItemQuantity : KhataError
    data object InvalidItemPrice : KhataError

    // ---- Payments ----
    data object NoOutstandingBalance : KhataError
    data class PaymentExceedsOutstanding(val outstanding: Long) : KhataError

    // ---- Editing / deleting history ----
    data object TransactionNotFound : KhataError

    /** The change would leave the customer having paid more than they ever owed. */
    data object WouldCreateNegativeBalance : KhataError

    /** A detailed credit's amount always equals the sum of its items, so it cannot be edited directly. */
    data object DetailedAmountLocked : KhataError

    // ---- Generic ----
    data object InvalidTransaction : KhataError
    data object DatabaseError : KhataError
}
