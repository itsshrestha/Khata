package com.khata.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Teal40,
    onPrimary = Color.White,
    primaryContainer = Teal90,
    onPrimaryContainer = Teal10,
    secondary = Amber40,
    onSecondary = Color.White,
    secondaryContainer = Amber90,
    onSecondaryContainer = Color(0xFF2B1700),
    background = Neutral99,
    onBackground = Neutral10,
    surface = Neutral99,
    onSurface = Neutral10,
    surfaceVariant = Neutral95,
    error = ErrorLight,
)

private val DarkColors = darkColorScheme(
    primary = Teal80,
    onPrimary = Teal20,
    primaryContainer = Teal30,
    onPrimaryContainer = Teal90,
    secondary = Amber80,
    onSecondary = Color(0xFF4A2800),
    secondaryContainer = Color(0xFF6A3B00),
    onSecondaryContainer = Amber90,
    background = Neutral10,
    onBackground = Neutral90,
    surface = Neutral10,
    onSurface = Neutral90,
    surfaceVariant = Color(0xFF232B2A),
    error = ErrorDark,
)

/** Semantic colors for the ledger, so credits and payments look the same everywhere. */
@Immutable
data class LedgerColors(
    val credit: Color,
    val payment: Color,
)

private val LightLedgerColors = LedgerColors(credit = CreditLight, payment = PaymentLight)
private val DarkLedgerColors = LedgerColors(credit = CreditDark, payment = PaymentDark)

val LocalLedgerColors = staticCompositionLocalOf { LightLedgerColors }

/** Access with `MaterialTheme.ledgerColors`. */
val MaterialTheme.ledgerColors: LedgerColors
    @Composable
    @ReadOnlyComposable
    get() = LocalLedgerColors.current

@Composable
fun KhataTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    androidx.compose.runtime.CompositionLocalProvider(
        LocalLedgerColors provides if (darkTheme) DarkLedgerColors else LightLedgerColors,
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = KhataTypography,
            content = content,
        )
    }
}
