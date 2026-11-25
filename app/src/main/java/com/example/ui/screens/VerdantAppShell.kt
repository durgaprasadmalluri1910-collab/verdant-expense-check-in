package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.auth.signOut
import com.example.data.CategoryBudget
import com.example.data.CurrencyOption
import com.example.data.RecurringBill
import com.example.data.TransactionRecord
import com.example.ui.components.BudgetEditorSheet
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ProfileSettingsSheet
import com.example.ui.components.RecurringBillEditorSheet
import com.example.ui.components.TransactionEditorSheet
import com.example.ui.state.FinanceDashboardData
import com.example.ui.state.FinanceViewModel
import com.example.ui.state.UiState
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

enum class VerdantTab(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    OVERVIEW("Overview", Icons.Filled.Dashboard, Icons.Outlined.Dashboard),
    TRANSACTIONS("Activity", Icons.AutoMirrored.Filled.ReceiptLong, Icons.AutoMirrored.Outlined.ReceiptLong),
    ANALYTICS("Insights", Icons.Filled.Analytics, Icons.Outlined.Analytics),
    PLAN("Plan", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerdantAppShell(
    viewModel: FinanceViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var activeTab by rememberSaveable { mutableStateOf(VerdantTab.OVERVIEW) }
    var showTransactionSheet by remember { mutableStateOf(false) }
    var editingTransaction by remember { mutableStateOf<TransactionRecord?>(null) }
    var showBudgetSheet by remember { mutableStateOf(false) }
    var editingBudget by remember { mutableStateOf<CategoryBudget?>(null) }
    var showBillSheet by remember { mutableStateOf(false) }
    var editingBill by remember { mutableStateOf<RecurringBill?>(null) }
    var showProfileSheet by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    val currentUser = Firebase.auth.currentUser
    val currentData = (uiState as? UiState.Success<FinanceDashboardData>)?.data

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AccountBalanceWallet,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { showProfileSheet = true },
                            modifier = Modifier.testTag("profile_settings_button")
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = CircleShape,
                                modifier = Modifier.size(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    val initial = currentUser?.displayName?.firstOrNull()?.uppercaseChar()
                                        ?: currentUser?.email?.firstOrNull()?.uppercaseChar()
                                    if (initial != null) {
                                        Text(
                                            text = initial.toString(),
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Filled.Person,
                                            contentDescription = "Account and preferences",
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            },
            bottomBar = {
                if (!useRail) {
                    NavigationBar(
                        windowInsets = WindowInsets.navigationBars,
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        VerdantTab.entries.forEach { tab ->
                            val selected = activeTab == tab
                            NavigationBarItem(
                                selected = selected,
                                onClick = { activeTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag("nav_tab_${tab.name}")
                            )
                        }
                    }
                }
            },
            floatingActionButton = {
                if (currentData != null && activeTab != VerdantTab.ANALYTICS) {
                    FloatingActionButton(
                        onClick = {
                            editingTransaction = null
                            showTransactionSheet = true
                        },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.testTag("fab_add_transaction")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Record transaction"
                        )
                    }
                }
            },
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            containerColor = MaterialTheme.colorScheme.background
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        Spacer(modifier = Modifier.height(12.dp))
                        VerdantTab.entries.forEach { tab ->
                            val selected = activeTab == tab
                            NavigationRailItem(
                                selected = selected,
                                onClick = { activeTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag("rail_tab_${tab.name}")
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Box(modifier = Modifier.widthIn(max = 840.dp).fillMaxSize()) {
                        when (val state = uiState) {
                            UiState.Loading -> {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "Syncing your finances...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            is UiState.Error -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    EmptyStateCard(
                                        icon = Icons.Filled.AccountBalanceWallet,
                                        title = "Sync Interrupted",
                                        subtitle = state.message,
                                        actionLabel = "Account Settings",
                                        onAction = { showProfileSheet = true }
                                    )
                                }
                            }

                            is UiState.Success -> {
                                val data = state.data
                                when (activeTab) {
                                    VerdantTab.OVERVIEW -> OverviewScreen(
                                        data = data,
                                        onPreviousMonth = { viewModel.shiftSelectedMonth(-1) },
                                        onNextMonth = { viewModel.shiftSelectedMonth(1) },
                                        onResetCurrentMonth = { viewModel.resetToCurrentMonth() },
                                        onAddTransaction = {
                                            editingTransaction = null
                                            showTransactionSheet = true
                                        },
                                        onEditTransaction = { tx ->
                                            editingTransaction = tx
                                            showTransactionSheet = true
                                        },
                                        onEditBudget = { budget ->
                                            editingBudget = budget
                                            showBudgetSheet = true
                                        },
                                        onAddBudget = {
                                            editingBudget = null
                                            showBudgetSheet = true
                                        },
                                        onPayBill = { bill -> viewModel.payRecurringBill(bill) },
                                        onNavigateToTransactions = { activeTab = VerdantTab.TRANSACTIONS },
                                        onNavigateToPlan = { activeTab = VerdantTab.PLAN }
                                    )

                                    VerdantTab.TRANSACTIONS -> TransactionsScreen(
                                        data = data,
                                        onPreviousMonth = { viewModel.shiftSelectedMonth(-1) },
                                        onNextMonth = { viewModel.shiftSelectedMonth(1) },
                                        onResetCurrentMonth = { viewModel.resetToCurrentMonth() },
                                        onSearchChange = { viewModel.setSearchQuery(it) },
                                        onFilterTypeChange = { viewModel.setFilterType(it) },
                                        onFilterCategoryToggle = { viewModel.setFilterCategory(it) },
                                        onClearFilters = { viewModel.clearFilters() },
                                        onAddTransaction = {
                                            editingTransaction = null
                                            showTransactionSheet = true
                                        },
                                        onEditTransaction = { tx ->
                                            editingTransaction = tx
                                            showTransactionSheet = true
                                        }
                                    )

                                    VerdantTab.ANALYTICS -> AnalyticsScreen(
                                        data = data,
                                        onPreviousMonth = { viewModel.shiftSelectedMonth(-1) },
                                        onNextMonth = { viewModel.shiftSelectedMonth(1) },
                                        onResetCurrentMonth = { viewModel.resetToCurrentMonth() },
                                        onAddTransaction = {
                                            editingTransaction = null
                                            showTransactionSheet = true
                                        }
                                    )

                                    VerdantTab.PLAN -> PlanScreen(
                                        data = data,
                                        onPreviousMonth = { viewModel.shiftSelectedMonth(-1) },
                                        onNextMonth = { viewModel.shiftSelectedMonth(1) },
                                        onResetCurrentMonth = { viewModel.resetToCurrentMonth() },
                                        onAddBudget = {
                                            editingBudget = null
                                            showBudgetSheet = true
                                        },
                                        onEditBudget = { budget ->
                                            editingBudget = budget
                                            showBudgetSheet = true
                                        },
                                        onDeleteBudget = { id -> viewModel.deleteBudget(id) },
                                        onAddBill = {
                                            editingBill = null
                                            showBillSheet = true
                                        },
                                        onEditBill = { bill ->
                                            editingBill = bill
                                            showBillSheet = true
                                        },
                                        onPayBill = { bill -> viewModel.payRecurringBill(bill) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    val activeCurrency = currentData?.currency ?: CurrencyOption.USD

    if (showTransactionSheet) {
        TransactionEditorSheet(
            currency = activeCurrency,
            existing = editingTransaction,
            onDismiss = {
                showTransactionSheet = false
                editingTransaction = null
            },
            onSave = { id, type, amountCents, title, category, note, occurredAt ->
                viewModel.saveTransaction(
                    transactionId = id,
                    type = type,
                    amountCents = amountCents,
                    title = title,
                    category = category,
                    note = note,
                    occurredAt = occurredAt
                ) { ok ->
                    if (ok) {
                        showTransactionSheet = false
                        editingTransaction = null
                    }
                }
            },
            onDelete = { id ->
                viewModel.deleteTransaction(id) { ok ->
                    if (ok) {
                        showTransactionSheet = false
                        editingTransaction = null
                    }
                }
            }
        )
    }

    if (showBudgetSheet) {
        val usedCategories = currentData?.budgetProgresses
            ?.map { it.budget.category.lowercase() }
            ?.toSet()
            ?: emptySet()
        BudgetEditorSheet(
            currency = activeCurrency,
            existing = editingBudget,
            usedCategories = usedCategories,
            onDismiss = {
                showBudgetSheet = false
                editingBudget = null
            },
            onSave = { id, category, limitCents ->
                viewModel.saveBudget(id, category, limitCents) { ok ->
                    if (ok) {
                        showBudgetSheet = false
                        editingBudget = null
                    }
                }
            }
        )
    }

    if (showBillSheet) {
        RecurringBillEditorSheet(
            currency = activeCurrency,
            existing = editingBill,
            onDismiss = {
                showBillSheet = false
                editingBill = null
            },
            onSave = { id, title, amountCents, category, cadence, nextDueAt, autoPay, active ->
                viewModel.saveRecurringBill(
                    billId = id,
                    title = title,
                    amountCents = amountCents,
                    category = category,
                    cadence = cadence,
                    nextDueAt = nextDueAt,
                    autoPay = autoPay,
                    active = active
                ) { ok ->
                    if (ok) {
                        showBillSheet = false
                        editingBill = null
                    }
                }
            },
            onDelete = { id ->
                viewModel.deleteRecurringBill(id) { ok ->
                    if (ok) {
                        showBillSheet = false
                        editingBill = null
                    }
                }
            }
        )
    }

    if (showProfileSheet) {
        ProfileSettingsSheet(
            userEmail = currentUser?.email,
            userName = currentUser?.displayName,
            currentCurrency = activeCurrency,
            onSelectCurrency = { option -> viewModel.updateCurrency(option) },
            onSignOut = {
                showProfileSheet = false
                signOut(credentialManager = credentialManager, scope = scope)
            },
            onDismiss = { showProfileSheet = false }
        )
    }
}
