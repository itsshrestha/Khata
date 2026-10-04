package com.khata.app.data.repository

import com.khata.app.domain.model.Balance
import com.khata.app.domain.model.Outcome
import com.khata.app.domain.model.PaymentMethod
import com.khata.app.domain.model.PaymentReceipt
import com.khata.app.domain.model.Transaction
import com.khata.app.domain.model.TransactionFilter
import com.khata.app.domain.model.TransactionType
import com.khata.app.domain.model.TransactionWithCustomer
import com.khata.app.domain.usecase.CreditItemDraft
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface TransactionRepository {

    fun observeCustomerTransactions(customerId: String): Flow<List<Transaction>>

    fun observeTransactions(filter: TransactionFilter = TransactionFilter()): Flow<List<TransactionWithCustomer>>

    fun observeRecentTransactions(limit: Int): Flow<List<TransactionWithCustomer>>

    fun observeTransaction(id: String): Flow<Transaction?>

    fun observeCustomerBalance(customerId: String): Flow<Balance>

    fun observeOverallBalance(): Flow<Balance>

    fun observeTotal(type: TransactionType, from: LocalDate, to: LocalDate): Flow<Long>

    suspend fun addQuickCredit(
        customerId: String,
        amount: Long?,
        description: String?,
        date: LocalDate,
    ): Outcome<String>

    suspend fun addDetailedCredit(
        customerId: String,
        items: List<CreditItemDraft>,
        description: String?,
        date: LocalDate,
    ): Outcome<String>

    suspend fun recordPayment(
        customerId: String,
        amount: Long?,
        method: PaymentMethod,
        note: String?,
        date: LocalDate,
    ): Outcome<PaymentReceipt>

    suspend fun updateTransaction(
        id: String,
        amount: Long?,
        description: String?,
        paymentMethod: PaymentMethod?,
        date: LocalDate,
    ): Outcome<Unit>

    suspend fun deleteTransaction(id: String): Outcome<Unit>
}
