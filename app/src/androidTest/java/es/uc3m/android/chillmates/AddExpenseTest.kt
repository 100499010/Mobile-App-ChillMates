package es.uc3m.android.chillmates

import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import es.uc3m.android.chillmates.expensestracker.AddExpenseDialog
import es.uc3m.android.chillmates.expensestracker.BalanceUi
import es.uc3m.android.chillmates.expensestracker.ExpenseResult
import es.uc3m.android.chillmates.expensestracker.ExpensesRoute
import es.uc3m.android.chillmates.expensestracker.ExpensesUiState
import es.uc3m.android.chillmates.model.Expense
import es.uc3m.android.chillmates.ui.theme.ChillMatesTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AddExpenseTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun addExpense_clickAddFillFieldsAndSave_verifiesExpenseInList() {
        composeTestRule.setContent {
            ChillMatesTheme {
                var showAddExpenseDialog by remember { mutableStateOf(false) }

                var uiState by remember {
                    mutableStateOf(
                        ExpensesUiState(
                            expenses = emptyList(),
                            balances = listOf(
                                BalanceUi("Laura", 0.0),
                                BalanceUi("Carlos", 0.0),
                                BalanceUi("Miguel", 0.0)
                            ),
                            availableFlatmates = listOf("Laura", "Carlos", "Miguel")
                        )
                    )
                }

                ExpensesRoute(
                    state = uiState,
                    onAddExpenseClick = {
                        showAddExpenseDialog = true
                    },
                    onTicketClick = {}
                )

                if (showAddExpenseDialog) {
                    AddExpenseDialog(
                        availableFlatmates = uiState.availableFlatmates,
                        onSave = { result: ExpenseResult ->
                            uiState = uiState.copy(
                                expenses = uiState.expenses + Expense(
                                    id = "expense_test",
                                    title = result.title,
                                    amount = result.amount,
                                    paidBy = result.paidBy,
                                    splitBetween = result.splitBetween,
                                    ticketUri = null
                                )
                            )
                            showAddExpenseDialog = false
                        },
                        onDismiss = {
                            showAddExpenseDialog = false
                        }
                    )
                }
            }
        }

        composeTestRule
            .onNodeWithText(context.getString(R.string.btn_add_expense))
            .performClick()

        composeTestRule
            .onNodeWithText(context.getString(R.string.label_expense_title))
            .performTextInput("Pizza")

        composeTestRule
            .onNodeWithText(context.getString(R.string.add_expense_amount_label))
            .performTextInput("18.50")

        composeTestRule
            .onNodeWithText(context.getString(R.string.btn_save))
            .performClick()

        composeTestRule
            .onNodeWithText("Pizza")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("€18.50")
            .assertIsDisplayed()
    }
}