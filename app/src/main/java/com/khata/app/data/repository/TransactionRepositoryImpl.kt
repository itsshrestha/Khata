package com.khata.app.data.repository

import androidx.room.withTransaction
import com.khata.app.data.local.database.KhataDatabase
import com.khata.app.data.local.entity.SyncEntityType
import com.khata.app.data.local.entity.SyncOperationEntity
import com.khata.app.data.local.entity.SyncOperationType
import com.khata.app.data.local.entity.SyncStatus
import com.khata.app.data.local.entity.TransactionEntity
import com.khata.app.domain.model.Balance
import com.khata.app.domain.model.KhataError
import com.khata.app.domain.model.Outcome
import com.khata.app.domain.model.PaymentMethod
import com.khata.app.domain.model.PaymentReceipt
import com.khata.app.domain.model.Transaction
import com.khata.app.domain.model.TransactionFilter
import com.khata.app.domain.model.TransactionType
import com.khata.app.domain.model.TransactionWithCustomer
import com.khata.app.domain.usecase.CreditItemDraft
import com.khata.app.domain.usecase.ValidateCreditUseCase
import com.khata.app.domain.usecase.ValidatePaymentUseCase
import com.khata.app.domain.usecase.ValidateTransactionChangeUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.LocalDate
import java.util.UUID

class TransactionRepositoryImpl(
    private val database: KhataDatabase,
    private val validateCredit: ValidateCreditUseCase = ValidateCreditUseCase(),
    private val validatePayment: ValidatePaymentUseCase = ValidatePaymentUseCase(),
    private val validateTransactionChange: ValidateTransactionChangeUseCase = ValidateTransactionChangeUseCase(),
    private val clock: Clock = Clock.systemDefaultZone(),
    private val onDataChanged: (() -> Unit)? = null,
) : TransactionRepository {

    private val customerDao = database.customerDao()
    private val transactionDao = database.transactionDao()
    private val itemDao = database.transactionItemDao()
    private val syncDao = database.syncOperationDao()

    override fun observeCustomerTransactions(customerId: String): Flow<List<Transaction>> =
        transactionDao.observeForCustomer(customerId).map { rows ->
            rows.map { entity ->
                entity.toDomain(items = itemDao.getForTransaction(entity.id).map { it.toDomain() })
            }
        }

    override fun observeTransactions(filter: TransactionFilter): Flow<List<TransactionWithCustomer>> =
        transactionDao.observeFiltered(
            type = filter.type,
            customerId = filter.customerId,
            method = filter.paymentMethod,
            fromDay = filter.fromDate?.toEpochDay(),
            toDay = filter.toDate?.toEpochDay(),
        ).map { rows ->
            rows.map { row ->
                val items = itemDao.getForTransaction(row.transaction.id).map { it.toDomain() }
                TransactionWithCustomer(
                    transaction = row.transaction.toDomain(items = items),
                    customerName = row.customerName,
                )
            }
        }

    override fun observeRecentTransactions(limit: Int): Flow<List<TransactionWithCustomer>> =
        transactionDao.observeRecent(limit).map { rows ->
            rows.map { row ->
                val items = itemDao.getForTransaction(row.transaction.id).map { it.toDomain() }
                TransactionWithCustomer(
                    transaction = row.transaction.toDomain(items = items),
                    customerName = row.customerName,
                )
            }
        }

    override fun observeTransaction(id: String): Flow<Transaction?> =
        transactionDao.observeById(id).map { entity ->
            entity?.toDomain(items = itemDao.getForTransaction(entity.id).map { it.toDomain() })
        }

    override fun observeCustomerBalance(customerId: String): Flow<Balance> =
        transactionDao.observeTotalsForCustomer(customerId).map { it.toDomain() }

    override fun observeOverallBalance(): Flow<Balance> =
        transactionDao.observeOverallTotals().map { it.toDomain() }

    override fun observeTotal(type: TransactionType, from: LocalDate, to: LocalDate): Flow<Long> =
        transactionDao.observeTotalForType(type, from.toEpochDay(), to.toEpochDay())

    override suspend fun addQuickCredit(
        customerId: String,
        amount: Long?,
        description: String?,
        date: LocalDate,
    ): Outcome<String> {
        val validAmount = when (val result = validateCredit.quick(amount)) {
            is Outcome.Failure -> return result
            is Outcome.Success -> result.value
        }
        return safeDatabaseCall {
            val now = clock.millis()
            val txId = UUID.randomUUID().toString()
            database.withTransaction<Outcome<String>> {
                requireActiveCustomer(customerId)?.let { return@withTransaction Outcome.Failure(it) }
                transactionDao.insert(
                    newEntity(txId, customerId, TransactionType.CREDIT, validAmount, description, null, date, now),
                )
                syncDao.insert(
                    SyncOperationEntity(
                        entityType = SyncEntityType.TRANSACTION,
                        entityId = txId,
                        operationType = SyncOperationType.CREATE,
                        createdAt = now,
                    ),
                )
                Outcome.Success(txId)
            }.also { if (it is Outcome.Success) onDataChanged?.invoke() }
        }
    }

    override suspend fun addDetailedCredit(
        customerId: String,
        items: List<CreditItemDraft>,
        description: String?,
        date: LocalDate,
    ): Outcome<String> {
        val credit = when (val result = validateCredit.detailed(items)) {
            is Outcome.Failure -> return result
            is Outcome.Success -> result.value
        }
        return safeDatabaseCall {
            val now = clock.millis()
            val txId = UUID.randomUUID().toString()
            database.withTransaction<Outcome<String>> {
                requireActiveCustomer(customerId)?.let { return@withTransaction Outcome.Failure(it) }
                transactionDao.insert(
                    newEntity(txId, customerId, TransactionType.CREDIT, credit.total, description, null, date, now),
                )
                val itemEntities = credit.items.map { it.toEntity(transactionId = txId) }
                itemDao.insertAll(itemEntities)
                syncDao.insert(
                    SyncOperationEntity(
                        entityType = SyncEntityType.TRANSACTION,
                        entityId = txId,
                        operationType = SyncOperationType.CREATE,
                        createdAt = now,
                    ),
                )
                Outcome.Success(txId)
            }.also { if (it is Outcome.Success) onDataChanged?.invoke() }
        }
    }

    override suspend fun recordPayment(
        customerId: String,
        amount: Long?,
        method: PaymentMethod,
        note: String?,
        date: LocalDate,
    ): Outcome<PaymentReceipt> = safeDatabaseCall {
        val now = clock.millis()
        val txId = UUID.randomUUID().toString()
        database.withTransaction<Outcome<PaymentReceipt>> {
            requireActiveCustomer(customerId)?.let { return@withTransaction Outcome.Failure(it) }

            val before = transactionDao.getTotalsForCustomer(customerId).toDomain()
            val validAmount = when (val result = validatePayment(amount, before.outstanding)) {
                is Outcome.Failure -> return@withTransaction result
                is Outcome.Success -> result.value
            }

            transactionDao.insert(
                newEntity(txId, customerId, TransactionType.PAYMENT, validAmount, note, method, date, now),
            )
            syncDao.insert(
                SyncOperationEntity(
                    entityType = SyncEntityType.TRANSACTION,
                    entityId = txId,
                    operationType = SyncOperationType.CREATE,
                    createdAt = now,
                ),
            )
            Outcome.Success(
                PaymentReceipt(
                    transactionId = txId,
                    previousOutstanding = before.outstanding,
                    remainingOutstanding = before.outstanding - validAmount,
                ),
            )
        }.also { if (it is Outcome.Success) onDataChanged?.invoke() }
    }

    override suspend fun updateTransaction(
        id: String,
        amount: Long?,
        description: String?,
        paymentMethod: PaymentMethod?,
        date: LocalDate,
    ): Outcome<Unit> = safeDatabaseCall {
        val now = clock.millis()
        database.withTransaction<Outcome<Unit>> {
            val existing = transactionDao.getById(id) ?: return@withTransaction Outcome.Failure(KhataError.TransactionNotFound)
            val items = itemDao.getForTransaction(id).map { it.toDomain() }
            val domainTx = existing.toDomain(items = items)

            val currentOutstanding = transactionDao.getTotalsForCustomer(existing.customerId).toDomain().outstanding

            val targetAmount = if (amount != null && amount != existing.amount) {
                if (domainTx.items.isNotEmpty()) {
                    return@withTransaction Outcome.Failure(KhataError.DetailedAmountLocked)
                }
                when (val result = validateTransactionChange.validateAmountEdit(domainTx, amount, currentOutstanding)) {
                    is Outcome.Failure -> return@withTransaction result
                    is Outcome.Success -> result.value
                }
            } else {
                existing.amount
            }

            val updatedEntity = existing.copy(
                amount = targetAmount,
                description = description?.trim()?.takeIf { it.isNotEmpty() },
                paymentMethod = paymentMethod,
                transactionDate = date.toEpochDay(),
                updatedAt = now,
                syncStatus = SyncStatus.PENDING_UPDATE,
            )
            transactionDao.update(updatedEntity)
            syncDao.insert(
                SyncOperationEntity(
                    entityType = SyncEntityType.TRANSACTION,
                    entityId = id,
                    operationType = SyncOperationType.UPDATE,
                    createdAt = now,
                ),
            )
            Outcome.Success(Unit)
        }.also { if (it is Outcome.Success) onDataChanged?.invoke() }
    }

    override suspend fun deleteTransaction(id: String): Outcome<Unit> = safeDatabaseCall {
        val now = clock.millis()
        database.withTransaction<Outcome<Unit>> {
            val existing = transactionDao.getById(id) ?: return@withTransaction Outcome.Failure(KhataError.TransactionNotFound)
            val items = itemDao.getForTransaction(id).map { it.toDomain() }
            val domainTx = existing.toDomain(items = items)

            val currentOutstanding = transactionDao.getTotalsForCustomer(existing.customerId).toDomain().outstanding
            when (val result = validateTransactionChange.validateDelete(domainTx, currentOutstanding)) {
                is Outcome.Failure -> return@withTransaction result
                is Outcome.Success -> {
                    transactionDao.softDeleteById(id, now, SyncStatus.PENDING_DELETE)
                    syncDao.insert(
                        SyncOperationEntity(
                            entityType = SyncEntityType.TRANSACTION,
                            entityId = id,
                            operationType = SyncOperationType.DELETE,
                            createdAt = now,
                        ),
                    )
                    Outcome.Success(Unit)
                }
            }
        }.also { if (it is Outcome.Success) onDataChanged?.invoke() }
    }

    private suspend fun requireActiveCustomer(customerId: String): KhataError? {
        val customer = customerDao.getById(customerId) ?: return KhataError.CustomerNotFound
        return if (customer.isArchived) KhataError.InvalidTransaction else null
    }

    private fun newEntity(
        id: String,
        customerId: String,
        type: TransactionType,
        amount: Long,
        description: String?,
        method: PaymentMethod?,
        date: LocalDate,
        now: Long,
    ): TransactionEntity {
        return TransactionEntity(
            id = id,
            customerId = customerId,
            type = type,
            amount = amount,
            description = description?.trim()?.takeIf { it.isNotEmpty() },
            paymentMethod = method,
            transactionDate = date.toEpochDay(),
            createdAt = now,
            updatedAt = now,
            syncStatus = SyncStatus.PENDING_CREATE,
            deviceName = com.khata.app.utils.DeviceUtils.getDeviceName(),
        )
    }
}
