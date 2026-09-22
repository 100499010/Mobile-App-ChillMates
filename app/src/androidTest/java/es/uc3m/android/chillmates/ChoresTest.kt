package es.uc3m.android.chillmates

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import es.uc3m.android.chillmates.chores.AddChoreDialog
import es.uc3m.android.chillmates.chores.ChoreCard
import es.uc3m.android.chillmates.chores.ChoreWeekMode
import es.uc3m.android.chillmates.model.Chore
import es.uc3m.android.chillmates.model.ChoreAssignment
import es.uc3m.android.chillmates.ui.theme.ChillMatesTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChoresTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun choreCard_clickCheckbox_verifiesToggle() {
        composeTestRule.setContent {
            ChillMatesTheme {
                var done by remember { mutableStateOf(false) }

                ChoreCard(
                    chore = Chore(
                        id = "chore_1",
                        title = "Clean kitchen",
                        done = done,
                        assignments = mapOf(
                            "2025-W01" to ChoreAssignment(
                                assignee = "Laura",
                                done = done
                            )
                        )
                    ),
                    displayedAssignee = "Laura",
                    displayedDone = done,
                    mode = ChoreWeekMode.CURRENT,
                    hasRecord = true,
                    currentUserName = "Laura",
                    onUpdateChore = { _, isDone ->
                        done = isDone
                    },
                    onSwapRequest = {},
                    onSendReminder = {}
                )
            }
        }

        composeTestRule
            .onNodeWithText("Clean kitchen")
            .assertIsDisplayed()

        composeTestRule
            .onAllNodesWithText("")
            .fetchSemanticsNodes()

        composeTestRule
            .onNode(
                androidx.compose.ui.test.hasClickAction() and
                        androidx.compose.ui.test.isToggleable()
            )
            .performClick()

        composeTestRule
            .onNode(
                androidx.compose.ui.test.hasClickAction() and
                        androidx.compose.ui.test.isToggleable()
            )
            .assertIsOn()
    }

    @Test
    fun addChore_clickAddNewChore_verifiesChoreAdded() {
        composeTestRule.setContent {
            ChillMatesTheme {
                val chores = remember {
                    mutableStateListOf(
                        Chore(
                            id = "chore_1",
                            title = "Clean bathroom",
                            done = false
                        )
                    )
                }

                var showDialog by remember { mutableStateOf(false) }

                Column {
                    chores.forEach { chore ->
                        androidx.compose.material3.Text(text = chore.title)
                    }

                    androidx.compose.material3.Button(
                        onClick = { showDialog = true }
                    ) {
                        androidx.compose.material3.Text(
                            text = context.getString(R.string.btn_add_new_chore)
                        )
                    }

                    if (showDialog) {
                        AddChoreDialog(
                            flatmates = listOf("Everyone", "Laura", "Carlos", "Miguel"),
                            onDismiss = {
                                showDialog = false
                            },
                            onAdd = { title, assignee ->
                                chores.add(
                                    Chore(
                                        id = "chore_${chores.size + 1}",
                                        title = title,
                                        done = false,
                                        assignments = mapOf(
                                            "2025-W01" to ChoreAssignment(
                                                assignee = assignee,
                                                done = false
                                            )
                                        )
                                    )
                                )
                                showDialog = false
                            }
                        )
                    }
                }
            }
        }

        composeTestRule
            .onNodeWithText("Clean bathroom")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText(context.getString(R.string.btn_add_new_chore))
            .performClick()

        composeTestRule
            .onNodeWithText(context.getString(R.string.label_chore_title))
            .performTextInput("Take out trash")

        composeTestRule
            .onNodeWithText(context.getString(R.string.btn_add))
            .performClick()

        composeTestRule
            .onNodeWithText("Take out trash")
            .assertIsDisplayed()
    }
}