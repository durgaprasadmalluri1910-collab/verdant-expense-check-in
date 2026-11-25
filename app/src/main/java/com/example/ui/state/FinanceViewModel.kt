package com.example.ui.state

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.BillCadence
import com.example.data.CategoryBudget
import com.example.data.CurrencyOption
import com.example.data.FinanceRepository
import com.example.data.RecurringBill
import com.example.data.TransactionRecord
import com.example.data.TransactionType
import com.example.data.UserPreferences
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class TransactionFilterType(val label: String) {
    ALL("All"),
    EXPENSE("Expenses"),
    INCOME("Income")
}

enum class BudgetAlertLevel {
    NORMAL,
    WARNING,
    EXCEEDED
}

data class BudgetProgress(
    val budget: CategoryBudget,
    val spentCents: Long,
    val remainingCents: Long,
    val ratio: Float,
    val alertLevel: BudgetAlertLevel
)

data class MonthlyCashflowPoint(
    val monthKey: MonthKey,
    val label: String,
    val incomeCents: Long,
    val expenseCents: Long
)

data class CategorySpendSlice(
    val category: String,
    val amountCents: Long,
    val percentage: Float,
    val count: Int
)

data class DailySpendPoint(
    val day: Int,
    val cumulativeExpenseCents: Long
)

data class FinanceDashboardData(
    val currency: CurrencyOption,
    val selectedMonth: MonthKey,
    val searchQuery: String,
    val filterType: TransactionFilterType,
    val filterCategory: String?,
    val allTransactions: List<TransactionRecord>,
    val monthTransactions: List<TransactionRecord>,
    val filteredTransactions: List<TransactionRecord>,
    val allTimeNetCents: Long,
    val monthIncomeCents: Long,
    val monthExpenseCents: Long,
    val monthNetCents: Long,
    val budgetProgresses: List<BudgetProgress>,
    val alertBudgets: List<BudgetProgress>,
    val recurringBills: List<RecurringBill>,
    val upcomingBills: List<RecurringBill>,
    val monthlyRecurringTotalCents: Long,
    val cashflowTrend: List<MonthlyCashflowPoint>,
    val categorySlices: List<CategorySpendSlice>,
    val dailyVelocity: List<DailySpendPoint>
)

private data class FilterInputs(
    val selectedMonth: MonthKey,
    val searchQuery: String,
    val filterType: TransactionFilterType,
    val filterCategory: String?
)

private data class FirestoreStreams(
    val transactions: List<TransactionRecord>,
    val budgets: List<CategoryBudget>,
    val bills: List<RecurringBill>,
    val prefs: UserPreferences?
)

class FinanceViewModel(
    private val repository: FinanceRepository,
    private val currentUserId: String
) : ViewModel() {

    private val selectedMonth = MutableStateFlow(MonthKey.current())
    private val searchQuery = MutableStateFlow("")
    private val filterType = MutableStateFlow(TransactionFilterType.ALL)
    private val filterCategory = MutableStateFlow<String?>(null)

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val events: SharedFlow<String> = _events.asSharedFlow()

    private val firestoreStreams = combine(
        repository.observeTransactions(currentUserId),
        repository.observeBudgets(currentUserId),
        repository.observeRecurringBills(currentUserId),
        repository.observePreferences(currentUserId)
    ) { transactions, budgets, bills, prefs ->
        FirestoreStreams(transactions, budgets, bills, prefs)
    }

    private val filterInputs = combine(
        selectedMonth,
        searchQuery,
        filterType,
        filterCategory
    ) { month, query, type, category ->
        FilterInputs(month, query, type, category)
    }

    val uiState: StateFlow<UiState<FinanceDashboardData>> = combine(
        firestoreStreams,
        filterInputs
    ) { streams, filters ->
        UiState.Success(buildDashboard(streams, filters)) as UiState<FinanceDashboardData>
    }.catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val rootError = firestoreError ?: error
        Log.w(TAG, "Error observing finance streams", rootError)
        emit(UiState.Error(rootError.localizedMessage ?: "Unable to sync financial records."))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading
    )

    fun shiftSelectedMonth(delta: Int) {
        selectedMonth.value = selectedMonth.value.shift(delta)
    }

    fun resetToCurrentMonth() {
        selectedMonth.value = MonthKey.current()
    }

    fun setSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun setFilterType(type: TransactionFilterType) {
        filterType.value = type
    }

    fun setFilterCategory(category: String?) {
        filterCategory.value = if (filterCategory.value == category) null else category
    }

    fun clearFilters() {
        searchQuery.value = ""
        filterType.value = TransactionFilterType.ALL
        filterCategory.value = null
    }

    fun saveTransaction(
        transactionId: String?,
        type: TransactionType,
        amountCents: Long,
        title: String,
        category: String,
        note: String?,
        occurredAt: Timestamp,
        onComplete: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            val result = repository.saveTransaction(
                userId = currentUserId,
                transactionId = transactionId,
                type = type,
                amountCents = amountCents,
                title = title,
                category = category,
                note = note,
                occurredAt = occurredAt
            )
            if (result.isSuccess) {
                _events.tryEmit(if (transactionId == null) "Transaction recorded" else "Transaction updated")
                onComplete(true)
            } else {
                _events.tryEmit(result.exceptionOrNull()?.localizedMessage ?: "Could not save transaction")
                onComplete(false)
            }
        }
    }

    fun deleteTransaction(transactionId: String, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val result = repository.deleteTransaction(currentUserId, transactionId)
            if (result.isSuccess) {
                _events.tryEmit("Transaction deleted")
                onComplete(true)
            } else {
                _events.tryEmit(result.exceptionOrNull()?.localizedMessage ?: "Could not delete transaction")
                onComplete(false)
            }
        }
    }

    fun saveBudget(
        budgetId: String?,
        category: String,
        limitCents: Long,
        onComplete: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            val result = repository.saveBudget(currentUserId, budgetId, category, limitCents)
            if (result.isSuccess) {
                _events.tryEmit(if (budgetId == null) "Monthly budget added" else "Budget updated")
                onComplete(true)
            } else {
                _events.tryEmit(result.exceptionOrNull()?.localizedMessage ?: "Could not save budget")
                onComplete(false)
            }
        }
    }

    fun deleteBudget(budgetId: String) {
        viewModelScope.launch {
            val result = repository.deleteBudget(currentUserId, budgetId)
            _events.tryEmit(
                if (result.isSuccess) "Budget removed"
                else result.exceptionOrNull()?.localizedMessage ?: "Could not remove budget"
            )
        }
    }

    fun saveRecurringBill(
        billId: String?,
        title: String,
        amountCents: Long,
        category: String,
        cadence: BillCadence,
        nextDueAt: Timestamp,
        autoPay: Boolean,
        active: Boolean,
        onComplete: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            val result = repository.saveRecurringBill(
                userId = currentUserId,
                billId = billId,
                title = title,
                amountCents = amountCents,
                category = category,
                cadence = cadence,
                nextDueAt = nextDueAt,
                autoPay = autoPay,
                active = active
            )
            if (result.isSuccess) {
                _events.tryEmit(if (billId == null) "Recurring bill added" else "Recurring bill updated")
                onComplete(true)
            } else {
                _events.tryEmit(result.exceptionOrNull()?.localizedMessage ?: "Could not save bill")
                onComplete(false)
            }
        }
    }

    fun deleteRecurringBill(billId: String, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val result = repository.deleteRecurringBill(currentUserId, billId)
            if (result.isSuccess) {
                _events.tryEmit("Recurring bill deleted")
                onComplete(true)
            } else {
                _events.tryEmit(result.exceptionOrNull()?.localizedMessage ?: "Could not delete bill")
                onComplete(false)
            }
        }
    }

    fun payRecurringBill(bill: RecurringBill) {
        viewModelScope.launch {
            val result = repository.payRecurringBill(currentUserId, bill)
            _events.tryEmit(
                if (result.isSuccess) "Logged payment for ${bill.title}"
                else result.exceptionOrNull()?.localizedMessage ?: "Could not log bill payment"
            )
        }
    }

    fun updateCurrency(currencyOption: CurrencyOption) {
        viewModelScope.launch {
            val result = repository.updateCurrency(currentUserId, currencyOption)
            _events.tryEmit(
                if (result.isSuccess) "Currency switched to ${currencyOption.code}"
                else result.exceptionOrNull()?.localizedMessage ?: "Could not update currency"
            )
        }
    }

    private fun buildDashboard(
        streams: FirestoreStreams,
        filters: FilterInputs
    ): FinanceDashboardData {
        val currency = streams.prefs?.currencyOption ?: CurrencyOption.USD
        val allTx = streams.transactions

        val allTimeIncome = allTx
            .filter { it.transactionType == TransactionType.INCOME }
            .sumOf { it.amountCents }
        val allTimeExpense = allTx
            .filter { it.transactionType == TransactionType.EXPENSE }
            .sumOf { it.amountCents }

        val monthTx = allTx.filter { tx ->
            val ts = tx.occurredAt ?: tx.createdAt ?: Timestamp.now()
            MonthKey.fromTimestamp(ts) == filters.selectedMonth
        }

        val monthIncome = monthTx
            .filter { it.transactionType == TransactionType.INCOME }
            .sumOf { it.amountCents }
        val monthExpense = monthTx
            .filter { it.transactionType == TransactionType.EXPENSE }
            .sumOf { it.amountCents }

        val queryLower = filters.searchQuery.trim().lowercase()
        val filteredTx = monthTx.filter { tx ->
            val matchesType = when (filters.filterType) {
                TransactionFilterType.ALL -> true
                TransactionFilterType.EXPENSE -> tx.transactionType == TransactionType.EXPENSE
                TransactionFilterType.INCOME -> tx.transactionType == TransactionType.INCOME
            }
            val matchesCategory = filters.filterCategory == null ||
                tx.category.equals(filters.filterCategory, ignoreCase = true)
            val matchesQuery = queryLower.isEmpty() ||
                tx.title.lowercase().contains(queryLower) ||
                tx.category.lowercase().contains(queryLower) ||
                (tx.note?.lowercase()?.contains(queryLower) == true)
            matchesType && matchesCategory && matchesQuery
        }

        val expenseByCategory = monthTx
            .filter { it.transactionType == TransactionType.EXPENSE }
            .groupBy { it.category }

        val budgetProgresses = streams.budgets.map { budget ->
            val spent = expenseByCategory[budget.category]?.sumOf { it.amountCents } ?: 0L
            val remaining = budget.limitCents - spent
            val ratio = if (budget.limitCents > 0L) {
                spent.toFloat() / budget.limitCents.toFloat()
            } else {
                0f
            }
            val alertLevel = when {
                ratio >= 1.0f -> BudgetAlertLevel.EXCEEDED
                ratio >= 0.8f -> BudgetAlertLevel.WARNING
                else -> BudgetAlertLevel.NORMAL
            }
            BudgetProgress(
                budget = budget,
                spentCents = spent,
                remainingCents = remaining,
                ratio = ratio,
                alertLevel = alertLevel
            )
        }.sortedByDescending { it.ratio }

        val alertBudgets = budgetProgresses.filter { it.alertLevel != BudgetAlertLevel.NORMAL }

        val upcomingThresholdMs = System.currentTimeMillis() + 14L * 24L * 60L * 60L * 1000L
        val upcomingBills = streams.bills.filter { bill ->
            bill.active && (bill.nextDueAt?.toDate()?.time ?: Long.MAX_VALUE) <= upcomingThresholdMs
        }

        val monthlyRecurringTotalCents = streams.bills
            .filter { it.active }
            .sumOf { bill ->
                when (bill.billCadence) {
                    BillCadence.WEEKLY -> (bill.amountCents * 52L) / 12L
                    BillCadence.MONTHLY -> bill.amountCents
                    BillCadence.YEARLY -> bill.amountCents / 12L
                }
            }

        val trendMonths = (-5..0).map { offset -> filters.selectedMonth.shift(offset) }
        val txByMonth = allTx.groupBy { tx ->
            val ts = tx.occurredAt ?: tx.createdAt ?: Timestamp.now()
            MonthKey.fromTimestamp(ts)
        }
        val cashflowTrend = trendMonths.map { monthKey ->
            val list = txByMonth[monthKey].orEmpty()
            MonthlyCashflowPoint(
                monthKey = monthKey,
                label = monthKey.shortLabel(),
                incomeCents = list.filter { it.transactionType == TransactionType.INCOME }.sumOf { it.amountCents },
                expenseCents = list.filter { it.transactionType == TransactionType.EXPENSE }.sumOf { it.amountCents }
            )
        }

        val categorySlices = expenseByCategory.entries
            .map { (category, items) ->
                val total = items.sumOf { it.amountCents }
                val pct = if (monthExpense > 0L) (total.toFloat() / monthExpense.toFloat()) else 0f
                CategorySpendSlice(
                    category = category,
                    amountCents = total,
                    percentage = pct,
                    count = items.size
                )
            }
            .sortedByDescending { it.amountCents }

        val totalDays = filters.selectedMonth.daysInMonth()
        val dailyMap = monthTx
            .filter { it.transactionType == TransactionType.EXPENSE }
            .groupBy { tx ->
                val ts = tx.occurredAt ?: tx.createdAt ?: Timestamp.now()
                dayOfMonth(ts).coerceIn(1, totalDays)
            }
        var runningCents = 0L
        val currentMonth = MonthKey.current()
        val maxDayToPlot = if (filters.selectedMonth == currentMonth) {
            dayOfMonth(Timestamp.now()).coerceIn(1, totalDays)
        } else {
            totalDays
        }
        val dailyVelocity = (1..maxDayToPlot).map { day ->
            runningCents += dailyMap[day]?.sumOf { it.amountCents } ?: 0L
            DailySpendPoint(day = day, cumulativeExpenseCents = runningCents)
        }

        return FinanceDashboardData(
            currency = currency,
            selectedMonth = filters.selectedMonth,
            searchQuery = filters.searchQuery,
            filterType = filters.filterType,
            filterCategory = filters.filterCategory,
            allTransactions = allTx,
            monthTransactions = monthTx,
            filteredTransactions = filteredTx,
            allTimeNetCents = allTimeIncome - allTimeExpense,
            monthIncomeCents = monthIncome,
            monthExpenseCents = monthExpense,
            monthNetCents = monthIncome - monthExpense,
            budgetProgresses = budgetProgresses,
            alertBudgets = alertBudgets,
            recurringBills = streams.bills,
            upcomingBills = upcomingBills,
            monthlyRecurringTotalCents = monthlyRecurringTotalCents,
            cashflowTrend = cashflowTrend,
            categorySlices = categorySlices,
            dailyVelocity = dailyVelocity
        )
    }

    private companion object {
        const val TAG = "FinanceVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
