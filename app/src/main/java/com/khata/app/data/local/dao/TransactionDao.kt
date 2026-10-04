package com.khata.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.khata.app.data.local.entity.BalanceTotalsRow
import com.khata.app.data.local.entity.SyncStatus
import com.khata.app.data.local.entity.TransactionEntity
import com.khata.app.data.local.entity.TransactionWithCustomerRow
import com.khata.app.domain.model.PaymentMethod
import com.khata.app.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: TransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<TransactionEntity>)

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Query("UPDATE transactions SET deletedAt = :deletedAt, updatedAt = :deletedAt, syncStatus = :syncStatus WHERE id = :id")
    suspend fun softDeleteById(id: String, deletedAt: Long, syncStatus: SyncStatus)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("UPDATE transactions SET deletedAt = :deletedAt, updatedAt = :deletedAt, syncStatus = :syncStatus WHERE customerId = :customerId")
    suspend fun softDeleteForCustomer(customerId: String, deletedAt: Long, syncStatus: SyncStatus)

    @Query("DELETE FROM transactions WHERE customerId = :customerId")
    suspend fun deleteForCustomer(customerId: String)

    @Query("SELECT * FROM transactions WHERE id = :id AND deletedAt IS NULL")
    suspend fun getById(id: String): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getByIdIncludingDeleted(id: String): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE id = :id AND deletedAt IS NULL")
    fun observeById(id: String): Flow<TransactionEntity?>

    @Query("SELECT * FROM transactions WHERE syncStatus != 'SYNCED'")
    suspend fun getPendingSync(): List<TransactionEntity>

    @Query("SELECT * FROM transactions")
    suspend fun getAllForSync(): List<TransactionEntity>

    @Query("UPDATE transactions SET syncStatus = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: String, status: SyncStatus)

    /** A customer's full history, oldest first (statement order). */
    @Query(
        """
        SELECT * FROM transactions
        WHERE customerId = :customerId
          AND deletedAt IS NULL
        ORDER BY transactionDate ASC, createdAt ASC, id ASC
        """,
    )
    fun observeForCustomer(customerId: String): Flow<List<TransactionEntity>>

    /**
     * Global history, newest first, with optional filters.
     */
    @Query(
        """
        SELECT t.*, c.name AS customerName
        FROM transactions t
        JOIN customers c ON c.id = t.customerId
        WHERE t.deletedAt IS NULL
          AND c.deletedAt IS NULL
          AND (:type IS NULL OR t.type = :type)
          AND (:customerId IS NULL OR t.customerId = :customerId)
          AND (:method IS NULL OR t.paymentMethod = :method)
          AND (:fromDay IS NULL OR t.transactionDate >= :fromDay)
          AND (:toDay IS NULL OR t.transactionDate <= :toDay)
        ORDER BY t.transactionDate DESC, t.createdAt DESC, t.id DESC
        """,
    )
    fun observeFiltered(
        type: TransactionType?,
        customerId: String?,
        method: PaymentMethod?,
        fromDay: Long?,
        toDay: Long?,
    ): Flow<List<TransactionWithCustomerRow>>

    @Query(
        """
        SELECT t.*, c.name AS customerName
        FROM transactions t
        JOIN customers c ON c.id = t.customerId
        WHERE t.deletedAt IS NULL
          AND c.deletedAt IS NULL
        ORDER BY t.transactionDate DESC, t.createdAt DESC, t.id DESC
        LIMIT :limit
        """,
    )
    fun observeRecent(limit: Int): Flow<List<TransactionWithCustomerRow>>

    // ---- Balances: computed from non-deleted transactions ----

    @Query(
        """
        SELECT COALESCE(SUM(CASE WHEN type = 'CREDIT' THEN amount END), 0) AS totalCredit,
               COALESCE(SUM(CASE WHEN type = 'PAYMENT' THEN amount END), 0) AS totalPaid
        FROM transactions
        WHERE customerId = :customerId
          AND deletedAt IS NULL
        """,
    )
    suspend fun getTotalsForCustomer(customerId: String): BalanceTotalsRow

    @Query(
        """
        SELECT COALESCE(SUM(CASE WHEN type = 'CREDIT' THEN amount END), 0) AS totalCredit,
               COALESCE(SUM(CASE WHEN type = 'PAYMENT' THEN amount END), 0) AS totalPaid
        FROM transactions
        WHERE customerId = :customerId
          AND deletedAt IS NULL
        """,
    )
    fun observeTotalsForCustomer(customerId: String): Flow<BalanceTotalsRow>

    /** Totals across every customer: shop-wide outstanding credit. */
    @Query(
        """
        SELECT COALESCE(SUM(CASE WHEN type = 'CREDIT' THEN amount END), 0) AS totalCredit,
               COALESCE(SUM(CASE WHEN type = 'PAYMENT' THEN amount END), 0) AS totalPaid
        FROM transactions
        WHERE deletedAt IS NULL
        """,
    )
    fun observeOverallTotals(): Flow<BalanceTotalsRow>

    /** Sum of one transaction type within range. */
    @Query(
        """
        SELECT COALESCE(SUM(amount), 0) FROM transactions
        WHERE type = :type
          AND deletedAt IS NULL
          AND transactionDate BETWEEN :fromDay AND :toDay
        """,
    )
    fun observeTotalForType(type: TransactionType, fromDay: Long, toDay: Long): Flow<Long>
}
