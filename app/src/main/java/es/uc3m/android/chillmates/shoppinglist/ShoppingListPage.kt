package es.uc3m.android.chillmates.shoppinglist

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import es.uc3m.android.chillmates.BuildConfig
import es.uc3m.android.chillmates.LargeSectionHeader
import es.uc3m.android.chillmates.NavGraph
import es.uc3m.android.chillmates.PageContentWrapper
import es.uc3m.android.chillmates.R
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun ShoppingListPage(navController: NavController) {
    val vm: ShoppingViewModel = viewModel()
    val context = LocalContext.current
    val savedStateHandle = navController.currentBackStackEntry?.savedStateHandle

    var itemToPrompt by remember { mutableStateOf<ShoppingItemUi?>(null) }

    val selectedSupermarketIds = savedStateHandle
        ?.get<ArrayList<String>>(NavGraph.RESULT_MAP_SELECTED_IDS)
        ?.toSet()
    val selectedSupermarketNames = selectedSupermarketIds
        ?.mapNotNull { id ->
            vm.uiState.availableSupermarkets.firstOrNull { it.id == id }?.name
        }
        ?.toSet()

    val returnedExpenseTitle = savedStateHandle?.get<String>(NavGraph.RESULT_EXPENSE_TITLE)
    val returnedExpenseAmount = savedStateHandle?.get<Double>(NavGraph.RESULT_EXPENSE_AMOUNT)
    val returnedExpensePaidBy = savedStateHandle?.get<String>(NavGraph.RESULT_EXPENSE_PAID_BY)
    val returnedExpenseSplitBetween = savedStateHandle
        ?.get<ArrayList<String>>(NavGraph.RESULT_EXPENSE_SPLIT_BETWEEN)
        ?.toList()
    val returnedExpenseItemId = savedStateHandle?.get<String?>(NavGraph.RESULT_EXPENSE_ITEM_ID)

    LaunchedEffect(selectedSupermarketIds, vm.uiState.availableSupermarkets) {
        if (selectedSupermarketNames != null) {
            vm.onFilterSupermarkets(selectedSupermarketNames)
            savedStateHandle?.remove<ArrayList<String>>(NavGraph.RESULT_MAP_SELECTED_IDS)
        }
    }

    LaunchedEffect(returnedExpenseTitle, returnedExpenseAmount, returnedExpensePaidBy, returnedExpenseSplitBetween, returnedExpenseItemId) {
        if (
            returnedExpenseTitle != null &&
            returnedExpenseAmount != null &&
            returnedExpensePaidBy != null &&
            returnedExpenseSplitBetween != null &&
            returnedExpenseItemId != null
        ) {
            vm.addExpenseFromShopping(
                itemId = returnedExpenseItemId,
                title = returnedExpenseTitle,
                amount = returnedExpenseAmount,
                paidBy = returnedExpensePaidBy,
                splitBetween = returnedExpenseSplitBetween
            )

            savedStateHandle?.remove<String>(NavGraph.RESULT_EXPENSE_TITLE)
            savedStateHandle?.remove<Double>(NavGraph.RESULT_EXPENSE_AMOUNT)
            savedStateHandle?.remove<String>(NavGraph.RESULT_EXPENSE_PAID_BY)
            savedStateHandle?.remove<ArrayList<String>>(NavGraph.RESULT_EXPENSE_SPLIT_BETWEEN)
            savedStateHandle?.remove<String?>(NavGraph.RESULT_EXPENSE_ITEM_ID)
        }
    }

    ShoppingListRoute(
        state = vm.uiState,
        onFilter = vm::onFilterSupermarket,
        onBoughtAction = { item ->
            if (item.bought) {
                vm.setItemBought(item.id, false)
            } else {
                itemToPrompt = item
            }
        },
        onDelete = vm::onDeleteItem,
        onAddItem = vm::addItem,
        onAddSupermarket = { alias, officialName, placeId, address, latitude, longitude, onResult ->
            vm.addSupermarket(
                alias = alias,
                officialName = officialName,
                placeId = placeId,
                address = address,
                latitude = latitude,
                longitude = longitude,
                onResult = onResult
            )
        },
        onOpenMap = {
            val selectedIds = vm.uiState.selectedSupermarkets.mapNotNull { selectedName ->
                vm.uiState.availableSupermarkets.firstOrNull { it.name == selectedName }?.id
            }
             savedStateHandle?.set(
                 NavGraph.REQUEST_MAP_SUPERMARKET_IDS,
                 ArrayList(vm.uiState.availableSupermarkets.map { it.id })
             )
             savedStateHandle?.set(
                 NavGraph.REQUEST_MAP_SUPERMARKET_NAMES,
                 ArrayList(vm.uiState.availableSupermarkets.map { it.name })
             )
             savedStateHandle?.set(
                 NavGraph.REQUEST_MAP_SUPERMARKET_LATITUDES,
                 ArrayList(vm.uiState.availableSupermarkets.map { it.latitude })
             )
             savedStateHandle?.set(
                 NavGraph.REQUEST_MAP_SUPERMARKET_LONGITUDES,
                 ArrayList(vm.uiState.availableSupermarkets.map { it.longitude })
             )
             savedStateHandle?.set(
                 NavGraph.REQUEST_MAP_SELECTED_IDS,
                 ArrayList(selectedIds)
             )
             navController.navigate(NavGraph.SelectSupermarket.route)
         }
    )

    if (itemToPrompt != null) {
        AlertDialog(
            onDismissRequest = { itemToPrompt = null },
            title = { Text(stringResource(R.string.dialog_add_expense_title)) },
            text = {
                Text(stringResource(R.string.dialog_add_expense_text, itemToPrompt?.name ?: ""))
            },
            confirmButton = {
                Button(onClick = {
                    val item = itemToPrompt
                    if (item != null) {
                        savedStateHandle?.set(NavGraph.REQUEST_EXPENSE_ITEM_ID, item.id)
                        savedStateHandle?.set(NavGraph.REQUEST_EXPENSE_TITLE, item.name)
                        savedStateHandle?.set(NavGraph.REQUEST_EXPENSE_PAID_BY, vm.uiState.currentUserName)
                        savedStateHandle?.set(
                            NavGraph.REQUEST_EXPENSE_SPLIT_BETWEEN,
                            ArrayList(item.neededBy)
                        )
                        savedStateHandle?.set(
                            NavGraph.REQUEST_EXPENSE_AVAILABLE_FLATMATES,
                            ArrayList(vm.uiState.availableFlatmates)
                        )
                        navController.navigate(NavGraph.AddExpense.route)
                    }
                    itemToPrompt = null
                }) {
                    Text(stringResource(R.string.btn_create_expense))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    val item = itemToPrompt
                    if (item != null) {
                        vm.setItemBought(item.id, true)
                    }
                    itemToPrompt = null
                }) {
                    Text(stringResource(R.string.btn_not_now))
                }
            }
        )
    }

}

@Composable
fun ShoppingListRoute(
    state: ShoppingUiState,
    onFilter: (String?) -> Unit,
    onBoughtAction: (ShoppingItemUi) -> Unit,
    onDelete: (String) -> Unit,
    onAddItem: (ShoppingItemUi) -> Unit,
    onAddSupermarket: (String, String, String, String, Double, Double, (Result<SupermarketUi>) -> Unit) -> Unit,
    onOpenMap: () -> Unit
) {
    var showBoughtItems by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    val activeSupermarkets = when {
        state.selectedSupermarkets.isNotEmpty() -> state.selectedSupermarkets
        state.selectedSupermarket != null -> setOf(state.selectedSupermarket)
        else -> emptySet()
    }
    val supermarketLabel = when {
        activeSupermarkets.isEmpty() -> stringResource(R.string.filter_all)
        activeSupermarkets.size == 1 -> activeSupermarkets.first()
        else -> stringResource(R.string.filter_multi_format, activeSupermarkets.first(), activeSupermarkets.size - 1)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        LargeSectionHeader(title = stringResource(R.string.shopping_list_title))
        PageContentWrapper {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
                    .padding(top = 24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SupermarketDropdown(
                        selected = supermarketLabel,
                        onFilter = onFilter,
                        supermarkets = state.availableSupermarkets.map { it.name },
                        modifier = Modifier.weight(1f)
                    )

                    IconButton(onClick = onOpenMap) {
                        Icon(
                            Icons.Default.Map,
                            contentDescription = stringResource(R.string.cd_open_map)
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                Box(modifier = Modifier.weight(1f)) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val filteredItems = if (activeSupermarkets.isEmpty()) {
                            state.items
                        } else {
                            state.items.filter { item ->
                                item.supermarkets.any { it in activeSupermarkets }
                            }
                        }

                        val itemsToShow = if (showBoughtItems) {
                            filteredItems
                        } else {
                            filteredItems.filter { !it.bought }
                        }

                        items(itemsToShow) { item ->
                            ShoppingItemRow(
                                item = item,
                                onBoughtAction = { onBoughtAction(item) },
                                onDelete = onDelete
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier.padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { showAddDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.btn_add_new_item))
                    }

                    OutlinedButton(
                        onClick = { showBoughtItems = !showBoughtItems },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Text(
                            if (showBoughtItems) stringResource(R.string.btn_hide_bought)
                            else stringResource(R.string.btn_see_bought)
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddItemDialog(
            supermarkets = state.availableSupermarkets,
            flatmates = state.availableFlatmates,
            onDismiss = { showAddDialog = false },
            onAddSupermarket = onAddSupermarket,
            onAdd = { newItem ->
                onAddItem(newItem)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun SupermarketDropdown(
    supermarkets: List<String>,
    selected: String?,
    onFilter: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        OutlinedTextField(
            value = selected ?: stringResource(R.string.filter_all),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.filter_label)) },
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null
                )
            },
            modifier = Modifier
                .fillMaxWidth()
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.filter_all)) },
                onClick = {
                    onFilter(null)
                    expanded = false
                }
            )
            supermarkets.forEach { name ->
                DropdownMenuItem(
                    text = { Text(name) },
                    onClick = {
                        onFilter(name)
                        expanded = false
                    }
                )
            }
        }

        Surface(
            modifier = Modifier
                .matchParentSize(),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0f),
            onClick = { expanded = true }
        ) {}
    }
}

@Composable
private fun ShoppingItemRow(
    item: ShoppingItemUi,
    onBoughtAction: () -> Unit,
    onDelete: (String) -> Unit
) {
    val textDecoration = if (item.bought) TextDecoration.LineThrough else TextDecoration.None
    val alpha = if (item.bought) 0.6f else 1f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(alpha)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "${item.name} (${item.quantity})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textDecoration = textDecoration
            )

            val neededByText = if (item.neededBy.isEmpty()) {
                stringResource(R.string.needed_by_all)
            } else {
                stringResource(R.string.needed_by_format, item.neededBy.joinToString(", "))
            }

            Text(
                text = neededByText,
                style = MaterialTheme.typography.bodyMedium,
                fontStyle = FontStyle.Italic
            )
        }

        Spacer(Modifier.width(12.dp))

        Row {
            IconButton(onClick = onBoughtAction) {
                Icon(
                    imageVector = if (item.bought) Icons.AutoMirrored.Filled.Undo else Icons.Default.Check,
                    contentDescription = stringResource(R.string.cd_toggle_bought)
                )
            }
            IconButton(onClick = { onDelete(item.id) }) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.cd_delete)
                )
            }
        }
    }
}


@Composable
private fun AddItemDialog(
    supermarkets: List<SupermarketUi>,
    flatmates: List<String>,
    onDismiss: () -> Unit,
    onAddSupermarket: (String, String, String, String, Double, Double, (Result<SupermarketUi>) -> Unit) -> Unit,
    onAdd: (ShoppingItemUi) -> Unit
) {
    val addSupermarketFailedText = stringResource(R.string.error_add_supermarket_failed)
    var name by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var selectedSupermarkets by remember { mutableStateOf(supermarkets.map { it.name }.toSet()) }
    var selectedFlatmates by remember { mutableStateOf(flatmates.toSet()) }
    var showAddSupermarketDialog by remember { mutableStateOf(false) }
    var addSupermarketError by remember { mutableStateOf<String?>(null) }
    var localSupermarkets by remember(supermarkets) { mutableStateOf(supermarkets) }

    var supermarketsExpanded by remember { mutableStateOf(false) }
    var flatmatesExpanded by remember { mutableStateOf(false) }

    val marketListMaxHeight = 164.dp
    val flatmatesListHeight = 164.dp

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onAdd(
                            ShoppingItemUi(
                                id = System.currentTimeMillis().toString(),
                                name = name,
                                quantity = quantity,
                                neededBy = selectedFlatmates.toList(),
                                supermarkets = selectedSupermarkets.toList(),
                                bought = false
                            )
                        )
                    }
                }
            ) {
                Text(stringResource(R.string.btn_add))
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_cancel))
            }
        },
        title = { Text(stringResource(R.string.dialog_add_item_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.label_product_name)) },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    label = { Text(stringResource(R.string.label_quantity)) },
                    modifier = Modifier.fillMaxWidth()
                )

                // Supermarkets Section (Collapsible)
                Text(stringResource(R.string.label_supermarkets), fontWeight = FontWeight.Bold)

                OutlinedCard(
                    onClick = { supermarketsExpanded = !supermarketsExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${selectedSupermarkets.size} ${stringResource(R.string.label_selected)}",
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = if (supermarketsExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                contentDescription = null
                            )
                        }

                        if (supermarketsExpanded) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                TextButton(onClick = { 
                                    selectedSupermarkets = localSupermarkets.map { it.name }.toSet()
                                }) {
                                    Text(stringResource(R.string.btn_select_all))
                                }
                                TextButton(onClick = { 
                                    selectedSupermarkets = emptySet()
                                }) {
                                    Text(stringResource(R.string.btn_unselect_all))
                                }
                            }

                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = marketListMaxHeight),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(localSupermarkets, key = { it.id.ifBlank { it.name } }) { market ->
                                    val marketName = market.name
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(
                                            checked = marketName in selectedSupermarkets,
                                            onCheckedChange = { checked ->
                                                selectedSupermarkets =
                                                    if (checked) selectedSupermarkets + marketName
                                                    else selectedSupermarkets - marketName
                                            }
                                        )
                                        Text(market.displayLabel())
                                    }
                                }
                            }

                            OutlinedButton(
                                onClick = {
                                    addSupermarketError = null
                                    showAddSupermarketDialog = true
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(stringResource(R.string.btn_add_new_supermarket))
                            }

                            addSupermarketError?.let {
                                Text(
                                    text = it,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }

                // Flatmates Section (Collapsible)
                Text(stringResource(R.string.label_for_who), fontWeight = FontWeight.Bold)

                OutlinedCard(
                    onClick = { flatmatesExpanded = !flatmatesExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${selectedFlatmates.size} ${stringResource(R.string.label_selected)}",
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = if (flatmatesExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                contentDescription = null
                            )
                        }

                        if (flatmatesExpanded) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                TextButton(onClick = { 
                                    selectedFlatmates = flatmates.toSet()
                                }) {
                                    Text(stringResource(R.string.btn_select_all))
                                }
                                TextButton(onClick = { 
                                    selectedFlatmates = emptySet()
                                }) {
                                    Text(stringResource(R.string.btn_unselect_all))
                                }
                            }

                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(flatmatesListHeight),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(flatmates, key = { it }) { person ->
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(
                                            checked = person in selectedFlatmates,
                                            onCheckedChange = { checked ->
                                                selectedFlatmates =
                                                    if (checked) selectedFlatmates + person
                                                    else selectedFlatmates - person
                                            }
                                        )
                                        Text(person)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    )

    if (showAddSupermarketDialog) {
        AddSupermarketDialog(
            existingAliases = localSupermarkets.map { it.name },
            onDismiss = { showAddSupermarketDialog = false },
            onConfirm = { alias, place ->
                showAddSupermarketDialog = false
                onAddSupermarket(
                    alias,
                    place.officialName,
                    place.placeId,
                    place.address,
                    place.latitude,
                    place.longitude
                ) { result ->
                    result.onSuccess { added ->
                        localSupermarkets = (localSupermarkets + added).distinctBy { it.id }
                        selectedSupermarkets = selectedSupermarkets + added.name
                        addSupermarketError = null
                    }.onFailure {
                        addSupermarketError = it.message ?: addSupermarketFailedText
                    }
                }
            }
        )
    }
}


private data class PlacePredictionUi(
    val placeId: String,
    val primaryText: String,
    val secondaryText: String
)

private data class SelectedPlaceUi(
    val placeId: String,
    val officialName: String,
    val address: String,
    val latitude: Double,
    val longitude: Double
)

@Composable
private fun AddSupermarketDialog(
    existingAliases: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (String, SelectedPlaceUi) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val noPlacesFoundText = stringResource(R.string.error_no_places_found)
    val placesSearchFailedText = stringResource(R.string.error_places_search_failed)
    val placeDetailsFailedText = stringResource(R.string.error_place_details_failed)
    val placeMissingLocationText = stringResource(R.string.error_place_missing_location)
    val placesUnavailableText = stringResource(R.string.shopping_places_unavailable)

    var alias by remember { mutableStateOf("") }
    var query by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var isLoadingPlace by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf<String?>(null) }
    var predictions by remember { mutableStateOf<List<PlacePredictionUi>>(emptyList()) }
    var selectedPlace by remember { mutableStateOf<SelectedPlaceUi?>(null) }

    val aliasAlreadyExists = existingAliases.any { it.equals(alias.trim(), ignoreCase = true) }

    val placesClient = remember {
        val apiKey = BuildConfig.MAPS_API_KEY
        if (apiKey.isBlank()) {
            null
        } else {
            runCatching {
                if (!Places.isInitialized()) {
                    Places.initializeWithNewPlacesApiEnabled(context.applicationContext, apiKey)
                }
                Places.createClient(context.applicationContext)
            }.getOrNull()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_add_supermarket_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = alias,
                    onValueChange = { alias = it },
                    label = { Text(stringResource(R.string.label_supermarket_alias)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                if (aliasAlreadyExists && alias.isNotBlank()) {
                    Text(
                        text = stringResource(R.string.error_supermarket_alias_exists),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text(stringResource(R.string.label_search_place)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Button(
                    onClick = {
                        val client = placesClient
                        if (client == null) {
                            searchError = placesUnavailableText
                            return@Button
                        }
                        if (query.isBlank()) return@Button
                        isSearching = true
                        searchError = null
                        selectedPlace = null

                        scope.launch {
                            try {
                                val request = FindAutocompletePredictionsRequest.builder()
                                    .setQuery(query)
                                    .build()
                                val response = client.findAutocompletePredictions(request).await()
                                predictions = response.autocompletePredictions.map {
                                    PlacePredictionUi(
                                        placeId = it.placeId,
                                        primaryText = it.getPrimaryText(null).toString(),
                                        secondaryText = it.getSecondaryText(null).toString()
                                    )
                                }
                                if (predictions.isEmpty()) {
                                    searchError = noPlacesFoundText
                                }
                            } catch (e: Exception) {
                                searchError = e.message ?: placesSearchFailedText
                                predictions = emptyList()
                            } finally {
                                isSearching = false
                            }
                        }
                    },
                    enabled = !isSearching && placesClient != null,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Search, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.btn_search_place))
                }

                if (isSearching || isLoadingPlace) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(predictions, key = { it.placeId }) { prediction ->
                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                isLoadingPlace = true
                                searchError = null
                                scope.launch {
                                    try {
                                        val fields = listOf(
                                            Place.Field.ID,
                                            Place.Field.DISPLAY_NAME,
                                            Place.Field.FORMATTED_ADDRESS,
                                            Place.Field.LOCATION
                                        )
                                        val request = FetchPlaceRequest.builder(prediction.placeId, fields).build()
                                        val client = placesClient ?: return@launch
                                        val place = client.fetchPlace(request).await().place
                                        val location = place.location
                                        if (location != null) {
                                            selectedPlace = SelectedPlaceUi(
                                                placeId = place.id.orEmpty(),
                                                officialName = place.displayName ?: prediction.primaryText,
                                                address = place.formattedAddress.orEmpty(),
                                                latitude = location.latitude,
                                                longitude = location.longitude
                                            )
                                        } else {
                                            searchError = placeMissingLocationText
                                        }
                                    } catch (e: Exception) {
                                        searchError = e.message ?: placeDetailsFailedText
                                    }finally {
                                        isLoadingPlace = false
                                    }
                                }
                            }
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(prediction.primaryText, fontWeight = FontWeight.SemiBold)
                                if (prediction.secondaryText.isNotBlank()) {
                                    Text(
                                        prediction.secondaryText,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                selectedPlace?.let {
                    Text(
                        text = stringResource(R.string.label_selected_place, it.officialName),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    if (it.address.isNotBlank()) {
                        Text(
                            text = it.address,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                searchError?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val place = selectedPlace ?: return@Button
                    onConfirm(alias.trim(), place)
                },
                enabled = alias.trim().isNotEmpty() && !aliasAlreadyExists && selectedPlace != null
            ) {
                Text(stringResource(R.string.btn_confirm_add_supermarket))
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_cancel))
            }
        }
    )
}

private fun SupermarketUi.displayLabel(): String {
    return if (officialName.isNotBlank() && !officialName.equals(name, ignoreCase = true)) {
        "$name ($officialName)"
    } else {
        name
    }
}
