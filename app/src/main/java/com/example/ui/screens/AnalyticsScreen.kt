package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.components.CategoryDonutChart
import com.example.ui.components.DailySpendingVelocityChart
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.MonthSelectorBar
import com.example.ui.components.MonthlyCashflowBarChart
import com.example.ui.state.FinanceDashboardData
import com.example.ui.state.formatCurrency
import com.example.ui.theme.VerdantThemeTokens
import com.example.ui.theme.tabular
import kotlin.math.roundToInt

@Composable
fun AnalyticsScreen(
    data: FinanceDashboardData,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onResetCurrentMonth: () -> Unit,
    onAddTransaction: () -> Unit,
    modifier: Modifier = Modifier
) {
    val savingsRatePct = if (data.monthIncomeCents > 0L) {
        (((data.monthIncomeCents - data.monthExpenseCents).toFloat() / data.monthIncomeCents.toFloat()) * 100f)
            .roundToInt()
    } else {
        0
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            MonthSelectorBar(
                selectedMonth = data.selectedMonth,
                onPreviousMonth = onPreviousMonth,
                onNextMonth = onNextMonth,
                onResetCurrentMonth = onResetCurrentMonth
            )
        }

        item {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, VerdantThemeTokens.colors.cardBorder),
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Monthly Net Savings",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = formatCurrency(data.monthNetCents, data.currency, showPlusSign = data.monthNetCents > 0),
                            style = MaterialTheme.typography.headlineMedium.tabular(),
                            color = if (data.monthNetCents >= 0) {
                                VerdantThemeTokens.colors.income
                            } else {
                                VerdantThemeTokens.colors.expense
                            }
                        )
                    }
                    Surface(
                        color = if (savingsRatePct >= 0) {
                            VerdantThemeTokens.colors.incomeContainer
                        } else {
                            VerdantThemeTokens.colors.expenseContainer
                        },
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = "Savings Rate",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "$savingsRatePct%",
                                style = MaterialTheme.typography.titleLarge.tabular().copy(fontWeight = FontWeight.Bold),
                                color = if (savingsRatePct >= 0) {
                                    VerdantThemeTokens.colors.income
                                } else {
                                    VerdantThemeTokens.colors.expense
                                }
                            )
                        }
                    }
                }
            }
        }

        item {
            AnalyticsSectionCard(
                title = "6-Month Cashflow Trends",
                subtitle = "Tap any month column to inspect income vs. expenses"
            ) {
                MonthlyCashflowBarChart(
                    points = data.cashflowTrend,
                    currency = data.currency
                )
            }
        }

        if (data.categorySlices.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Filled.PieChart,
                    title = "No spending breakdown for ${data.selectedMonth.shortLabel()}",
                    subtitle = "Log expenses in ${data.selectedMonth.fullLabel()} to view interactive category shares and spending velocity.",
                    actionLabel = "Log Expense",
                    onAction = onAddTransaction
                )
            }
        } else {
            item {
                AnalyticsSectionCard(
                    title = "Spending by Category",
                    subtitle = "Tap a slice or category row to inspect details"
                ) {
                    CategoryDonutChart(
                        slices = data.categorySlices,
                        currency = data.currency
                    )
                }
            }

            item {
                AnalyticsSectionCard(
                    title = "Daily Spending Velocity",
                    subtitle = "Cumulative expense trajectory for ${data.selectedMonth.fullLabel()}"
                ) {
                    DailySpendingVelocityChart(
                        points = data.dailyVelocity,
                        currency = data.currency
                    )
                }
            }
        }
    }
}

@Composable
private fun AnalyticsSectionCard(
    title: String,
    subtitle: String,
    content: @Composable () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, VerdantThemeTokens.colors.cardBorder),
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            content()
        }
    }
}
