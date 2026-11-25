package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.TransactionRecord
import com.example.data.TransactionType
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.MonthSelectorBar
import com.example.ui.components.TransactionRowCard
import com.example.ui.state.FinanceDashboardData
import com.example.ui.state.TransactionFilterType
import com.example.ui.state.formatCurrency
import com.example.ui.state.formatTransactionDayHeader
import com.example.ui.theme.VerdantThemeTokens
import com.example.ui.theme.tabular
import com.google.firebase.Timestamp

@Composable
fun TransactionsScreen(
    data: FinanceDashboardData,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onResetCurrentMonth: () -> Unit,
    onSearchChange: (String) -> Unit,
    onFilterTypeChange: (TransactionFilterType) -> Unit,
    onFilterCategoryToggle: (String) -> Unit,
    onClearFilters: () -> Unit,
    onAddTransaction: () -> Unit,
    onEditTransaction: (TransactionRecord) -> Unit,
    modifier: Modifier = Modifier
) {
    val monthCategories = remember(data.monthTransactions) {
        data.monthTransactions.map { it.category }.distinct().sorted()
    }
    val grouped = remember(data.filteredTransactions) {
        data.filteredTransactions.groupBy { tx ->
            formatTransactionDayHeader(tx.occurredAt ?: tx.createdAt ?: Timestamp.now())
        }
    }
    val filteredNet = remember(data.filteredTransactions) {
        data.filteredTransactions.sumOf { tx ->
            if (tx.transactionType == TransactionType.INCOME) tx.amountCents else -tx.amountCents
        }
    }
    val hasActiveFilter = data.searchQuery.isNotBlank() ||
        data.filterType != TransactionFilterType.ALL ||
        data.filterCategory != null

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
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
            OutlinedTextField(
                value = data.searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("Search merchant, category, or note...") },
                leadingIcon = {
                    Icon(imageVector = Icons.Filled.Search, contentDescription = null)
                },
                trailingIcon = {
                    if (data.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(imageVector = Icons.Filled.Close, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("transactions_search_input")
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TransactionFilterType.entries.forEach { type ->
                        FilterChip(
                            selected = data.filterType == type,
                            onClick = { onFilterTypeChange(type) },
                            label = { Text(type.label) },
                            modifier = Modifier.testTag("filter_type_${type.name}")
                        )
                    }
                    if (monthCategories.isNotEmpty()) {
                        monthCategories.forEach { cat ->
                            FilterChip(
                                selected = data.filterCategory.equals(cat, ignoreCase = true),
                                onClick = { onFilterCategoryToggle(cat) },
                                label = { Text(cat) }
                            )
                        }
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    border = BorderStroke(1.dp, VerdantThemeTokens.colors.cardBorder),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${data.filteredTransactions.size} transaction${if (data.filteredTransactions.size == 1) "" else "s"}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (hasActiveFilter) {
                                TextButton(
                                    onClick = onClearFilters,
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                ) {
                                    Text("Reset", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                            Text(
                                text = formatCurrency(filteredNet, data.currency, showPlusSign = filteredNet > 0),
                                style = MaterialTheme.typography.titleSmall.tabular().copy(fontWeight = FontWeight.Bold),
                                color = if (filteredNet >= 0) {
                                    VerdantThemeTokens.colors.income
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            )
                        }
                    }
                }
            }
        }

        if (grouped.isEmpty()) {
            item {
                Spacer(modifier = Modifier.height(6.dp))
                EmptyStateCard(
                    icon = Icons.AutoMirrored.Filled.ReceiptLong,
                    title = if (hasActiveFilter) "No matching transactions" else "No transactions yet",
                    subtitle = if (hasActiveFilter) {
                        "Try clearing your search or category filter."
                    } else {
                        "Record your first expense or income entry for ${data.selectedMonth.fullLabel()}."
                    },
                    actionLabel = if (hasActiveFilter) "Clear Filters" else "Record Transaction",
                    onAction = if (hasActiveFilter) onClearFilters else onAddTransaction
                )
            }
        } else {
            grouped.forEach { (dayLabel, itemsForDay) ->
                item(key = "header_$dayLabel") {
                    Text(
                        text = dayLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
                    )
                }
                items(
                    items = itemsForDay,
                    key = { "tx_${it.id}" }
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
}
