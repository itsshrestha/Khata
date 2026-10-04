package com.khata.app.utils

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Formats dates ONLY for display. The database stores real date types (epoch days / epoch millis),
 * never formatted strings.
 */
object DateFormatter {

    private val fullFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.ENGLISH)
    private val shortFormatter = DateTimeFormatter.ofPattern("MMM dd", Locale.ENGLISH)
    private val inputFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' hh:mm a", Locale.ENGLISH)

    /** Formats exact timestamp e.g. "Oct 04, 2026 at 09:39 PM". */
    fun dateTimeFull(epochMillis: Long): String {
        val instant = java.time.Instant.ofEpochMilli(epochMillis)
        val zdt = java.time.ZonedDateTime.ofInstant(instant, java.time.ZoneId.systemDefault())
        return dateTimeFormatter.format(zdt)
    }

    /** "Today", "Yesterday", otherwise "Oct 03, 2026". [today] is injected for testability. */
    fun friendly(date: LocalDate, today: LocalDate): String = when (date) {
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        else -> fullFormatter.format(date)
    }

    /** "Oct 03" for compact statement rows. */
    fun short(date: LocalDate): String = shortFormatter.format(date)

    /** "Oct 03, 2026". */
    fun full(date: LocalDate): String = fullFormatter.format(date)

    /** "2026-10-03" for date input fields. */
    fun forInput(date: LocalDate): String = inputFormatter.format(date)

    /** Greeting for the dashboard header based on the local hour (0-23). */
    fun greeting(hourOfDay: Int): String = when (hourOfDay) {
        in 5..11 -> "Good Morning"
        in 12..16 -> "Good Afternoon"
        in 17..20 -> "Good Evening"
        else -> "Hello"
    }
}
