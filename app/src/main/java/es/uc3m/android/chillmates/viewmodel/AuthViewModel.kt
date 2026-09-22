package es.uc3m.android.chillmates.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.userProfileChangeRequest
import es.uc3m.android.chillmates.R
import es.uc3m.android.chillmates.model.Flat
import es.uc3m.android.chillmates.model.User
import es.uc3m.android.chillmates.model.USERS_COLLECTION
import es.uc3m.android.chillmates.repository.FlatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await

sealed class AuthState {
    data object Loading : AuthState()
    data object Authenticated : AuthState()
    data object NeedsFlatSetup : AuthState()
    data object Unauthenticated : AuthState()
    data class Error(val message: String) : AuthState()
}

data class AuthUiState(
    val authState: AuthState = AuthState.Loading,
    val isLoginMode: Boolean = true,
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val displayName: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val flatSetupMode: FlatSetupMode = FlatSetupMode.NONE,
    val flatName: String = "",
    val inviteCode: String = "",
    val createdFlat: Flat? = null
)

enum class FlatSetupMode {
    NONE,
    CREATE,
    JOIN
}

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val flatRepository = FlatRepository()

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    init {
        checkAuthState()
    }

    fun checkAuthState() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(authState = AuthState.Loading)
            try {
                val currentUserFirebase = auth.currentUser
                if (currentUserFirebase != null) {
                    val user = loadUserFromFirestore(currentUserFirebase.uid)
                    _currentUser.value = user
                    if (user?.hasFlat() == true) {
                        _uiState.value = _uiState.value.copy(authState = AuthState.Authenticated)
                    } else {
                        _uiState.value = _uiState.value.copy(authState = AuthState.NeedsFlatSetup)
                    }
                } else {
                    _uiState.value = _uiState.value.copy(authState = AuthState.Unauthenticated)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(authState = AuthState.Unauthenticated)
            }
        }
    }

    private suspend fun loadUserFromFirestore(uid: String): User? {
        repeat(3) { attempt ->
            try {
                val doc = firestore.collection(USERS_COLLECTION).document(uid).get().await()
                val user = doc.toObject(User::class.java)?.apply { id = uid }
                if (user != null) return user
            } catch (e: Exception) {
                e.printStackTrace()
                if (attempt < 2) delay(500)
            }
        }
        return null
    }

    fun onEmailChange(email: String) {
        _uiState.value = _uiState.value.copy(email = email, errorMessage = null)
    }

    fun onPasswordChange(password: String) {
        _uiState.value = _uiState.value.copy(password = password, errorMessage = null)
    }

    fun onConfirmPasswordChange(confirmPassword: String) {
        _uiState.value = _uiState.value.copy(confirmPassword = confirmPassword, errorMessage = null)
    }

    fun onDisplayNameChange(displayName: String) {
        _uiState.value = _uiState.value.copy(displayName = displayName, errorMessage = null)
    }

    fun onToggleMode() {
        _uiState.value = _uiState.value.copy(
            isLoginMode = !_uiState.value.isLoginMode,
            errorMessage = null,
            confirmPassword = ""
        )
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun login() {
        val email = _uiState.value.email.trim()
        val password = _uiState.value.password

        if (email.isEmpty() || password.isEmpty()) {
            _uiState.value = _uiState.value.copy(errorMessage = getApplication<Application>().resources.getString(R.string.auth_error_fill_fields))
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                auth.signInWithEmailAndPassword(email, password).await()
                val firebaseUser = auth.currentUser
                if (firebaseUser != null) {
                    val user = loadUserFromFirestore(firebaseUser.uid)
                    _currentUser.value = user
                    if (user?.hasFlat() == true) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            authState = AuthState.Authenticated
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            authState = AuthState.NeedsFlatSetup
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Login failed"
                )
            }
        }
    }

    fun signUp() {
        val email = _uiState.value.email.trim()
        val password = _uiState.value.password
        val confirmPassword = _uiState.value.confirmPassword
        val displayName = _uiState.value.displayName.trim()

        if (email.isEmpty() || password.isEmpty() || displayName.isEmpty()) {
            _uiState.value = _uiState.value.copy(errorMessage = getApplication<Application>().resources.getString(R.string.auth_error_fill_fields))
            return
        }

        if (password != confirmPassword) {
            _uiState.value = _uiState.value.copy(errorMessage = getApplication<Application>().resources.getString(R.string.auth_error_passwords_no_match))
            return
        }

        if (password.length < 6) {
            _uiState.value = _uiState.value.copy(errorMessage = getApplication<Application>().resources.getString(R.string.auth_error_password_too_short))
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val result = auth.createUserWithEmailAndPassword(email, password).await()
                val firebaseUser = result.user
                if (firebaseUser != null) {
                    val profileUpdates = userProfileChangeRequest {
                        this.displayName = displayName
                    }
                    firebaseUser.updateProfile(profileUpdates).await()

                    val newUser = User(
                        id = firebaseUser.uid,
                        email = email,
                        displayName = displayName,
                        joinedDate = System.currentTimeMillis()
                    )
                    firestore.collection(USERS_COLLECTION)
                        .document(firebaseUser.uid)
                        .set(newUser)
                        .await()

                    _currentUser.value = newUser
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        authState = AuthState.NeedsFlatSetup,
                        flatSetupMode = FlatSetupMode.CREATE
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Sign up failed"
                )
            }
        }
    }

    fun setFlatSetupMode(mode: FlatSetupMode) {
        _uiState.value = _uiState.value.copy(
            flatSetupMode = mode,
            errorMessage = null,
            flatName = "",
            inviteCode = ""
        )
    }

    fun onFlatNameChange(name: String) {
        _uiState.value = _uiState.value.copy(flatName = name, errorMessage = null)
    }

    fun onInviteCodeChange(code: String) {
        _uiState.value = _uiState.value.copy(inviteCode = code.uppercase(), errorMessage = null)
    }

    fun completeFlatSetup() {
        val currentState = _uiState.value
        val userId = _currentUser.value?.id
        if (userId == null) {
            _uiState.value = _uiState.value.copy(
                errorMessage = getApplication<Application>().resources.getString(R.string.auth_error_user_not_loaded)
            )
            return
        }

        when (currentState.flatSetupMode) {
            FlatSetupMode.CREATE -> {
                val flatName = currentState.flatName.trim()
                if (flatName.isEmpty()) {
                    _uiState.value = currentState.copy(errorMessage = getApplication<Application>().resources.getString(R.string.auth_error_flat_name_required))
                    return
                }
                createFlat(flatName, userId)
            }
            FlatSetupMode.JOIN -> {
                val code = currentState.inviteCode.trim()
                if (code.isEmpty()) {
                    _uiState.value = currentState.copy(errorMessage = getApplication<Application>().resources.getString(R.string.auth_error_invite_code_required))
                    return
                }
                joinFlat(code, userId)
            }
            FlatSetupMode.NONE -> {
                _uiState.value = currentState.copy(errorMessage = getApplication<Application>().resources.getString(R.string.auth_error_select_option))
            }
        }
    }

    private fun createFlat(name: String, userId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val result = flatRepository.createFlat(name, userId)
                if (result.isSuccess) {
                    val flat = result.getOrThrow()

                    var updatedUser = loadUserFromFirestore(userId)
                    var retries = 0
                    while (updatedUser?.flatId == null && retries < 10) {
                        delay(500)
                        updatedUser = loadUserFromFirestore(userId)
                        retries++
                    }

                    _currentUser.value = updatedUser
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        createdFlat = flat,
                        authState = AuthState.Authenticated
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = result.exceptionOrNull()?.message ?: "Failed to create flat"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Failed to create flat"
                )
            }
        }
    }

    private fun joinFlat(code: String, userId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val result = flatRepository.joinFlat(code, userId)
                if (result.isSuccess) {

                    var updatedUser = loadUserFromFirestore(userId)
                    var retries = 0
                    while (updatedUser?.flatId == null && retries < 10) {
                        delay(500)
                        updatedUser = loadUserFromFirestore(userId)
                        retries++
                    }

                    _currentUser.value = updatedUser
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        authState = AuthState.Authenticated
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = result.exceptionOrNull()?.message ?: "Invalid invite code"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Failed to join flat"
                )
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            try {
                auth.signOut()
                _currentUser.value = null
                _uiState.value = AuthUiState(
                    authState = AuthState.Unauthenticated
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Logout failed"
                )
            }
        }
    }
}