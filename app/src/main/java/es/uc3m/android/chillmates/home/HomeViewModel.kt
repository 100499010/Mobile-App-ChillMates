package es.uc3m.android.chillmates.home

import android.app.Application
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import es.uc3m.android.chillmates.R
import es.uc3m.android.chillmates.model.Notice
import es.uc3m.android.chillmates.model.UpcomingEvent
import es.uc3m.android.chillmates.model.USERS_COLLECTION
import es.uc3m.android.chillmates.repository.FlatRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Immutable
data class NoticeUi(
    val id: String,
    val title: String,
    val details: String,
    val acceptedBy: List<MemberUi> = emptyList(),
    val declinedBy: List<MemberUi> = emptyList(),
    val poll: Boolean = false
)

@Immutable
data class MemberUi(val name: String)

@Immutable
data class UpcomingEventUi(
    val id: String,
    val dateLabel: String,
    val description: String,
    val isRemindMeEnabled: Boolean = false,
    val location: String = ""
)

@Immutable
data class HomeUiState(
    val notices: List<NoticeUi> = emptyList(),
    val upcomingEvents: List<UpcomingEventUi> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val repository = FlatRepository()

    var uiState by mutableStateOf(HomeUiState())
        private set

    init {
        loadHomeData()
    }

    private fun loadHomeData() {
        viewModelScope.launch {
            try {
                uiState = uiState.copy(isLoading = true, errorMessage = null)
                val userId = auth.currentUser?.uid
                if (userId == null) {
                    uiState = uiState.copy(isLoading = false, errorMessage = "User not logged in.")
                    return@launch
                }

                val userDoc = db.collection(USERS_COLLECTION).document(userId).get().await()
                val flatId = userDoc.getString("flatId")

                if (flatId.isNullOrEmpty()) {
                    uiState = uiState.copy(isLoading = false, errorMessage = "No flat joined.")
                    return@launch
                }

                val flatDoc = db.collection("flats").document(flatId).get().await()
                if (!flatDoc.exists()) {
                    uiState = uiState.copy(isLoading = false, errorMessage = "Flat not found.")
                    return@launch
                }

                launch {
                    repository.observeNotices(flatId).collect { notices ->
                        mapNoticesToUi(notices)
                    }
                }

                launch {
                    repository.observeEvents(flatId).collect { events ->
                        mapEventsToUi(events)
                    }
                }

            } catch (e: Exception) {
                uiState = uiState.copy(isLoading = false, errorMessage = e.message ?: "Error loading data")
            }
        }
    }

    private fun mapNoticesToUi(notices: List<Notice>) {
        val noticeUis = notices.map { n ->
            val acceptedList = n.acceptedBy?.map { MemberUi(it) } ?: emptyList()
            val declinedList = n.declinedBy?.map { MemberUi(it) } ?: emptyList()
            NoticeUi(
                id = n.id,
                title = n.title,
                details = n.details,
                acceptedBy = acceptedList,
                declinedBy = declinedList,
                poll = n.poll
            )
        }
        uiState = uiState.copy(notices = noticeUis, isLoading = false)
    }

    private fun mapEventsToUi(events: List<UpcomingEvent>) {
        val eventUis = events.map { e ->
            UpcomingEventUi(
                id = e.id,
                dateLabel = e.dateLabel,
                description = e.description,
                location = e.location ?: ""
            )
        }.sortedBy { it.dateLabel }

        uiState = uiState.copy(upcomingEvents = eventUis, isLoading = false)
    }

    fun acceptPoll(noticeId: String) {
        viewModelScope.launch {
            try {
                val userId = auth.currentUser?.uid ?: return@launch
                val userName = auth.currentUser?.displayName.takeIf { !it.isNullOrBlank() } ?: getApplication<Application>().resources.getString(R.string.home_fallback_user)
                val userDoc = db.collection(USERS_COLLECTION).document(userId).get().await()
                val flatId = userDoc.getString("flatId") ?: return@launch

                val currentNotice = uiState.notices.find { it.id == noticeId } ?: return@launch
                val currentAccepted = currentNotice.acceptedBy.map { it.name }.toMutableList()
                val currentDeclined = currentNotice.declinedBy.map { it.name }.toMutableList()

                if (!currentAccepted.contains(userName)) {
                    currentAccepted.add(userName)
                    repository.updateNoticeAcceptedBy(flatId, noticeId, currentAccepted)
                }
                if (currentDeclined.contains(userName)) {
                    currentDeclined.remove(userName)
                    repository.updateNoticeDeclinedBy(flatId, noticeId, currentDeclined)
                }
            } catch (e: Exception) { }
        }
    }

    fun declinePoll(noticeId: String) {
        viewModelScope.launch {
            try {
                val userId = auth.currentUser?.uid ?: return@launch
                val userName = auth.currentUser?.displayName.takeIf { !it.isNullOrBlank() } ?: getApplication<Application>().resources.getString(R.string.home_fallback_user)
                val userDoc = db.collection(USERS_COLLECTION).document(userId).get().await()
                val flatId = userDoc.getString("flatId") ?: return@launch

                val currentNotice = uiState.notices.find { it.id == noticeId } ?: return@launch
                val currentAccepted = currentNotice.acceptedBy.map { it.name }.toMutableList()
                val currentDeclined = currentNotice.declinedBy.map { it.name }.toMutableList()

                if (currentAccepted.contains(userName)) {
                    currentAccepted.remove(userName)
                    repository.updateNoticeAcceptedBy(flatId, noticeId, currentAccepted)
                }
                if (!currentDeclined.contains(userName)) {
                    currentDeclined.add(userName)
                    repository.updateNoticeDeclinedBy(flatId, noticeId, currentDeclined)
                }
            } catch (e: Exception) { }
        }
    }

    fun addNotice(title: String, details: String, poll: Boolean) {
        viewModelScope.launch {
            try {
                val userId = auth.currentUser?.uid ?: return@launch
                val userDoc = db.collection(USERS_COLLECTION).document(userId).get().await()
                val flatId = userDoc.getString("flatId") ?: return@launch

                val newNotice = es.uc3m.android.chillmates.model.Notice(
                    id = "n_${System.currentTimeMillis()}",
                    title = title,
                    details = details,
                    acceptedBy = emptyList(),
                    declinedBy = emptyList(),
                    poll = poll
                )
                repository.addNotice(flatId, newNotice)
            } catch (e: Exception) { }
        }
    }

    fun toggleReminder(eventId: String) {
        val updatedEvents = uiState.upcomingEvents.map {
            if (it.id == eventId) it.copy(isRemindMeEnabled = !it.isRemindMeEnabled) else it
        }
        uiState = uiState.copy(upcomingEvents = updatedEvents)
    }

    fun addEvent(dateLabel: String, description: String, location: String) {
        viewModelScope.launch {
            try {
                val userId = auth.currentUser?.uid ?: return@launch
                val userDoc = db.collection(USERS_COLLECTION).document(userId).get().await()
                val flatId = userDoc.getString("flatId") ?: return@launch

                val newEvent = es.uc3m.android.chillmates.model.UpcomingEvent(
                    id = "ev_${System.currentTimeMillis()}",
                    dateLabel = dateLabel,
                    description = description,
                    location = location
                )
                repository.addEvent(flatId, newEvent)
            } catch (e: Exception) { }
        }
    }
}