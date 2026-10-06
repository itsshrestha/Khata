package com.khata.app.ui.reports

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.khata.app.domain.model.CustomerWithBalance
import com.khata.app.ui.LocalAppContainer
import com.khata.app.ui.components.CustomerAvatar
import com.khata.app.ui.components.DateRangeFilterDialog
import com.khata.app.ui.theme.ledgerColors
import com.khata.app.ui.transactions.DateRangePreset
import com.khata.app.utils.CurrencyFormatter
import com.khata.app.utils.DateFormatter
import java.time.LocalDate

@Composable
fun ReportsRoute(
    onCustomerClick: (String) -> Unit,
) {
    val container = LocalAppContainer.current
    val viewModel: ReportsViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                ReportsViewModel(
                    transactionRepository = container.transactionRepository,
                    customerRepository = container.customerRepository,
                )
            }
        },
    )
    val state = viewModel.uiState.collectAsStateWithLifecycle().value
    ReportsScreen(
        state = state,
        currency = container.currencyFormatter,
        onCustomerClick = onCustomerClick,
        onSetPeriod = viewModel::setPeriod,
        onSetCustomDateRange = viewModel::setCustomDateRange,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    state: ReportsUiState,
    currency: CurrencyFormatter,
    onCustomerClick: (String) -> Unit,
    onSetPeriod: (DateRangePreset) -> Unit,
    onSetCustomDateRange: (LocalDate, LocalDate) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
) {
    val creditColor = MaterialTheme.ledgerColors.credit
    val paymentColor = MaterialTheme.ledgerColors.payment
    var showFilterDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text("Reports & Insights") }) },
    ) { innerPadding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(innerPadding), Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Period selector chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    DateRangePreset.entries.forEach { preset ->
                        FilterChip(
                            selected = state.customFromDate == null && state.period == preset,
                            onClick = { onSetPeriod(preset) },
                            label = { Text(preset.displayName) },
                        )
                    }
                    FilterChip(
                        selected = state.customFromDate != null,
                        onClick = { showFilterDialog = true },
                        label = {
                            Text(
                                text = if (state.customFromDate != null && state.customToDate != null) {
                                    "📅 ${DateFormatter.full(state.customFromDate)} – ${DateFormatter.full(state.customToDate)}"
                                } else {
                                    "🔍 Advanced Search"
                                },
                            )
                        },
                    )
                }

                // Summary KPI Cards Grid
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    KpiCard(
                        label = "Credit Sales",
                        value = currency.format(state.periodCredit),
                        valueColor = creditColor,
                        modifier = Modifier.weight(1f),
                    )
                    KpiCard(
                        label = "Collections",
                        value = currency.format(state.periodCollection),
                        valueColor = paymentColor,
                        modifier = Modifier.weight(1f),
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    KpiCard(
                        label = "Net Change",
                        value = (if (state.netChange > 0) "+" else "") + currency.format(state.netChange),
                        valueColor = if (state.netChange >= 0) creditColor else paymentColor,
                        modifier = Modifier.weight(1f),
                    )
                    KpiCard(
                        label = "Total Outstanding",
                        value = currency.format(state.totalOutstanding),
                        valueColor = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                }

                // Weekly Comparison Chart
                Text(
                    text = "Daily Trend (Last 7 Days)",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = 8.dp),
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        // Chart Legend
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            LegendItem(color = creditColor, label = "Credit")
                            LegendItem(color = paymentColor, label = "Collection")
                        }

                        // Canvas Bar Chart
                        WeeklyBarChart(
                            dailyBreakdown = state.dailyBreakdown,
                            creditColor = creditColor,
                            paymentColor = paymentColor,
                            modifier = Modifier.fillMaxWidth().height(160.dp),
                        )
                    }
                }

                // Top Debtors Section
                Text(
                    text = "Top Debtors (Highest Balance)",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = 8.dp),
                )

                if (state.topDebtors.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    ) {
                        Text(
                            text = "No customers with outstanding balances.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(20.dp),
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.topDebtors.forEach { customerWithBalance ->
                            DebtorRow(
                                customerWithBalance = customerWithBalance,
                                currency = currency,
                                onClick = { onCustomerClick(customerWithBalance.customer.id) },
                            )
                        }
                    }
                }
            }
        }

        if (showFilterDialog) {
            DateRangeFilterDialog(
                initialFromDate = state.customFromDate,
                initialToDate = state.customToDate,
                onApply = { from, to ->
                    onSetCustomDateRange(from, to)
                    showFilterDialog = false
                },
                onReset = {
                    onSetPeriod(DateRangePreset.THIS_MONTH)
                    showFilterDialog = false
                },
                onDismiss = { showFilterDialog = false },
            )
        }
    }
}

@Composable
private fun KpiCard(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = valueColor,
            )
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Surface(modifier = Modifier.size(12.dp), shape = CircleShape, color = color) {}
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun WeeklyBarChart(
    dailyBreakdown: List<DailySummary>,
    creditColor: Color,
    paymentColor: Color,
    modifier: Modifier = Modifier,
) {
    val maxVal = remember(dailyBreakdown) {
        val maxAmount = dailyBreakdown.maxOfOrNull { maxOf(it.creditAmount, it.paymentAmount) } ?: 0L
        if (maxAmount == 0L) 1L else maxAmount
    }

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val itemCount = dailyBreakdown.size
        val barGroupWidth = width / itemCount
        val barWidth = barGroupWidth * 0.3f
        val gap = barGroupWidth * 0.1f

        dailyBreakdown.forEachIndexed { index, day ->
            val xCenter = index * barGroupWidth + barGroupWidth / 2f
            val creditHeight = (day.creditAmount.toFloat() / maxVal.toFloat()) * (height * 0.85f)
            val paymentHeight = (day.paymentAmount.toFloat() / maxVal.toFloat()) * (height * 0.85f)

            // Credit Bar
            drawRect(
                color = creditColor,
                topLeft = Offset(xCenter - barWidth - gap / 2f, height - creditHeight),
                size = Size(barWidth, creditHeight),
            )

            // Payment Bar
            drawRect(
                color = paymentColor,
                topLeft = Offset(xCenter + gap / 2f, height - paymentHeight),
                size = Size(barWidth, paymentHeight),
            )
        }
    }
}

@Composable
private fun DebtorRow(
    customerWithBalance: CustomerWithBalance,
    currency: CurrencyFormatter,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CustomerAvatar(name = customerWithBalance.customer.name, size = 44.dp)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = customerWithBalance.customer.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                customerWithBalance.customer.phone?.let { phone ->
                    Text(
                        text = phone,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                text = currency.format(customerWithBalance.balance.outstanding),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}
