package com.khata.app.domain.usecase

import com.khata.app.utils.MoneyParser

/**
 * Turns what the user typed on the detailed-credit form into drafts, and computes the live total
 * shown while they type. Pure functions, so the form arithmetic is unit-tested.
 */
object CreditFormMapper {

    fun toDraft(name: String, quantityText: String, unitPriceText: String) = CreditItemDraft(
        itemName = name,
        quantity = quantityText.trim().takeIf { it.isNotEmpty() }?.toDoubleOrNull(),
        unitPrice = MoneyParser.parseToMinorUnits(unitPriceText),
    )

    /**
     * Sum of the lines that are complete and positive. Incomplete lines count as zero here, so the
     * running total never shows an error while someone is mid-typing; real validation happens on save.
     */
    fun liveTotal(drafts: List<CreditItemDraft>): Long = drafts.sumOf { draft ->
        val quantity = draft.quantity
        val unitPrice = draft.unitPrice
        if (quantity != null && unitPrice != null && quantity > 0.0 && unitPrice > 0L) {
            ValidateCreditUseCase.lineTotal(quantity, unitPrice)
        } else {
            0L
        }
    }

    /** Fallback description for a detailed credit: "Rice, Cooking Oil, Biscuits". */
    fun summarizeItems(drafts: List<CreditItemDraft>, maxLength: Int = 120): String {
        val joined = drafts.map { it.itemName.trim() }.filter { it.isNotEmpty() }.joinToString(", ")
        return if (joined.length <= maxLength) joined else joined.take(maxLength - 1).trimEnd() + "…"
    }
}
