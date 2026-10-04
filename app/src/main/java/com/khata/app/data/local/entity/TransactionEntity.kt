package com.khata.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.khata.app.domain.model.PaymentMethod
import com.khata.app.domain.model.TransactionType
import java.util.UUID

/**
 * One ledger entry. Append-only ledger: credits and payments are separate rows.
 */
@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = CustomerEntity::class,
            parentColumns = ["id"],
            childColumns = ["customerId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index(value = ["customerId"]),
        Index(value = ["transactionDate"]),
        Index(value = ["type"]),
        Index(value = ["syncStatus"]),
        Index(value = ["deletedAt"]),
    ],
)
data class TransactionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val customerId: String,
    val type: TransactionType,
    val amount: Long,
    val description: String? = null,
    val paymentMethod: PaymentMethod? = null,
    /** Business date as `LocalDate.toEpochDay()`. */
    val transactionDate: Long,
    /** Epoch milliseconds (UTC). */
    val createdAt: Long,
    val updatedAt: Long,
    val syncStatus: SyncStatus = SyncStatus.PENDING_CREATE,
    val deletedAt: Long? = null,
    val deviceName: String? = null,
)
