package com.khata.app.utils

import com.khata.app.domain.model.TransactionType
import kotlin.math.abs

/**
 * Formats money for display. Amounts are minor units (paisa); the currency symbol lives ONLY here,
 * never in calculations, so it can be changed (or made a setting) later.
 *
 * Uses the Indian/Nepali digit grouping: 1,50,000.
 */
class CurrencyFormatter(
    val symbol: String = DEFAULT_SYMBOL,
    private val minorUnitsPerUnit: Long = DEFAULT_MINOR_UNITS_PER_UNIT,
) {

    /** "Rs. 1,250" or "Rs. 1,250.50". Negative values render as "-Rs. 500". */
    fun format(minorUnits: Long): String {
        val sign = if (minorUnits < 0) "-" else ""
        return "$sign$symbol ${formatNumber(abs(minorUnits))}"
    }

    /** "+ Rs. 1,200" for credits and "- Rs. 500" for payments. [amount] is the positive stored amount. */
    fun formatSigned(amount: Long, type: TransactionType): String {
        val prefix = when (type) {
            TransactionType.CREDIT -> "+"
            TransactionType.PAYMENT -> "-"
        }
        return "$prefix ${format(abs(amount))}"
    }

    /** The number without a symbol, e.g. "1,50,000". Decimals appear only when non-zero. */
    fun formatNumber(minorUnits: Long): String {
        val absolute = abs(minorUnits)
        val whole = absolute / minorUnitsPerUnit
        val fraction = absolute % minorUnitsPerUnit
        val grouped = groupIndian(whole.toString())
        if (fraction == 0L) return grouped
        val decimals = fraction.toString().padStart(decimalPlaces, '0')
        return "$grouped.$decimals"
    }

    private val decimalPlaces: Int = minorUnitsPerUnit.toString().length - 1

    private fun groupIndian(digits: String): String {
        if (digits.length <= 3) return digits
        val lastThree = digits.takeLast(3)
        val rest = digits.dropLast(3)
        val groups = rest.reversed().chunked(2).map { it.reversed() }.reversed()
        return groups.joinToString(",") + "," + lastThree
    }

    companion object {
        const val DEFAULT_SYMBOL = "Rs."
        const val DEFAULT_MINOR_UNITS_PER_UNIT = 100L
    }
}
