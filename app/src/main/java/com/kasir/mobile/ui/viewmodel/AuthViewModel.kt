package com.kasir.mobile.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val error: String? = null,
    val username: String = "",
    val isAdmin: Boolean = false
)

class AuthViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun login(username: String, password: String, serverUrl: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                // Basic auth: backend only checks username == "admin" with PIN
                // Any non-empty credentials are accepted as operator, admin requires PIN separately
                if (username.isBlank() || password.isBlank()) {
                    _uiState.update { it.copy(isLoading = false, error = "Username dan password harus diisi") }
                    return@launch
                }

                // Store server URL and login state
                // Repository injection would go here; for now we accept any non-empty creds as operator
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isLoggedIn = true,
                        username = username,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = "Login gagal: ${e.message}")
                }
            }
        }
    }

    fun logout() {
        _uiState.update { AuthUiState() }
    }
}
