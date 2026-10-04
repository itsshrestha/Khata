package com.khata.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.khata.app.data.local.entity.CustomerEntity
import com.khata.app.data.local.entity.CustomerWithTotalsRow
import com.khata.app.data.local.entity.SyncStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(customer: CustomerEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(customers: List<CustomerEntity>)

    @Update
    suspend fun update(customer: CustomerEntity)

    @Query("SELECT * FROM customers WHERE id = :id AND deletedAt IS NULL")
    suspend fun getById(id: String): CustomerEntity?

    @Query("SELECT * FROM customers WHERE id = :id")
    suspend fun getByIdIncludingDeleted(id: String): CustomerEntity?

    @Query("UPDATE customers SET isArchived = :archived, updatedAt = :updatedAt, syncStatus = :syncStatus WHERE id = :id")
    suspend fun setArchived(id: String, archived: Boolean, updatedAt: Long, syncStatus: SyncStatus)

    @Query("UPDATE customers SET deletedAt = :deletedAt, updatedAt = :deletedAt, syncStatus = :syncStatus WHERE id = :id")
    suspend fun softDeleteById(id: String, deletedAt: Long, syncStatus: SyncStatus)

    @Query("DELETE FROM customers WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM customers WHERE syncStatus != 'SYNCED'")
    suspend fun getPendingSync(): List<CustomerEntity>

    @Query("SELECT * FROM customers")
    suspend fun getAllForSync(): List<CustomerEntity>

    @Query("UPDATE customers SET syncStatus = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: String, status: SyncStatus)

    @Query(
        """
        SELECT c.*,
               COALESCE(SUM(CASE WHEN t.type = 'CREDIT' AND t.deletedAt IS NULL THEN t.amount END), 0) AS totalCredit,
               COALESCE(SUM(CASE WHEN t.type = 'PAYMENT' AND t.deletedAt IS NULL THEN t.amount END), 0) AS totalPaid
        FROM customers c
        LEFT JOIN transactions t ON t.customerId = c.id
        WHERE c.isArchived = :archived
          AND c.deletedAt IS NULL
          AND (c.name LIKE '%' || :query || '%' ESCAPE '\' OR IFNULL(c.phone, '') LIKE '%' || :query || '%' ESCAPE '\')
        GROUP BY c.id
        ORDER BY c.name COLLATE NOCASE ASC
        """,
    )
    fun observeWithTotals(query: String, archived: Boolean): Flow<List<CustomerWithTotalsRow>>

    @Query(
        """
        SELECT c.*,
               COALESCE(SUM(CASE WHEN t.type = 'CREDIT' AND t.deletedAt IS NULL THEN t.amount END), 0) AS totalCredit,
               COALESCE(SUM(CASE WHEN t.type = 'PAYMENT' AND t.deletedAt IS NULL THEN t.amount END), 0) AS totalPaid
        FROM customers c
        LEFT JOIN transactions t ON t.customerId = c.id
        WHERE c.id = :id
          AND c.deletedAt IS NULL
        GROUP BY c.id
        """,
    )
    fun observeOneWithTotals(id: String): Flow<CustomerWithTotalsRow?>

    @Query("SELECT COUNT(*) FROM customers WHERE isArchived = 0 AND deletedAt IS NULL")
    fun observeActiveCount(): Flow<Int>

    @Query(
        """
        SELECT COUNT(*) FROM (
            SELECT c.id
            FROM customers c
            JOIN transactions t ON t.customerId = c.id
            WHERE c.isArchived = 0
              AND c.deletedAt IS NULL
              AND t.deletedAt IS NULL
            GROUP BY c.id
            HAVING SUM(CASE WHEN t.type = 'CREDIT' THEN t.amount ELSE -t.amount END) > 0
        )
        """,
    )
    fun observeWithOutstandingCount(): Flow<Int>
}
