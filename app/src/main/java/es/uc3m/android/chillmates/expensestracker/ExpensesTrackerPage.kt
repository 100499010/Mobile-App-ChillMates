package es.uc3m.android.chillmates.expensestracker

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import es.uc3m.android.chillmates.LargeSectionHeader
import es.uc3m.android.chillmates.NavGraph
import es.uc3m.android.chillmates.PageContentWrapper
import es.uc3m.android.chillmates.R
import es.uc3m.android.chillmates.model.Expense
import kotlin.math.absoluteValue
import java.util.Locale
import androidx.compose.material.icons.filled.Check

@Composable
fun ExpensesTrackerPage(navController: NavController) {
    val vm: ExpensesViewModel = viewModel()
    val context = LocalContext.current
    val toastTicketAttached = stringResource(R.string.toast_ticket_attached)
    val toastCancelled = stringResource(R.string.toast_cancelled)
    val toastExpenseAdded = stringResource(R.string.toast_expense_added)
    val savedStateHandle = navController.currentBackStackEntry?.savedStateHandle

    var pendingExpenseId by remember { mutableStateOf<String?>(null) }
    var ticketToView by remember { mutableStateOf<Uri?>(null) }

    val returnedExpenseTitle = savedStateHandle?.get<String>(NavGraph.RESULT_EXPENSE_TITLE)
    val returnedExpenseAmount = savedStateHandle?.get<Double>(NavGraph.RESULT_EXPENSE_AMOUNT)
    val returnedExpensePaidBy = savedStateHandle?.get<String>(NavGraph.RESULT_EXPENSE_PAID_BY)
    val returnedExpenseSplitBetween = savedStateHandle
        ?.get<ArrayList<String>>(NavGraph.RESULT_EXPENSE_SPLIT_BETWEEN)
        ?.toList()

    LaunchedEffect(returnedExpenseTitle, returnedExpenseAmount, returnedExpensePaidBy, returnedExpenseSplitBetween) {
        if (
            returnedExpenseTitle != null &&
            returnedExpenseAmount != null &&
            returnedExpensePaidBy != null &&
            returnedExpenseSplitBetween != null
        ) {
            vm.onExpenseAdded(
                returnedExpenseTitle,
                returnedExpenseAmount,
                returnedExpensePaidBy,
                returnedExpenseSplitBetween
            )
            Toast.makeText(context, toastExpenseAdded, Toast.LENGTH_SHORT).show()

            savedStateHandle?.remove<String>(NavGraph.RESULT_EXPENSE_TITLE)
            savedStateHandle?.remove<Double>(NavGraph.RESULT_EXPENSE_AMOUNT)
            savedStateHandle?.remove<String>(NavGraph.RESULT_EXPENSE_PAID_BY)
            savedStateHandle?.remove<ArrayList<String>>(NavGraph.RESULT_EXPENSE_SPLIT_BETWEEN)
            savedStateHandle?.remove<String?>(NavGraph.RESULT_EXPENSE_ITEM_ID)
        }
    }

    val getImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        val expenseId = pendingExpenseId
        pendingExpenseId = null

        if (uri != null && expenseId != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) {
            }

            vm.onTicketSelected(expenseId, uri)
            Toast.makeText(context, toastTicketAttached, Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, toastCancelled, Toast.LENGTH_SHORT).show()
        }
    }

    if (ticketToView != null) {
        TicketViewerDialog(
            uri = ticketToView!!,
            onDismiss = { ticketToView = null }
        )
    }

    ExpensesRoute(
        state = vm.uiState,
        onAddExpenseClick = {
            savedStateHandle?.set(NavGraph.REQUEST_EXPENSE_TITLE, "")
            savedStateHandle?.set(NavGraph.REQUEST_EXPENSE_PAID_BY, "")
            savedStateHandle?.set(NavGraph.REQUEST_EXPENSE_SPLIT_BETWEEN, arrayListOf<String>())
            savedStateHandle?.set(
                NavGraph.REQUEST_EXPENSE_AVAILABLE_FLATMATES,
                ArrayList(vm.uiState.availableFlatmates)
            )
            savedStateHandle?.remove<String>(NavGraph.REQUEST_EXPENSE_ITEM_ID)
            navController.navigate(NavGraph.AddExpense.route)
        },
        onTicketClick = { expenseId ->
            when (val action = vm.onTicketRequested(expenseId)) {
                is TicketAction.PickTicketImage -> {
                    pendingExpenseId = expenseId
                    getImageLauncher.launch(arrayOf("image/*"))
                }
                is TicketAction.ViewTicket -> {
                    ticketToView = action.uri
                }
            }
        }
    )

}

/**
 * In-app dialog to preview an attached receipt image.
 */
@Composable
private fun TicketViewerDialog(
    uri: Uri,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.label_ticket),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.btn_close)) }
                }

                Spacer(modifier = Modifier.height(8.dp))

                AndroidView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(420.dp),
                    factory = { ctx: android.content.Context ->
                        android.widget.ImageView(ctx).apply {
                            adjustViewBounds = true
                            scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
                        }
                    },
                    update = { imageView: android.widget.ImageView ->
                        imageView.setImageURI(uri)
                    }
                )
            }
        }
    }
}

@Composable
private fun ExpenseRowItem(
    expense: Expense,
    onTicketClick: () -> Unit
) {
    val ticketText = stringResource(
        id = if (expense.ticketUri.isNullOrBlank()) R.string.ticket_add_label else R.string.ticket_see_label
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = expense.title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium)
            )

            if (expense.splitBetween.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.label_for_members, expense.splitBetween.joinToString(", ")),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            Text(
                text = ticketText,
                style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                modifier = Modifier
                    .clickable(onClick = onTicketClick)
                    .padding(vertical = 2.dp)
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = formatMoney(expense.amount),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = expense.paidBy,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

@Composable
private fun AddExpenseAction(onAddClick: () -> Unit) {
    OutlinedButton(
        onClick = onAddClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp),
        shape = RoundedCornerShape(10.dp)
    ) {
        Icon(imageVector = Icons.Default.Add, contentDescription = stringResource(R.string.cd_add_expense))
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = stringResource(R.string.btn_add_expense))
    }
}

@Composable
fun ExpensesRoute(
    state: ExpensesUiState,
    onAddExpenseClick: () -> Unit,
    onTicketClick: (String) -> Unit
) {
    val currentUserName = state.availableFlatmates.firstOrNull() ?: ""
    var showOnlyMine by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        LargeSectionHeader(
            title = stringResource(id = R.string.expenses_title)
        )

        PageContentWrapper {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    OutlinedButton(
                        onClick = { showOnlyMine = !showOnlyMine },
                        modifier = Modifier.fillMaxWidth(),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary)
                        )
                    ) {
                        Text(
                            text = if (showOnlyMine) {
                                stringResource(R.string.btn_all_expenses)
                            } else {
                                stringResource(R.string.btn_my_expenses)
                            },
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                val displayExpenses = if (showOnlyMine) {
                    state.expenses.filter {
                        it.paidBy == currentUserName || it.splitBetween.contains(currentUserName)
                    }
                } else {
                    state.expenses
                }

                val displayBalances = if (showOnlyMine) {
                    state.balances.filter { it.name == currentUserName }
                } else {
                    state.balances
                }

                ExpensesContentSection(
                    expenses = displayExpenses,
                    balances = displayBalances,
                    showLargeBalance = showOnlyMine,
                    onAddExpenseClick = onAddExpenseClick,
                    onTicketClick = onTicketClick
                )
            }
        }
    }
}

@Composable
private fun ExpensesContentSection(
    expenses: List<Expense>,
    balances: List<BalanceUi>,
    showLargeBalance: Boolean,
    onAddExpenseClick: () -> Unit,
    onTicketClick: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 24.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(expenses, key = { it.id }) { expense ->
                ExpenseRowItem(
                    expense = expense,
                    onTicketClick = { onTicketClick(expense.id) }
                )
                DividerThin()
            }
        }

        Box(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
            AddExpenseAction(onAddClick = onAddExpenseClick)
        }

        BalancesFooterSection(
            balances = balances,
            isLargeView = showLargeBalance,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 0.dp)
        )
    }
}

@Composable
private fun BalancesFooterSection(
    balances: List<BalanceUi>,
    isLargeView: Boolean,
    modifier: Modifier = Modifier
) {
    if (isLargeView && balances.isNotEmpty()) {
        Box(modifier = modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
            BalanceCardItem(
                name = stringResource(R.string.expenses_your_balance),
                balance = balances[0].balance,
                modifier = Modifier.fillMaxWidth().height(90.dp),
                isLarge = true
            )
        }
    } else {
        LazyRow(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(balances) { b ->
                val itemWidth = when (balances.size) {
                    1 -> 340.dp
                    2 -> 160.dp
                    else -> 110.dp
                }

                BalanceCardItem(
                    name = b.name,
                    balance = b.balance,
                    modifier = Modifier.width(itemWidth)
                )
            }
        }
    }
}

@Composable
private fun BalanceCardItem(
    name: String,
    balance: Double,
    modifier: Modifier = Modifier,
    isLarge: Boolean = false
) {
    val isPositive = balance >= 0.0
    val amountText = (if (isPositive) "+" else "-") +
            formatMoney(balance.absoluteValue).replace("€", "") + "€"

    ElevatedCard(
        modifier = modifier.height(if (isLarge) 90.dp else 75.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (isLarge) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = name,
                style = if (isLarge) MaterialTheme.typography.titleMedium else MaterialTheme.typography.labelLarge,
                maxLines = 1
            )
            Text(
                text = amountText,
                style = (if (isLarge) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleMedium).copy(
                    fontWeight = FontWeight.Bold,
                    color = if (isPositive) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                )
            )
        }
    }
}

@Composable
private fun DividerThin() {
    HorizontalDivider(
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
    )
}

private fun formatMoney(amount: Double): String =
    "€" + String.format(Locale.getDefault(), "%.2f", amount)
