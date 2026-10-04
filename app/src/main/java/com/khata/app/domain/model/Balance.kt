package com.khata.app.domain.model

/** Where a balance stands, for labels like "Outstanding: Rs. 500" or "✓ Paid". */
enum class BalanceStatus {
    /** The customer owes money. */
    OWES,

    /** There is credit history and nothing is owed any more. */
    SETTLED,

    /** The customer has never taken anything on credit. */
    NO_CREDIT,
}

/**
 * A derived balance. This is NEVER persisted as a source of truth; it is always computed from
 * the transaction history: outstanding = totalCredit - totalPaid.
 */
data class Balance(
    val totalCredit: Long,
    val totalPaid: Long,
) {
    /** What the customer still owes. Can be negative only if data was edited into an advance. */
    val outstanding: Long get() = totalCredit - totalPaid

    /** True when the customer owes nothing (fully paid, or never bought on credit). */
    val isSettled: Boolean get() = outstanding <= 0L

    val status: BalanceStatus
        get() = when {
            outstanding > 0L -> BalanceStatus.OWES
            totalCredit > 0L -> BalanceStatus.SETTLED
            else -> BalanceStatus.NO_CREDIT
        }

    companion object {
        val ZERO = Balance(totalCredit = 0, totalPaid = 0)
    }
}
