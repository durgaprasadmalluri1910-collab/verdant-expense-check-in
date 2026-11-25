package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.BillCadence
import com.example.data.CategoryBudget
import com.example.data.CurrencyOption
import com.example.data.FinanceCategory
import com.example.data.RecurringBill
import com.example.data.TransactionRecord
import com.example.data.TransactionType
import com.example.ui.state.centsToEditableString
import com.example.ui.state.formatShortDate
import com.example.ui.state.parseCurrencyInputToCents
import com.example.ui.theme.VerdantThemeTokens
import com.example.ui.theme.tabular
import com.google.firebase.Timestamp
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionEditorSheet(
    currency: CurrencyOption,
    existing: TransactionRecord?,
    onDismiss: () -> Unit,
    onSave: (
        transactionId: String?,
        type: TransactionType,
        amountCents: Long,
        title: String,
        category: String,
        note: String?,
        occurredAt: Timestamp
    ) -> Unit,
    onDelete: ((String) -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var txType by remember(existing) {
        mutableStateOf(existing?.transactionType ?: TransactionType.EXPENSE)
    }
    var amountText by remember(existing) {
        mutableStateOf(
            if (existing != null) centsToEditableString(existing.amountCents, currency) else ""
        )
    }
    var titleText by remember(existing) { mutableStateOf(existing?.title ?: "") }
    var category by remember(existing) {
        mutableStateOf(
            existing?.category ?: if (txType == TransactionType.EXPENSE) {
                FinanceCategory.GROCERIES.label
            } else {
                FinanceCategory.SALARY.label
            }
        )
    }
    var noteText by remember(existing) { mutableStateOf(existing?.note ?: "") }
    var occurredMillis by remember(existing) {
        mutableLongStateOf(existing?.occurredAt?.toDate()?.time ?: System.currentTimeMillis())
    }
    var showDatePicker by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    val availableCategories = if (txType == TransactionType.EXPENSE) {
        FinanceCategory.expenseCategories
    } else {
        FinanceCategory.incomeCategories
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = if (existing == null) "Record Transaction" else "Edit Transaction",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(14.dp))

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                TransactionType.entries.forEachIndexed { index, type ->
                    SegmentedButton(
                        selected = txType == type,
                        onClick = {
                            txType = type
                            val nextCats = if (type == TransactionType.EXPENSE) {
                                FinanceCategory.expenseCategories
                            } else {
                                FinanceCategory.incomeCategories
                            }
                            if (nextCats.none { it.label == category }) {
                                category = nextCats.first().label
                            }
                        },
                        shape = SegmentedButtonDefaults.itemShape(index, TransactionType.entries.size),
                        modifier = Modifier.testTag("tx_type_${type.name}")
                    ) {
                        Text(type.label)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = amountText,
                onValueChange = {
                    amountText = it
                    validationError = null
                },
                label = { Text("Amount (${currency.code})") },
                prefix = {
                    Text(
                        text = "${currency.symbol} ",
                        style = MaterialTheme.typography.titleLarge.tabular()
                    )
                },
                textStyle = MaterialTheme.typography.headlineSmall.tabular(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tx_amount_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = titleText,
                onValueChange = {
                    if (it.length <= 80) titleText = it
                    validationError = null
                },
                label = { Text("Merchant or description") },
                placeholder = { Text(if (txType == TransactionType.EXPENSE) "e.g. Trader Joe's" else "e.g. Monthly Payroll") },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tx_title_input")
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Category",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                availableCategories.forEach { cat ->
                    val selected = category.equals(cat.label, ignoreCase = true)
                    FilterChip(
                        selected = selected,
                        onClick = { category = cat.label },
                        label = { Text(cat.label) },
                        leadingIcon = {
                            Icon(
                                imageVector = cat.icon,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Date",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.CalendarToday,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(formatShortDate(Timestamp(Date(occurredMillis))))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = noteText,
                onValueChange = { if (it.length <= 240) noteText = it },
                label = { Text("Note (optional)") },
                maxLines = 2,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tx_note_input")
            )

            if (validationError != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = validationError.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = {
                    val cents = parseCurrencyInputToCents(amountText, currency)
                    val cleanTitle = titleText.trim()
                    when {
                        cents == null || cents <= 0L ->
                            validationError = "Enter a valid amount greater than zero."
                        cleanTitle.isEmpty() ->
                            validationError = "Enter a merchant or description."
                        else -> onSave(
                            existing?.id,
                            txType,
                            cents,
                            cleanTitle,
                            category,
                            noteText.trim().takeIf { it.isNotEmpty() },
                            Timestamp(Date(occurredMillis))
                        )
                    }
                },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_tx_button")
            ) {
                Icon(imageVector = Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (existing == null) "Save Transaction" else "Update Transaction")
            }

            if (existing != null && onDelete != null) {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = { onDelete(existing.id) },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("delete_tx_button")
                ) {
                    Icon(imageVector = Icons.Filled.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete Transaction")
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = occurredMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { occurredMillis = it }
                        showDatePicker = false
                    }
                ) {
                    Text("Select")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetEditorSheet(
    currency: CurrencyOption,
    existing: CategoryBudget?,
    usedCategories: Set<String>,
    onDismiss: () -> Unit,
    onSave: (budgetId: String?, category: String, limitCents: Long) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val selectableCategories = FinanceCategory.expenseCategories.filter {
        existing?.category?.equals(it.label, ignoreCase = true) == true ||
            !usedCategories.contains(it.label.lowercase())
    }
    var selectedCategory by remember(existing) {
        mutableStateOf(
            existing?.category ?: selectableCategories.firstOrNull()?.label ?: FinanceCategory.GROCERIES.label
        )
    }
    var limitText by remember(existing) {
        mutableStateOf(
            if (existing != null) centsToEditableString(existing.limitCents, currency) else ""
        )
    }
    var validationError by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = if (existing == null) "Set Monthly Category Budget" else "Edit Category Budget",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(14.dp))

            if (selectableCategories.isEmpty() && existing == null) {
                Text(
                    text = "All expense categories already have a monthly budget configured.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Close") }
            } else {
                Text(
                    text = "Category",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    selectableCategories.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory.equals(cat.label, ignoreCase = true),
                            onClick = { selectedCategory = cat.label },
                            label = { Text(cat.label) },
                            leadingIcon = {
                                Icon(
                                    imageVector = cat.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = limitText,
                    onValueChange = {
                        limitText = it
                        validationError = null
                    },
                    label = { Text("Monthly Limit (${currency.code})") },
                    prefix = { Text("${currency.symbol} ", style = MaterialTheme.typography.titleLarge.tabular()) },
                    textStyle = MaterialTheme.typography.headlineSmall.tabular(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("budget_limit_input")
                )

                if (validationError != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = validationError.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = {
                        val cents = parseCurrencyInputToCents(limitText, currency)
                        if (cents == null || cents <= 0L) {
                            validationError = "Enter a monthly limit greater than zero."
                        } else {
                            onSave(existing?.id, selectedCategory, cents)
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("save_budget_button")
                ) {
                    Text(if (existing == null) "Save Budget" else "Update Budget")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringBillEditorSheet(
    currency: CurrencyOption,
    existing: RecurringBill?,
    onDismiss: () -> Unit,
    onSave: (
        billId: String?,
        title: String,
        amountCents: Long,
        category: String,
        cadence: BillCadence,
        nextDueAt: Timestamp,
        autoPay: Boolean,
        active: Boolean
    ) -> Unit,
    onDelete: ((String) -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var titleText by remember(existing) { mutableStateOf(existing?.title ?: "") }
    var amountText by remember(existing) {
        mutableStateOf(
            if (existing != null) centsToEditableString(existing.amountCents, currency) else ""
        )
    }
    var category by remember(existing) {
        mutableStateOf(existing?.category ?: FinanceCategory.SUBSCRIPTIONS.label)
    }
    var cadence by remember(existing) {
        mutableStateOf(existing?.billCadence ?: BillCadence.MONTHLY)
    }
    var nextDueMillis by remember(existing) {
        mutableLongStateOf(existing?.nextDueAt?.toDate()?.time ?: System.currentTimeMillis())
    }
    var autoPay by remember(existing) { mutableStateOf(existing?.autoPay ?: false) }
    var active by remember(existing) { mutableStateOf(existing?.active ?: true) }
    var showDatePicker by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = if (existing == null) "Add Recurring Bill" else "Edit Recurring Bill",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = titleText,
                onValueChange = {
                    if (it.length <= 80) titleText = it
                    validationError = null
                },
                label = { Text("Bill or subscription name") },
                placeholder = { Text("e.g. Fiber Internet, Rent, Music") },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("bill_title_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = amountText,
                onValueChange = {
                    amountText = it
                    validationError = null
                },
                label = { Text("Amount (${currency.code})") },
                prefix = { Text("${currency.symbol} ", style = MaterialTheme.typography.titleLarge.tabular()) },
                textStyle = MaterialTheme.typography.headlineSmall.tabular(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("bill_amount_input")
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Billing Cadence",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                BillCadence.entries.forEachIndexed { idx, option ->
                    SegmentedButton(
                        selected = cadence == option,
                        onClick = { cadence = option },
                        shape = SegmentedButtonDefaults.itemShape(idx, BillCadence.entries.size)
                    ) {
                        Text(option.label)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Category",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FinanceCategory.expenseCategories.forEach { cat ->
                    FilterChip(
                        selected = category.equals(cat.label, ignoreCase = true),
                        onClick = { category = cat.label },
                        label = { Text(cat.label) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Next Due Date",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Filled.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(formatShortDate(Timestamp(Date(nextDueMillis))))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("AutoPay enabled", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Badge this bill as automatically charged",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = autoPay, onCheckedChange = { autoPay = it })
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Active schedule", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Include in upcoming due alerts & monthly total",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = active, onCheckedChange = { active = it })
            }

            if (validationError != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = validationError.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = {
                    val cents = parseCurrencyInputToCents(amountText, currency)
                    val cleanTitle = titleText.trim()
                    when {
                        cleanTitle.isEmpty() -> validationError = "Enter a bill or subscription name."
                        cents == null || cents <= 0L -> validationError = "Enter a valid amount greater than zero."
                        else -> onSave(
                            existing?.id,
                            cleanTitle,
                            cents,
                            category,
                            cadence,
                            Timestamp(Date(nextDueMillis)),
                            autoPay,
                            active
                        )
                    }
                },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_bill_button")
            ) {
                Text(if (existing == null) "Save Recurring Bill" else "Update Recurring Bill")
            }

            if (existing != null && onDelete != null) {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = { onDelete(existing.id) },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(imageVector = Icons.Filled.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete Recurring Bill")
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = nextDueMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { nextDueMillis = it }
                        showDatePicker = false
                    }
                ) { Text("Select") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSettingsSheet(
    userEmail: String?,
    userName: String?,
    currentCurrency: CurrencyOption,
    onSelectCurrency: (CurrencyOption) -> Unit,
    onSignOut: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = "Account & Preferences",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(14.dp))

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                border = BorderStroke(1.dp, VerdantThemeTokens.colors.cardBorder),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = userName?.takeIf { it.isNotBlank() } ?: "Google Account",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (!userEmail.isNullOrBlank()) {
                        Text(
                            text = userEmail,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Cloud sync active across devices",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Display Currency",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CurrencyOption.entries.forEach { option ->
                    FilterChip(
                        selected = option == currentCurrency,
                        onClick = { onSelectCurrency(option) },
                        label = { Text("${option.symbol} ${option.code}") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(18.dp))

            OutlinedButton(
                onClick = onSignOut,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("sign_out_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sign Out")
            }
        }
    }
}
