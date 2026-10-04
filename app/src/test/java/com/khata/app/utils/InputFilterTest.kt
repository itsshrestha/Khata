package com.khata.app.utils

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InputFilterTest {

    @Test
    fun `amount typing accepts in-progress values`() {
        for (text in listOf("", "1", "1250", "1.", "1.5", "1.50", ".5")) {
            assertTrue("'$text' should be accepted", MoneyParser.isValidAmountInput(text))
        }
    }

    @Test
    fun `amount typing rejects bad characters and extra decimals`() {
        for (text in listOf("1.234", "1..2", "abc", "-5", "1,250", "1 0", "1e5")) {
            assertFalse("'$text' should be rejected", MoneyParser.isValidAmountInput(text))
        }
    }

    @Test
    fun `quantity typing allows up to three decimals`() {
        assertTrue(MoneyParser.isValidQuantityInput("2.5"))
        assertTrue(MoneyParser.isValidQuantityInput("0.125"))
        assertFalse(MoneyParser.isValidQuantityInput("0.1255"))
        assertFalse(MoneyParser.isValidQuantityInput("-1"))
    }
}
