package com.example.data

import androidx.annotation.Keep
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.ElectricBolt
import androidx.compose.material.icons.outlined.Flight
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocalHospital
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Subscriptions
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.ui.graphics.vector.ImageVector
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class TransactionType(val label: String) {
    EXPENSE("Expense"),
    INCOME("Income");

    companion object {
        fun fromRaw(raw: String?): TransactionType =
            entries.firstOrNull { it.name == raw } ?: EXPENSE
    }
}

enum class BillCadence(val label: String) {
    WEEKLY("Weekly"),
    MONTHLY("Monthly"),
    YEARLY("Yearly");

    companion object {
        fun fromRaw(raw: String?): BillCadence =
            entries.firstOrNull { it.name == raw } ?: MONTHLY
    }
}

enum class CurrencyOption(
    val code: String,
    val symbol: String,
    val displayName: String,
    val decimals: Int = 2
) {
    USD("USD", "$", "US Dollar ($)"),
    EUR("EUR", "€", "Euro (€)"),
    GBP("GBP", "£", "British Pound (£)"),
    INR("INR", "₹", "Indian Rupee (₹)"),
    JPY("JPY", "¥", "Japanese Yen (¥)", decimals = 0),
    CAD("CAD", "CA$", "Canadian Dollar (CA$)"),
    AUD("AUD", "A$", "Australian Dollar (A$)");

    companion object {
        fun fromCode(code: String?): CurrencyOption =
            entries.firstOrNull { it.code == code } ?: USD
    }
}

enum class FinanceCategory(
    val label: String,
    val icon: ImageVector,
    val forIncome: Boolean = false
) {
    GROCERIES("Groceries", Icons.Outlined.ShoppingCart),
    DINING("Dining", Icons.Outlined.Restaurant),
    HOUSING("Housing", Icons.Outlined.Home),
    TRANSPORT("Transport", Icons.Outlined.DirectionsCar),
    UTILITIES("Utilities", Icons.Outlined.ElectricBolt),
    SUBSCRIPTIONS("Subscriptions", Icons.Outlined.Subscriptions),
    SHOPPING("Shopping", Icons.Outlined.ShoppingBag),
    HEALTH("Health", Icons.Outlined.LocalHospital),
    ENTERTAINMENT("Entertainment", Icons.Outlined.Movie),
    TRAVEL("Travel", Icons.Outlined.Flight),
    SALARY("Salary", Icons.Outlined.Payments, forIncome = true),
    FREELANCE("Freelance", Icons.Outlined.WorkOutline, forIncome = true),
    INVESTMENTS("Investments", Icons.Outlined.AccountBalance, forIncome = true),
    GIFT("Gift", Icons.Outlined.CardGiftcard, forIncome = true),
    OTHER("Other", Icons.Outlined.MoreHoriz);

    companion object {
        val expenseCategories: List<FinanceCategory>
            get() = entries.filter { !it.forIncome }

        val incomeCategories: List<FinanceCategory>
            get() = entries.filter { it.forIncome || it == OTHER }

        fun iconFor(label: String): ImageVector =
            entries.firstOrNull { it.label.equals(label, ignoreCase = true) }?.icon
                ?: Icons.Outlined.MoreHoriz
    }
}

@Keep
@IgnoreExtraProperties
data class TransactionRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val type: String = TransactionType.EXPENSE.name,
    val amountCents: Long = 0L,
    val title: String = "",
    val category: String = FinanceCategory.OTHER.label,
    val note: String? = null,
    val occurredAt: Timestamp? = null,
    val recurringBillId: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val transactionType: TransactionType
        get() = TransactionType.fromRaw(type)
}

@Keep
@IgnoreExtraProperties
data class CategoryBudget(
    @DocumentId val id: String = "",
    val userId: String = "",
    val category: String = FinanceCategory.GROCERIES.label,
    val limitCents: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class RecurringBill(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val amountCents: Long = 0L,
    val category: String = FinanceCategory.SUBSCRIPTIONS.label,
    val cadence: String = BillCadence.MONTHLY.name,
    val nextDueAt: Timestamp? = null,
    val autoPay: Boolean = false,
    val active: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val billCadence: BillCadence
        get() = BillCadence.fromRaw(cadence)
}

@Keep
@IgnoreExtraProperties
data class UserPreferences(
    @DocumentId val id: String = "",
    val userId: String = "",
    val currencyCode: String = CurrencyOption.USD.code,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val currencyOption: CurrencyOption
        get() = CurrencyOption.fromCode(currencyCode)
}
