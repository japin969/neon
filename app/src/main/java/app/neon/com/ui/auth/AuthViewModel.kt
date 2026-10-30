package app.neon.com.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.neon.com.data.model.User
import app.neon.com.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val currentUser: User? = null,
    val isLoading: Boolean = false,
    val isPasswordVisible: Boolean = false,
    val loginError: String? = null,
    val registrationError: String? = null,
    val isLoginLocked: Boolean = false,
    val lockoutRemainingSeconds: Int = 0,
    val failedLoginAttempts: Int = 0,
    val autoUsername: String = ""
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private companion object {
        const val MAX_FAILED_ATTEMPTS = 5
        const val LOCKOUT_SECONDS = 30
        const val MIN_PASSWORD_LENGTH = 8
    }

    private val _uiState = MutableStateFlow(
        AuthUiState(currentUser = authRepository.getCachedUser())
    )
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    val currentUser: StateFlow<User?> = _uiState
        .map { it.currentUser }
        .stateIn(viewModelScope, SharingStarted.Eagerly, _uiState.value.currentUser)

    val errorMessage: StateFlow<String?> = _uiState
        .map { it.loginError ?: it.registrationError }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val isLoading: StateFlow<Boolean> = _uiState
        .map { it.isLoading }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private var lockoutJob: Job? = null

    fun signInWithEmail(email: String, password: String) {
        val state = _uiState.value
        if (state.isLoading) return
        if (state.isLoginLocked) {
            _uiState.update {
                it.copy(loginError = "Terlalu banyak percobaan. Coba lagi dalam ${it.lockoutRemainingSeconds} detik.")
            }
            return
        }

        val cleanEmail = email.trim()
        if (cleanEmail.isBlank() || password.isEmpty()) {
            _uiState.update { it.copy(loginError = "Email dan password wajib diisi.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, loginError = null) }
            authRepository.signInWithEmail(cleanEmail, password).collect { result ->
                result.fold(
                    onSuccess = { user ->
                        _uiState.update {
                            it.copy(currentUser = user, isLoading = false, loginError = null, failedLoginAttempts = 0)
                        }
                    },
                    onFailure = { error ->
                        val attempts = _uiState.value.failedLoginAttempts + 1
                        val shouldLock = attempts >= MAX_FAILED_ATTEMPTS
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                failedLoginAttempts = attempts,
                                loginError = if (shouldLock) {
                                    "Terlalu banyak percobaan. Coba lagi dalam $LOCKOUT_SECONDS detik."
                                } else {
                                    error.message ?: "Gagal masuk. Periksa email dan password Anda."
                                }
                            )
                        }
                        if (shouldLock) startLockoutTimer()
                    }
                )
            }
        }
    }

    fun signUpWithEmail(email: String, password: String, confirmPassword: String) {
        if (_uiState.value.isLoading) return
        val cleanEmail = email.trim()

        val validationError = when {
            cleanEmail.isBlank() -> "Email wajib diisi."
            password.length < MIN_PASSWORD_LENGTH -> "Password minimal $MIN_PASSWORD_LENGTH karakter."
            !password.any { it.isUpperCase() } -> "Password harus mengandung huruf besar."
            !password.any { it.isLowerCase() } -> "Password harus mengandung huruf kecil."
            !password.any { it.isDigit() } -> "Password harus mengandung angka."
            password != confirmPassword -> "Konfirmasi password tidak cocok."
            else -> null
        }

        if (validationError != null) {
            _uiState.update { it.copy(registrationError = validationError) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, registrationError = null) }
            authRepository.signUpWithEmail(cleanEmail, password).collect { result ->
                result.fold(
                    onSuccess = { uid ->
                        val user = authRepository.getCachedUser() ?: User(uid = uid, email = cleanEmail)
                        _uiState.update { it.copy(currentUser = user, isLoading = false, registrationError = null) }
                    },
                    onFailure = { error ->
                        _uiState.update {
                            it.copy(isLoading = false, registrationError = error.message ?: "Pendaftaran gagal. Coba lagi.")
                        }
                    }
                )
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(loginError = null, registrationError = null) }
    }

    fun togglePasswordVisibility() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun generateAutoUsername() {
        val suffix = (1000..9999).random()
        _uiState.update { it.copy(autoUsername = "neont_$suffix") }
    }

    fun saveAutoUsername(onSuccess: () -> Unit) {
        if (_uiState.value.autoUsername.isBlank()) generateAutoUsername()
        onSuccess()
    }

    private fun startLockoutTimer() {
        lockoutJob?.cancel()
        lockoutJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoginLocked = true, lockoutRemainingSeconds = LOCKOUT_SECONDS) }
            for (remaining in LOCKOUT_SECONDS - 1 downTo 0) {
                delay(1_000L)
                _uiState.update { it.copy(lockoutRemainingSeconds = remaining) }
            }
            _uiState.update {
                it.copy(isLoginLocked = false, lockoutRemainingSeconds = 0, failedLoginAttempts = 0, loginError = null)
            }
        }
    }
}
