package com.khata.app.domain.model

/** Kind of ledger entry. Both kinds store a POSITIVE amount; the type decides the direction. */
enum class TransactionType {
    /** Customer took goods on credit: increases what they owe. */
    CREDIT,

    /** Customer paid money back: decreases what they owe. */
    PAYMENT,
}

/** How a payment was received. Only meaningful for [TransactionType.PAYMENT]. */
enum class PaymentMethod(val displayName: String) {
    CASH("Cash"),
    ESEWA("eSewa"),
    KHALTI("Khalti"),
    BANK_TRANSFER("Bank Transfer"),
    OTHER("Other"),
}
