package es.uc3m.android.chillmates

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.ui.graphics.vector.ImageVector

sealed class NavGraph(val route: String, @StringRes val labelRes: Int, val icon: ImageVector) {

    data object Home : NavGraph("home", R.string.nav_home, Icons.Filled.Home)
    data object Chores : NavGraph("chores", R.string.nav_chores, Icons.Filled.CheckCircle)
    data object Expenses : NavGraph("expenses", R.string.nav_expenses, Icons.Filled.AttachMoney)
    data object Repairs : NavGraph("repairs", R.string.nav_repairs, Icons.Filled.Build)
    data object Shopping : NavGraph("shopping", R.string.nav_shop, Icons.Filled.ShoppingCart)

    // User profile screen (opened from top-right avatar)
    data object User : NavGraph("user", R.string.user_section_profile, Icons.Filled.Settings)

    // Settings profile screen (opened from top-right avatar)
    data object Settings : NavGraph("settings", R.string.settings_title, Icons.Filled.Person)

    // Dialog routes (modal bottom sheets)
    data object AddEvent : NavGraph("add_event", R.string.add_event_title, Icons.Filled.DateRange)
    data object AddExpense : NavGraph("add_expense", R.string.title_add_expense, Icons.Filled.DateRange)
    data object SelectSupermarket : NavGraph("select_supermarket", R.string.select_supermarket, Icons.Filled.ShoppingCart)

    // Route with argument example
    data object RepairDetail : NavGraph("repair_detail/{id}", R.string.repairs_title, Icons.Filled.Build) {
        fun createRoute(id: String) = "repair_detail/$id"
    }

    // Helper list for the Bottom Bar
    companion object {
        val bottomBarItems = listOf(Chores, Shopping, Home, Expenses, Repairs)

        const val REQUEST_EXPENSE_TITLE = "request_expense_title"
        const val REQUEST_EXPENSE_PAID_BY = "request_expense_paid_by"
        const val REQUEST_EXPENSE_SPLIT_BETWEEN = "request_expense_split_between"
        const val REQUEST_EXPENSE_AVAILABLE_FLATMATES = "request_expense_available_flatmates"
        const val REQUEST_EXPENSE_ITEM_ID = "request_expense_item_id"

        const val RESULT_EXPENSE_TITLE = "result_expense_title"
        const val RESULT_EXPENSE_AMOUNT = "result_expense_amount"
        const val RESULT_EXPENSE_PAID_BY = "result_expense_paid_by"
        const val RESULT_EXPENSE_SPLIT_BETWEEN = "result_expense_split_between"
        const val RESULT_EXPENSE_ITEM_ID = "result_expense_item_id"

        const val RESULT_EVENT_DATE_LABEL = "result_event_date_label"
        const val RESULT_EVENT_DESCRIPTION = "result_event_description"
        const val RESULT_EVENT_LOCATION = "result_event_location"

        const val REQUEST_MAP_SUPERMARKET_IDS = "request_map_supermarket_ids"
        const val REQUEST_MAP_SUPERMARKET_NAMES = "request_map_supermarket_names"
        const val REQUEST_MAP_SUPERMARKET_LATITUDES = "request_map_supermarket_latitudes"
        const val REQUEST_MAP_SUPERMARKET_LONGITUDES = "request_map_supermarket_longitudes"
        const val REQUEST_MAP_SELECTED_IDS = "request_map_selected_ids"
        const val RESULT_MAP_SELECTED_IDS = "result_map_selected_ids"
    }
}