package es.uc3m.android.chillmates.chores

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import es.uc3m.android.chillmates.model.Chore
import es.uc3m.android.chillmates.model.CHORES_SUBCOLLECTION
import es.uc3m.android.chillmates.model.ChoreAssignment
import es.uc3m.android.chillmates.model.USERS_COLLECTION
import es.uc3m.android.chillmates.repository.FlatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Calendar

class ChoresViewModel(
    private val repository: FlatRepository = FlatRepository()
) : ViewModel() {

    private val _chores = MutableStateFlow<List<Chore>>(emptyList())
    val chores: StateFlow<List<Chore>> = _chores

    private val _flatmates = MutableStateFlow<List<String>>(listOf("Everyone"))
    val flatmates: StateFlow<List<String>> = _flatmates

    private val _currentUserName = MutableStateFlow<String>("Me")
    val currentUserName: StateFlow<String> = _currentUserName

    private var currentFlatId: String? = null
    private var lastRotationChecked = false

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

                _currentUserName.value = userName
                currentFlatId = flatId

                launch {
                    repository.observeFlat(flatId).collect { flat ->
                        if (flat != null) {
                            val membersResult = repository.getMemberDetails(flat.memberIds)
                            if (membersResult.isSuccess) {
                                val memberNames = membersResult.getOrNull()?.map { it.second } ?: emptyList()
                                _flatmates.value = listOf("Everyone") + memberNames
                            }

                            if (!lastRotationChecked) {
                                lastRotationChecked = true
                                checkAutoRotation(flatId, flat.lastRotationTimestamp, flat.memberIds)
                            }
                        }
                    }
                }

                launch {
                    repository.observeChores(flatId).collect { dbChores ->
                        _chores.value = dbChores
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun checkAutoRotation(flatId: String, lastRotationTimestamp: Long, memberIds: List<String>) {
        viewModelScope.launch {
            val currentTime = System.currentTimeMillis()
            val oneWeekInMillis = 7L * 24 * 60 * 60 * 1000

            if (lastRotationTimestamp == 0L) {
                repository.updateFlatRotationTimestamp(flatId, currentTime)
            } else if (currentTime - lastRotationTimestamp >= oneWeekInMillis) {
                val currentChoresResult = repository.getChoresOnce(flatId)
                if (currentChoresResult.isSuccess) {
                    val currentChores = currentChoresResult.getOrNull() ?: emptyList()
                    performAutoRotation(currentChores, memberIds, flatId, currentTime)
                }
            }
        }
    }

    fun updateChoreStatus(choreId: String, done: Boolean) {
        val flatId = currentFlatId ?: return
        val weekKey = currentWeekKey()

        val currentList = _chores.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == choreId }

        if (index != -1) {
            val chore = currentList[index]
            val assignment = chore.assignments[weekKey]
            val assignee = assignment?.assignee ?: "Everyone"

            val newAssignments = chore.assignments.toMutableMap()
            newAssignments[weekKey] = ChoreAssignment(assignee = assignee, done = done)
            val updatedChore = chore.copy(assignments = newAssignments)

            currentList[index] = updatedChore
            _chores.value = currentList.toList()

            viewModelScope.launch {
                try {
                    repository.updateChoreStatus(flatId, choreId, weekKey, done, assignee)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun addChore(title: String, assignee: String) {
        val flatId = currentFlatId ?: return
        val weekKey = currentWeekKey()
        val newChore = Chore(
            id = System.currentTimeMillis().toString(),
            title = title,
            done = false,
            assignments = mapOf(weekKey to ChoreAssignment(assignee = assignee, done = false))
        )

        viewModelScope.launch {
            try {
                repository.addChore(flatId, newChore)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun swapChore(choreId: String, newAssignee: String) {
        val flatId = currentFlatId ?: return
        val weekKey = currentWeekKey()
        val currentChore = _chores.value.firstOrNull { it.id == choreId } ?: return
        val currentAssignment = currentChore.assignments[weekKey]
            ?: ChoreAssignment(assignee = "Everyone", done = false)

        viewModelScope.launch {
            try {
                repository.updateChoreAssignee(flatId, choreId, weekKey, newAssignee, currentAssignment.done)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun saveReconfiguredChores(updatedList: List<Chore>) {
        val flatId = currentFlatId ?: return

        viewModelScope.launch {
            try {
                repository.saveChoresBatch(flatId, updatedList)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteChore(choreId: String) {
        val flatId = currentFlatId ?: return
        viewModelScope.launch {
            try {
                repository.deleteChore(flatId, choreId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun performAutoRotation(currentChores: List<Chore>, memberIds: List<String>, flatId: String, currentTime: Long) {
        if (currentChores.isEmpty() || memberIds.isEmpty()) return

        viewModelScope.launch {
            val membersResult = repository.getMemberDetails(memberIds)
            val mates = membersResult.getOrNull()?.map { it.second } ?: return@launch
            if (mates.isEmpty()) return@launch
            val weekKey = weekKeyForTimestamp(currentTime)

            val updatedList = currentChores.map { chore ->
                val latestWeekKey = chore.assignments.keys.maxOrNull()
                val currentAssignee = latestWeekKey?.let { chore.assignments[it]?.assignee } ?: "Everyone"

                if (currentAssignee == "Everyone") {
                    val assignment = ChoreAssignment(assignee = currentAssignee, done = false)
                    chore.copy(
                        assignments = chore.assignments + (weekKey to assignment)
                    )
                } else {
                    val currentIndex = mates.indexOf(currentAssignee)
                    val nextAssignee = if (currentIndex != -1 && mates.size > 1) {
                        mates[(currentIndex + 1) % mates.size]
                    } else if (currentIndex == -1 && mates.isNotEmpty()) {
                        mates[0]
                    } else {
                        currentAssignee
                    }
                    val assignment = ChoreAssignment(assignee = nextAssignee, done = false)
                    chore.copy(
                        assignments = chore.assignments + (weekKey to assignment)
                    )
                }
            }

            try {
                repository.performAutoRotationBatch(flatId, updatedList, currentTime)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun currentWeekKey(): String {
        return weekKeyForTimestamp(System.currentTimeMillis())
    }

    private fun weekKeyForTimestamp(timestamp: Long): String {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = timestamp
            firstDayOfWeek = Calendar.MONDAY
            minimalDaysInFirstWeek = 4
        }
        val week = calendar.get(Calendar.WEEK_OF_YEAR)
        val year = calendar.get(Calendar.YEAR)
        return String.format("%04d-W%02d", year, week)
    }
}