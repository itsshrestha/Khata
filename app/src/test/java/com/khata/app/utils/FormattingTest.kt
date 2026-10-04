package com.khata.app.utils

import com.khata.app.domain.model.KhataError
import com.khata.app.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class FormattingTest {

    private val currency = CurrencyFormatter()

    @Test
    fun `currency uses Indian grouping`() {
        assertEquals("Rs. 0", currency.format(0))
        assertEquals("Rs. 500", currency.format(50_000))
        assertEquals("Rs. 1,250", currency.format(125_000))
        assertEquals("Rs. 25,000", currency.format(2_500_000))
        assertEquals("Rs. 1,50,000", currency.format(15_000_000))
        assertEquals("Rs. 84,500", currency.format(8_450_000))
        assertEquals("Rs. 12,34,56,789", currency.format(1_234_567_890_0))
    }

    @Test
    fun `currency shows paisa only when present`() {
        assertEquals("Rs. 1,250.50", currency.format(125_050))
        assertEquals("Rs. 0.05", currency.format(5))
        assertEquals("Rs. 10.10", currency.format(1_010))
    }

    @Test
    fun `negative amounts keep the sign in front`() {
        assertEquals("-Rs. 1,500", currency.format(-150_000))
    }

    @Test
    fun `signed format shows plus for credit and minus for payment`() {
        assertEquals("+ Rs. 1,200", currency.formatSigned(120_000, TransactionType.CREDIT))
        assertEquals("- Rs. 500", currency.formatSigned(50_000, TransactionType.PAYMENT))
    }

    @Test
    fun `currency symbol is configurable`() {
        assertEquals("NPR 1,250", CurrencyFormatter(symbol = "NPR").format(125_000))
    }

    @Test
    fun `money parser handles typical user input`() {
        assertEquals(125_000L, MoneyParser.parseToMinorUnits("1250"))
        assertEquals(125_000L, MoneyParser.parseToMinorUnits("1,250"))
        assertEquals(125_050L, MoneyParser.parseToMinorUnits("1250.50"))
        assertEquals(125_050L, MoneyParser.parseToMinorUnits(" Rs. 1,250.5 "))
        assertEquals(0L, MoneyParser.parseToMinorUnits("0"))
    }

    @Test
    fun `money parser rejects malformed input`() {
        for (text in listOf("", "  ", "abc", "1.234", "1..2", "12a", ".5", "1e5")) {
            assertNull("input '$text'", MoneyParser.parseToMinorUnits(text))
        }
    }

    @Test
    fun `money parser reports negatives so validation can reject them`() {
        assertEquals(-5_000L, MoneyParser.parseToMinorUnits("-50"))
    }

    @Test
    fun `money input string round trips`() {
        assertEquals("1250", MoneyParser.toInputString(125_000))
        assertEquals("1250.5", MoneyParser.toInputString(125_050))
        assertEquals("0.05", MoneyParser.toInputString(5))
    }

    @Test
    fun `friendly dates`() {
        val today = LocalDate.of(2026, 10, 3)
        assertEquals("Today", DateFormatter.friendly(today, today))
        assertEquals("Yesterday", DateFormatter.friendly(today.minusDays(1), today))
        assertEquals("Oct 01, 2026", DateFormatter.friendly(today.minusDays(2), today))
        assertEquals("Oct 03", DateFormatter.short(today))
        assertEquals("2026-10-03", DateFormatter.forInput(today))
    }

    @Test
    fun `greeting by hour`() {
        assertEquals("Good Morning", DateFormatter.greeting(9))
        assertEquals("Good Afternoon", DateFormatter.greeting(14))
        assertEquals("Good Evening", DateFormatter.greeting(19))
        assertEquals("Hello", DateFormatter.greeting(2))
    }

    @Test
    fun `overpayment message matches the spec wording`() {
        assertEquals(
            "Payment cannot exceed the outstanding balance of Rs. 500.",
            KhataError.PaymentExceedsOutstanding(50_000).toUserMessage(currency),
        )
    }
}
