package com.khata.app.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.khata.app.data.local.dao.CustomerDao
import com.khata.app.data.local.dao.SyncOperationDao
import com.khata.app.data.local.dao.TransactionDao
import com.khata.app.data.local.dao.TransactionItemDao
import com.khata.app.data.local.entity.CustomerEntity
import com.khata.app.data.local.entity.SyncOperationEntity
import com.khata.app.data.local.entity.TransactionEntity
import com.khata.app.data.local.entity.TransactionItemEntity

/**
 * The single local source of truth. Version 2 converts IDs to UUID strings and adds sync queue.
 */
@Database(
    entities = [
        CustomerEntity::class,
        TransactionEntity::class,
        TransactionItemEntity::class,
        SyncOperationEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
abstract class KhataDatabase : RoomDatabase() {

    abstract fun customerDao(): CustomerDao
    abstract fun transactionDao(): TransactionDao
    abstract fun transactionItemDao(): TransactionItemDao
    abstract fun syncOperationDao(): SyncOperationDao

    companion object {
        const val NAME = "khata.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Create new customers table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS customers_new (
                        id TEXT NOT NULL PRIMARY KEY,
                        name TEXT NOT NULL,
                        phone TEXT,
                        address TEXT,
                        notes TEXT,
                        isArchived INTEGER NOT NULL DEFAULT 0,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        syncStatus TEXT NOT NULL DEFAULT 'SYNCED',
                        deletedAt INTEGER
                    )
                    """.trimIndent(),
                )

                db.execSQL(
                    """
                    INSERT INTO customers_new (id, name, phone, address, notes, isArchived, createdAt, updatedAt, syncStatus, deletedAt)
                    SELECT CAST(id AS TEXT), name, phone, address, notes, isArchived, createdAt, updatedAt, 'SYNCED', NULL
                    FROM customers
                    """.trimIndent(),
                )
                db.execSQL("DROP TABLE customers")
                db.execSQL("ALTER TABLE customers_new RENAME TO customers")

                db.execSQL("CREATE INDEX IF NOT EXISTS index_customers_name ON customers(name)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_customers_isArchived ON customers(isArchived)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_customers_syncStatus ON customers(syncStatus)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_customers_deletedAt ON customers(deletedAt)")

                // 2. Create new transactions table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS transactions_new (
                        id TEXT NOT NULL PRIMARY KEY,
                        customerId TEXT NOT NULL,
                        type TEXT NOT NULL,
                        amount INTEGER NOT NULL,
                        description TEXT,
                        paymentMethod TEXT,
                        transactionDate INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        syncStatus TEXT NOT NULL DEFAULT 'SYNCED',
                        deletedAt INTEGER,
                        FOREIGN KEY(customerId) REFERENCES customers(id) ON DELETE RESTRICT
                    )
                    """.trimIndent(),
                )

                db.execSQL(
                    """
                    INSERT INTO transactions_new (id, customerId, type, amount, description, paymentMethod, transactionDate, createdAt, updatedAt, syncStatus, deletedAt)
                    SELECT CAST(id AS TEXT), CAST(customerId AS TEXT), type, amount, description, paymentMethod, transactionDate, createdAt, updatedAt, 'SYNCED', NULL
                    FROM transactions
                    """.trimIndent(),
                )
                db.execSQL("DROP TABLE transactions")
                db.execSQL("ALTER TABLE transactions_new RENAME TO transactions")

                db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_customerId ON transactions(customerId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_transactionDate ON transactions(transactionDate)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_type ON transactions(type)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_syncStatus ON transactions(syncStatus)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_deletedAt ON transactions(deletedAt)")

                // 3. Create new transaction_items table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS transaction_items_new (
                        id TEXT NOT NULL PRIMARY KEY,
                        transactionId TEXT NOT NULL,
                        itemName TEXT NOT NULL,
                        quantity REAL NOT NULL,
                        unitPrice INTEGER NOT NULL,
                        totalPrice INTEGER NOT NULL,
                        syncStatus TEXT NOT NULL DEFAULT 'SYNCED',
                        deletedAt INTEGER,
                        FOREIGN KEY(transactionId) REFERENCES transactions(id) ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )

                db.execSQL(
                    """
                    INSERT INTO transaction_items_new (id, transactionId, itemName, quantity, unitPrice, totalPrice, syncStatus, deletedAt)
                    SELECT CAST(id AS TEXT), CAST(transactionId AS TEXT), itemName, quantity, unitPrice, totalPrice, 'SYNCED', NULL
                    FROM transaction_items
                    """.trimIndent(),
                )
                db.execSQL("DROP TABLE transaction_items")
                db.execSQL("ALTER TABLE transaction_items_new RENAME TO transaction_items")

                db.execSQL("CREATE INDEX IF NOT EXISTS index_transaction_items_transactionId ON transaction_items(transactionId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_transaction_items_syncStatus ON transaction_items(syncStatus)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_transaction_items_deletedAt ON transaction_items(deletedAt)")

                // 4. Create sync_operations table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS sync_operations (
                        id TEXT NOT NULL PRIMARY KEY,
                        entityType TEXT NOT NULL,
                        entityId TEXT NOT NULL,
                        operationType TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        retryCount INTEGER NOT NULL DEFAULT 0,
                        lastError TEXT
                    )
                    """.trimIndent(),
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE customers ADD COLUMN deviceName TEXT")
                db.execSQL("ALTER TABLE transactions ADD COLUMN deviceName TEXT")
            }
        }

        fun create(context: Context): KhataDatabase =
            Room.databaseBuilder(context.applicationContext, KhataDatabase::class.java, NAME)
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()
    }
}
