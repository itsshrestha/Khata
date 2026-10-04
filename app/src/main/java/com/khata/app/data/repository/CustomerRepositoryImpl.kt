package com.khata.app.data.repository

import androidx.room.withTransaction
import com.khata.app.data.local.dao.CustomerDao
import com.khata.app.data.local.database.KhataDatabase
import com.khata.app.data.local.entity.CustomerEntity
import com.khata.app.data.local.entity.SyncEntityType
import com.khata.app.data.local.entity.SyncOperationEntity
import com.khata.app.data.local.entity.SyncOperationType
import com.khata.app.data.local.entity.SyncStatus
import com.khata.app.domain.model.Customer
import com.khata.app.domain.model.CustomerWithBalance
import com.khata.app.domain.model.KhataError
import com.khata.app.domain.model.Outcome
import com.khata.app.domain.usecase.CustomerInput
import com.khata.app.domain.usecase.ValidateCustomerUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.util.UUID

class CustomerRepositoryImpl(
    private val customerDao: CustomerDao,
    private val database: KhataDatabase? = null,
    private val validateCustomer: ValidateCustomerUseCase = ValidateCustomerUseCase(),
    private val clock: Clock = Clock.systemDefaultZone(),
    private val onDataChanged: (() -> Unit)? = null,
) : CustomerRepository {

    override fun observeCustomers(searchQuery: String): Flow<List<CustomerWithBalance>> =
        customerDao.observeWithTotals(escapeLike(searchQuery.trim()), archived = false)
            .map { rows -> rows.map { it.toDomain() } }

    override fun observeArchivedCustomers(): Flow<List<CustomerWithBalance>> =
        customerDao.observeWithTotals("", archived = true)
            .map { rows -> rows.map { it.toDomain() } }

    override fun observeCustomer(id: String): Flow<CustomerWithBalance?> =
        customerDao.observeOneWithTotals(id).map { it?.toDomain() }

    override fun observeActiveCustomerCount(): Flow<Int> = customerDao.observeActiveCount()

    override fun observeCustomersWithOutstandingCount(): Flow<Int> =
        customerDao.observeWithOutstandingCount()

    override suspend fun addCustomer(
        name: String,
        phone: String?,
        address: String?,
        notes: String?,
    ): Outcome<String> = when (val validated = validateCustomer(name, phone, address, notes)) {
        is Outcome.Failure -> validated
        is Outcome.Success -> safeDatabaseCall {
            val now = clock.millis()
            val newId = UUID.randomUUID().toString()
            val entity = validated.value.toNewEntity(newId, now)

            if (database != null) {
                database.withTransaction {
                    customerDao.insert(entity)
                    database.syncOperationDao().insert(
                        SyncOperationEntity(
                            entityType = SyncEntityType.CUSTOMER,
                            entityId = newId,
                            operationType = SyncOperationType.CREATE,
                            createdAt = now,
                        ),
                    )
                }
            } else {
                customerDao.insert(entity)
            }
            onDataChanged?.invoke()
            Outcome.Success(newId)
        }
    }

    override suspend fun updateCustomer(
        id: String,
        name: String,
        phone: String?,
        address: String?,
        notes: String?,
    ): Outcome<Unit> = when (val validated = validateCustomer(name, phone, address, notes)) {
        is Outcome.Failure -> validated
        is Outcome.Success -> safeDatabaseCall {
            val existing = customerDao.getById(id)
                ?: return@safeDatabaseCall Outcome.Failure(KhataError.CustomerNotFound)
            val now = clock.millis()
            val updated = existing.copy(
                name = validated.value.name,
                phone = validated.value.phone,
                address = validated.value.address,
                notes = validated.value.notes,
                updatedAt = now,
                syncStatus = SyncStatus.PENDING_UPDATE,
            )

            if (database != null) {
                database.withTransaction {
                    customerDao.update(updated)
                    database.syncOperationDao().insert(
                        SyncOperationEntity(
                            entityType = SyncEntityType.CUSTOMER,
                            entityId = id,
                            operationType = SyncOperationType.UPDATE,
                            createdAt = now,
                        ),
                    )
                }
            } else {
                customerDao.update(updated)
            }
            onDataChanged?.invoke()
            Outcome.Success(Unit)
        }
    }

    override suspend fun archiveCustomer(id: String): Outcome<Unit> = setArchived(id, archived = true)

    override suspend fun unarchiveCustomer(id: String): Outcome<Unit> = setArchived(id, archived = false)

    override suspend fun deleteCustomer(id: String): Outcome<Unit> = safeDatabaseCall {
        if (customerDao.getById(id) == null) {
            return@safeDatabaseCall Outcome.Failure(KhataError.CustomerNotFound)
        }
        val now = clock.millis()
        if (database != null) {
            database.withTransaction {
                database.transactionItemDao().softDeleteItemsForCustomer(id, now, SyncStatus.PENDING_DELETE)
                database.transactionDao().softDeleteForCustomer(id, now, SyncStatus.PENDING_DELETE)
                customerDao.softDeleteById(id, now, SyncStatus.PENDING_DELETE)
                database.syncOperationDao().insert(
                    SyncOperationEntity(
                        entityType = SyncEntityType.CUSTOMER,
                        entityId = id,
                        operationType = SyncOperationType.DELETE,
                        createdAt = now,
                    ),
                )
            }
        } else {
            customerDao.deleteById(id)
        }
        onDataChanged?.invoke()
        Outcome.Success(Unit)
    }

    override suspend fun getCustomer(id: String): Customer? = customerDao.getById(id)?.toDomain()

    private suspend fun setArchived(id: String, archived: Boolean): Outcome<Unit> = safeDatabaseCall {
        if (customerDao.getById(id) == null) {
            return@safeDatabaseCall Outcome.Failure(KhataError.CustomerNotFound)
        }
        val now = clock.millis()
        if (database != null) {
            database.withTransaction {
                customerDao.setArchived(id, archived, now, SyncStatus.PENDING_UPDATE)
                database.syncOperationDao().insert(
                    SyncOperationEntity(
                        entityType = SyncEntityType.CUSTOMER,
                        entityId = id,
                        operationType = SyncOperationType.UPDATE,
                        createdAt = now,
                    ),
                )
            }
        } else {
            customerDao.setArchived(id, archived, now, SyncStatus.PENDING_UPDATE)
        }
        onDataChanged?.invoke()
        Outcome.Success(Unit)
    }

    private fun CustomerInput.toNewEntity(id: String, nowMillis: Long) = CustomerEntity(
        id = id,
        name = name,
        phone = phone,
        address = address,
        notes = notes,
        createdAt = nowMillis,
        updatedAt = nowMillis,
        syncStatus = SyncStatus.PENDING_CREATE,
        deviceName = com.khata.app.utils.DeviceUtils.getDeviceName(),
    )
}
