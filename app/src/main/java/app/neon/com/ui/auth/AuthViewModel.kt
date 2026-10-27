package app.neon.com.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.neon.com.data.model.User
import app.neon.com.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val username: String = "",
    val isPasswordVisible: Boolean = false,
    val isConfirmPasswordVisible: Boolean = false,
    val isCheckingUsername: Boolean = false,
    val isUsernameAvailable: Boolean? = null,
    val usernameStatusMessage: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val authenticatedUser: User? = null,
    val registeredUid: String? = null,
    val registeredEmail: String? = null
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private var usernameCheckJob: Job? = null

    init {
        checkExistingSession()
    }

    private fun checkExistingSession() {
        val cached = authRepository.getCachedUser()
        if (cached != null && cached.username.isNotBlank()) {
            _uiState.update { it.copy(authenticatedUser = cached) }
        }
    }

    fun onEmailChange(newEmail: String) {
        _uiState.update { it.copy(email = newEmail, errorMessage = null) }
    }

    fun onPasswordChange(newPassword: String) {
        _uiState.update { it.copy(password = newPassword, errorMessage = null) }
    }

    fun onConfirmPasswordChange(newConfirm: String) {
        _uiState.update { it.copy(confirmPassword = newConfirm, errorMessage = null) }
    }

    fun togglePasswordVisibility() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun toggleConfirmPasswordVisibility() {
        _uiState.update { it.copy(isConfirmPasswordVisible = !it.isConfirmPasswordVisible) }
    }

    fun onUsernameChange(rawUsername: String) {
        val sanitized = rawUsername.trim().lowercase().filter { it.isLetterOrDigit() || it == '_' }
        _uiState.update {
            it.copy(
                username = sanitized,
                errorMessage = null,
                isUsernameAvailable = null,
                usernameStatusMessage = null
            )
        }

        usernameCheckJob?.cancel()

        if (sanitized.length < 3) {
            _uiState.update {
                it.copy(
                    isCheckingUsername = false,
                    isUsernameAvailable = false,
                    usernameStatusMessage = if (sanitized.isNotEmpty()) "Minimal 3 karakter" else null
                )
            }
            return
        }

        usernameCheckJob = viewModelScope.launch {
            _uiState.update { it.copy(isCheckingUsername = true) }
            delay(400) // Debounce typing

            authRepository.checkUsernameAvailability(sanitized).collect { result ->
                result.fold(
                    onSuccess = { available ->
                        _uiState.update {
                            it.copy(
                                isCheckingUsername = false,
                                isUsernameAvailable = available,
                                usernameStatusMessage = if (available) "@$sanitized tersedia!" else "Username sudah terpakai"
                            )
                        }
                    },
                    onFailure = { error ->
                        _uiState.update {
                            it.copy(
                                isCheckingUsername = false,
                                isUsernameAvailable = false,
                                usernameStatusMessage = error.localizedMessage ?: "Gagal memeriksa username"
                            )
                        }
                    }
                )
            }
        }
    }

    fun login(
        onSuccess: (User) -> Unit,
        onRequiresUsername: (uid: String, email: String) -> Unit
    ) {
        val state = _uiState.value
        if (state.email.isBlank() || state.password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Email dan password wajib diisi!") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {

            authRepository.signInWithEmail(state.email, state.password).collect { result ->
                result.fold(
                    onSuccess = { user ->
                        _uiState.update { it.copy(isLoading = false, authenticatedUser = user) }
                        if (user.username.isBlank()) {
                            onRequiresUsername(user.uid, user.email)
                        } else {
                            onSuccess(user)
                        }
                    },
                    onFailure = { error ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = error.localizedMessage ?: "Login gagal. Periksa email/password."
                            )
                        }
                    }
                )
            }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun register(
        onSuccess: (uid: String, email: String) -> Unit
    ) {
        val state = _uiState.value
        if (state.email.isBlank() || state.password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Email dan password wajib diisi!") }
            return
        }
        if (state.password.length < 6) {
            _uiState.update { it.copy(errorMessage = "Password minimal 6 karakter!") }
            return
        }
        if (state.password != state.confirmPassword) {
            _uiState.update { it.copy(errorMessage = "Konfirmasi password tidak cocok!") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {

            authRepository.signUpWithEmail(state.email, state.password).collect { result ->
                result.fold(
                    onSuccess = { uid ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                registeredUid = uid,
                                registeredEmail = state.email
                            )
                        }
                        onSuccess(uid, state.email)
                    },
                    onFailure = { error ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = error.localizedMessage ?: "Pendaftaran akun gagal."
                            )
                        }
                    }
                )
            }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun createUsername(
        uid: String,
        email: String,
        onSuccess: (User) -> Unit
    ) {
        val state = _uiState.value
        if (state.username.isBlank() || state.username.length < 3) {
            _uiState.update { it.copy(errorMessage = "Username minimal 3 karakter!") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {

            val targetUid = if (uid.isNotBlank()) uid else state.registeredUid ?: ""
            val targetEmail = if (email.isNotBlank()) email else state.registeredEmail ?: state.email

            authRepository.createUserProfile(targetUid, targetEmail, state.username).collect { result ->
                result.fold(
                    onSuccess = { user ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                authenticatedUser = user
                            )
                        }
                        onSuccess(user)
                    },
                    onFailure = { error ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = error.localizedMessage ?: "Gagal membuat profil username."
                            )
                        }
                    }
                )
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }                        
}
