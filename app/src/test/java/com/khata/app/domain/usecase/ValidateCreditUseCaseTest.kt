package com.khata.app.domain.usecase

import com.khata.app.domain.model.KhataError
import com.khata.app.domain.model.Outcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidateCreditUseCaseTest {

    private val validate = ValidateCreditUseCase()

    @Test
    fun `quick credit with positive amount is accepted`() {
        assertEquals(Outcome.Success(200_000L), validate.quick(200_000L))
    }

    @Test
    fun `quick credit rejects zero negative and missing`() {
        assertEquals(Outcome.Failure(KhataError.InvalidAmount), validate.quick(0))
        assertEquals(Outcome.Failure(KhataError.InvalidAmount), validate.quick(-5))
        assertEquals(Outcome.Failure(KhataError.InvalidAmount), validate.quick(null))
    }

    @Test
    fun `quick credit rejects absurdly large amounts`() {
        assertEquals(
            Outcome.Failure(KhataError.AmountTooLarge),
            validate.quick(ValidateCreditUseCase.MAX_AMOUNT + 1),
        )
    }

    @Test
    fun `detailed credit matches the spec example total of 1850`() {
        val outcome = validate.detailed(
            listOf(
                CreditItemDraft("Rice", 10.0, 12_000),
                CreditItemDraft("Cooking Oil", 2.0, 25_000),
                CreditItemDraft("Biscuits", 5.0, 3_000),
            ),
        ) as Outcome.Success

        assertEquals(185_000L, outcome.value.total)
        assertEquals(listOf(120_000L, 50_000L, 15_000L), outcome.value.items.map { it.totalPrice })
    }

    @Test
    fun `detailed credit supports fractional quantities`() {
        val outcome = validate.detailed(listOf(CreditItemDraft("Sugar", 2.5, 9_000))) as Outcome.Success
        assertEquals(22_500L, outcome.value.total)
    }

    @Test
    fun `detailed credit trims item names`() {
        val outcome = validate.detailed(listOf(CreditItemDraft("  Rice  ", 1.0, 100))) as Outcome.Success
        assertEquals("Rice", outcome.value.items.single().itemName)
    }

    @Test
    fun `detailed credit rejects bad input`() {
        assertEquals(Outcome.Failure(KhataError.NoItems), validate.detailed(emptyList()))
        assertEquals(
            Outcome.Failure(KhataError.EmptyItemName),
            validate.detailed(listOf(CreditItemDraft("  ", 1.0, 100))),
        )
        assertEquals(
            Outcome.Failure(KhataError.InvalidItemQuantity),
            validate.detailed(listOf(CreditItemDraft("Rice", 0.0, 100))),
        )
        assertEquals(
            Outcome.Failure(KhataError.InvalidItemQuantity),
            validate.detailed(listOf(CreditItemDraft("Rice", null, 100))),
        )
        assertEquals(
            Outcome.Failure(KhataError.InvalidItemPrice),
            validate.detailed(listOf(CreditItemDraft("Rice", 1.0, 0))),
        )
        assertEquals(
            Outcome.Failure(KhataError.InvalidItemPrice),
            validate.detailed(listOf(CreditItemDraft("Rice", 1.0, null))),
        )
    }

    @Test
    fun `line total rounds half up to a whole paisa`() {
        assertEquals(15L, ValidateCreditUseCase.lineTotal(0.5, 30)) // 15.0
        assertEquals(2L, ValidateCreditUseCase.lineTotal(0.5, 3)) // 1.5 -> 2
        assertTrue(ValidateCreditUseCase.lineTotal(1.0, 1) == 1L)
    }
}
