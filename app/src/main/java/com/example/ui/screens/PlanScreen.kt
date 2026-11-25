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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.CategoryBudget
import com.example.data.RecurringBill
import com.example.ui.components.BudgetProgressCard
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.MonthSelectorBar
import com.example.ui.components.RecurringBillCard
import com.example.ui.state.FinanceDashboardData
import com.example.ui.state.formatCurrency
import com.example.ui.theme.VerdantThemeTokens
import com.example.ui.theme.tabular

enum class PlanSubTab(val label: String) {
    BUDGETS("Budgets"),
    BILLS("Recurring Bills")
}

@Composable
fun PlanScreen(
    data: FinanceDashboardData,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onResetCurrentMonth: () -> Unit,
    onAddBudget: () -> Unit,
    onEditBudget: (CategoryBudget) -> Unit,
    onDeleteBudget: (String) -> Unit,
    onAddBill: () -> Unit,
    onEditBill: (RecurringBill) -> Unit,
    onPayBill: (RecurringBill) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedSubTab by rememberSaveable { mutableStateOf(PlanSubTab.BUDGETS) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                PlanSubTab.entries.forEachIndexed { index, tab ->
                    SegmentedButton(
                        selected = selectedSubTab == tab,
                        onClick = { selectedSubTab = tab },
                        shape = SegmentedButtonDefaults.itemShape(index, PlanSubTab.entries.size),
                        modifier = Modifier.testTag("plan_subtab_${tab.name}")
                    ) {
                        Text(tab.label)
                    }
                }
            }
        }

        if (selectedSubTab == PlanSubTab.BUDGETS) {
            item {
                MonthSelectorBar(
                    selectedMonth = data.selectedMonth,
                    onPreviousMonth = onPreviousMonth,
                    onNextMonth = onNextMonth,
                    onResetCurrentMonth = onResetCurrentMonth
                )
            }

            item {
                val totalLimit = data.budgetProgresses.sumOf { it.budget.limitCents }
                val totalSpent = data.budgetProgresses.sumOf { it.spentCents }
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, VerdantThemeTokens.colors.cardBorder),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Total Budgeted (${data.selectedMonth.shortLabel()})",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${formatCurrency(totalSpent, data.currency)} / ${formatCurrency(totalLimit, data.currency)}",
                                style = MaterialTheme.typography.titleLarge.tabular().copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Button(
                            onClick = onAddBudget,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("add_budget_button")
                        ) {
                            Icon(imageVector = Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add Budget")
                        }
                    }
                }
            }

            if (data.budgetProgresses.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.Filled.Savings,
                        title = "Create your first monthly budget",
                        subtitle = "Set spending caps by category to automatically flag 80% warnings and overspends.",
                        actionLabel = "Add Category Budget",
                        onAction = onAddBudget
                    )
                }
            } else {
                items(
                    items = data.budgetProgresses,
                    key = { "plan_budget_${it.budget.id}" }
                ) { progress ->
                    BudgetProgressCard(
                        progress = progress,
                        currency = data.currency,
                        onEdit = { onEditBudget(progress.budget) },
                        onDelete = { onDeleteBudget(progress.budget.id) }
                    )
                }
            }
        } else {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, VerdantThemeTokens.colors.cardBorder),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Est. Monthly Recurring",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = formatCurrency(data.monthlyRecurringTotalCents, data.currency),
                                style = MaterialTheme.typography.titleLarge.tabular().copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${data.recurringBills.count { it.active }} active schedule${if (data.recurringBills.count { it.active } == 1) "" else "s"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Button(
                            onClick = onAddBill,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("add_bill_button")
                        ) {
                            Icon(imageVector = Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add Bill")
                        }
                    }
                }
            }

            if (data.recurringBills.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.Filled.EventRepeat,
                        title = "Track subscriptions & fixed bills",
                        subtitle = "Add rent, internet, or subscriptions and log payments in one tap.",
                        actionLabel = "Add Recurring Bill",
                        onAction = onAddBill
                    )
                }
            } else {
                items(
                    items = data.recurringBills,
                    key = { "plan_bill_${it.id}" }
                ) { bill ->
                    RecurringBillCard(
                        bill = bill,
                        currency = data.currency,
                        onPayNow = { onPayBill(bill) },
                        onEdit = { onEditBill(bill) }
                    )
                }
            }
        }
    }
}
