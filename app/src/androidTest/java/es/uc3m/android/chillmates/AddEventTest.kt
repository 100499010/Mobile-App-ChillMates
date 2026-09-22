package es.uc3m.android.chillmates

import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import es.uc3m.android.chillmates.home.AddEventScreen
import es.uc3m.android.chillmates.home.EventResult
import es.uc3m.android.chillmates.ui.theme.ChillMatesTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AddEventTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun addEvent_fillFieldsAndSave_verifiesResultShown() {
        composeTestRule.setContent {
            ChillMatesTheme {
                val savedEvent = remember { mutableStateOf<EventResult?>(null) }

                if (savedEvent.value == null) {
                    AddEventScreen(
                        onSave = { result ->
                            savedEvent.value = result
                        },
                        onBack = {}
                    )
                } else {
                    androidx.compose.material3.Text(
                        text = "Saved event: ${savedEvent.value!!.description}"
                    )
                }
            }
        }

        composeTestRule
            .onNodeWithText(context.getString(R.string.add_event_label_day))
            .performTextInput("Mon")

        composeTestRule
            .onNodeWithText(context.getString(R.string.add_event_label_time))
            .performTextInput("10:00")

        composeTestRule
            .onNodeWithText(context.getString(R.string.add_event_label_description))
            .performTextInput("Test Event")

        composeTestRule
            .onNodeWithText(context.getString(R.string.add_event_label_location))
            .performTextInput("Home")

        composeTestRule
            .onNodeWithText(context.getString(R.string.add_event_btn_save))
            .performClick()

        composeTestRule
            .onNodeWithText("Saved event: Test Event")
            .assertIsDisplayed()
    }
}