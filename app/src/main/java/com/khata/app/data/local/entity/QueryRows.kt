package com.khata.app.data.local.entity

import androidx.room.Embedded

/**
 * Query result rows (not tables). Balances are computed in SQL from the transactions table,
 * so there is no stored "balance" column to drift out of sync.
 */

/** Credit/payment totals; the balance is `totalCredit - totalPaid`. */
data class BalanceTotalsRow(
    val totalCredit: Long,
    val totalPaid: Long,
)

data class CustomerWithTotalsRow(
    @Embedded val customer: CustomerEntity,
    val totalCredit: Long,
    val totalPaid: Long,
)

data class TransactionWithCustomerRow(
    @Embedded val transaction: TransactionEntity,
    val customerName: String,
)
