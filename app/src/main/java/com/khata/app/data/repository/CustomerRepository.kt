package com.khata.app.data.repository

import com.khata.app.domain.model.Customer
import com.khata.app.domain.model.CustomerWithBalance
import com.khata.app.domain.model.Outcome
import kotlinx.coroutines.flow.Flow

interface CustomerRepository {

    /** Active (non-archived) customers with derived balances, filtered by name or phone. */
    fun observeCustomers(searchQuery: String = ""): Flow<List<CustomerWithBalance>>

    /** Archived customers, kept so their history stays reachable. */
    fun observeArchivedCustomers(): Flow<List<CustomerWithBalance>>

    fun observeCustomer(id: String): Flow<CustomerWithBalance?>

    fun observeActiveCustomerCount(): Flow<Int>

    fun observeCustomersWithOutstandingCount(): Flow<Int>

    /** Validates raw form values, then saves. Returns new customer id. */
    suspend fun addCustomer(
        name: String,
        phone: String?,
        address: String?,
        notes: String?,
    ): Outcome<String>

    suspend fun updateCustomer(
        id: String,
        name: String,
        phone: String?,
        address: String?,
        notes: String?,
    ): Outcome<Unit>

    /** Soft delete / archive: hides the customer. */
    suspend fun archiveCustomer(id: String): Outcome<Unit>

    suspend fun unarchiveCustomer(id: String): Outcome<Unit>

    /** Soft-deletes customer and associated records for cloud sync propagation. */
    suspend fun deleteCustomer(id: String): Outcome<Unit>

    suspend fun getCustomer(id: String): Customer?
}
