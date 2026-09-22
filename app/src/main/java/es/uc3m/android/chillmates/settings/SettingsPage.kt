package es.uc3m.android.chillmates.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class SettingsUiState(
    val darkModeEnabled: Boolean = false,
    val soundEnabled: Boolean = true
)

class SettingsViewModel(
    private val dataStoreHelper: SettingsDataStoreHelper
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {

        viewModelScope.launch {
            dataStoreHelper.darkModeEnabled.collectLatest { enabled ->
                _uiState.value = _uiState.value.copy(darkModeEnabled = enabled)
                applyDarkMode(enabled)
            }
        }

        viewModelScope.launch {
            dataStoreHelper.soundEnabled.collectLatest { enabled ->
                _uiState.value = _uiState.value.copy(soundEnabled = enabled)
            }
        }
    }

    fun onDarkModeToggle(enabled: Boolean) {
        viewModelScope.launch {
            dataStoreHelper.saveDarkModeEnabled(enabled)
        }
    }

    fun onSoundToggle(enabled: Boolean) {
        viewModelScope.launch {
            dataStoreHelper.saveSoundEnabled(enabled)
        }
    }

    private fun applyDarkMode(enabled: Boolean) {
        val nightMode = if (enabled) {
            AppCompatDelegate.MODE_NIGHT_YES
        } else {
            AppCompatDelegate.MODE_NIGHT_NO
        }
        AppCompatDelegate.setDefaultNightMode(nightMode)
    }
}