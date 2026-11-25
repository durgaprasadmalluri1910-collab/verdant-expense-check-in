package com.example.data

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.Calendar

class FinanceRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(expectedUserId: String): String {
        val uid = auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
        check(uid == expectedUserId) { "Signed-in user changed; refusing cross-user access." }
        return uid
    }

    private fun userRoot(userId: String) = db.collection("users").document(userId)

    private fun <T> Flow<T>.wrapFirestoreRead(
        userId: String,
        operationType: OperationType,
        path: String
    ): Flow<T> = catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == userId) {
            handleFirestoreError(exceptionToLog, operationType, path)
        }
        throw error
    }

    fun observeTransactions(userId: String): Flow<List<TransactionRecord>> = flow {
        requireUserId(userId)
        val path = "users/$userId/transactions"
        emitAll(
            userRoot(userId).collection("transactions")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        TransactionRecord::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.occurredAt ?: it.createdAt ?: Timestamp(0, 0) }
                }
                .wrapFirestoreRead(userId, OperationType.LIST, path)
        )
    }

    fun observeBudgets(userId: String): Flow<List<CategoryBudget>> = flow {
        requireUserId(userId)
        val path = "users/$userId/budgets"
        emitAll(
            userRoot(userId).collection("budgets")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        CategoryBudget::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedBy { it.category }
                }
                .wrapFirestoreRead(userId, OperationType.LIST, path)
        )
    }

    fun observeRecurringBills(userId: String): Flow<List<RecurringBill>> = flow {
        requireUserId(userId)
        val path = "users/$userId/bills"
        emitAll(
            userRoot(userId).collection("bills")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        RecurringBill::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedBy { it.nextDueAt ?: Timestamp(Long.MAX_VALUE / 1000, 0) }
                }
                .wrapFirestoreRead(userId, OperationType.LIST, path)
        )
    }

    fun observePreferences(userId: String): Flow<UserPreferences?> = flow {
        requireUserId(userId)
        val path = "users/$userId/preferences/profile"
        emitAll(
            userRoot(userId).collection("preferences").document("profile")
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            UserPreferences::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else {
                        null
                    }
                }
                .wrapFirestoreRead(userId, OperationType.GET, path)
        )
    }

    suspend fun saveTransaction(
        userId: String,
        transactionId: String?,
        type: TransactionType,
        amountCents: Long,
        title: String,
        category: String,
        note: String?,
        occurredAt: Timestamp
    ): Result<Unit> {
        val cleanTitle = title.trim()
        val cleanCategory = category.trim()
        val cleanNote = note?.trim()?.takeIf { it.isNotEmpty() }
        var docPath = "users/$userId/transactions"
        return try {
            val uid = requireUserId(userId)
            require(cleanTitle.length in 1..80) { "Title must be between 1 and 80 characters." }
            require(cleanCategory.length in 1..40) { "Category must be between 1 and 40 characters." }
            require(amountCents in 1L..100_000_000_000L) { "Amount must be greater than zero." }
            require(cleanNote == null || cleanNote.length <= 240) { "Note must be 240 characters or fewer." }

            val col = userRoot(uid).collection("transactions")
            if (transactionId.isNullOrBlank()) {
                val docRef = col.document()
                docPath = docRef.path
                val payload = mapOf(
                    "userId" to uid,
                    "type" to type.name,
                    "amountCents" to amountCents,
                    "title" to cleanTitle,
                    "category" to cleanCategory,
                    "note" to cleanNote,
                    "occurredAt" to occurredAt,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                docRef.set(payload).await()
            } else {
                val docRef = col.document(transactionId)
                docPath = docRef.path
                val updates = mutableMapOf<String, Any>(
                    "type" to type.name,
                    "amountCents" to amountCents,
                    "title" to cleanTitle,
                    "category" to cleanCategory,
                    "note" to (cleanNote ?: ""),
                    "occurredAt" to occurredAt,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(updates).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val op = if (transactionId.isNullOrBlank()) OperationType.CREATE else OperationType.UPDATE
            handleFirestoreError(e, op, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteTransaction(userId: String, transactionId: String): Result<Unit> {
        val docPath = "users/$userId/transactions/$transactionId"
        return try {
            val uid = requireUserId(userId)
            require(transactionId.isNotBlank()) { "Transaction ID cannot be blank." }
            userRoot(uid).collection("transactions").document(transactionId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun saveBudget(
        userId: String,
        budgetId: String?,
        category: String,
        limitCents: Long
    ): Result<Unit> {
        val cleanCategory = category.trim()
        var docPath = "users/$userId/budgets"
        return try {
            val uid = requireUserId(userId)
            require(cleanCategory.length in 1..40) { "Category must be between 1 and 40 characters." }
            require(limitCents in 1L..100_000_000_000L) { "Budget limit must be greater than zero." }

            val col = userRoot(uid).collection("budgets")
            if (budgetId.isNullOrBlank()) {
                val docRef = col.document()
                docPath = docRef.path
                val payload = mapOf(
                    "userId" to uid,
                    "category" to cleanCategory,
                    "limitCents" to limitCents,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            } else {
                val docRef = col.document(budgetId)
                docPath = docRef.path
                docRef.update(
                    mapOf(
                        "category" to cleanCategory,
                        "limitCents" to limitCents,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val op = if (budgetId.isNullOrBlank()) OperationType.CREATE else OperationType.UPDATE
            handleFirestoreError(e, op, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteBudget(userId: String, budgetId: String): Result<Unit> {
        val docPath = "users/$userId/budgets/$budgetId"
        return try {
            val uid = requireUserId(userId)
            require(budgetId.isNotBlank()) { "Budget ID cannot be blank." }
            userRoot(uid).collection("budgets").document(budgetId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun saveRecurringBill(
        userId: String,
        billId: String?,
        title: String,
        amountCents: Long,
        category: String,
        cadence: BillCadence,
        nextDueAt: Timestamp,
        autoPay: Boolean,
        active: Boolean
    ): Result<Unit> {
        val cleanTitle = title.trim()
        val cleanCategory = category.trim()
        var docPath = "users/$userId/bills"
        return try {
            val uid = requireUserId(userId)
            require(cleanTitle.length in 1..80) { "Bill title must be between 1 and 80 characters." }
            require(cleanCategory.length in 1..40) { "Category must be between 1 and 40 characters." }
            require(amountCents in 1L..100_000_000_000L) { "Bill amount must be greater than zero." }

            val col = userRoot(uid).collection("bills")
            if (billId.isNullOrBlank()) {
                val docRef = col.document()
                docPath = docRef.path
                val payload = mapOf(
                    "userId" to uid,
                    "title" to cleanTitle,
                    "amountCents" to amountCents,
                    "category" to cleanCategory,
                    "cadence" to cadence.name,
                    "nextDueAt" to nextDueAt,
                    "autoPay" to autoPay,
                    "active" to active,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            } else {
                val docRef = col.document(billId)
                docPath = docRef.path
                docRef.update(
                    mapOf(
                        "title" to cleanTitle,
                        "amountCents" to amountCents,
                        "category" to cleanCategory,
                        "cadence" to cadence.name,
                        "nextDueAt" to nextDueAt,
                        "autoPay" to autoPay,
                        "active" to active,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val op = if (billId.isNullOrBlank()) OperationType.CREATE else OperationType.UPDATE
            handleFirestoreError(e, op, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteRecurringBill(userId: String, billId: String): Result<Unit> {
        val docPath = "users/$userId/bills/$billId"
        return try {
            val uid = requireUserId(userId)
            require(billId.isNotBlank()) { "Bill ID cannot be blank." }
            userRoot(uid).collection("bills").document(billId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun payRecurringBill(userId: String, bill: RecurringBill): Result<Unit> {
        val docPath = "users/$userId/bills/${bill.id}"
        return try {
            val uid = requireUserId(userId)
            require(bill.id.isNotBlank()) { "Bill ID cannot be blank." }
            require(bill.amountCents in 1L..100_000_000_000L) { "Bill amount must be greater than zero." }

            val currentDue = bill.nextDueAt ?: Timestamp.now()
            val nextDue = computeNextDueTimestamp(currentDue, bill.billCadence)
            val nowTs = Timestamp.now()

            val batch = db.batch()
            val txRef = userRoot(uid).collection("transactions").document()
            val billRef = userRoot(uid).collection("bills").document(bill.id)

            val txPayload = mapOf(
                "userId" to uid,
                "type" to TransactionType.EXPENSE.name,
                "amountCents" to bill.amountCents,
                "title" to bill.title.trim().take(80),
                "category" to bill.category.trim().take(40),
                "note" to "Recurring ${bill.billCadence.label.lowercase()} bill",
                "recurringBillId" to bill.id,
                "occurredAt" to nowTs,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            batch.set(txRef, txPayload)
            batch.update(
                billRef,
                mapOf(
                    "nextDueAt" to nextDue,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateCurrency(userId: String, currencyOption: CurrencyOption): Result<Unit> {
        val docPath = "users/$userId/preferences/profile"
        return try {
            val uid = requireUserId(userId)
            val docRef = userRoot(uid).collection("preferences").document("profile")
            val snap = docRef.get().await()
            if (snap.exists()) {
                docRef.update(
                    mapOf(
                        "currencyCode" to currencyOption.code,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "currencyCode" to currencyOption.code,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    companion object {
        fun computeNextDueTimestamp(currentDue: Timestamp, cadence: BillCadence): Timestamp {
            val cal = Calendar.getInstance().apply {
                time = currentDue.toDate()
                when (cadence) {
                    BillCadence.WEEKLY -> add(Calendar.WEEK_OF_YEAR, 1)
                    BillCadence.MONTHLY -> add(Calendar.MONTH, 1)
                    BillCadence.YEARLY -> add(Calendar.YEAR, 1)
                }
            }
            return Timestamp(cal.time)
        }
    }
}
