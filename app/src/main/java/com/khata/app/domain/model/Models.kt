package com.khata.app.domain.model

import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/**
 * Domain models. All money values are [Long] minor units (paisa): Rs. 12.50 == 1250.
 * IDs are UUID strings to ensure multi-device synchronization integrity.
 */

data class Customer(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val phone: String? = null,
    val address: String? = null,
    val notes: String? = null,
    val isArchived: Boolean = false,
    val createdAt: Instant,
    val updatedAt: Instant,
    val deviceName: String? = null,
)

data class Transaction(
    val id: String = UUID.randomUUID().toString(),
    val customerId: String,
    val type: TransactionType,
    /** Always positive. Direction comes from [type]. */
    val amount: Long,
    val description: String? = null,
    /** Only set for [TransactionType.PAYMENT]. */
    val paymentMethod: PaymentMethod? = null,
    /** The business date the user picked. */
    val date: LocalDate,
    val createdAt: Instant,
    val updatedAt: Instant,
    /** Only populated for detailed CREDIT transactions. */
    val items: List<TransactionItem> = emptyList(),
    val deviceName: String? = null,
)

data class TransactionItem(
    val id: String = UUID.randomUUID().toString(),
    val transactionId: String = "",
    val itemName: String,
    val quantity: Double,
    /** Minor units per single unit. */
    val unitPrice: Long,
    /** Minor units; quantity * unitPrice rounded to a whole minor unit. */
    val totalPrice: Long,
)

/** A customer together with their derived balance. */
data class CustomerWithBalance(
    val customer: Customer,
    val balance: Balance,
)

/** A transaction together with the owning customer's name, for global lists. */
data class TransactionWithCustomer(
    val transaction: Transaction,
    val customerName: String,
)

/** Optional filters for the global transaction list. `null` means "no filter". */
data class TransactionFilter(
    val type: TransactionType? = null,
    val customerId: String? = null,
    val paymentMethod: PaymentMethod? = null,
    val fromDate: LocalDate? = null,
    val toDate: LocalDate? = null,
)

/** What the user sees after a payment is saved. */
data class PaymentReceipt(
    val transactionId: String,
    val previousOutstanding: Long,
    val remainingOutstanding: Long,
)
