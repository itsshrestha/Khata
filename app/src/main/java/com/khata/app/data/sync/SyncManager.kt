package com.khata.app.data.sync

import androidx.room.withTransaction
import com.khata.app.data.local.database.KhataDatabase
import com.khata.app.data.local.entity.CustomerEntity
import com.khata.app.data.local.entity.SyncEntityType
import com.khata.app.data.local.entity.SyncOperationEntity
import com.khata.app.data.local.entity.SyncStatus
import com.khata.app.data.local.entity.TransactionEntity
import com.khata.app.data.local.entity.TransactionItemEntity
import com.khata.app.domain.model.PaymentMethod
import com.khata.app.domain.model.TransactionType
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface SyncState {
    data object Synced : SyncState
    data object Syncing : SyncState
    data class Offline(val message: String = "Offline") : SyncState
    data class Error(val message: String) : SyncState
}

class SyncManager(
    private val database: KhataDatabase,
    private val authManager: AuthManager,
    private val supabaseClient: SupabaseClient = SupabaseClientProvider.client,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO),
) {

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Synced)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private val _lastSyncTimestamp = MutableStateFlow<Long?>(null)
    val lastSyncTimestamp: StateFlow<Long?> = _lastSyncTimestamp.asStateFlow()

    fun triggerSync() {
        scope.launch {
            performSync()
        }
    }

    suspend fun performSync(): Result<Unit> = runCatching {
        val userId = authManager.currentUserId
        if (userId == null) {
            _syncState.value = SyncState.Offline("Sign in to enable cloud sync")
            return Result.success(Unit)
        }

        _syncState.value = SyncState.Syncing

        try {
            uploadAllLocalData(userId)
            uploadPendingQueue(userId)
            downloadRemoteChanges(userId)

            val now = System.currentTimeMillis()
            _lastSyncTimestamp.value = now
            _syncState.value = SyncState.Synced
        } catch (e: Exception) {
            android.util.Log.e("KhataSync", "Sync failed error", e)
            _syncState.value = SyncState.Error(e.localizedMessage ?: "Sync failed")
            throw e
        }
    }

    private fun ensureValidUuid(rawId: String): String {
        return if (rawId.matches(Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\$"))) {
            rawId
        } else {
            java.util.UUID.nameUUIDFromBytes(rawId.toByteArray()).toString()
        }
    }

    private suspend fun uploadAllLocalData(userId: String) {
        val customers = database.customerDao().getAllForSync()
        for (customer in customers) {
            val validId = ensureValidUuid(customer.id)
            if (validId != customer.id) {
                database.openHelper.writableDatabase.execSQL("UPDATE transactions SET customerId = ? WHERE customerId = ?", arrayOf(validId, customer.id))
                database.openHelper.writableDatabase.execSQL("UPDATE customers SET id = ? WHERE id = ?", arrayOf(validId, customer.id))
            }
            val remote = RemoteCustomer(
                id = validId,
                shopId = userId,
                name = customer.name,
                phone = customer.phone,
                address = customer.address,
                notes = customer.notes,
                isArchived = customer.isArchived,
                createdAt = customer.createdAt,
                updatedAt = customer.updatedAt,
                deletedAt = customer.deletedAt,
                deviceName = customer.deviceName,
            )
            supabaseClient.from("customers").upsert(remote)
            database.customerDao().updateSyncStatus(validId, SyncStatus.SYNCED)
        }

        val transactions = database.transactionDao().getAllForSync()
        for (tx in transactions) {
            val validTxId = ensureValidUuid(tx.id)
            val validCustId = ensureValidUuid(tx.customerId)
            if (validTxId != tx.id || validCustId != tx.customerId) {
                database.openHelper.writableDatabase.execSQL("UPDATE transaction_items SET transactionId = ? WHERE transactionId = ?", arrayOf(validTxId, tx.id))
                database.openHelper.writableDatabase.execSQL("UPDATE transactions SET id = ?, customerId = ? WHERE id = ?", arrayOf(validTxId, validCustId, tx.id))
            }
            val remote = RemoteTransaction(
                id = validTxId,
                shopId = userId,
                customerId = validCustId,
                type = tx.type.name,
                amount = tx.amount,
                description = tx.description,
                paymentMethod = tx.paymentMethod?.name,
                transactionDate = tx.transactionDate,
                createdAt = tx.createdAt,
                updatedAt = tx.updatedAt,
                deletedAt = tx.deletedAt,
                deviceName = tx.deviceName,
            )
            supabaseClient.from("transactions").upsert(remote)
            database.transactionDao().updateSyncStatus(validTxId, SyncStatus.SYNCED)
        }

        val items = database.transactionItemDao().getAllForSync()
        for (item in items) {
            val validItemId = ensureValidUuid(item.id)
            val validTxId = ensureValidUuid(item.transactionId)
            if (validItemId != item.id || validTxId != item.transactionId) {
                database.openHelper.writableDatabase.execSQL("UPDATE transaction_items SET id = ?, transactionId = ? WHERE id = ?", arrayOf(validItemId, validTxId, item.id))
            }
            val remote = RemoteTransactionItem(
                id = validItemId,
                shopId = userId,
                transactionId = validTxId,
                itemName = item.itemName,
                quantity = item.quantity,
                unitPrice = item.unitPrice,
                totalPrice = item.totalPrice,
                deletedAt = item.deletedAt,
            )
            supabaseClient.from("transaction_items").upsert(remote)
            database.transactionItemDao().updateSyncStatus(validItemId, SyncStatus.SYNCED)
        }
    }

    private suspend fun uploadPendingQueue(userId: String) {
        val pendingOps = database.syncOperationDao().getAllPending()
        if (pendingOps.isEmpty()) return

        for (op in pendingOps) {
            try {
                when (op.entityType) {
                    SyncEntityType.CUSTOMER -> uploadCustomer(op, userId)
                    SyncEntityType.TRANSACTION -> uploadTransaction(op, userId)
                    SyncEntityType.TRANSACTION_ITEM -> uploadTransactionItem(op, userId)
                }
                database.syncOperationDao().deleteById(op.id)
            } catch (e: Exception) {
                database.syncOperationDao().update(
                    op.copy(
                        retryCount = op.retryCount + 1,
                        lastError = e.localizedMessage,
                    ),
                )
            }
        }
    }

    private suspend fun uploadCustomer(op: SyncOperationEntity, userId: String) {
        val customer = database.customerDao().getByIdIncludingDeleted(op.entityId) ?: return
        val remote = RemoteCustomer(
            id = ensureValidUuid(customer.id),
            shopId = userId,
            name = customer.name,
            phone = customer.phone,
            address = customer.address,
            notes = customer.notes,
            isArchived = customer.isArchived,
            createdAt = customer.createdAt,
            updatedAt = customer.updatedAt,
            deletedAt = customer.deletedAt,
            deviceName = customer.deviceName,
        )
        supabaseClient.from("customers").upsert(remote)
        database.customerDao().updateSyncStatus(customer.id, SyncStatus.SYNCED)
    }

    private suspend fun uploadTransaction(op: SyncOperationEntity, userId: String) {
        val tx = database.transactionDao().getByIdIncludingDeleted(op.entityId) ?: return
        val remote = RemoteTransaction(
            id = ensureValidUuid(tx.id),
            shopId = userId,
            customerId = ensureValidUuid(tx.customerId),
            type = tx.type.name,
            amount = tx.amount,
            description = tx.description,
            paymentMethod = tx.paymentMethod?.name,
            transactionDate = tx.transactionDate,
            createdAt = tx.createdAt,
            updatedAt = tx.updatedAt,
            deletedAt = tx.deletedAt,
            deviceName = tx.deviceName,
        )
        supabaseClient.from("transactions").upsert(remote)
        database.transactionDao().updateSyncStatus(tx.id, SyncStatus.SYNCED)
    }

    private suspend fun uploadTransactionItem(op: SyncOperationEntity, userId: String) {
        val items = database.transactionItemDao().getPendingSync()
        for (item in items) {
            val remote = RemoteTransactionItem(
                id = ensureValidUuid(item.id),
                shopId = userId,
                transactionId = ensureValidUuid(item.transactionId),
                itemName = item.itemName,
                quantity = item.quantity,
                unitPrice = item.unitPrice,
                totalPrice = item.totalPrice,
                deletedAt = item.deletedAt,
            )
            supabaseClient.from("transaction_items").upsert(remote)
            database.transactionItemDao().updateSyncStatus(item.id, SyncStatus.SYNCED)
        }
    }

    private suspend fun downloadRemoteChanges(userId: String) {
        val lastSync = _lastSyncTimestamp.value ?: 0L

        val remoteCustomers = supabaseClient.from("customers")
            .select {
                filter {
                    eq("shop_id", userId)
                    gt("updated_at", lastSync)
                }
            }
            .decodeList<RemoteCustomer>()

        val remoteTransactions = supabaseClient.from("transactions")
            .select {
                filter {
                    eq("shop_id", userId)
                    gt("updated_at", lastSync)
                }
            }
            .decodeList<RemoteTransaction>()

        val remoteItems = supabaseClient.from("transaction_items")
            .select {
                filter {
                    eq("shop_id", userId)
                }
            }
            .decodeList<RemoteTransactionItem>()

        database.withTransaction {
            for (rc in remoteCustomers) {
                val local = database.customerDao().getByIdIncludingDeleted(rc.id)
                if (local == null || local.syncStatus == SyncStatus.SYNCED || rc.updatedAt >= local.updatedAt) {
                    database.customerDao().insert(
                        CustomerEntity(
                            id = rc.id,
                            name = rc.name,
                            phone = rc.phone,
                            address = rc.address,
                            notes = rc.notes,
                            isArchived = rc.isArchived,
                            createdAt = rc.createdAt,
                            updatedAt = rc.updatedAt,
                            syncStatus = SyncStatus.SYNCED,
                            deletedAt = rc.deletedAt,
                            deviceName = rc.deviceName,
                        ),
                    )
                }
            }

            for (rt in remoteTransactions) {
                if (database.customerDao().getByIdIncludingDeleted(rt.customerId) != null) {
                    val local = database.transactionDao().getByIdIncludingDeleted(rt.id)
                    if (local == null || local.syncStatus == SyncStatus.SYNCED || rt.updatedAt >= local.updatedAt) {
                        val pMethod: PaymentMethod? = rt.paymentMethod?.let { methodStr ->
                            try {
                                PaymentMethod.valueOf(methodStr)
                            } catch (_: Exception) {
                                null
                            }
                        }
                        database.transactionDao().insert(
                            TransactionEntity(
                                id = rt.id,
                                customerId = rt.customerId,
                                type = TransactionType.valueOf(rt.type),
                                amount = rt.amount,
                                description = rt.description,
                                paymentMethod = pMethod,
                                transactionDate = rt.transactionDate,
                                createdAt = rt.createdAt,
                                updatedAt = rt.updatedAt,
                                syncStatus = SyncStatus.SYNCED,
                                deletedAt = rt.deletedAt,
                                deviceName = rt.deviceName,
                            ),
                        )
                    }
                }
            }

            for (ri in remoteItems) {
                if (database.transactionDao().getByIdIncludingDeleted(ri.transactionId) != null) {
                    database.transactionItemDao().insertAll(
                        listOf(
                            TransactionItemEntity(
                                id = ri.id,
                                transactionId = ri.transactionId,
                                itemName = ri.itemName,
                                quantity = ri.quantity,
                                unitPrice = ri.unitPrice,
                                totalPrice = ri.totalPrice,
                                syncStatus = SyncStatus.SYNCED,
                                deletedAt = ri.deletedAt,
                            ),
                        ),
                    )
                }
            }
        }
    }
}
