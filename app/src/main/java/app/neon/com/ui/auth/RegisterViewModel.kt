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

data class RegisterUiState(
    val registeredUser: FirebaseUser? = null,
    val generalError: String? = null,
    val email: String = "",
    val emailError: String? = null,
    val password: String = "",
    val passwordError: String? = null,
    val isPasswordVisible: Boolean = false,
    val confirmPassword: String = "",
    val confirmPasswordError: String? = null,
    val isConfirmPasswordVisible: Boolean = false,
    val isLoading: Boolean = false
)

class RegisterViewModel(
    private val authRepoSecure: AuthRepoSecure = AuthRepoSecure()
) : ViewModel() {
    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    fun onEmailChanged(email: String) {
        _uiState.update { it.copy(email = Utils.sanitizeInput(email), emailError = null, generalError = null) }
    }

    fun onPasswordChanged(password: String) {
        _uiState.update { it.copy(password = password, passwordError = null, generalError = null) }
    }

    fun togglePasswordVisibility() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun onConfirmPasswordChanged(confirmPassword: String) {
        _uiState.update { it.copy(confirmPassword = confirmPassword, confirmPasswordError = null, generalError = null) }
    }

    fun toggleConfirmPasswordVisibility() {
        _uiState.update { it.copy(isConfirmPasswordVisible = !it.isConfirmPasswordVisible) }
    }

    fun register() {
        val current = _uiState.value
        var valid = true

        if (!Utils.isValidEmail(current.email)) {
            _uiState.update { it.copy(emailError = "Format email tidak valid") }
            valid = false
        }
        val passwordErrorMsg = Utils.getPasswordValidationErrorMessage(current.password)
        if (passwordErrorMsg != null) {
            _uiState.update { it.copy(passwordError = passwordErrorMsg) }
            valid = false
        }
        if (current.password != current.confirmPassword) {
            _uiState.update { it.copy(confirmPasswordError = "Password tidak cocok") }
            valid = false
        }
        if (!valid) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, generalError = null) }
            when (val result = authRepoSecure.signUpWithEmail(current.email, current.password)) {
                is AuthResult.Success -> {
                    val firebaseUser = result.data
                    // Bikin username acak otomatis
                    val randomUsername = "user${System.currentTimeMillis() % 100000000}"
                    
                    // Simpan profil ke database (pakai AuthRepoSecure lama yang punya createUserProfile)
                    val legacyRepo = app.neon.com.data.repository.AuthRepository()
                    legacyRepo.createUserProfile(firebaseUser.uid, current.email, randomUsername).collect { profileResult ->
                        profileResult.fold(
                            onSuccess = { user ->
                                _uiState.update { it.copy(isLoading = false, registeredUser = firebaseUser) }
                            },
                            onFailure = { error ->
                                _uiState.update { it.copy(isLoading = false, generalError = error.localizedMessage) }
                            }
                        )
                    }
                }
                is AuthResult.Error -> {
                    _uiState.update { it.copy(isLoading = false, generalError = result.message) }
                }
            }
        }
    }
}
