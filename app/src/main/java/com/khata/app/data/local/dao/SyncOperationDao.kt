package com.khata.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.khata.app.data.local.entity.SyncOperationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncOperationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(operation: SyncOperationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(operations: List<SyncOperationEntity>)

    @Query("SELECT * FROM sync_operations ORDER BY createdAt ASC")
    suspend fun getAllPending(): List<SyncOperationEntity>

    @Query("SELECT COUNT(*) FROM sync_operations")
    fun observePendingCount(): Flow<Int>

    @Query("DELETE FROM sync_operations WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM sync_operations WHERE entityId = :entityId")
    suspend fun deleteByEntityId(entityId: String)

    @Update
    suspend fun update(operation: SyncOperationEntity)

    @Query("DELETE FROM sync_operations")
    suspend fun deleteAll()
}
