package com.khata.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

enum class SyncEntityType {
    CUSTOMER,
    TRANSACTION,
    TRANSACTION_ITEM,
}

enum class SyncOperationType {
    CREATE,
    UPDATE,
    DELETE,
}

@Entity(tableName = "sync_operations")
data class SyncOperationEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val entityType: SyncEntityType,
    val entityId: String,
    val operationType: SyncOperationType,
    val createdAt: Long,
    val retryCount: Int = 0,
    val lastError: String? = null,
)
