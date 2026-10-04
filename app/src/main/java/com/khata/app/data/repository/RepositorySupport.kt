package com.khata.app.data.repository

import com.khata.app.domain.model.KhataError
import com.khata.app.domain.model.Outcome
import kotlin.coroutines.cancellation.CancellationException

/**
 * Runs a database block and converts unexpected failures into a friendly [KhataError.DatabaseError]
 * so technical details never reach the UI. Coroutine cancellation is always re-thrown.
 */
internal suspend inline fun <T> safeDatabaseCall(block: () -> Outcome<T>): Outcome<T> =
    try {
        block()
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (_: Exception) {
        Outcome.Failure(KhataError.DatabaseError)
    }

/** Escapes LIKE wildcards so a search for "50%" matches literally. Pairs with `ESCAPE '\'` in SQL. */
internal fun escapeLike(text: String): String =
    text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
