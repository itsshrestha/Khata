package com.khata.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.khata.app.data.local.entity.SyncStatus
import com.khata.app.data.local.entity.TransactionItemEntity

@Dao
interface TransactionItemDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<TransactionItemEntity>)

    @Query("SELECT * FROM transaction_items WHERE transactionId = :transactionId AND deletedAt IS NULL ORDER BY id ASC")
    suspend fun getForTransaction(transactionId: String): List<TransactionItemEntity>

    @Query("UPDATE transaction_items SET deletedAt = :deletedAt, syncStatus = :syncStatus WHERE transactionId IN (SELECT id FROM transactions WHERE customerId = :customerId)")
    suspend fun softDeleteItemsForCustomer(customerId: String, deletedAt: Long, syncStatus: SyncStatus)

    @Query("DELETE FROM transaction_items WHERE transactionId IN (SELECT id FROM transactions WHERE customerId = :customerId)")
    suspend fun deleteItemsForCustomer(customerId: String)

    @Query("SELECT * FROM transaction_items WHERE syncStatus != 'SYNCED'")
    suspend fun getPendingSync(): List<TransactionItemEntity>

    @Query("SELECT * FROM transaction_items")
    suspend fun getAllForSync(): List<TransactionItemEntity>

    @Query("UPDATE transaction_items SET syncStatus = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: String, status: SyncStatus)

    @Query("DELETE FROM transaction_items WHERE id = :id")
    suspend fun deleteById(id: String)
}
