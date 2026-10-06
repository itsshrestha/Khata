package com.khata.app

import android.content.Context
import com.khata.app.data.local.database.KhataDatabase
import com.khata.app.data.repository.CustomerRepository
import com.khata.app.data.repository.CustomerRepositoryImpl
import com.khata.app.data.repository.TransactionRepository
import com.khata.app.data.repository.TransactionRepositoryImpl
import com.khata.app.data.sync.AuthManager
import com.khata.app.data.sync.SyncManager
import com.khata.app.data.sync.SyncWorker
import com.khata.app.utils.CurrencyFormatter

/**
 * Manual dependency container. ViewModels receive repositories and sync components from here.
 */
class AppContainer(context: Context) {

    val database: KhataDatabase = KhataDatabase.create(context)

    val authManager: AuthManager by lazy {
        AuthManager()
    }

    val syncManager: SyncManager by lazy {
        SyncManager(
            database = database,
            authManager = authManager,
        )
    }

    val customerRepository: CustomerRepository by lazy {
        CustomerRepositoryImpl(
            customerDao = database.customerDao(),
            database = database,
            onDataChanged = { syncManager.triggerSync() },
        )
    }

    val transactionRepository: TransactionRepository by lazy {
        TransactionRepositoryImpl(
            database = database,
            onDataChanged = { syncManager.triggerSync() },
        )
    }

    val currencyFormatter: CurrencyFormatter = CurrencyFormatter()

    init {
        SyncWorker.schedulePeriodicSync(context)
        syncManager.triggerSync()
        syncManager.startForegroundAutoSync(30_000L)
    }
}
