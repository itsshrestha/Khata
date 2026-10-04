package com.khata.app.data.local.entity

/**
 * Synchronization state for local database records.
 */
enum class SyncStatus {
    SYNCED,
    PENDING_CREATE,
    PENDING_UPDATE,
    PENDING_DELETE,
}
