package com.khata.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.khata.app.domain.model.Balance
import com.khata.app.domain.model.BalanceStatus
import com.khata.app.ui.theme.ledgerColors
import com.khata.app.utils.CurrencyFormatter

/** Round badge with the customer's first letter. */
@Composable
fun CustomerAvatar(name: String, modifier: Modifier = Modifier, size: Dp = 48.dp) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = name.trim().take(1).uppercase().ifEmpty { "?" },
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

/** One-line balance label used in lists: "Outstanding: Rs. 5,500", "✓ Paid" or "No credit yet". */
@Composable
fun BalanceStatusText(
    balance: Balance,
    currency: CurrencyFormatter,
    modifier: Modifier = Modifier,
) {
    when (balance.status) {
        BalanceStatus.OWES -> Text(
            text = "Outstanding: ${currency.format(balance.outstanding)}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.ledgerColors.credit,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = modifier,
        )
        BalanceStatus.SETTLED -> Text(
            text = "✓ Paid",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.ledgerColors.payment,
            modifier = modifier,
        )
        BalanceStatus.NO_CREDIT -> Text(
            text = "No credit yet",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier,
        )
    }
}
