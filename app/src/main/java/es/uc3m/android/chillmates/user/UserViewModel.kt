package es.uc3m.android.chillmates.user

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import es.uc3m.android.chillmates.R
import es.uc3m.android.chillmates.model.Flat
import es.uc3m.android.chillmates.model.User
import es.uc3m.android.chillmates.model.USERS_COLLECTION
import es.uc3m.android.chillmates.repository.FlatRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class FlatMemberUi(
    val id: String,
    val name: String
)

data class UserUiState(
    val user: User? = null,
    val flat: Flat? = null,
    val members: List<FlatMemberUi> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val showCreateJoinDialog: Boolean = false,
    val createJoinMode: CreateJoinMode = CreateJoinMode.CREATE,
    val dialogFlatName: String = "",
    val dialogInviteCode: String = ""
)

enum class CreateJoinMode {
    CREATE,
    JOIN
}

class UserViewModel(application: Application) : AndroidViewModel(application) {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val flatRepository = FlatRepository()

    var uiState by mutableStateOf(UserUiState())
        private set


    init {
        loadUserData()
    }

    fun loadUserData() {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true)
            try {
                val userId = auth.currentUser?.uid
                if (userId == null) {
                    uiState = uiState.copy(isLoading = false)
                    return@launch
                }

                val userDoc = firestore.collection(USERS_COLLECTION).document(userId).get().await()
                val user = userDoc.toObject(User::class.java)?.apply { id = userId }
                
                uiState = uiState.copy(user = user)
                
                if (user?.flatId != null) {
                    loadFlatData(user.flatId!!)
                } else {
                    uiState = uiState.copy(
                        isLoading = false,
                        flat = null,
                        members = emptyList()
                    )
                }
            } catch (e: Exception) {
                uiState = uiState.copy(
                    isLoading = false,
                    errorMessage = e.message
                )
            }
        }
    }

    private fun loadFlatData(flatId: String) {
        viewModelScope.launch {
            try {
                flatRepository.observeFlat(flatId).collect { flat ->
                    if (flat != null) {
                        uiState = uiState.copy(
                            flat = flat,
                            isLoading = false
                        )
                        loadMembers(flat.memberIds)
                    } else {
                        uiState = uiState.copy(
                            flat = null,
                            members = emptyList(),
                            isLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                uiState = uiState.copy(
                    isLoading = false,
                    errorMessage = e.message
                )
            }
        }
    }

    private fun loadMembers(memberIds: List<String>) {
        viewModelScope.launch {
            try {
                val members = memberIds.mapNotNull { userId ->
                    try {
                        val doc = firestore.collection(USERS_COLLECTION).document(userId).get().await()
                        val user = doc.toObject(User::class.java)
                        user?.let { FlatMemberUi(userId, it.displayName) }
                    } catch (e: Exception) {
                        null
                    }
                }
                uiState = uiState.copy(members = members)
            } catch (e: Exception) {
            }
        }
    }


    fun onShowCreateJoinDialog() {
        uiState = uiState.copy(
            showCreateJoinDialog = true,
            createJoinMode = CreateJoinMode.CREATE,
            dialogFlatName = "",
            dialogInviteCode = "",
            errorMessage = null,
            successMessage = null
        )
    }

    fun onDismissCreateJoinDialog() {
        uiState = uiState.copy(
            showCreateJoinDialog = false,
            errorMessage = null,
            successMessage = null
        )
    }

    fun onCreateJoinModeChange(mode: CreateJoinMode) {
        uiState = uiState.copy(
            createJoinMode = mode,
            errorMessage = null,
            dialogFlatName = if (mode == CreateJoinMode.JOIN) "" else uiState.dialogFlatName,
            dialogInviteCode = if (mode == CreateJoinMode.CREATE) "" else uiState.dialogInviteCode
        )
    }

    fun onDialogFlatNameChange(name: String) {
        uiState = uiState.copy(dialogFlatName = name, errorMessage = null)
    }

    fun onDialogInviteCodeChange(code: String) {
        uiState = uiState.copy(dialogInviteCode = code.uppercase(), errorMessage = null)
    }

    fun onConfirmCreateJoin() {
        val userId = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, errorMessage = null)

            when (uiState.createJoinMode) {
                CreateJoinMode.CREATE -> {
                    val flatName = uiState.dialogFlatName.trim()
                    if (flatName.isEmpty()) {
                        uiState = uiState.copy(
                            isLoading = false,
                            errorMessage = getApplication<Application>().resources.getString(R.string.user_error_flat_name_required)
                        )
                        return@launch
                    }

                    val result = flatRepository.createFlat(flatName, userId)
                    if (result.isSuccess) {
                        uiState = uiState.copy(
                            showCreateJoinDialog = false,
                            successMessage = getApplication<Application>().resources.getString(R.string.user_success_flat_created)
                        )
                        loadUserData()
                    } else {
                        uiState = uiState.copy(
                            isLoading = false,
                            errorMessage = result.exceptionOrNull()?.message ?: "Failed to create flat"
                        )
                    }
                }
                CreateJoinMode.JOIN -> {
                    val code = uiState.dialogInviteCode.trim()
                    if (code.isEmpty()) {
                        uiState = uiState.copy(
                            isLoading = false,
                            errorMessage = getApplication<Application>().resources.getString(R.string.user_error_invite_code_required)
                        )
                        return@launch
                    }

                    val result = flatRepository.joinFlat(code, userId)
                    if (result.isSuccess) {
                        uiState = uiState.copy(
                            showCreateJoinDialog = false,
                            successMessage = getApplication<Application>().resources.getString(R.string.user_success_flat_joined)
                        )
                        loadUserData()
                    } else {
                        uiState = uiState.copy(
                            isLoading = false,
                            errorMessage = result.exceptionOrNull()?.message ?: "Invalid invite code"
                        )
                    }
                }
            }
        }
    }

    fun onLeaveFlat() {
        val flatId = uiState.flat?.id ?: return
        val userId = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, errorMessage = null)

            val result = flatRepository.leaveFlat(flatId, userId)
            if (result.isSuccess) {
                uiState = uiState.copy(
                    flat = null,
                    members = emptyList(),
                    successMessage = getApplication<Application>().resources.getString(R.string.user_success_left_flat)
                )
                loadUserData()
            } else {
                uiState = uiState.copy(
                    isLoading = false,
                    errorMessage = result.exceptionOrNull()?.message ?: "Failed to leave flat"
                )
            }
        }
    }

    fun onChangePhotoClick(uri: Uri) {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: return@launch
            try {
                firestore.collection(USERS_COLLECTION).document(userId)
                    .update("avatarUrl", uri.toString())
                    .await()
                uiState = uiState.copy(
                    user = uiState.user?.copy(avatarUrl = uri.toString())
                )
            } catch (e: Exception) {
                uiState = uiState.copy(errorMessage = e.message)
            }
        }
    }

    fun onUpdateProfile(displayName: String) {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: return@launch
            val cleanName = displayName.trim()

            if (cleanName.isBlank()) return@launch

            uiState = uiState.copy(isLoading = true, errorMessage = null)

            try {
                firestore.collection(USERS_COLLECTION)
                    .document(userId)
                    .update("displayName", cleanName)
                    .await()

                auth.currentUser?.updateProfile(
                    com.google.firebase.auth.userProfileChangeRequest {
                        this.displayName = cleanName
                    }
                )?.await()

                uiState = uiState.copy(
                    isLoading = false,
                    user = uiState.user?.copy(displayName = cleanName),
                    successMessage = getApplication<Application>().resources.getString(R.string.user_success_name_updated)
                )

                uiState.flat?.memberIds?.let { memberIds ->
                    loadMembers(memberIds)
                }
            } catch (e: Exception) {
                uiState = uiState.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Failed to update name"
                )
            }
        }
    }

    fun clearError() {
        uiState = uiState.copy(errorMessage = null)
    }

    fun clearSuccessMessage() {
        uiState = uiState.copy(successMessage = null)
    }
    fun onUpdatePassword(currentPassword: String, newPassword: String) {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, errorMessage = null)
            try {
                val user = auth.currentUser ?: throw Exception("No user logged in")
                val email = user.email ?: throw Exception("No email found")

                val credential = com.google.firebase.auth.EmailAuthProvider.getCredential(email, currentPassword)
                user.reauthenticate(credential).await()

                user.updatePassword(newPassword).await()
                uiState = uiState.copy(
                    isLoading = false,
                    successMessage = getApplication<Application>().resources.getString(R.string.user_success_password_updated)
                )
            } catch (e: Exception) {
                val errorMessage = when (e) {
                    is com.google.firebase.auth.FirebaseAuthWeakPasswordException ->
                        getApplication<Application>().resources.getString(R.string.auth_error_password_too_short)
                    is com.google.firebase.auth.FirebaseAuthInvalidCredentialsException ->
                        "Current password is incorrect"
                    else -> e.message ?: "Error updating password"
                }
                uiState = uiState.copy(
                    isLoading = false,
                    errorMessage = errorMessage
                )
            }
        }
    }
    fun onLogout() {
        auth.signOut()
    }
}
