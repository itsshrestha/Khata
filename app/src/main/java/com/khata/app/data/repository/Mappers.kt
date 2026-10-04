package com.khata.app.data.repository

import com.khata.app.data.local.entity.BalanceTotalsRow
import com.khata.app.data.local.entity.CustomerEntity
import com.khata.app.data.local.entity.CustomerWithTotalsRow
import com.khata.app.data.local.entity.TransactionEntity
import com.khata.app.data.local.entity.TransactionItemEntity
import com.khata.app.data.local.entity.TransactionWithCustomerRow
import com.khata.app.domain.model.Balance
import com.khata.app.domain.model.Customer
import com.khata.app.domain.model.CustomerWithBalance
import com.khata.app.domain.model.Transaction
import com.khata.app.domain.model.TransactionItem
import com.khata.app.domain.model.TransactionWithCustomer
import java.time.Instant
import java.time.LocalDate

/**
 * Entity <-> domain mapping.
 */

internal fun CustomerEntity.toDomain() = Customer(
    id = id,
    name = name,
    phone = phone,
    address = address,
    notes = notes,
    isArchived = isArchived,
    createdAt = Instant.ofEpochMilli(createdAt),
    updatedAt = Instant.ofEpochMilli(updatedAt),
    deviceName = deviceName,
)

internal fun BalanceTotalsRow.toDomain() = Balance(totalCredit = totalCredit, totalPaid = totalPaid)

internal fun CustomerWithTotalsRow.toDomain() = CustomerWithBalance(
    customer = customer.toDomain(),
    balance = Balance(totalCredit = totalCredit, totalPaid = totalPaid),
)

internal fun TransactionEntity.toDomain(items: List<TransactionItem> = emptyList()) = Transaction(
    id = id,
    customerId = customerId,
    type = type,
    amount = amount,
    description = description,
    paymentMethod = paymentMethod,
    date = LocalDate.ofEpochDay(transactionDate),
    createdAt = Instant.ofEpochMilli(createdAt),
    updatedAt = Instant.ofEpochMilli(updatedAt),
    items = items,
    deviceName = deviceName,
)

internal fun TransactionWithCustomerRow.toDomain() = TransactionWithCustomer(
    transaction = transaction.toDomain(),
    customerName = customerName,
)

internal fun TransactionItemEntity.toDomain() = TransactionItem(
    id = id,
    transactionId = transactionId,
    itemName = itemName,
    quantity = quantity,
    unitPrice = unitPrice,
    totalPrice = totalPrice,
)

internal fun TransactionItem.toEntity(transactionId: String) = TransactionItemEntity(
    id = id,
    transactionId = transactionId,
    itemName = itemName,
    quantity = quantity,
    unitPrice = unitPrice,
    totalPrice = totalPrice,
)
