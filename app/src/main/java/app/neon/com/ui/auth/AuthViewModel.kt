package app.neon.com.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.neon.com.data.model.User
import app.neon.com.data.repository.AuthRepository
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
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

private fun Throwable.hasCause(predicate: (Throwable) -> Boolean): Boolean =
    generateSequence(this) { it.cause }.take(10).any(predicate)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private companion object {
        const val MAX_FAILED_ATTEMPTS = 5
        const val LOCKOUT_SECONDS = 30
        const val MIN_PASSWORD_LENGTH = 8
        const val REQUEST_TIMEOUT_MS = 20_000L
    }

    private val _uiState = MutableStateFlow(
        AuthUiState(currentUser = authRepository.getCachedUser())
    )
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    val currentUser: StateFlow<User?> = _uiState.map { it.currentUser }
        .stateIn(viewModelScope, SharingStarted.Eagerly, _uiState.value.currentUser)

    val errorMessage: StateFlow<String?> = _uiState.map { it.loginError ?: it.registrationError }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val isLoading: StateFlow<Boolean> = _uiState.map { it.isLoading }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    @Volatile
    private var lockoutJob: Job? = null

    fun signInWithEmail(email: String, password: String) {
        val state = _uiState.value
        if (state.isLoading) return
        if (state.isLoginLocked) {
            _uiState.update { it.copy(loginError = "Terlalu banyak percobaan. Coba lagi dalam ${it.lockoutRemainingSeconds} detik.") }
            return
        }

        val cleanEmail = email.trim()
        if (!isValidEmail(cleanEmail)) {
            _uiState.update { it.copy(loginError = "Format email tidak valid.") }
            return
        }
        if (password.isEmpty()) {
            _uiState.update { it.copy(loginError = "Password tidak boleh kosong.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, loginError = null) }
            val result: Result<User>? = try {
                withTimeoutOrNull(REQUEST_TIMEOUT_MS) {
                    authRepository.signInWithEmail(cleanEmail, password).flowOn(Dispatchers.IO).firstOrNull()
                }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { Result.failure(e) }
            handleSignInResult(result)
        }
    }

    private fun handleSignInResult(result: Result<User>?) {
        if (result == null) {
            _uiState.update { it.copy(isLoading = false, loginError = "Server tidak merespons. Periksa koneksi Anda.") }
            return
        }
        result.fold(
            onSuccess = { user ->
                _uiState.update { it.copy(currentUser = user, isLoading = false, loginError = null, failedLoginAttempts = 0) }
            },
            onFailure = { error ->
                if (error.hasCause { it is FirebaseNetworkException }) {
                    _uiState.update { it.copy(isLoading = false, loginError = "Tidak ada koneksi internet.") }
                } else {
                    var shouldLock = false
                    _uiState.update { s ->
                        val attempts = s.failedLoginAttempts + 1
                        shouldLock = attempts >= MAX_FAILED_ATTEMPTS
                        s.copy(
                            isLoading = false,
                            failedLoginAttempts = attempts,
                            loginError = if (shouldLock) "Terlalu banyak percobaan. Tunggu $LOCKOUT_SECONDS detik." else "Email atau password salah."
                        )
                    }
                    if (shouldLock) startLockoutTimer()
                }
            }
        )
    }

    fun signUpWithEmail(email: String, password: String, confirmPassword: String, onSuccess: () -> Unit = {}) {
        if (_uiState.value.isLoading) return
        val cleanEmail = email.trim()
        val validationError = when {
            !isValidEmail(cleanEmail) -> "Format email tidak valid."
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
            val result: Result<String>? = try {
                withTimeoutOrNull(REQUEST_TIMEOUT_MS) {
                    authRepository.signUpWithEmail(cleanEmail, password).flowOn(Dispatchers.IO).firstOrNull()
                }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { Result.failure(e) }
            handleSignUpResult(result, onSuccess)
        }
    }

    private fun handleSignUpResult(result: Result<String>?, onSuccess: () -> Unit) {
        if (result == null) {
            _uiState.update { it.copy(isLoading = false, registrationError = "Server tidak merespons.") }
            return
        }
        result.fold(
            onSuccess = {
                _uiState.update { it.copy(isLoading = false, registrationError = null) }
                onSuccess()
            },
            onFailure = { error ->
                val message = when {
                    error.hasCause { it is FirebaseAuthUserCollisionException } -> "Email sudah terdaftar."
                    error.hasCause { it is FirebaseNetworkException } -> "Tidak ada koneksi internet."
                    else -> "Pendaftaran gagal. Coba lagi."
                }
                _uiState.update { it.copy(isLoading = false, registrationError = message) }
            }
        )
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

    fun saveUsername(username: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val uid = authRepository.getCurrentFirebaseUser()?.uid
        val email = authRepository.getCachedUser()?.email ?: authRepository.getCurrentFirebaseUser()?.email
        if (uid.isNullOrBlank() || email.isNullOrBlank()) {
            onError("Sesi tidak ditemukan. Silakan daftar atau masuk ulang.")
            return
        }
        saveUsername(uid, email, username, onSuccess, onError)
    }

    fun saveUsername(uid: String, email: String, username: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            authRepository.createUserProfile(uid, email, username).collect { result ->
                result.fold(
                    onSuccess = { user ->
                        _uiState.update { it.copy(currentUser = user) }
                        onSuccess()
                    },
                    onFailure = { error -> onError(error.message ?: "Gagal menyimpan username") }
                )
            }
        }
    }

    private fun startLockoutTimer() {
        if (lockoutJob?.isActive == true) return
        lockoutJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoginLocked = true, lockoutRemainingSeconds = LOCKOUT_SECONDS) }
            for (remaining in LOCKOUT_SECONDS - 1 downTo 0) {
                delay(1_000L)
                _uiState.update { it.copy(lockoutRemainingSeconds = remaining) }
            }
            _uiState.update { it.copy(isLoginLocked = false, lockoutRemainingSeconds = 0, failedLoginAttempts = 0, loginError = null) }
        }
    }

    private fun isValidEmail(email: String): Boolean =
        email.isNotBlank() && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
}
