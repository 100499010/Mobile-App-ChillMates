package es.uc3m.android.chillmates.model

const val EXPENSES_SUBCOLLECTION = "expenses"

data class Expense(
    val id: String = "",
    val title: String = "",
    val amount: Double = 0.0,
    val paidBy: String = "",
    val ticketUri: String? = null,
    val splitBetween: List<String> = emptyList()
)