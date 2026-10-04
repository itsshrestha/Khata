package com.khata.app.domain.usecase

import com.khata.app.domain.model.KhataError
import com.khata.app.domain.model.Outcome

/**
 * Validates a payment against the customer's CURRENT outstanding balance.
 * It never silently changes the amount: an invalid payment is rejected with a clear error.
 */
class ValidatePaymentUseCase {

    /** @return the same [amount] on success, or the reason it was rejected. */
    operator fun invoke(amount: Long?, outstanding: Long): Outcome<Long> {
        if (outstanding <= 0L) return Outcome.Failure(KhataError.NoOutstandingBalance)
        if (amount == null || amount <= 0L) return Outcome.Failure(KhataError.InvalidAmount)
        if (amount > outstanding) {
            return Outcome.Failure(KhataError.PaymentExceedsOutstanding(outstanding))
        }
        return Outcome.Success(amount)
    }
}
