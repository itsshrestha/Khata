package com.khata.app.domain.usecase

import com.khata.app.TestData.credit
import com.khata.app.TestData.payment
import com.khata.app.TestData.rupees
import com.khata.app.domain.model.KhataError
import com.khata.app.domain.model.Outcome
import org.junit.Assert.assertEquals
import org.junit.Test

class ValidateTransactionChangeUseCaseTest {

    private val rules = ValidateTransactionChangeUseCase()
    private val calculate = CalculateBalanceUseCase()

    // ---- Delete ----

    @Test
    fun `deleting a payment increases the balance and is always allowed`() {
        // Credit 5000, paid 1000 -> owes 4000. Deleting the 1000 payment -> owes 5000.
        val outcome = rules.validateDelete(payment(1000), currentOutstanding = rupees(4000))
        assertEquals(Outcome.Success(rupees(5000)), outcome)
    }

    @Test
    fun `spec dialog deleting a 1000 payment raises balance by exactly 1000`() {
        val history = listOf(credit(3000), payment(1000))
        val outstanding = calculate(history).outstanding
        val after = rules.outstandingAfterDelete(history[1], outstanding)
        assertEquals(rupees(1000), after - outstanding)
    }

    @Test
    fun `deleting a credit lowers the balance`() {
        // Credits 5000 + 2000, paid 1000 -> owes 6000. Delete the 2000 credit -> owes 4000.
        val outcome = rules.validateDelete(credit(2000), currentOutstanding = rupees(6000))
        assertEquals(Outcome.Success(rupees(4000)), outcome)
    }

    @Test
    fun `deleting a credit down to exactly zero is allowed`() {
        val outcome = rules.validateDelete(credit(1000), currentOutstanding = rupees(1000))
        assertEquals(Outcome.Success(0L), outcome)
    }

    @Test
    fun `deleting a credit that payments already cover is rejected`() {
        // Credit 1000, paid 800 -> owes 200. Deleting the 1000 credit would leave -800.
        val outcome = rules.validateDelete(credit(1000), currentOutstanding = rupees(200))
        assertEquals(Outcome.Failure(KhataError.WouldCreateNegativeBalance), outcome)
    }

    @Test
    fun `deleting one of two credits is only allowed while payments stay covered`() {
        val history = listOf(credit(1000), credit(1000), payment(1500))
        val outstanding = calculate(history).outstanding // 500
        // Removing a 1000 credit leaves credit 1000 vs paid 1500 -> negative -> rejected.
        assertEquals(
            Outcome.Failure(KhataError.WouldCreateNegativeBalance),
            rules.validateDelete(history[0], outstanding),
        )
    }

    // ---- Edit amount ----

    @Test
    fun `editing a payment up to what was owed without it is allowed`() {
        // Credit 1000, payment 400 -> owes 600. The payment may grow to at most 1000.
        val original = payment(400)
        assertEquals(Outcome.Success(rupees(1000)), rules.validateAmountEdit(original, rupees(1000), rupees(600)))
        assertEquals(Outcome.Success(rupees(300)), rules.validateAmountEdit(original, rupees(300), rupees(600)))
    }

    @Test
    fun `editing a payment above what was owed is rejected with the real maximum`() {
        val outcome = rules.validateAmountEdit(payment(400), rupees(1000) + 1, rupees(600))
        assertEquals(
            Outcome.Failure(KhataError.PaymentExceedsOutstanding(outstanding = rupees(1000))),
            outcome,
        )
    }

    @Test
    fun `increasing a credit is always allowed`() {
        assertEquals(
            Outcome.Success(rupees(5000)),
            rules.validateAmountEdit(credit(1000), rupees(5000), currentOutstanding = rupees(200)),
        )
    }

    @Test
    fun `reducing a credit below what was paid is rejected`() {
        // Credit 1000, paid 800 (owes 200). Reducing credit to 700 would leave -100.
        val outcome = rules.validateAmountEdit(credit(1000), rupees(700), currentOutstanding = rupees(200))
        assertEquals(Outcome.Failure(KhataError.WouldCreateNegativeBalance), outcome)
    }

    @Test
    fun `reducing a credit exactly to what was paid is allowed`() {
        val outcome = rules.validateAmountEdit(credit(1000), rupees(800), currentOutstanding = rupees(200))
        assertEquals(Outcome.Success(rupees(800)), outcome)
    }

    @Test
    fun `edited amounts must be positive and sane`() {
        assertEquals(Outcome.Failure(KhataError.InvalidAmount), rules.validateAmountEdit(credit(100), 0, rupees(100)))
        assertEquals(Outcome.Failure(KhataError.InvalidAmount), rules.validateAmountEdit(credit(100), -1, rupees(100)))
        assertEquals(Outcome.Failure(KhataError.InvalidAmount), rules.validateAmountEdit(credit(100), null, rupees(100)))
        assertEquals(
            Outcome.Failure(KhataError.AmountTooLarge),
            rules.validateAmountEdit(credit(100), ValidateCreditUseCase.MAX_AMOUNT + 1, rupees(100)),
        )
    }

    @Test
    fun `balance after edit matches recalculating from the edited history`() {
        val history = listOf(credit(5000), payment(1000), payment(1500))
        val outstanding = calculate(history).outstanding
        val edited = listOf(history[0], payment(1200), history[2])
        assertEquals(
            calculate(edited).outstanding,
            rules.outstandingAfterEdit(history[1], rupees(1200), outstanding),
        )
    }

    @Test
    fun `balance after delete matches recalculating without that entry`() {
        val history = listOf(credit(5000), payment(1000), payment(1500))
        val outstanding = calculate(history).outstanding
        assertEquals(
            calculate(listOf(history[0], history[2])).outstanding,
            rules.outstandingAfterDelete(history[1], outstanding),
        )
    }
}
