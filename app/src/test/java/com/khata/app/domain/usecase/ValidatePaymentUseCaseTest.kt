package com.khata.app.domain.usecase

import com.khata.app.TestData.credit
import com.khata.app.TestData.payment
import com.khata.app.TestData.rupees
import com.khata.app.domain.model.KhataError
import com.khata.app.domain.model.Outcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidatePaymentUseCaseTest {

    private val validate = ValidatePaymentUseCase()
    private val calculate = CalculateBalanceUseCase()

    private fun failure(outcome: Outcome<Long>): KhataError =
        (outcome as Outcome.Failure).error

    @Test
    fun `partial payment is accepted unchanged`() {
        val outcome = validate(rupees(1000), outstanding = rupees(5000))
        assertEquals(Outcome.Success(rupees(1000)), outcome)
    }

    @Test
    fun `payment equal to outstanding is accepted`() {
        val outcome = validate(rupees(500), outstanding = rupees(500))
        assertEquals(Outcome.Success(rupees(500)), outcome)
    }

    @Test
    fun `credit 1000 payment 1500 is rejected with the outstanding balance`() {
        val outstanding = calculate(listOf(credit(1000))).outstanding
        val error = failure(validate(rupees(1500), outstanding))
        assertEquals(KhataError.PaymentExceedsOutstanding(rupees(1000)), error)
    }

    @Test
    fun `one paisa over the outstanding balance is rejected`() {
        val error = failure(validate(rupees(500) + 1, outstanding = rupees(500)))
        assertTrue(error is KhataError.PaymentExceedsOutstanding)
    }

    @Test
    fun `zero payment is rejected`() {
        assertEquals(KhataError.InvalidAmount, failure(validate(0, rupees(500))))
    }

    @Test
    fun `negative payment is rejected`() {
        assertEquals(KhataError.InvalidAmount, failure(validate(-100, rupees(500))))
    }

    @Test
    fun `missing amount is rejected`() {
        assertEquals(KhataError.InvalidAmount, failure(validate(null, rupees(500))))
    }

    @Test
    fun `customer with zero balance cannot pay`() {
        val outstanding = calculate(listOf(credit(1000), payment(1000))).outstanding
        assertEquals(KhataError.NoOutstandingBalance, failure(validate(rupees(1), outstanding)))
    }

    @Test
    fun `customer with no transactions cannot pay`() {
        assertEquals(KhataError.NoOutstandingBalance, failure(validate(rupees(1), 0)))
    }

    @Test
    fun `full spec workflow 5000 then 1000 2500 1500 reaches zero and rejects extra`() {
        val history = mutableListOf(credit(5000))
        for (amount in listOf(1000L, 2500L, 1500L)) {
            val outstanding = calculate(history).outstanding
            val outcome = validate(rupees(amount), outstanding)
            assertTrue("payment of $amount should be accepted", outcome is Outcome.Success)
            history += payment(amount)
        }
        assertEquals(0L, calculate(history).outstanding)
        assertEquals(
            KhataError.NoOutstandingBalance,
            failure(validate(rupees(1), calculate(history).outstanding)),
        )
    }
}
