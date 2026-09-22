package es.uc3m.android.chillmates

import android.app.Application
import android.content.res.Resources
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import es.uc3m.android.chillmates.chores.ChoresViewModel
import es.uc3m.android.chillmates.expensestracker.ExpensesViewModel
import es.uc3m.android.chillmates.home.HomeViewModel
import es.uc3m.android.chillmates.model.*
import es.uc3m.android.chillmates.repository.FlatRepository
import es.uc3m.android.chillmates.shoppinglist.ShoppingItemUi
import es.uc3m.android.chillmates.shoppinglist.ShoppingViewModel
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ViewModelsUnitTest {


    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var mockAuth: FirebaseAuth
    private lateinit var mockUser: FirebaseUser
    private lateinit var mockDb: FirebaseFirestore
    private lateinit var mockRepository: FlatRepository
    private lateinit var mockApp: Application
    private lateinit var mockResources: Resources

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        mockkStatic(FirebaseAuth::class)
        mockkStatic(FirebaseFirestore::class)
        mockkStatic("kotlinx.coroutines.tasks.TasksKt")


        mockkConstructor(FlatRepository::class)

        mockAuth = mockk(relaxed = true)
        mockUser = mockk(relaxed = true)
        mockDb = mockk(relaxed = true)
        mockRepository = mockk(relaxed = true)
        mockApp = mockk(relaxed = true)
        mockResources = mockk(relaxed = true)

        every { FirebaseAuth.getInstance() } returns mockAuth
        every { mockAuth.currentUser } returns mockUser
        every { mockUser.uid } returns "testUserId"
        every { mockUser.displayName } returns "Test User"
        every { FirebaseFirestore.getInstance() } returns mockDb

        every { mockApp.resources } returns mockResources
        every { mockResources.getString(any()) } returns "Mock User"

        val mockCollection = mockk<CollectionReference>(relaxed = true)
        val mockDoc = mockk<DocumentReference>(relaxed = true)
        val mockTask = mockk<Task<DocumentSnapshot>>(relaxed = true)
        val mockSnapshot = mockk<DocumentSnapshot>(relaxed = true)

        every { mockDb.collection(any()) } returns mockCollection
        every { mockCollection.document(any()) } returns mockDoc
        every { mockDoc.get() } returns mockTask
        coEvery { mockTask.await() } returns mockSnapshot

        every { mockSnapshot.getString(any()) } returns "flat_1"
        every { mockSnapshot.exists() } returns true

        // Base Mocks for injected instances (Chores, Expenses, Shopping)
        coEvery { mockRepository.getMemberDetails(any()) } returns Result.success(listOf("u1" to "Alice", "u2" to "Bob"))
        coEvery { mockRepository.getSupermarketsForFlat(any()) } returns Result.success(listOf())
        coEvery { mockRepository.getChoresOnce(any()) } returns Result.success(listOf())

        every { mockRepository.observeFlat(any()) } returns flowOf(Flat(id = "flat_1", name = "Test Flat", memberIds = listOf("testUserId")))
        every { mockRepository.observeNotices(any()) } returns flowOf(emptyList())
        every { mockRepository.observeEvents(any()) } returns flowOf(emptyList())
        every { mockRepository.observeExpenses(any()) } returns flowOf(emptyList())
        every { mockRepository.observeChores(any()) } returns flowOf(emptyList())
        every { mockRepository.observeShoppingItems(any()) } returns flowOf(emptyList())

        // Base Mocks for internally constructed instances (HomeViewModel)
        every { anyConstructed<FlatRepository>().observeFlat(any()) } returns flowOf(Flat(id = "flat_1", name = "Test Flat", memberIds = listOf("testUserId")))
        every { anyConstructed<FlatRepository>().observeNotices(any()) } returns flowOf(emptyList())
        every { anyConstructed<FlatRepository>().observeEvents(any()) } returns flowOf(emptyList())
        every { anyConstructed<FlatRepository>().observeExpenses(any()) } returns flowOf(emptyList())
        every { anyConstructed<FlatRepository>().observeChores(any()) } returns flowOf(emptyList())
        every { anyConstructed<FlatRepository>().observeShoppingItems(any()) } returns flowOf(emptyList())
        coEvery { anyConstructed<FlatRepository>().getMemberDetails(any()) } returns Result.success(listOf("u1" to "Alice", "u2" to "Bob"))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
    }

    // ==========================================
    // 2.1 HomeViewModelTest
    // ==========================================

    @Test
    fun testHomeViewModel_onAddEvent_verifiesEventCreated() = runTest {
        val viewModel = HomeViewModel(mockApp)

        viewModel.addEvent("Mon 10:00", "Test Event", "Home")

        coVerify { anyConstructed<FlatRepository>().addEvent(any(), any()) }
    }

    @Test
    fun testHomeViewModel_onAcceptNotice_verifiesAcceptance() = runTest {
        val fakeNotice = Notice(id = "n_1", title = "Clean", details = "Clean the floor", poll = true)
        every { anyConstructed<FlatRepository>().observeNotices(any()) } returns flowOf(listOf(fakeNotice))

        val viewModel = HomeViewModel(mockApp)

        viewModel.acceptPoll("n_1")

        coVerify { anyConstructed<FlatRepository>().updateNoticeAcceptedBy(any(), eq("n_1"), any()) }
    }

    @Test
    fun testHomeViewModel_onToggleRemindMe_verifiesStateToggle() = runTest {
        val fakeEvent = UpcomingEvent(id = "e_1", dateLabel = "Mon 10:00", description = "Test")
        every { anyConstructed<FlatRepository>().observeEvents(any()) } returns flowOf(listOf(fakeEvent))

        val viewModel = HomeViewModel(mockApp)

        viewModel.toggleReminder("e_1")

        val updatedEvent = viewModel.uiState.upcomingEvents.first { it.id == "e_1" }
        assertTrue(updatedEvent.isRemindMeEnabled)
    }

    // ==========================================
    // 2.2 ExpensesViewModelTest
    // ==========================================

    @Test
    fun testExpensesViewModel_onExpenseAdded_verifiesExpenseInList() = runTest {
        val viewModel = ExpensesViewModel(mockRepository)

        viewModel.onExpenseAdded("Groceries", 50.0, "Alice", listOf("Alice", "Bob"))

        coVerify { mockRepository.addExpense(any(), any()) }
    }

    @Test
    fun testExpensesViewModel_recalculateBalances_verifiesPositiveNegativeDebt() = runTest {
        val expenses = listOf(
            Expense(id = "1", title = "Internet", amount = 100.0, paidBy = "Alice", splitBetween = listOf("Alice", "Bob"), ticketUri = null)
        )
        every { mockRepository.observeExpenses(any()) } returns flowOf(expenses)

        val viewModel = ExpensesViewModel(mockRepository)

        val balances = viewModel.uiState.balances
        val aliceBalance = balances.find { it.name == "Alice" }?.balance
        val bobBalance = balances.find { it.name == "Bob" }?.balance

        assertEquals(50.0, aliceBalance)
        assertEquals(-50.0, bobBalance)
    }

    // ==========================================
    // 2.3 ChoresViewModelTest
    // ==========================================

    @Test
    fun testChoresViewModel_toggleChoreDone_verifiesIsDoneFlip() = runTest {
        val fakeChore = Chore(
            id = "chore_1",
            title = "Trash",
            done = false,
            assignments = emptyMap()
        )
        every { mockRepository.observeChores(any()) } returns flowOf(listOf(fakeChore))

        val viewModel = ChoresViewModel(mockRepository)

        viewModel.updateChoreStatus("chore_1", true)

        coVerify { mockRepository.updateChoreStatus(any(), eq("chore_1"), any(), eq(true), any()) }
    }

    @Test
    fun testChoresViewModel_addRandomChore_verifiesChoreAdded() = runTest {
        val viewModel = ChoresViewModel(mockRepository)

        viewModel.addChore("Clean Windows", "Everyone")

        coVerify { mockRepository.addChore(any(), any()) }
    }

    // ==========================================
    // 2.4 ShoppingListViewModelTest
    // ==========================================

    @Test
    fun testShoppingViewModel_addItem_verifiesItemInList() = runTest {
        val viewModel = ShoppingViewModel(mockRepository)

        val newItem = ShoppingItemUi(id = "item_1", name = "Milk", quantity = "2", neededBy = listOf("Alice"), supermarkets = listOf(), bought = false)
        viewModel.addItem(newItem)

        coVerify { mockRepository.addShoppingItem(any(), any()) }
    }

    @Test
    fun testShoppingViewModel_setItemBought_verifiesBoughtFlip() = runTest {
        val viewModel = ShoppingViewModel(mockRepository)

        viewModel.setItemBought("item_1", true)

        coVerify { mockRepository.updateShoppingItemBought(any(), eq("item_1"), eq(true)) }
    }
}