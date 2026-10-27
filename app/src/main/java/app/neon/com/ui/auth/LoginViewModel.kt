package app.neon.com.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseUser
import app.neon.com.data.repository.AuthRepoSecure
import app.neon.com.data.repository.AuthResult
import app.neon.com.util.Utils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val loggedInUser: FirebaseUser? = null,
    val generalError: String? = null,
    val email: String = "",
    val emailError: String? = null,
    val password: String = "",
    val passwordError: String? = null,
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean = false
)

class LoginViewModel(
    private val authRepoSecure: AuthRepoSecure = AuthRepoSecure()
) : ViewModel() {
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onEmailChanged(email: String) {
        _uiState.update { it.copy(email = Utils.sanitizeInput(email), emailError = null, generalError = null) }
    }

    fun onPasswordChanged(password: String) {
        _uiState.update { it.copy(password = password, passwordError = null, generalError = null) }
    }

    fun togglePasswordVisibility() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun login() {
        val current = _uiState.value
        var valid = true

        if (!Utils.isValidEmail(current.email)) {
            _uiState.update { it.copy(emailError = "Format email tidak valid") }
            valid = false
        }
        if (current.password.isBlank()) {
            _uiState.update { it.copy(passwordError = "Password tidak boleh kosong") }
            valid = false
        }
        if (!valid) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, generalError = null) }
            when (val result = authRepoSecure.signInWithEmail(current.email, current.password)) {
                is AuthResult.Success -> {
                    _uiState.update { it.copy(isLoading = false, loggedInUser = result.data) }
                }
                is AuthResult.Error -> {
                    _uiState.update { it.copy(isLoading = false, generalError = result.message) }
                }
            }
        }
    }
}
