package com.khata.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/** A line item of a detailed CREDIT transaction. */
@Entity(
    tableName = "transaction_items",
    foreignKeys = [
        ForeignKey(
            entity = TransactionEntity::class,
            parentColumns = ["id"],
            childColumns = ["transactionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["transactionId"]),
        Index(value = ["syncStatus"]),
        Index(value = ["deletedAt"]),
    ],
)
data class TransactionItemEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val transactionId: String,
    val itemName: String,
    val quantity: Double,
    /** Minor units per single unit. */
    val unitPrice: Long,
    /** Minor units. */
    val totalPrice: Long,
    val syncStatus: SyncStatus = SyncStatus.PENDING_CREATE,
    val deletedAt: Long? = null,
)
