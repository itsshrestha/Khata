package com.khata.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * A shop customer. Customers with history are never hard-deleted locally: they are soft-deleted or archived.
 */
@Entity(
    tableName = "customers",
    indices = [
        Index(value = ["name"]),
        Index(value = ["isArchived"]),
        Index(value = ["syncStatus"]),
        Index(value = ["deletedAt"]),
    ],
)
data class CustomerEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val phone: String? = null,
    val address: String? = null,
    val notes: String? = null,
    @ColumnInfo(defaultValue = "0") val isArchived: Boolean = false,
    /** Epoch milliseconds (UTC), from `Instant.toEpochMilli()`. */
    val createdAt: Long,
    /** Epoch milliseconds (UTC). Hook for sync/conflict resolution. */
    val updatedAt: Long,
    val syncStatus: SyncStatus = SyncStatus.PENDING_CREATE,
    val deletedAt: Long? = null,
    val deviceName: String? = null,
)
