package com.khata.app.utils

import java.math.BigDecimal
import java.math.RoundingMode

/** Converts between what a user types ("1,250.50") and stored minor units (125050). */
object MoneyParser {

    private const val MINOR_UNITS_PER_UNIT = 100

    /**
     * Parses user input into minor units. Accepts digits, thousands separators (commas) and at most
     * two decimal places. Returns `null` for blank or malformed input. Does NOT reject zero or
     * negatives: that is the job of the validation use cases.
     */
    fun parseToMinorUnits(input: String): Long? {
        val cleaned = input.trim().replace(",", "").removePrefix("Rs.").removePrefix("Rs").trim()
        if (cleaned.isEmpty()) return null
        if (!cleaned.matches(Regex("""-?\d+(\.\d{1,2})?"""))) return null
        return try {
            BigDecimal(cleaned)
                .multiply(BigDecimal(MINOR_UNITS_PER_UNIT))
                .setScale(0, RoundingMode.UNNECESSARY)
                .longValueExact()
        } catch (_: ArithmeticException) {
            null
        }
    }

    private val amountInput = Regex("""\d*(\.\d{0,2})?""")
    private val quantityInput = Regex("""\d*(\.\d{0,3})?""")

    /** True for an acceptable in-progress amount while typing: digits, optional '.', max 2 decimals. */
    fun isValidAmountInput(text: String): Boolean = amountInput.matches(text)

    /** True for an acceptable in-progress quantity while typing: digits, optional '.', max 3 decimals. */
    fun isValidQuantityInput(text: String): Boolean = quantityInput.matches(text)

    /** Minor units to an editable string without grouping, e.g. 125050 -> "1250.5", 125000 -> "1250". */
    fun toInputString(minorUnits: Long): String {
        val value = BigDecimal(minorUnits).divide(BigDecimal(MINOR_UNITS_PER_UNIT))
        return value.stripTrailingZeros().toPlainString()
    }
}
