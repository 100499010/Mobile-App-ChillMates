package es.uc3m.android.chillmates

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import android.view.SoundEffectConstants
import android.view.HapticFeedbackConstants
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import es.uc3m.android.chillmates.chores.ChoresPage
import es.uc3m.android.chillmates.expensestracker.AddExpenseDialog
import es.uc3m.android.chillmates.home.HomePage
import es.uc3m.android.chillmates.home.AddEventScreen
import es.uc3m.android.chillmates.expensestracker.ExpensesTrackerPage
import es.uc3m.android.chillmates.repairs.RepairPage
import es.uc3m.android.chillmates.settings.SettingsDataStoreHelper
import es.uc3m.android.chillmates.settings.SettingsPage
import es.uc3m.android.chillmates.shoppinglist.SelectSupermarketScreen
import es.uc3m.android.chillmates.shoppinglist.Supermarket
import es.uc3m.android.chillmates.shoppinglist.ShoppingListPage
import es.uc3m.android.chillmates.ui.theme.ChillMatesTheme
import es.uc3m.android.chillmates.user.UserPage
import es.uc3m.android.chillmates.viewmodel.AuthState
import es.uc3m.android.chillmates.viewmodel.AuthViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val data: Uri? = intent?.data
        if (data != null && data.pathSegments.contains("join")) {
            val flatId = data.lastPathSegment
        }

        val dataStoreHelper = SettingsDataStoreHelper(applicationContext)

        enableEdgeToEdge()
        setContent {
            val isDarkMode by dataStoreHelper.darkModeEnabled.collectAsState(
                initial = isSystemInDarkTheme()
            )

            val soundEffectsEnabled by dataStoreHelper.soundEnabled.collectAsState(initial = true)

            ChillMatesTheme(darkTheme = isDarkMode) {
                ChillMatesApp(soundEffectsEnabled = soundEffectsEnabled)
            }
        }
    }
}

@Composable
fun WelcomeBackScreen(
    userName: String,
    flatName: String?,
    onContinue: () -> Unit,
    onChangeAccount: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.secondaryContainer
                            )
                        ),
                        shape = RoundedCornerShape(50)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = null,
                    modifier = Modifier.size(50.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "${stringResource(R.string.main_welcome_back)} ${userName.ifEmpty { stringResource(R.string.main_welcome_fallback_name) }}!",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.main_welcome_to),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = flatName ?: stringResource(R.string.main_welcome_fallback_flat),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = onContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.welcome_back_continue),
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(
                onClick = onChangeAccount,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Logout,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.welcome_back_change_account),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChillMatesApp(
    authViewModel: AuthViewModel = viewModel(),
    soundEffectsEnabled: Boolean = true
) {
    val authState by authViewModel.uiState.collectAsState()
    val currentUser by authViewModel.currentUser.collectAsState()

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val rootView = LocalView.current

    val loginRoute = stringResource(R.string.login_route)
    val welcomeBackRoute = "welcome-back"

    LaunchedEffect(soundEffectsEnabled) {
        rootView.isSoundEffectsEnabled = soundEffectsEnabled
    }

    if (authState.authState is AuthState.Loading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    if (authState.authState is AuthState.Authenticated && currentUser == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    if (authState.authState is AuthState.Unauthenticated) {
        LoginPage(navController, authViewModel)
        return
    }

    if (authState.authState is AuthState.NeedsFlatSetup) {
        FlatSetupPage(
            authViewModel = authViewModel,
            onComplete = {}
        )
        return
    }

    val startDestination = welcomeBackRoute

    val isAuxiliaryRoute = currentRoute == NavGraph.AddEvent.route ||
            currentRoute == NavGraph.AddExpense.route ||
            currentRoute == NavGraph.SelectSupermarket.route

    val showBottomBar = currentRoute != null &&
            currentRoute != loginRoute &&
            currentRoute != welcomeBackRoute &&
            !isAuxiliaryRoute

    val bottomNavItems = NavGraph.bottomBarItems

    NavigationSuiteScaffold(
        modifier = Modifier.pointerInput(soundEffectsEnabled) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent()
                    if (soundEffectsEnabled && event.changes.any { it.changedToDownIgnoreConsumed() }) {
                        rootView.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        rootView.playSoundEffect(SoundEffectConstants.CLICK)
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        navigationSuiteItems = {
            if (showBottomBar) {
                bottomNavItems.forEach { screen ->
                    item(
                        icon = {
                            Icon(
                                screen.icon,
                                contentDescription = stringResource(screen.labelRes)
                            )
                        },
                        label = { Text(stringResource(screen.labelRes)) },
                        selected = currentRoute == screen.route,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Scaffold(
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets(0.dp),
                topBar = {
                    if (showBottomBar) {
                        TopAppBar(
                            title = { Text(stringResource(R.string.app_name)) },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            actions = {
                                SettingsTopBarButton(
                                    onClick = { navController.navigate(NavGraph.Settings.route) }
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                ProfileTopBarButton(
                                    onClick = { navController.navigate(NavGraph.User.route) }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                        )
                    }
                }
            ) { innerPadding ->
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = startDestination
                    ) {
                        composable(loginRoute) {
                            LoginPage(navController, authViewModel)
                        }

                        composable(welcomeBackRoute) {
                            WelcomeBackScreen(
                                userName = currentUser?.displayName ?: "",
                                flatName = currentUser?.flatName,
                                onContinue = {
                                    navController.navigate(NavGraph.Home.route) {
                                        popUpTo(welcomeBackRoute) { inclusive = true }
                                    }
                                },
                                onChangeAccount = {
                                    authViewModel.logout()
                                }
                            )
                        }

                        composable(NavGraph.Home.route) {
                            HomePage(navController)
                        }

                        composable(NavGraph.Chores.route) {
                            ChoresPage(navController)
                        }

                        composable(NavGraph.Expenses.route) {
                            ExpensesTrackerPage(navController)
                        }

                        composable(NavGraph.Repairs.route) {
                            RepairPage(navController)
                        }

                        composable(NavGraph.Shopping.route) {
                            ShoppingListPage(navController)
                        }

                        composable(NavGraph.AddEvent.route) {
                            AddEventScreen(
                                onSave = { result ->
                                    navController.previousBackStackEntry?.savedStateHandle?.apply {
                                        set(NavGraph.RESULT_EVENT_DATE_LABEL, result.dateLabel)
                                        set(NavGraph.RESULT_EVENT_DESCRIPTION, result.description)
                                        set(NavGraph.RESULT_EVENT_LOCATION, result.location)
                                    }
                                    navController.popBackStack()
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(NavGraph.AddExpense.route) {
                            val requestHandle = navController.previousBackStackEntry?.savedStateHandle
                            val initialTitle = requestHandle?.get<String>(NavGraph.REQUEST_EXPENSE_TITLE).orEmpty()
                            val initialPaidBy = requestHandle?.get<String>(NavGraph.REQUEST_EXPENSE_PAID_BY).orEmpty()
                            val initialSplitBetween = requestHandle
                                ?.get<ArrayList<String>>(NavGraph.REQUEST_EXPENSE_SPLIT_BETWEEN)
                                ?.toList()
                                .orEmpty()
                            val availableFlatmates = requestHandle
                                ?.get<ArrayList<String>>(NavGraph.REQUEST_EXPENSE_AVAILABLE_FLATMATES)
                                ?.toList()
                                .orEmpty()
                            val pendingItemId = requestHandle?.get<String>(NavGraph.REQUEST_EXPENSE_ITEM_ID)

                            AddExpenseDialog(
                                availableFlatmates = availableFlatmates,
                                initialTitle = initialTitle,
                                initialPaidBy = initialPaidBy,
                                initialSplitBetween = initialSplitBetween,
                                onSave = { result ->
                                    navController.previousBackStackEntry?.savedStateHandle?.apply {
                                        set(NavGraph.RESULT_EXPENSE_TITLE, result.title)
                                        set(NavGraph.RESULT_EXPENSE_AMOUNT, result.amount)
                                        set(NavGraph.RESULT_EXPENSE_PAID_BY, result.paidBy)
                                        set(NavGraph.RESULT_EXPENSE_SPLIT_BETWEEN, ArrayList(result.splitBetween))
                                        set(NavGraph.RESULT_EXPENSE_ITEM_ID, pendingItemId)

                                        remove<String>(NavGraph.REQUEST_EXPENSE_TITLE)
                                        remove<String>(NavGraph.REQUEST_EXPENSE_PAID_BY)
                                        remove<ArrayList<String>>(NavGraph.REQUEST_EXPENSE_SPLIT_BETWEEN)
                                        remove<ArrayList<String>>(NavGraph.REQUEST_EXPENSE_AVAILABLE_FLATMATES)
                                        remove<String>(NavGraph.REQUEST_EXPENSE_ITEM_ID)
                                    }
                                    navController.popBackStack()
                                },
                                onDismiss = {
                                    navController.previousBackStackEntry?.savedStateHandle?.apply {
                                        remove<String>(NavGraph.REQUEST_EXPENSE_TITLE)
                                        remove<String>(NavGraph.REQUEST_EXPENSE_PAID_BY)
                                        remove<ArrayList<String>>(NavGraph.REQUEST_EXPENSE_SPLIT_BETWEEN)
                                        remove<ArrayList<String>>(NavGraph.REQUEST_EXPENSE_AVAILABLE_FLATMATES)
                                        remove<String>(NavGraph.REQUEST_EXPENSE_ITEM_ID)
                                    }
                                    navController.popBackStack()
                                }
                            )
                        }

                        composable(NavGraph.SelectSupermarket.route) {
                            val requestHandle = navController.previousBackStackEntry?.savedStateHandle
                            val ids = requestHandle
                                ?.get<ArrayList<String>>(NavGraph.REQUEST_MAP_SUPERMARKET_IDS)
                                ?.toList()
                                .orEmpty()
                            val names = requestHandle
                                ?.get<ArrayList<String>>(NavGraph.REQUEST_MAP_SUPERMARKET_NAMES)
                                ?.toList()
                                .orEmpty()
                            val latitudes = requestHandle
                                ?.get<ArrayList<Double>>(NavGraph.REQUEST_MAP_SUPERMARKET_LATITUDES)
                                ?.toList()
                                .orEmpty()
                            val longitudes = requestHandle
                                ?.get<ArrayList<Double>>(NavGraph.REQUEST_MAP_SUPERMARKET_LONGITUDES)
                                ?.toList()
                                .orEmpty()
                            val initialSelectedIds = requestHandle
                                ?.get<ArrayList<String>>(NavGraph.REQUEST_MAP_SELECTED_IDS)
                                ?.toSet()
                                .orEmpty()

                            val supermarkets = ids.indices.mapNotNull { index ->
                                val name = names.getOrNull(index) ?: return@mapNotNull null
                                val latitude = latitudes.getOrNull(index) ?: return@mapNotNull null
                                val longitude = longitudes.getOrNull(index) ?: return@mapNotNull null
                                Supermarket(
                                    id = ids[index],
                                    name = name,
                                    latitude = latitude,
                                    longitude = longitude
                                )
                            }

                            SelectSupermarketScreen(
                                supermarkets = supermarkets,
                                initialSelectedIds = initialSelectedIds,
                                onSupermarketSelected = { selectedIds ->
                                    navController.previousBackStackEntry?.savedStateHandle?.apply {
                                        set(NavGraph.RESULT_MAP_SELECTED_IDS, ArrayList(selectedIds))
                                        remove<ArrayList<String>>(NavGraph.REQUEST_MAP_SUPERMARKET_IDS)
                                        remove<ArrayList<String>>(NavGraph.REQUEST_MAP_SUPERMARKET_NAMES)
                                        remove<ArrayList<Double>>(NavGraph.REQUEST_MAP_SUPERMARKET_LATITUDES)
                                        remove<ArrayList<Double>>(NavGraph.REQUEST_MAP_SUPERMARKET_LONGITUDES)
                                        remove<ArrayList<String>>(NavGraph.REQUEST_MAP_SELECTED_IDS)
                                    }
                                    navController.popBackStack()
                                },
                                onDismiss = {
                                    navController.previousBackStackEntry?.savedStateHandle?.apply {
                                        remove<ArrayList<String>>(NavGraph.REQUEST_MAP_SUPERMARKET_IDS)
                                        remove<ArrayList<String>>(NavGraph.REQUEST_MAP_SUPERMARKET_NAMES)
                                        remove<ArrayList<Double>>(NavGraph.REQUEST_MAP_SUPERMARKET_LATITUDES)
                                        remove<ArrayList<Double>>(NavGraph.REQUEST_MAP_SUPERMARKET_LONGITUDES)
                                        remove<ArrayList<String>>(NavGraph.REQUEST_MAP_SELECTED_IDS)
                                    }
                                    navController.popBackStack()
                                }
                            )
                        }

                        composable(NavGraph.User.route) {
                            UserPage(navController, authViewModel)
                        }

                        composable(NavGraph.Settings.route) {
                            SettingsPage(
                                navController = navController,
                                onLogout = { authViewModel.logout() }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileTopBarButton(onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Surface(
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            Box(modifier = Modifier.size(34.dp), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = stringResource(R.string.nav_cd_profile),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun SettingsTopBarButton(onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Surface(
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            Box(modifier = Modifier.size(34.dp), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = stringResource(R.string.nav_cd_settings),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}