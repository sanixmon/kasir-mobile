package com.kasir.mobile.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kasir.mobile.data.ServiceLocator
import com.kasir.mobile.data.local.SessionManager
import com.kasir.mobile.data.model.UserDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val error: String? = null,
    val username: String = "",
    val isAdmin: Boolean = false,
    val serverUrl: String = "",
    val users: List<UserDto> = emptyList()
)

class AuthViewModel(private val app: Application) : AndroidViewModel(app) {

    private val sessionManager = SessionManager(app)

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        // Restore the last used server URL
        viewModelScope.launch {
            val session = sessionManager.sessionDataFlow.first()
            _uiState.update { it.copy(serverUrl = session.serverUrl.ifBlank { ServiceLocator.DEFAULT_SERVER_URL }) }
        }
    }

    fun setServerUrl(url: String) {
        _uiState.update { it.copy(serverUrl = url.trim()) }
    }

    /**
     * Cashier shift login. Verified server-side via the login_cashier RPC
     * (empty stored password falls back to the default 'jayalahevren').
     */
    fun loginAsCashier(username: String, password: String, serverUrl: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            if (username.isBlank() || password.isBlank()) {
                _uiState.update { it.copy(isLoading = false, error = "Username dan password harus diisi") }
                return@launch
            }
            try {
                ServiceLocator.setServerUrl(serverUrl)
                val repository = ServiceLocator.repository()
                val res = repository.loginCashier(username.trim(), password).getOrThrow()

                if (!res.success || res.user == null) {
                    _uiState.update {
                        it.copy(isLoading = false, error = res.error ?: "Nama kasir tidak ditemukan!")
                    }
                    return@launch
                }

                val userMatch = res.user
                sessionManager.setServerUrl(serverUrl)
                sessionManager.setCurrentUser(userMatch.username, userMatch.username)
                val isAdmin = userMatch.role.equals("admin", ignoreCase = true)
                sessionManager.setAdminStatus(isAdmin)

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isLoggedIn = true,
                        username = userMatch.username,
                        isAdmin = isAdmin,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Tidak dapat terhubung ke server: ${e.message ?: "Periksa URL server"}"
                    )
                }
            }
        }
    }

    /**
     * Admin portal login. Mirrors kasir-db RoleSelection admin flow:
     * verifies the admin password via the verify_admin RPC.
     */
    fun loginAsAdmin(password: String, serverUrl: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            if (password.isBlank()) {
                _uiState.update { it.copy(isLoading = false, error = "Password admin harus diisi") }
                return@launch
            }
            try {
                ServiceLocator.setServerUrl(serverUrl)
                val repository = ServiceLocator.repository()
                val res = repository.verifyAdmin(password).getOrThrow()
                if (!res.valid) {
                    _uiState.update { it.copy(isLoading = false, error = "Password admin tidak sesuai!") }
                    return@launch
                }

                sessionManager.setServerUrl(serverUrl)
                sessionManager.setCurrentUser("admin", "Admin")
                sessionManager.setAdminStatus(true)

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isLoggedIn = true,
                        username = "Admin",
                        isAdmin = true,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Tidak dapat terhubung ke server: ${e.message ?: "Periksa URL server"}"
                    )
                }
            }
        }
    }

    fun logout() {
        _uiState.update { AuthUiState(serverUrl = _uiState.value.serverUrl) }
    }
}
