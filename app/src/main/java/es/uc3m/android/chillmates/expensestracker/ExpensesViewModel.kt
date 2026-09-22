package es.uc3m.android.chillmates.expensestracker

import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import es.uc3m.android.chillmates.model.Expense
import es.uc3m.android.chillmates.model.USERS_COLLECTION
import es.uc3m.android.chillmates.repository.FlatRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Immutable
data class BalanceUi(val name: String, val balance: Double)

@Immutable
data class ExpensesUiState(
    val title: String = "expenses_title",
    val expenses: List<Expense> = emptyList(),
    val balances: List<BalanceUi> = emptyList(),
    val availableFlatmates: List<String> = emptyList()
)

sealed class TicketAction {
    data object PickTicketImage : TicketAction()
    data class ViewTicket(val uri: Uri) : TicketAction()
}

class ExpensesViewModel(
    private val repository: FlatRepository = FlatRepository()
) : ViewModel() {

    var uiState by mutableStateOf(ExpensesUiState())
        private set

    private var currentFlatId: String? = null
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    init {
        loadUserAndFlatData()
    }

    private fun loadUserAndFlatData() {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: return@launch
            try {
                val userDoc = db.collection(USERS_COLLECTION).document(userId).get().await()
                val flatId = userDoc.getString("flatId")

                if (flatId != null) {
                    currentFlatId = flatId

                    launch {
                        repository.observeFlat(flatId).collect { flat ->
                            if (flat != null) {
                                val membersResult = repository.getMemberDetails(flat.memberIds)
                                val memberNames = membersResult.getOrNull()?.map { it.second } ?: emptyList()
                                uiState = uiState.copy(availableFlatmates = memberNames)
                                recalculateBalances()
                            }
                        }
                    }

                    launch {
                        repository.observeExpenses(flatId).collect { dbExpenses ->
                            uiState = uiState.copy(expenses = dbExpenses)
                            recalculateBalances()
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun onExpenseAdded(title: String, amount: Double, paidBy: String, splitBetween: List<String>) {
        val flatId = currentFlatId ?: return
        val newExpense = Expense(
            id = System.currentTimeMillis().toString(),
            title = title,
            amount = amount,
            paidBy = paidBy,
            splitBetween = splitBetween,
            ticketUri = null
        )
        viewModelScope.launch { repository.addExpense(flatId, newExpense) }
    }

    fun onTicketRequested(expenseId: String): TicketAction {
        val expense = uiState.expenses.firstOrNull { it.id == expenseId } ?: return TicketAction.PickTicketImage
        return if (expense.ticketUri.isNullOrBlank()) TicketAction.PickTicketImage else TicketAction.ViewTicket(Uri.parse(expense.ticketUri))
    }

    fun onTicketSelected(expenseId: String, uri: Uri) {
        val flatId = currentFlatId ?: return
        viewModelScope.launch { repository.updateExpenseTicket(flatId, expenseId, uri.toString()) }
    }

    private fun recalculateBalances() {
        val totals = mutableMapOf<String, Double>()
        uiState.availableFlatmates.forEach { totals[it] = 0.0 }

        uiState.expenses.forEach { expense ->
            val payer = expense.paidBy
            val amount = expense.amount
            val participants = if (expense.splitBetween.isEmpty()) {
                uiState.availableFlatmates
            } else {
                expense.splitBetween
            }

            if (participants.isNotEmpty()) {
                val share = amount / participants.size
                totals[payer] = (totals[payer] ?: 0.0) + amount
                participants.forEach { person -> totals[person] = (totals[person] ?: 0.0) - share }
            }
        }

        val newBalances = totals.map { (name, balance) -> BalanceUi(name, balance) }.sortedByDescending { it.balance }
        uiState = uiState.copy(balances = newBalances)
    }

    fun addTestExpenses() {
        val flatId = currentFlatId ?: return
        val testExpenses = listOf(
            Expense(id = System.currentTimeMillis().toString() + "_1", title = "Weekly Supermarket", amount = 54.20, paidBy = uiState.availableFlatmates.firstOrNull() ?: "Me", splitBetween = uiState.availableFlatmates, ticketUri = null),
            Expense(id = System.currentTimeMillis().toString() + "_2", title = "Fiber Internet", amount = 29.99, paidBy = uiState.availableFlatmates.lastOrNull() ?: "Me", splitBetween = uiState.availableFlatmates, ticketUri = null)
        )
        viewModelScope.launch { testExpenses.forEach { repository.addExpense(flatId, it) } }
    }
}