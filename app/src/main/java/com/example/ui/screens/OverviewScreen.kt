package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.RecurringBill
import com.example.data.TransactionRecord
import com.example.ui.components.BudgetProgressCard
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.MonthSelectorBar
import com.example.ui.components.TransactionRowCard
import com.example.ui.state.BudgetAlertLevel
import com.example.ui.state.DueUrgency
import com.example.ui.state.FinanceDashboardData
import com.example.ui.state.formatCurrency
import com.example.ui.state.formatDueStatus
import com.example.ui.theme.VerdantThemeTokens
import com.example.ui.theme.tabular
import com.google.firebase.Timestamp

@Composable
fun OverviewScreen(
    data: FinanceDashboardData,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onResetCurrentMonth: () -> Unit,
    onAddTransaction: () -> Unit,
    onEditTransaction: (TransactionRecord) -> Unit,
    onEditBudget: (com.example.data.CategoryBudget) -> Unit,
    onAddBudget: () -> Unit,
    onPayBill: (RecurringBill) -> Unit,
    onNavigateToTransactions: () -> Unit,
    onNavigateToPlan: () -> Unit,
    modifier: Modifier = Modifier
) {
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
            CashflowHeroCard(data = data)
        }

        if (data.alertBudgets.isNotEmpty()) {
            item {
                BudgetAlertsBanner(
                    data = data,
                    onViewBudgets = onNavigateToPlan
                )
            }
        }

        if (data.upcomingBills.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionHeader(
                        title = "Due Soon",
                        actionLabel = "All bills",
                        onAction = onNavigateToPlan
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        data.upcomingBills.forEach { bill ->
                            UpcomingBillMiniCard(
                                bill = bill,
                                currency = data.currency,
                                onPay = { onPayBill(bill) }
                            )
                        }
                    }
                }
            }
        }

        item {
            SectionHeader(
                title = "Monthly Budgets",
                actionLabel = if (data.budgetProgresses.isEmpty()) "Set budget" else "Manage",
                onAction = if (data.budgetProgresses.isEmpty()) onAddBudget else onNavigateToPlan
            )
        }

        if (data.budgetProgresses.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Filled.Savings,
                    title = "No monthly budgets yet",
                    subtitle = "Set category limits to get real-time 80% warnings and overspend alerts.",
                    actionLabel = "Create First Budget",
                    onAction = onAddBudget
                )
            }
        } else {
            items(
                items = data.budgetProgresses.take(3),
                key = { "ov_budget_${it.budget.id}" }
            ) { progress ->
                BudgetProgressCard(
                    progress = progress,
                    currency = data.currency,
                    onEdit = { onEditBudget(progress.budget) }
                )
            }
        }

        item {
            SectionHeader(
                title = "Recent Transactions",
                actionLabel = if (data.monthTransactions.isNotEmpty()) "See all" else null,
                onAction = if (data.monthTransactions.isNotEmpty()) onNavigateToTransactions else null
            )
        }

        if (data.monthTransactions.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.AutoMirrored.Filled.ReceiptLong,
                    title = "No transactions in ${data.selectedMonth.shortLabel()}",
                    subtitle = "Tap below to log an expense or income and watch your dashboard update live.",
                    actionLabel = "Record Transaction",
                    onAction = onAddTransaction
                )
            }
        } else {
            items(
                items = data.monthTransactions.take(5),
                key = { "ov_tx_${it.id}" }
            ) { tx ->
                TransactionRowCard(
                    transaction = tx,
                    currency = data.currency,
                    onClick = { onEditTransaction(tx) }
                )
            }
        }
    }
}

@Composable
private fun CashflowHeroCard(data: FinanceDashboardData) {
    val extended = VerdantThemeTokens.colors
    Surface(
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(extended.heroStart, extended.heroEnd)
                    )
                )
                .padding(20.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${data.selectedMonth.fullLabel()} Cashflow",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                    Surface(
                        color = Color.White.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Live Sync",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = formatCurrency(data.monthNetCents, data.currency, showPlusSign = data.monthNetCents > 0),
                    style = MaterialTheme.typography.displaySmall.tabular(),
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "All-time net balance: ${formatCurrency(data.allTimeNetCents, data.currency)}",
                    style = MaterialTheme.typography.bodySmall.tabular(),
                    color = Color.White.copy(alpha = 0.8f)
                )

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HeroMetricPill(
                        label = "Income",
                        amount = formatCurrency(data.monthIncomeCents, data.currency),
                        icon = Icons.Filled.ArrowDownward,
                        modifier = Modifier.weight(1f)
                    )
                    HeroMetricPill(
                        label = "Expenses",
                        amount = formatCurrency(data.monthExpenseCents, data.currency),
                        icon = Icons.Filled.ArrowUpward,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroMetricPill(
    label: String,
    amount: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color.White.copy(alpha = 0.14f),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = Color.White.copy(alpha = 0.2f),
                shape = CircleShape,
                modifier = Modifier.size(28.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.8f)
                )
                Text(
                    text = amount,
                    style = MaterialTheme.typography.titleSmall.tabular().copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun BudgetAlertsBanner(
    data: FinanceDashboardData,
    onViewBudgets: () -> Unit
) {
    val hasExceeded = data.alertBudgets.any { it.alertLevel == BudgetAlertLevel.EXCEEDED }
    val bannerBg = if (hasExceeded) {
        VerdantThemeTokens.colors.expenseContainer
    } else {
        VerdantThemeTokens.colors.warningContainer
    }
    val accent = if (hasExceeded) {
        VerdantThemeTokens.colors.expense
    } else {
        VerdantThemeTokens.colors.warning
    }
    val textColor = if (hasExceeded) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        VerdantThemeTokens.colors.onWarningContainer
    }

    Surface(
        color = bannerBg,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.35f)),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onViewBudgets)
            .testTag("budget_alert_banner")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.WarningAmber,
                contentDescription = "Budget alert",
                tint = accent,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                val exceededCount = data.alertBudgets.count { it.alertLevel == BudgetAlertLevel.EXCEEDED }
                val warningCount = data.alertBudgets.count { it.alertLevel == BudgetAlertLevel.WARNING }
                val title = when {
                    exceededCount > 0 -> "$exceededCount budget${if (exceededCount > 1) "s" else ""} exceeded"
                    else -> "$warningCount budget${if (warningCount > 1) "s" else ""} nearing limit"
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = textColor
                )
                Text(
                    text = data.alertBudgets.joinToString(", ") { it.budget.category },
                    style = MaterialTheme.typography.bodySmall,
                    color = textColor.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = "Review",
                style = MaterialTheme.typography.labelLarge,
                color = accent
            )
        }
    }
}

@Composable
private fun UpcomingBillMiniCard(
    bill: RecurringBill,
    currency: com.example.data.CurrencyOption,
    onPay: () -> Unit
) {
    val dueTs = bill.nextDueAt ?: Timestamp.now()
    val (dueText, urgency) = formatDueStatus(dueTs)
    val urgencyColor = when (urgency) {
        DueUrgency.OVERDUE -> VerdantThemeTokens.colors.expense
        DueUrgency.DUE_SOON -> VerdantThemeTokens.colors.warning
        DueUrgency.UPCOMING -> MaterialTheme.colorScheme.primary
    }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, VerdantThemeTokens.colors.cardBorder),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.width(210.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = dueText,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = urgencyColor
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = bill.title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = formatCurrency(bill.amountCents, currency),
                style = MaterialTheme.typography.titleMedium.tabular().copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = onPay,
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Log Paid", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction) {
                Text(actionLabel)
            }
        }
    }
}
