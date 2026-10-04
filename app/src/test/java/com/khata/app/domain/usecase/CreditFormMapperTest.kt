package com.khata.app.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CreditFormMapperTest {

    private fun drafts(vararg rows: Triple<String, String, String>) =
        rows.map { (name, quantity, price) -> CreditFormMapper.toDraft(name, quantity, price) }

    @Test
    fun `spec example totals Rs 1850`() {
        val list = drafts(
            Triple("Rice", "10", "120"),
            Triple("Cooking Oil", "2", "250"),
            Triple("Biscuits", "5", "30"),
        )
        assertEquals(185_000L, CreditFormMapper.liveTotal(list))
    }

    @Test
    fun `text is parsed into minor units and quantity`() {
        val draft = CreditFormMapper.toDraft("Sugar", "2.5", "90.50")
        assertEquals(2.5, draft.quantity!!, 0.0)
        assertEquals(9_050L, draft.unitPrice)
    }

    @Test
    fun `blank or malformed numbers become null so validation can reject them`() {
        val draft = CreditFormMapper.toDraft("Rice", "", "abc")
        assertNull(draft.quantity)
        assertNull(draft.unitPrice)
    }

    @Test
    fun `incomplete lines count as zero in the live total`() {
        val list = drafts(
            Triple("Rice", "10", "120"),
            Triple("", "", ""),
            Triple("Oil", "2", ""),
        )
        assertEquals(120_000L, CreditFormMapper.liveTotal(list))
    }

    @Test
    fun `zero or negative lines never reduce the total`() {
        val list = drafts(Triple("Rice", "10", "120"), Triple("Bad", "-2", "100"), Triple("Free", "1", "0"))
        assertEquals(120_000L, CreditFormMapper.liveTotal(list))
    }

    @Test
    fun `fractional line totals round to a whole paisa`() {
        assertEquals(1_000L, CreditFormMapper.liveTotal(drafts(Triple("Tea", "0.333", "30.03"))))
    }

    @Test
    fun `item summary joins names and skips blanks`() {
        val list = drafts(Triple(" Rice ", "1", "1"), Triple("", "1", "1"), Triple("Oil", "1", "1"))
        assertEquals("Rice, Oil", CreditFormMapper.summarizeItems(list))
    }

    @Test
    fun `long item summary is truncated`() {
        val list = drafts(Triple("a".repeat(200), "1", "1"))
        val summary = CreditFormMapper.summarizeItems(list, maxLength = 20)
        assertEquals(20, summary.length)
        assertEquals('…', summary.last())
    }
}
