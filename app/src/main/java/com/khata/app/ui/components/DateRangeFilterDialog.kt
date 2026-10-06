package com.khata.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.LocalDate

@Composable
fun DateRangeFilterDialog(
    initialFromDate: LocalDate? = null,
    initialToDate: LocalDate? = null,
    onApply: (fromDate: LocalDate, toDate: LocalDate) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
) {
    val today = remember { LocalDate.now() }
    var fromDate by remember { mutableStateOf(initialFromDate ?: today.minusDays(30)) }
    var toDate by remember { mutableStateOf(initialToDate ?: today) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Advanced Search — Date Range",
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = "Select a start date and end date to filter transactions.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                DateField(
                    label = "From Date (Start)",
                    date = fromDate,
                    onDateChange = { newFrom: LocalDate ->
                        fromDate = newFrom
                        if (fromDate.isAfter(toDate)) {
                            toDate = fromDate
                        }
                        errorMessage = null
                    },
                )

                DateField(
                    label = "To Date (End)",
                    date = toDate,
                    onDateChange = { newTo: LocalDate ->
                        if (newTo.isBefore(fromDate)) {
                            errorMessage = "To Date cannot be before From Date"
                        } else {
                            toDate = newTo
                            errorMessage = null
                        }
                    },
                )

                errorMessage?.let { err ->
                    Text(
                        text = err,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (!fromDate.isAfter(toDate)) {
                        onApply(fromDate, toDate)
                    }
                },
                enabled = errorMessage == null,
            ) {
                Text("Apply Search")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (initialFromDate != null || initialToDate != null) {
                    TextButton(onClick = onReset) {
                        Text("Reset Filter", color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        },
    )
}
