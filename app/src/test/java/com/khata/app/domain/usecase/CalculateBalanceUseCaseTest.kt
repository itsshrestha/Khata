package com.khata.app.domain.usecase

import com.khata.app.TestData.credit
import com.khata.app.TestData.payment
import com.khata.app.TestData.rupees
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CalculateBalanceUseCaseTest {

    private val calculate = CalculateBalanceUseCase()

    @Test
    fun `no transactions gives zero balance`() {
        val balance = calculate(emptyList())
        assertEquals(0L, balance.outstanding)
        assertTrue(balance.isSettled)
    }

    @Test
    fun `credit 1000 gives balance 1000`() {
        val balance = calculate(listOf(credit(1000)))
        assertEquals(rupees(1000), balance.outstanding)
        assertFalse(balance.isSettled)
    }

    @Test
    fun `credit 1000 payment 500 gives balance 500`() {
        val balance = calculate(listOf(credit(1000), payment(500)))
        assertEquals(rupees(500), balance.outstanding)
    }

    @Test
    fun `credit 1000 payment 1000 gives balance 0 and is settled`() {
        val balance = calculate(listOf(credit(1000), payment(1000)))
        assertEquals(0L, balance.outstanding)
        assertTrue(balance.isSettled)
    }

    @Test
    fun `credit 5000 payments 1000 and 1500 gives balance 2500`() {
        val balance = calculate(listOf(credit(5000), payment(1000), payment(1500)))
        assertEquals(rupees(2500), balance.outstanding)
    }

    @Test
    fun `spec example 1 credit 2000 pay 500 balance 1500`() {
        assertEquals(rupees(1500), calculate(listOf(credit(2000), payment(500))).outstanding)
    }

    @Test
    fun `spec example 2 credit 10000 payments 2000 3000 1000 balance 4000`() {
        val balance = calculate(listOf(credit(10000), payment(2000), payment(3000), payment(1000)))
        assertEquals(rupees(4000), balance.outstanding)
    }

    @Test
    fun `spec example 3 multiple credits and payments balance 4500`() {
        val balance = calculate(
            listOf(credit(5000), credit(2000), credit(3000), payment(4000), payment(1500)),
        )
        assertEquals(rupees(10000), balance.totalCredit)
        assertEquals(rupees(5500), balance.totalPaid)
        assertEquals(rupees(4500), balance.outstanding)
    }

    @Test
    fun `spec example 4 credit 1000 payment 1000 balance 0`() {
        val balance = calculate(listOf(credit(1000), payment(1000)))
        assertEquals(0L, balance.outstanding)
    }

    @Test
    fun `original credit amount is never changed by payments`() {
        val original = credit(2000)
        calculate(listOf(original, payment(500), payment(700)))
        assertEquals(rupees(2000), original.amount)
    }

    @Test
    fun `transaction order does not change the balance`() {
        val ordered = listOf(credit(2000), payment(500), credit(800), payment(300))
        assertEquals(calculate(ordered), calculate(ordered.reversed()))
    }

    @Test
    fun `paisa amounts are exact with no floating point drift`() {
        val credit = credit(0).copy(amount = 1010) // Rs. 10.10
        val payment = payment(0).copy(amount = 910) // Rs. 9.10
        assertEquals(100L, calculate(listOf(credit, payment)).outstanding)
    }

    @Test
    fun `spec workflow 2000 then 500 then 700 then 800 ends at zero`() {
        val history = mutableListOf(credit(2000))
        assertEquals(rupees(2000), calculate(history).outstanding)
        history += payment(500)
        assertEquals(rupees(1500), calculate(history).outstanding)
        history += payment(700)
        assertEquals(rupees(800), calculate(history).outstanding)
        history += payment(800)
        assertEquals(0L, calculate(history).outstanding)
        assertEquals(4, history.size) // full history is kept
    }

    @Test
    fun `running balances follow the statement example`() {
        val history = listOf(credit(1500), payment(1000), credit(2200), payment(500))
        assertEquals(
            listOf(rupees(1500), rupees(500), rupees(2700), rupees(2200)),
            calculate.runningBalances(history),
        )
    }
}
