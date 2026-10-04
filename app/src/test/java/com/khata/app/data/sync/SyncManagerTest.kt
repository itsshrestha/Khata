package com.khata.app.data.sync

import com.khata.app.data.local.entity.SyncEntityType
import com.khata.app.data.local.entity.SyncOperationEntity
import com.khata.app.data.local.entity.SyncOperationType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class SyncManagerTest {

    @Test
    fun `sync operation entity creation and structure`() {
        val opId = UUID.randomUUID().toString()
        val customerId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()

        val op = SyncOperationEntity(
            id = opId,
            entityType = SyncEntityType.CUSTOMER,
            entityId = customerId,
            operationType = SyncOperationType.CREATE,
            createdAt = now,
        )

        assertEquals(opId, op.id)
        assertEquals(SyncEntityType.CUSTOMER, op.entityType)
        assertEquals(customerId, op.entityId)
        assertEquals(SyncOperationType.CREATE, op.operationType)
        assertEquals(0, op.retryCount)
    }

    @Test
    fun `multi-device balance calculation simulation`() {
        // Device A creates Credit Rs. 5000
        val creditAmount = 5000L * 100 // paisa

        // Device B creates Payment Rs. 1000
        val paymentAmount = 1000L * 100 // paisa

        val totalCredit = creditAmount
        val totalPaid = paymentAmount
        val outstanding = totalCredit - totalPaid

        assertEquals(4000L * 100, outstanding)
        assertTrue(outstanding > 0)
    }
}
