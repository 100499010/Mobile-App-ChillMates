package es.uc3m.android.chillmates

import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import es.uc3m.android.chillmates.shoppinglist.ShoppingItemUi
import es.uc3m.android.chillmates.shoppinglist.ShoppingListRoute
import es.uc3m.android.chillmates.shoppinglist.ShoppingUiState
import es.uc3m.android.chillmates.shoppinglist.SupermarketUi
import es.uc3m.android.chillmates.ui.theme.ChillMatesTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ShoppingListTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun shoppingList_selectSupermarket_verifiesFilter() {
        composeTestRule.setContent {
            ChillMatesTheme {
                var uiState by remember {
                    mutableStateOf(createShoppingUiState())
                }

                ShoppingListRoute(
                    state = uiState,
                    onFilter = { supermarketName ->
                        uiState = uiState.copy(
                            selectedSupermarket = supermarketName,
                            selectedSupermarkets = supermarketName?.let { setOf(it) } ?: emptySet()
                        )
                    },
                    onBoughtAction = {},
                    onDelete = {},
                    onAddItem = {},
                    onAddSupermarket = { _, _, _, _, _, _, callback ->
                        callback(Result.failure(Exception("Not needed in this test")))
                    },
                    onOpenMap = {}
                )
            }
        }

        composeTestRule
            .onNodeWithText(context.getString(R.string.filter_all))
            .performClick()

        composeTestRule
            .onNodeWithText("Mercadona")
            .performClick()

        composeTestRule
            .onNodeWithText("Leche (2L)")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Cerveza (6)")
            .assertDoesNotExist()
    }

    @Test
    fun shoppingList_markItemAsBought_verifiesItemHiddenFromPendingList() {
        composeTestRule.setContent {
            ChillMatesTheme {
                var uiState by remember {
                    mutableStateOf(createShoppingUiState())
                }

                ShoppingListRoute(
                    state = uiState,
                    onFilter = {},
                    onBoughtAction = { item ->
                        uiState = uiState.copy(
                            items = uiState.items.map {
                                if (it.id == item.id) it.copy(bought = !it.bought) else it
                            }
                        )
                    },
                    onDelete = {},
                    onAddItem = {},
                    onAddSupermarket = { _, _, _, _, _, _, callback ->
                        callback(Result.failure(Exception("Not needed in this test")))
                    },
                    onOpenMap = {}
                )
            }
        }

        composeTestRule
            .onNodeWithText("Leche (2L)")
            .assertIsDisplayed()

        composeTestRule
            .onAllNodesWithContentDescription(context.getString(R.string.cd_toggle_bought))[0]
            .performClick()

        composeTestRule
            .onNodeWithText("Leche (2L)")
            .assertDoesNotExist()
    }

    @Test
    fun shoppingList_deleteItem_verifiesItemRemoved() {
        composeTestRule.setContent {
            ChillMatesTheme {
                var uiState by remember {
                    mutableStateOf(createShoppingUiState())
                }

                ShoppingListRoute(
                    state = uiState,
                    onFilter = {},
                    onBoughtAction = {},
                    onDelete = { itemId ->
                        uiState = uiState.copy(
                            items = uiState.items.filterNot { it.id == itemId }
                        )
                    },
                    onAddItem = {},
                    onAddSupermarket = { _, _, _, _, _, _, callback ->
                        callback(Result.failure(Exception("Not needed in this test")))
                    },
                    onOpenMap = {}
                )
            }
        }

        composeTestRule
            .onNodeWithText("Pan (1 barra)")
            .assertIsDisplayed()

        composeTestRule
            .onAllNodesWithContentDescription(context.getString(R.string.cd_delete))[2]
            .performClick()

        composeTestRule
            .onNodeWithText("Pan (1 barra)")
            .assertDoesNotExist()
    }

    private fun createShoppingUiState(): ShoppingUiState {
        return ShoppingUiState(
            items = listOf(
                ShoppingItemUi(
                    id = "item_1",
                    name = "Leche",
                    quantity = "2L",
                    neededBy = listOf("Laura", "Carlos"),
                    supermarkets = listOf("Mercadona"),
                    bought = false
                ),
                ShoppingItemUi(
                    id = "item_2",
                    name = "Huevos",
                    quantity = "12",
                    neededBy = listOf("Miguel"),
                    supermarkets = listOf("Mercadona", "Carrefour"),
                    bought = false
                ),
                ShoppingItemUi(
                    id = "item_3",
                    name = "Pan",
                    quantity = "1 barra",
                    neededBy = listOf("Laura", "Miguel"),
                    supermarkets = listOf("Lidl"),
                    bought = false
                ),
                ShoppingItemUi(
                    id = "item_4",
                    name = "Cerveza",
                    quantity = "6",
                    neededBy = listOf("Carlos"),
                    supermarkets = listOf("Carrefour"),
                    bought = false
                )
            ),
            availableSupermarkets = listOf(
                SupermarketUi(
                    id = "market_1",
                    name = "Mercadona",
                    officialName = "Mercadona",
                    placeId = "mercadona_place",
                    latitude = 40.0,
                    longitude = -3.0,
                    address = "Street 1"
                ),
                SupermarketUi(
                    id = "market_2",
                    name = "Carrefour",
                    officialName = "Carrefour",
                    placeId = "carrefour_place",
                    latitude = 40.1,
                    longitude = -3.1,
                    address = "Street 2"
                ),
                SupermarketUi(
                    id = "market_3",
                    name = "Lidl",
                    officialName = "Lidl",
                    placeId = "lidl_place",
                    latitude = 40.2,
                    longitude = -3.2,
                    address = "Street 3"
                )
            ),
            availableFlatmates = listOf("Laura", "Carlos", "Miguel"),
            currentUserName = "Laura"
        )
    }
}