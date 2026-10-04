package com.khata.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class BalanceStatusTest {

    @Test
    fun `owing customer is OWES`() {
        assertEquals(BalanceStatus.OWES, Balance(totalCredit = 100_000, totalPaid = 40_000).status)
    }

    @Test
    fun `fully paid customer is SETTLED and keeps history`() {
        assertEquals(BalanceStatus.SETTLED, Balance(totalCredit = 100_000, totalPaid = 100_000).status)
    }

    @Test
    fun `customer without any credit is NO_CREDIT`() {
        assertEquals(BalanceStatus.NO_CREDIT, Balance.ZERO.status)
    }

    @Test
    fun `one paisa owed is still OWES`() {
        assertEquals(BalanceStatus.OWES, Balance(totalCredit = 101, totalPaid = 100).status)
    }
}
