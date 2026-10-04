package com.khata.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.khata.app.domain.model.Transaction
import com.khata.app.domain.model.TransactionType
import com.khata.app.ui.theme.ledgerColors
import com.khata.app.utils.CurrencyFormatter
import com.khata.app.utils.DateFormatter

@Composable
fun TransactionDetailDialog(
    transaction: Transaction,
    customerName: String? = null,
    currency: CurrencyFormatter,
    onDismiss: () -> Unit,
) {
    val isCredit = transaction.type == TransactionType.CREDIT
    val typeTitle = if (isCredit) "Credit Entry" else "Payment Receipt"
    val color = if (isCredit) MaterialTheme.ledgerColors.credit else MaterialTheme.ledgerColors.payment

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = typeTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = color,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = currency.formatSigned(transaction.amount, transaction.type),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = color,
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (!customerName.isNullOrBlank()) {
                    DetailRow(label = "Customer", value = customerName)
                }

                val exactTime = DateFormatter.dateTimeFull(transaction.createdAt.toEpochMilli())
                DetailRow(label = "Exact Time", value = exactTime)

                val device = transaction.deviceName ?: "Mobile Device"
                DetailRow(label = "Added By Device", value = device)

                if (!isCredit && transaction.paymentMethod != null) {
                    DetailRow(label = "Payment Method", value = transaction.paymentMethod.displayName)
                }

                if (!transaction.description.isNullOrBlank()) {
                    DetailRow(label = "Notes / Description", value = transaction.description)
                }

                if (transaction.items.isNotEmpty()) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text(
                        text = "Itemized Breakdown",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Table Header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text = "Item",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(2f),
                                )
                                Text(
                                    text = "Qty",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f),
                                )
                                Text(
                                    text = "Rate",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1.2f),
                                )
                                Text(
                                    text = "Total",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1.2f),
                                )
                            }
                            HorizontalDivider()
                            // Items Rows
                            transaction.items.forEach { item ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = item.itemName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(2f),
                                    )
                                    val qtyStr = if (item.quantity % 1.0 == 0.0) {
                                        item.quantity.toInt().toString()
                                    } else {
                                        item.quantity.toString()
                                    }
                                    Text(
                                        text = qtyStr,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f),
                                    )
                                    Text(
                                        text = currency.format(item.unitPrice),
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.weight(1.2f),
                                    )
                                    Text(
                                        text = currency.format(item.totalPrice),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.weight(1.2f),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
