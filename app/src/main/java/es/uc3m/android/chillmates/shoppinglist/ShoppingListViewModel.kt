package es.uc3m.android.chillmates.shoppinglist

import androidx.compose.runtime.*
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import es.uc3m.android.chillmates.model.Expense
import es.uc3m.android.chillmates.model.ShoppingItem
import es.uc3m.android.chillmates.model.Supermarket
import es.uc3m.android.chillmates.model.USERS_COLLECTION
import es.uc3m.android.chillmates.repository.FlatRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Immutable
data class ShoppingItemUi(
    val id: String,
    val name: String,
    val quantity: String,
    val neededBy: List<String>,
    val supermarkets: List<String>,
    val bought: Boolean = false
)

@Immutable
data class SupermarketUi(
    val id: String, val name: String, val officialName: String, val placeId: String, val latitude: Double, val longitude: Double, val address: String
)

data class ShoppingUiState(
    val items: List<ShoppingItemUi> = emptyList(),
    val selectedSupermarket: String? = null,
    val selectedSupermarkets: Set<String> = emptySet(),
    val availableSupermarkets: List<SupermarketUi> = emptyList(),
    val availableFlatmates: List<String> = emptyList(),
    val currentUserName: String = "Me"
)

class ShoppingViewModel(
    private val repository: FlatRepository = FlatRepository()
) : ViewModel() {

    var uiState by mutableStateOf(ShoppingUiState())
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
                val flatId = userDoc.getString("flatId") ?: return@launch
                val userName = userDoc.getString("displayName") ?: "Me"

                currentFlatId = flatId
                uiState = uiState.copy(currentUserName = userName)

                launch {
                    repository.observeFlat(flatId).collect { flat ->
                        if (flat != null) {
                            val membersResult = repository.getMemberDetails(flat.memberIds)
                            val memberNames = membersResult.getOrNull()?.map { it.second } ?: emptyList()

                            val supermarketsResult = repository.getSupermarketsForFlat(flatId)
                            val supermarkets = supermarketsResult.getOrNull().orEmpty().map { it.toUi() }

                            val selectedFilter = uiState.selectedSupermarket
                            val selectedFilters = uiState.selectedSupermarkets.ifEmpty { selectedFilter?.let { setOf(it) } ?: emptySet() }
                            val validFilters = selectedFilters.filter { selected -> supermarkets.any { it.name == selected } }.toSet()

                            uiState = uiState.copy(
                                availableFlatmates = memberNames,
                                availableSupermarkets = supermarkets,
                                selectedSupermarket = validFilters.firstOrNull(),
                                selectedSupermarkets = validFilters
                            )
                        }
                    }
                }

                launch {
                    repository.observeShoppingItems(flatId).collect { dbItems ->
                        uiState = uiState.copy(items = dbItems.map { it.toUi() })
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun onFilterSupermarket(name: String?) {
        uiState = uiState.copy(selectedSupermarket = name, selectedSupermarkets = if (name == null) emptySet() else setOf(name))
    }

    fun onFilterSupermarkets(names: Set<String>) {
        uiState = uiState.copy(selectedSupermarkets = names, selectedSupermarket = names.firstOrNull())
    }

    fun onDeleteItem(id: String) {
        val flatId = currentFlatId ?: return
        viewModelScope.launch { repository.deleteShoppingItem(flatId, id) }
    }

    fun setItemBought(itemId: String, bought: Boolean) {
        val flatId = currentFlatId ?: return
        viewModelScope.launch { repository.updateShoppingItemBought(flatId, itemId, bought) }
    }

    fun addItem(item: ShoppingItemUi) {
        val flatId = currentFlatId ?: return
        viewModelScope.launch { repository.addShoppingItem(flatId, item.toDb()) }
    }

    fun addSupermarket(alias: String, officialName: String, placeId: String, address: String, latitude: Double, longitude: Double, onResult: (Result<SupermarketUi>) -> Unit) {
        val flatId = currentFlatId ?: run { onResult(Result.failure(Exception("No flat selected"))); return }
        viewModelScope.launch {
            val result = repository.addSupermarketForFlat(flatId, Supermarket(name = alias, officialName = officialName, placeId = placeId, address = address, latitude = latitude, longitude = longitude))
            if (result.isSuccess) {
                val created = result.getOrThrow().toUi()
                uiState = uiState.copy(availableSupermarkets = (uiState.availableSupermarkets + created).distinctBy { it.id })
                onResult(Result.success(created))
            } else {
                onResult(Result.failure(result.exceptionOrNull() ?: Exception("Failed to add supermarket")))
            }
        }
    }

    fun addExpenseFromShopping(itemId: String, title: String, amount: Double, paidBy: String, splitBetween: List<String>) {
        val flatId = currentFlatId ?: return
        val newExpense = Expense(
            id = System.currentTimeMillis().toString(),
            title = title,
            amount = amount,
            paidBy = paidBy,
            splitBetween = splitBetween,
            ticketUri = null
        )
        viewModelScope.launch {
            repository.updateShoppingItemBought(flatId, itemId, true)
            repository.addExpense(flatId, newExpense)
        }
    }

    private fun ShoppingItem.toUi(): ShoppingItemUi = ShoppingItemUi(id, name, quantity, neededBy, supermarkets, bought)
    private fun ShoppingItemUi.toDb(): ShoppingItem = ShoppingItem(id, name, quantity, neededBy, supermarkets, bought)
    private fun Supermarket.toUi(): SupermarketUi = SupermarketUi(id ?: "", name, officialName, placeId, latitude, longitude, address)
}