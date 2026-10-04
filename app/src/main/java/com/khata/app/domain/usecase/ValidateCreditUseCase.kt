package com.khata.app.domain.usecase

import com.khata.app.domain.model.KhataError
import com.khata.app.domain.model.Outcome
import com.khata.app.domain.model.TransactionItem
import java.math.BigDecimal
import java.math.RoundingMode

/** A line the user typed on the detailed credit screen, before it is validated. */
data class CreditItemDraft(
    val itemName: String,
    val quantity: Double?,
    /** Minor units per single unit. */
    val unitPrice: Long?,
)

/** A validated detailed credit: the items and their summed total. */
data class DetailedCredit(
    val items: List<TransactionItem>,
    val total: Long,
)

class ValidateCreditUseCase {

    /** Quick credit: just an amount. Must be > 0 and within sane limits. */
    fun quick(amount: Long?): Outcome<Long> {
        if (amount == null || amount <= 0L) return Outcome.Failure(KhataError.InvalidAmount)
        if (amount > MAX_AMOUNT) return Outcome.Failure(KhataError.AmountTooLarge)
        return Outcome.Success(amount)
    }

    /** Detailed credit: every item needs a name, quantity > 0 and unit price > 0. */
    fun detailed(drafts: List<CreditItemDraft>): Outcome<DetailedCredit> {
        if (drafts.isEmpty()) return Outcome.Failure(KhataError.NoItems)

        val items = ArrayList<TransactionItem>(drafts.size)
        var total = 0L
        for (draft in drafts) {
            val name = draft.itemName.trim()
            if (name.isEmpty()) return Outcome.Failure(KhataError.EmptyItemName)
            val quantity = draft.quantity
            if (quantity == null || quantity.isNaN() || quantity <= 0.0) {
                return Outcome.Failure(KhataError.InvalidItemQuantity)
            }
            val unitPrice = draft.unitPrice
            if (unitPrice == null || unitPrice <= 0L) {
                return Outcome.Failure(KhataError.InvalidItemPrice)
            }
            val lineTotal = lineTotal(quantity, unitPrice)
            if (lineTotal <= 0L) return Outcome.Failure(KhataError.InvalidAmount)
            if (lineTotal > MAX_AMOUNT) return Outcome.Failure(KhataError.AmountTooLarge)

            total += lineTotal
            if (total > MAX_AMOUNT) return Outcome.Failure(KhataError.AmountTooLarge)
            items += TransactionItem(
                itemName = name,
                quantity = quantity,
                unitPrice = unitPrice,
                totalPrice = lineTotal,
            )
        }
        return Outcome.Success(DetailedCredit(items = items, total = total))
    }

    companion object {
        /** Rs. 10 crore in minor units. A guard against typos and overflow, not a business rule. */
        const val MAX_AMOUNT = 10_00_00_000L * 100L

        /** quantity x unitPrice rounded half-up to a whole minor unit (no Double drift). */
        fun lineTotal(quantity: Double, unitPrice: Long): Long =
            BigDecimal.valueOf(quantity)
                .multiply(BigDecimal.valueOf(unitPrice))
                .setScale(0, RoundingMode.HALF_UP)
                .toLong()
    }
}
