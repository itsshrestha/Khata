package com.khata.app.domain.usecase

import com.khata.app.domain.model.Balance
import com.khata.app.domain.model.Transaction
import com.khata.app.domain.model.TransactionType

/**
 * The single place that turns transactions into a balance.
 *
 *   outstanding = SUM(CREDIT) - SUM(PAYMENT)
 *
 * Transactions are the source of truth; a balance is never stored or mutated.
 */
class CalculateBalanceUseCase {

    operator fun invoke(transactions: List<Transaction>): Balance {
        var totalCredit = 0L
        var totalPaid = 0L
        for (transaction in transactions) {
            when (transaction.type) {
                TransactionType.CREDIT -> totalCredit += transaction.amount
                TransactionType.PAYMENT -> totalPaid += transaction.amount
            }
        }
        return Balance(totalCredit = totalCredit, totalPaid = totalPaid)
    }

    /**
     * Running outstanding balance after each transaction, for statements. [transactions] must
     * already be in chronological order. The result has the same size and order as the input.
     */
    fun runningBalances(transactions: List<Transaction>): List<Long> {
        var running = 0L
        return transactions.map { transaction ->
            running += when (transaction.type) {
                TransactionType.CREDIT -> transaction.amount
                TransactionType.PAYMENT -> -transaction.amount
            }
            running
        }
    }
}
