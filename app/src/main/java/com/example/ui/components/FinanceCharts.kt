package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.CurrencyOption
import com.example.ui.state.CategorySpendSlice
import com.example.ui.state.DailySpendPoint
import com.example.ui.state.MonthlyCashflowPoint
import com.example.ui.state.formatCurrency
import com.example.ui.theme.VerdantThemeTokens
import com.example.ui.theme.tabular
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun MonthlyCashflowBarChart(
    points: List<MonthlyCashflowPoint>,
    currency: CurrencyOption,
    modifier: Modifier = Modifier
) {
    if (points.isEmpty()) return

    var selectedIndex by remember(points.size) { mutableIntStateOf(points.lastIndex) }
    val selectedPoint = points.getOrElse(selectedIndex) { points.last() }

    val animProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "cashflowBarAnim"
    )

    val incomeColor = VerdantThemeTokens.colors.income
    val expenseColor = VerdantThemeTokens.colors.expense
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val highlightBg = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f)
    val maxVal = max(
        points.maxOfOrNull { max(it.incomeCents, it.expenseCents) } ?: 1L,
        100L
    ).toFloat()

    Column(modifier = modifier.fillMaxWidth()) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
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
                Column {
                    Text(
                        text = selectedPoint.monthKey.fullLabel(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val net = selectedPoint.incomeCents - selectedPoint.expenseCents
                    Text(
                        text = "Net ${formatCurrency(net, currency, showPlusSign = true)}",
                        style = MaterialTheme.typography.titleSmall.tabular(),
                        color = if (net >= 0) incomeColor else expenseColor
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    ChartLegendMetric(
                        label = "In",
                        value = formatCurrency(selectedPoint.incomeCents, currency),
                        color = incomeColor
                    )
                    ChartLegendMetric(
                        label = "Out",
                        value = formatCurrency(selectedPoint.expenseCents, currency),
                        color = expenseColor
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(168.dp)
                .pointerInput(points.size) {
                    detectTapGestures { offset ->
                        val groupWidth = size.width.toFloat() / points.size.coerceAtLeast(1)
                        val tapped = (offset.x / groupWidth).toInt().coerceIn(0, points.lastIndex)
                        selectedIndex = tapped
                    }
                }
        ) {
            val chartHeight = size.height
            val groupWidth = size.width / points.size.coerceAtLeast(1)
            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)

            listOf(0.25f, 0.5f, 0.75f, 1f).forEach { fraction ->
                val y = chartHeight * (1f - fraction)
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = dashEffect
                )
            }

            points.forEachIndexed { index, point ->
                val groupLeft = index * groupWidth
                if (index == selectedIndex) {
                    drawRoundRect(
                        color = highlightBg,
                        topLeft = Offset(groupLeft + 4.dp.toPx(), 0f),
                        size = Size(groupWidth - 8.dp.toPx(), chartHeight),
                        cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx())
                    )
                }

                val barWidth = (groupWidth * 0.24f).coerceIn(8.dp.toPx(), 18.dp.toPx())
                val gap = 4.dp.toPx()
                val centerX = groupLeft + groupWidth / 2f

                val incomeHeight = ((point.incomeCents / maxVal) * chartHeight * 0.92f * animProgress)
                    .coerceAtLeast(if (point.incomeCents > 0) 4.dp.toPx() else 0f)
                val expenseHeight = ((point.expenseCents / maxVal) * chartHeight * 0.92f * animProgress)
                    .coerceAtLeast(if (point.expenseCents > 0) 4.dp.toPx() else 0f)

                if (incomeHeight > 0f) {
                    drawRoundRect(
                        color = incomeColor,
                        topLeft = Offset(centerX - barWidth - gap / 2f, chartHeight - incomeHeight),
                        size = Size(barWidth, incomeHeight),
                        cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx())
                    )
                }

                if (expenseHeight > 0f) {
                    drawRoundRect(
                        color = expenseColor,
                        topLeft = Offset(centerX + gap / 2f, chartHeight - expenseHeight),
                        size = Size(barWidth, expenseHeight),
                        cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx())
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            points.forEachIndexed { index, point ->
                val isSelected = index == selectedIndex
                Text(
                    text = point.label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    ),
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { selectedIndex = index }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun ChartLegendMetric(
    label: String,
    value: String,
    color: Color
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge.tabular(),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun CategoryDonutChart(
    slices: List<CategorySpendSlice>,
    currency: CurrencyOption,
    modifier: Modifier = Modifier
) {
    if (slices.isEmpty()) return

    var selectedIndex by remember(slices.size) { mutableIntStateOf(0) }
    val safeIndex = selectedIndex.coerceIn(0, slices.lastIndex)
    val selectedSlice = slices[safeIndex]
    val palette = VerdantThemeTokens.colors.chartPalette
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val totalExpenseCents = slices.sumOf { it.amountCents }

    val animSweep by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "donutAnim"
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .size(188.dp)
                    .pointerInput(slices) {
                        detectTapGestures { offset ->
                            val cx = size.width / 2f
                            val cy = size.height / 2f
                            val dx = offset.x - cx
                            val dy = offset.y - cy
                            val dist = hypot(dx, dy)
                            val outerR = size.width / 2f
                            val innerR = outerR * 0.52f
                            if (dist in innerR..outerR) {
                                var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                                angle = (angle + 90f + 360f) % 360f
                                var accum = 0f
                                slices.forEachIndexed { idx, slice ->
                                    val sweep = slice.percentage * 360f
                                    if (angle >= accum && angle <= accum + sweep) {
                                        selectedIndex = idx
                                        return@detectTapGestures
                                    }
                                    accum += sweep
                                }
                            }
                        }
                    }
            ) {
                val strokeWidth = 28.dp.toPx()
                drawArc(
                    color = trackColor,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = strokeWidth)
                )

                var startAngle = -90f
                val gapDegrees = if (slices.size > 1) 2.5f else 0f
                slices.forEachIndexed { index, slice ->
                    val rawSweep = slice.percentage * 360f * animSweep
                    val sweep = (rawSweep - gapDegrees).coerceAtLeast(1f)
                    val sliceColor = palette[index % palette.size]
                    val activeStroke = if (index == safeIndex) strokeWidth + 5.dp.toPx() else strokeWidth
                    drawArc(
                        color = sliceColor,
                        startAngle = startAngle,
                        sweepAngle = sweep,
                        useCenter = false,
                        style = Stroke(width = activeStroke, cap = StrokeCap.Butt)
                    )
                    startAngle += rawSweep
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = selectedSlice.category,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = formatCurrency(selectedSlice.amountCents, currency),
                    style = MaterialTheme.typography.titleMedium.tabular().copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${(selectedSlice.percentage * 100f).roundToInt()}% of ${formatCurrency(totalExpenseCents, currency)}",
                    style = MaterialTheme.typography.labelSmall.tabular(),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            slices.forEachIndexed { index, slice ->
                val color = palette[index % palette.size]
                val isSelected = index == safeIndex
                Surface(
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                    } else {
                        Color.Transparent
                    },
                    border = if (isSelected) {
                        BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                    } else {
                        null
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { selectedIndex = index }
                        .testTag("donut_legend_$index")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(color)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = slice.category,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "(${slice.count})",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${(slice.percentage * 100f).roundToInt()}%",
                                style = MaterialTheme.typography.labelMedium.tabular(),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = formatCurrency(slice.amountCents, currency),
                                style = MaterialTheme.typography.titleSmall.tabular(),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DailySpendingVelocityChart(
    points: List<DailySpendPoint>,
    currency: CurrencyOption,
    modifier: Modifier = Modifier
) {
    if (points.isEmpty()) return

    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val maxExpense = max(points.maxOfOrNull { it.cumulativeExpenseCents } ?: 1L, 100L).toFloat()
    val latestSpend = points.last().cumulativeExpenseCents

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Cumulative Spend",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = formatCurrency(latestSpend, currency),
                style = MaterialTheme.typography.titleMedium.tabular().copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
        ) {
            val w = size.width
            val h = size.height
            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
            listOf(0.33f, 0.66f, 1f).forEach { f ->
                val y = h * (1f - f)
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(w, y),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = dashEffect
                )
            }

            if (points.size == 1) {
                val y = h - (points.first().cumulativeExpenseCents / maxExpense) * h * 0.9f
                drawCircle(color = lineColor, radius = 5.dp.toPx(), center = Offset(w / 2f, y))
                return@Canvas
            }

            val stepX = w / (points.size - 1).coerceAtLeast(1)
            val linePath = Path()
            val fillPath = Path()

            points.forEachIndexed { idx, pt ->
                val x = idx * stepX
                val y = (h - (pt.cumulativeExpenseCents / maxExpense) * h * 0.88f).coerceIn(4.dp.toPx(), h - 2.dp.toPx())
                if (idx == 0) {
                    linePath.moveTo(x, y)
                    fillPath.moveTo(x, h)
                    fillPath.lineTo(x, y)
                } else {
                    linePath.lineTo(x, y)
                    fillPath.lineTo(x, y)
                }
            }
            fillPath.lineTo((points.size - 1) * stepX, h)
            fillPath.close()

            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(lineColor.copy(alpha = 0.28f), lineColor.copy(alpha = 0.02f))
                )
            )
            drawPath(
                path = linePath,
                color = lineColor,
                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
            )

            val lastX = (points.size - 1) * stepX
            val lastY = (h - (latestSpend / maxExpense) * h * 0.88f).coerceIn(4.dp.toPx(), h - 2.dp.toPx())
            drawCircle(color = lineColor, radius = 4.5.dp.toPx(), center = Offset(lastX, lastY))
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Day ${points.first().day}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Day ${points.last().day}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
