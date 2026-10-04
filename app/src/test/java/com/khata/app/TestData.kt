package com.khata.app

import com.khata.app.domain.model.PaymentMethod
import com.khata.app.domain.model.Transaction
import com.khata.app.domain.model.TransactionType
import java.time.Instant
import java.time.LocalDate

/** Small builders so financial tests read like the examples in the spec. */
object TestData {
    private val now: Instant = Instant.parse("2026-10-03T00:00:00Z")
    private val date: LocalDate = LocalDate.of(2026, 10, 3)

    /** Amounts here are in whole rupees for readability; stored as paisa like production code. */
    fun credit(rupees: Long, customerId: String = "cust-1", on: LocalDate = date) = Transaction(
        customerId = customerId,
        type = TransactionType.CREDIT,
        amount = rupees * 100,
        date = on,
        createdAt = now,
        updatedAt = now,
    )

    fun payment(
        rupees: Long,
        customerId: String = "cust-1",
        method: PaymentMethod = PaymentMethod.CASH,
        on: LocalDate = date,
    ) = Transaction(
        customerId = customerId,
        type = TransactionType.PAYMENT,
        amount = rupees * 100,
        paymentMethod = method,
        date = on,
        createdAt = now,
        updatedAt = now,
    )

    fun rupees(value: Long): Long = value * 100
}
