package com.khata.app.domain.usecase

import com.khata.app.domain.model.KhataError
import com.khata.app.domain.model.Outcome
import com.khata.app.domain.model.Transaction
import com.khata.app.domain.model.TransactionType

/**
 * Safety rules for changing or removing financial history.
 *
 * Every rule is expressed through the balance: a credit adds to what is owed, a payment subtracts.
 * A change is allowed only if the customer's outstanding balance would stay at zero or above, so
 * history can never claim a customer paid more than they owed.
 */
class ValidateTransactionChangeUseCase {

    /** How this entry moves the outstanding balance: +amount for credit, -amount for payment. */
    fun balanceEffect(type: TransactionType, amount: Long): Long = when (type) {
        TransactionType.CREDIT -> amount
        TransactionType.PAYMENT -> -amount
    }

    /** The outstanding balance if [transaction] were deleted. Used for the confirmation text. */
    fun outstandingAfterDelete(transaction: Transaction, currentOutstanding: Long): Long =
        currentOutstanding - balanceEffect(transaction.type, transaction.amount)

    /** The outstanding balance if [original]'s amount became [newAmount]. */
    fun outstandingAfterEdit(original: Transaction, newAmount: Long, currentOutstanding: Long): Long =
        currentOutstanding -
            balanceEffect(original.type, original.amount) +
            balanceEffect(original.type, newAmount)

    /**
     * @return the outstanding balance after deletion, or why deletion is not allowed.
     * Deleting a payment is always fine. Deleting a credit is blocked if the remaining credits would
     * no longer cover what was already paid.
     */
    fun validateDelete(transaction: Transaction, currentOutstanding: Long): Outcome<Long> {
        val after = outstandingAfterDelete(transaction, currentOutstanding)
        if (after < 0L) return Outcome.Failure(KhataError.WouldCreateNegativeBalance)
        return Outcome.Success(after)
    }

    /** @return the validated new amount, or why the edit is not allowed. */
    fun validateAmountEdit(original: Transaction, newAmount: Long?, currentOutstanding: Long): Outcome<Long> {
        if (newAmount == null || newAmount <= 0L) return Outcome.Failure(KhataError.InvalidAmount)
        if (newAmount > ValidateCreditUseCase.MAX_AMOUNT) return Outcome.Failure(KhataError.AmountTooLarge)

        val after = outstandingAfterEdit(original, newAmount, currentOutstanding)
        if (after < 0L) {
            return Outcome.Failure(
                when (original.type) {
                    // The most this payment could be is what was owed without it.
                    TransactionType.PAYMENT -> KhataError.PaymentExceedsOutstanding(
                        outstanding = currentOutstanding + original.amount,
                    )
                    TransactionType.CREDIT -> KhataError.WouldCreateNegativeBalance
                },
            )
        }
        return Outcome.Success(newAmount)
    }
}
