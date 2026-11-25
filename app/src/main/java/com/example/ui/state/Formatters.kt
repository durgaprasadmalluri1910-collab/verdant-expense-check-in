package com.example.ui.state

import com.example.data.CurrencyOption
import com.google.firebase.Timestamp
import java.text.DecimalFormat
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

data class MonthKey(val year: Int, val monthOneBased: Int) : Comparable<MonthKey> {
    override fun compareTo(other: MonthKey): Int =
        if (year != other.year) year.compareTo(other.year) else monthOneBased.compareTo(other.monthOneBased)

    fun shift(deltaMonths: Int): MonthKey {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, monthOneBased - 1)
            set(Calendar.DAY_OF_MONTH, 1)
            add(Calendar.MONTH, deltaMonths)
        }
        return MonthKey(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
    }

    fun shortLabel(): String {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, monthOneBased - 1)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        return SimpleDateFormat("MMM", Locale.getDefault()).format(cal.time)
    }

    fun fullLabel(): String {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, monthOneBased - 1)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        return SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.time)
    }

    fun daysInMonth(): Int {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, monthOneBased - 1)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        return cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    companion object {
        fun current(): MonthKey = fromDate(Date())

        fun fromTimestamp(timestamp: Timestamp): MonthKey = fromDate(timestamp.toDate())

        fun fromDate(date: Date): MonthKey {
            val cal = Calendar.getInstance().apply { time = date }
            return MonthKey(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
        }
    }
}

enum class DueUrgency {
    OVERDUE,
    DUE_SOON,
    UPCOMING
}

fun formatCurrency(
    amountCents: Long,
    currency: CurrencyOption,
    showPlusSign: Boolean = false
): String {
    val isNegative = amountCents < 0
    val absCents = abs(amountCents)
    val prefix = when {
        isNegative -> "-"
        showPlusSign && amountCents > 0 -> "+"
        else -> ""
    }
    val formatter = NumberFormat.getNumberInstance(Locale.getDefault()) as DecimalFormat
    if (currency.decimals == 0) {
        formatter.maximumFractionDigits = 0
        formatter.minimumFractionDigits = 0
        val major = absCents / 100
        return "$prefix${currency.symbol}${formatter.format(major)}"
    } else {
        formatter.maximumFractionDigits = 2
        formatter.minimumFractionDigits = 2
        val value = absCents / 100.0
        return "$prefix${currency.symbol}${formatter.format(value)}"
    }
}

fun centsToEditableString(amountCents: Long, currency: CurrencyOption): String {
    if (amountCents <= 0L) return ""
    return if (currency.decimals == 0) {
        (amountCents / 100L).toString()
    } else {
        val major = amountCents / 100L
        val minor = amountCents % 100L
        if (minor == 0L) major.toString() else String.format(Locale.US, "%d.%02d", major, minor)
    }
}

fun parseCurrencyInputToCents(raw: String, currency: CurrencyOption): Long? {
    val cleaned = raw.trim().replace(",", "")
    if (cleaned.isEmpty()) return null
    val parsed = cleaned.toDoubleOrNull() ?: return null
    if (parsed <= 0.0 || parsed > 1_000_000_000.0) return null
    return if (currency.decimals == 0) {
        kotlin.math.round(parsed).toLong() * 100L
    } else {
        kotlin.math.round(parsed * 100.0).toLong()
    }
}

fun formatTransactionDayHeader(timestamp: Timestamp): String {
    val date = timestamp.toDate()
    val txCal = Calendar.getInstance().apply { time = date }
    val today = Calendar.getInstance()
    val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }

    return when {
        sameDay(txCal, today) -> "Today"
        sameDay(txCal, yesterday) -> "Yesterday"
        else -> SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(date)
    }
}

fun formatShortDate(timestamp: Timestamp): String =
    SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(timestamp.toDate())

fun formatMonthDay(timestamp: Timestamp): String =
    SimpleDateFormat("MMM d", Locale.getDefault()).format(timestamp.toDate())

fun formatDueStatus(timestamp: Timestamp): Pair<String, DueUrgency> {
    val nowCal = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val dueCal = Calendar.getInstance().apply {
        time = timestamp.toDate()
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val diffDays = ((dueCal.timeInMillis - nowCal.timeInMillis) / (24L * 60L * 60L * 1000L)).toInt()
    return when {
        diffDays < 0 -> "Overdue by ${abs(diffDays)}d" to DueUrgency.OVERDUE
        diffDays == 0 -> "Due today" to DueUrgency.DUE_SOON
        diffDays == 1 -> "Due tomorrow" to DueUrgency.DUE_SOON
        diffDays <= 7 -> "Due in ${diffDays}d" to DueUrgency.DUE_SOON
        else -> "Due ${formatMonthDay(timestamp)}" to DueUrgency.UPCOMING
    }
}

fun dayOfMonth(timestamp: Timestamp): Int {
    val cal = Calendar.getInstance().apply { time = timestamp.toDate() }
    return cal.get(Calendar.DAY_OF_MONTH)
}

private fun sameDay(a: Calendar, b: Calendar): Boolean =
    a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
        a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)
