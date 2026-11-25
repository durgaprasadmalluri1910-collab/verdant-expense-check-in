package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.CurrencyOption
import com.example.data.FinanceCategory
import com.example.data.RecurringBill
import com.example.data.TransactionRecord
import com.example.data.TransactionType
import com.example.ui.state.BudgetAlertLevel
import com.example.ui.state.BudgetProgress
import com.example.ui.state.DueUrgency
import com.example.ui.state.MonthKey
import com.example.ui.state.formatCurrency
import com.example.ui.state.formatDueStatus
import com.example.ui.state.formatShortDate
import com.example.ui.theme.VerdantThemeTokens
import com.example.ui.theme.tabular
import com.google.firebase.Timestamp
import kotlin.math.roundToInt

@Composable
fun MonthSelectorBar(
    selectedMonth: MonthKey,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onResetCurrentMonth: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, VerdantThemeTokens.colors.cardBorder),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = onPreviousMonth,
                modifier = Modifier.testTag("prev_month_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "Previous month"
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onResetCurrentMonth)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .testTag("current_month_label")
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = selectedMonth.fullLabel(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            IconButton(
                onClick = onNextMonth,
                modifier = Modifier.testTag("next_month_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Next month"
                )
            }
        }
    }
}

@Composable
fun TransactionRowCard(
    transaction: TransactionRecord,
    currency: CurrencyOption,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isIncome = transaction.transactionType == TransactionType.INCOME
    val amountColor = if (isIncome) VerdantThemeTokens.colors.income else MaterialTheme.colorScheme.onSurface
    val badgeBg = if (isIncome) {
        VerdantThemeTokens.colors.incomeContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val badgeTint = if (isIncome) {
        VerdantThemeTokens.colors.income
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val displayTs = transaction.occurredAt ?: transaction.createdAt ?: Timestamp.now()

    Surface(
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, VerdantThemeTokens.colors.cardBorder),
        shape = RoundedCornerShape(18.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .testTag("transaction_card_${transaction.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = badgeBg,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = FinanceCategory.iconFor(transaction.category),
                        contentDescription = null,
                        tint = badgeTint,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                val subtitle = buildString {
                    append(transaction.category)
                    append(" • ")
                    append(formatShortDate(displayTs))
                    if (!transaction.note.isNullOrBlank()) {
                        append(" • ")
                        append(transaction.note)
                    }
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            val signedCents = if (isIncome) transaction.amountCents else -transaction.amountCents
            Text(
                text = formatCurrency(signedCents, currency, showPlusSign = isIncome),
                style = MaterialTheme.typography.titleMedium.tabular().copy(fontWeight = FontWeight.Bold),
                color = amountColor
            )
        }
    }
}

@Composable
fun BudgetProgressCard(
    progress: BudgetProgress,
    currency: CurrencyOption,
    onEdit: () -> Unit,
    onDelete: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val targetColor = when (progress.alertLevel) {
        BudgetAlertLevel.EXCEEDED -> VerdantThemeTokens.colors.expense
        BudgetAlertLevel.WARNING -> VerdantThemeTokens.colors.warning
        BudgetAlertLevel.NORMAL -> VerdantThemeTokens.colors.income
    }
    val barColor by animateColorAsState(targetValue = targetColor, label = "budgetBarColor")
    val clampedProgress by animateFloatAsState(
        targetValue = progress.ratio.coerceIn(0f, 1f),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "budgetProgressAnim"
    )

    Surface(
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (progress.alertLevel == BudgetAlertLevel.EXCEEDED) {
                VerdantThemeTokens.colors.expense.copy(alpha = 0.45f)
            } else {
                VerdantThemeTokens.colors.cardBorder
            }
        ),
        shape = RoundedCornerShape(20.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("budget_card_${progress.budget.category}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        color = barColor.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = FinanceCategory.iconFor(progress.budget.category),
                                contentDescription = null,
                                tint = barColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = progress.budget.category,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${formatCurrency(progress.spentCents, currency)} of ${formatCurrency(progress.budget.limitCents, currency)}",
                            style = MaterialTheme.typography.bodySmall.tabular(),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (progress.alertLevel != BudgetAlertLevel.NORMAL) {
                        Surface(
                            color = if (progress.alertLevel == BudgetAlertLevel.EXCEEDED) {
                                VerdantThemeTokens.colors.expenseContainer
                            } else {
                                VerdantThemeTokens.colors.warningContainer
                            },
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.WarningAmber,
                                    contentDescription = null,
                                    tint = barColor,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (progress.alertLevel == BudgetAlertLevel.EXCEEDED) "Over" else "${(progress.ratio * 100).roundToInt()}%",
                                    style = MaterialTheme.typography.labelSmall.tabular(),
                                    color = barColor
                                )
                            }
                        }
                    }
                    IconButton(onClick = onEdit) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "Edit ${progress.budget.category} budget",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    if (onDelete != null) {
                        IconButton(onClick = onDelete) {
                            Icon(
                                imageVector = Icons.Filled.DeleteOutline,
                                contentDescription = "Delete ${progress.budget.category} budget",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = { clampedProgress },
                color = barColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val statusText = if (progress.remainingCents >= 0) {
                    "${formatCurrency(progress.remainingCents, currency)} remaining"
                } else {
                    "Over by ${formatCurrency(-progress.remainingCents, currency)}"
                }
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelMedium.tabular(),
                    color = if (progress.remainingCents >= 0) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        VerdantThemeTokens.colors.expense
                    }
                )
                Text(
                    text = "${(progress.ratio * 100f).roundToInt()}% used",
                    style = MaterialTheme.typography.labelMedium.tabular(),
                    color = barColor
                )
            }
        }
    }
}

@Composable
fun RecurringBillCard(
    bill: RecurringBill,
    currency: CurrencyOption,
    onPayNow: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dueTs = bill.nextDueAt ?: Timestamp.now()
    val (dueText, urgency) = formatDueStatus(dueTs)
    val urgencyColor = when (urgency) {
        DueUrgency.OVERDUE -> VerdantThemeTokens.colors.expense
        DueUrgency.DUE_SOON -> VerdantThemeTokens.colors.warning
        DueUrgency.UPCOMING -> MaterialTheme.colorScheme.primary
    }
    val urgencyBg = when (urgency) {
        DueUrgency.OVERDUE -> VerdantThemeTokens.colors.expenseContainer
        DueUrgency.DUE_SOON -> VerdantThemeTokens.colors.warningContainer
        DueUrgency.UPCOMING -> MaterialTheme.colorScheme.primaryContainer
    }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, VerdantThemeTokens.colors.cardBorder),
        shape = RoundedCornerShape(20.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onEdit)
            .testTag("bill_card_${bill.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = FinanceCategory.iconFor(bill.category),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = bill.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "${bill.billCadence.label} • ${bill.category}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (bill.autoPay) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "AutoPay",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = formatCurrency(bill.amountCents, currency),
                    style = MaterialTheme.typography.titleMedium.tabular().copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    color = urgencyBg,
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        text = if (bill.active) dueText else "Paused",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (bill.active) urgencyColor else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onEdit,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Edit", style = MaterialTheme.typography.labelMedium)
                    }
                    if (bill.active) {
                        Button(
                            onClick = onPayNow,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier.testTag("pay_bill_${bill.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Log Payment", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyStateCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, VerdantThemeTokens.colors.cardBorder),
        shape = RoundedCornerShape(22.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                shape = CircleShape,
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (actionLabel != null && onAction != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onAction,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(actionLabel)
                }
            }
        }
    }
}
