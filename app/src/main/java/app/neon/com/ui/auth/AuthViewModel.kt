package app.neon.com.ui.auth

import android.os.SystemClock
import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.neon.com.data.model.User
import app.neon.com.data.repository.AuthRepository
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

data class AuthUiState(
    val currentUser: User? = null,
    val isLoading: Boolean = false,
    val isPasswordVisible: Boolean = false,
    val loginError: String? = null,
    val registrationError: String? = null,
    val autoUsername: String = "",
    val resetPasswordStatus: String? = null
)

private fun Throwable.hasCause(predicate: (Throwable) -> Boolean): Boolean =
    generateSequence(this) { it.cause }.take(10).any(predicate)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    companion object {
        const val RESET_SUCCESS_MESSAGE = "Jika email terdaftar, link reset password telah dikirim. Periksa kotak masuk Anda."
        private const val MAX_FAILED_ATTEMPTS = 5
        private const val LOCKOUT_SECONDS = 30
        private const val RESET_COOLDOWN_SECONDS = 30
        private const val MIN_PASSWORD_LENGTH = 8
        private const val MAX_EMAIL_LENGTH = 254
        private const val REQUEST_TIMEOUT_MS = 20_000L
        private const val MAX_USERNAME_ATTEMPTS = 4
        private const val USERNAME_PREFIX = "userNE"
        private val UnsafeInput = Regex("[<>\"';]|--")

        @Volatile
        private var failedLoginAttempts = 0
        @Volatile
        private var lockedUntilMs = 0L
        @Volatile
        private var lastResetRequestMs = 0L
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

    fun signInWithEmail(email: String, password: String) {
        if (_uiState.value.isLoading) return
        val lockSeconds = secondsUntil(lockedUntilMs)
        if (lockSeconds > 0) {
            _uiState.update { it.copy(loginError = lockMessage(lockSeconds)) }
            return
        }
        val cleanEmail = email.trim()
        val emailError = validateEmail(cleanEmail)
        if (emailError != null) {
            _uiState.update { it.copy(loginError = emailError) }
            return
        }
        if (password.isEmpty()) {
            _uiState.update { it.copy(loginError = "Password tidak boleh kosong.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, loginError = null) }
            val result = awaitFirst(authRepository.signInWithEmail(cleanEmail, password))
            handleSignInResult(result)
        }
    }

    private fun handleSignInResult(result: Result<User>?) {
        if (result == null) {
            _uiState.update {
                it.copy(isLoading = false, loginError = "Server tidak merespons. Periksa koneksi Anda dan coba lagi.")
            }
            return
        }
        result.fold(
            onSuccess = { user ->
                failedLoginAttempts = 0
                _uiState.update { it.copy(currentUser = user, isLoading = false, loginError = null) }
            },
            onFailure = { error ->
                val message = when {
                    error.hasCause { it is FirebaseNetworkException } -> "Tidak ada koneksi internet. Coba lagi."
                    error.hasCause { it is FirebaseTooManyRequestsException } -> {
                        startLockout()
                        lockMessage(LOCKOUT_SECONDS)
                    }
                    registerFailedAttempt() -> lockMessage(LOCKOUT_SECONDS)
                    else -> "Email atau password salah."
                }
                _uiState.update { it.copy(isLoading = false, loginError = message) }
            }
        )
    }

    private fun registerFailedAttempt(): Boolean {
        failedLoginAttempts += 1
        if (failedLoginAttempts >= MAX_FAILED_ATTEMPTS) {
            startLockout()
            return true
        }
        return false
    }

    private fun startLockout() {
        failedLoginAttempts = 0
        lockedUntilMs = SystemClock.elapsedRealtime() + LOCKOUT_SECONDS * 1_000L
    }

    fun signUpWithEmail(email: String, password: String, confirmPassword: String, onSuccess: () -> Unit) {
        if (_uiState.value.isLoading) return
        val cleanEmail = email.trim()
        val validationError = validateEmail(cleanEmail)
            ?: validatePassword(password)
            ?: if (password != confirmPassword) "Konfirmasi password tidak cocok." else null
        if (validationError != null) {
            _uiState.update { it.copy(registrationError = validationError) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, registrationError = null) }
            val result = awaitFirst(authRepository.signUpWithEmail(cleanEmail, password))
            handleSignUpResult(result, cleanEmail, onSuccess)
        }
    }

    private suspend fun handleSignUpResult(result: Result<String>?, email: String, onRegistered: () -> Unit) {
        if (result == null) {
            _uiState.update {
                it.copy(isLoading = false, registrationError = "Server tidak merespons. Periksa koneksi Anda dan coba lagi.")
            }
            return
        }
        result.fold(
            onSuccess = { uid ->
                createAutoProfile(uid, email).fold(
                    onSuccess = { user ->
                        _uiState.update { it.copy(currentUser = user, isLoading = false, registrationError = null) }
                        onRegistered()
                    },
                    onFailure = {
                        _uiState.update {
                            it.copy(isLoading = false, registrationError = "Akun dibuat, tetapi profil gagal disimpan. Silakan masuk atau coba lagi.")
                        }
                    }
                )
            },
            onFailure = { error ->
                val message = when {
                    error.hasCause { it is FirebaseAuthUserCollisionException } -> "Email sudah terdaftar."
                    error.hasCause { it is FirebaseNetworkException } -> "Tidak ada koneksi internet. Coba lagi."
                    else -> "Pendaftaran gagal. Coba lagi."
                }
                _uiState.update { it.copy(isLoading = false, registrationError = message) }
            }
        )
    }

    private suspend fun createAutoProfile(uid: String, email: String): Result<User> {
        var lastError: Throwable = IllegalStateException("Profil gagal dibuat")
        repeat(MAX_USERNAME_ATTEMPTS) {
            val candidate = USERNAME_PREFIX + (1000..9999).random()
            _uiState.update { it.copy(autoUsername = candidate) }
            val isAvailable = awaitFirst(authRepository.checkUsernameAvailability(candidate))?.getOrNull()
            if (isAvailable == false) return@repeat
            val result = awaitFirst(authRepository.createUserProfile(uid, email, candidate))
            if (result == null) {
                lastError = IllegalStateException("Server tidak merespons")
            } else if (result.isSuccess) {
                return result
            } else {
                lastError = result.exceptionOrNull() ?: lastError
            }
        }
        return Result.failure(lastError)
    }

    fun saveUsername(username: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val firebaseUser = authRepository.getCurrentFirebaseUser()
        val uid = firebaseUser?.uid
        val email = authRepository.getCachedUser()?.email ?: firebaseUser?.email
        if (uid.isNullOrBlank() || email.isNullOrBlank()) {
            onError("Sesi tidak ditemukan. Silakan daftar atau masuk ulang.")
            return
        }
        if (UnsafeInput.containsMatchIn(username)) {
            onError("Username mengandung karakter yang tidak diizinkan.")
            return
        }
        viewModelScope.launch {
            val result = awaitFirst(authRepository.createUserProfile(uid, email, username))
            if (result == null) {
                onError("Server tidak merespons. Coba lagi.")
            } else {
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

    fun resetPassword(email: String) {
        if (_uiState.value.isLoading) return
        val cooldown = secondsUntil(lastResetRequestMs + RESET_COOLDOWN_SECONDS * 1_000L)
        if (cooldown > 0) {
            _uiState.update { it.copy(resetPasswordStatus = "Tunggu $cooldown detik sebelum mengirim ulang.") }
            return
        }
        val cleanEmail = email.trim()
        val emailError = validateEmail(cleanEmail)
        if (emailError != null) {
            _uiState.update { it.copy(resetPasswordStatus = emailError) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, resetPasswordStatus = null) }
            val message: String = try {
                val done = withTimeoutOrNull(REQUEST_TIMEOUT_MS) {
                    FirebaseAuth.getInstance().sendPasswordResetEmail(cleanEmail).await()
                    true
                }
                if (done == true) {
                    lastResetRequestMs = SystemClock.elapsedRealtime()
                    RESET_SUCCESS_MESSAGE
                } else {
                    "Server tidak merespons. Periksa koneksi Anda dan coba lagi."
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: FirebaseAuthInvalidUserException) {
                lastResetRequestMs = SystemClock.elapsedRealtime()
                RESET_SUCCESS_MESSAGE
            } catch (e: Exception) {
                when {
                    e.hasCause { it is FirebaseNetworkException } -> "Tidak ada koneksi internet. Coba lagi."
                    e.hasCause { it is FirebaseTooManyRequestsException } -> "Terlalu banyak permintaan. Coba lagi beberapa saat lagi."
                    else -> "Gagal mengirim link reset. Coba lagi."
                }
            }
            _uiState.update { it.copy(isLoading = false, resetPasswordStatus = message) }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(loginError = null, registrationError = null, resetPasswordStatus = null) }
    }

    fun togglePasswordVisibility() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    private suspend fun <T> awaitFirst(flow: Flow<Result<T>>): Result<T>? = try {
        withTimeoutOrNull(REQUEST_TIMEOUT_MS) { flow.flowOn(Dispatchers.IO).firstOrNull() }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    private fun secondsUntil(targetMs: Long): Int {
        val remainingMs = targetMs - SystemClock.elapsedRealtime()
        return if (remainingMs > 0) ((remainingMs + 999) / 1_000).toInt() else 0
    }

    private fun lockMessage(seconds: Int): String = "Terlalu banyak percobaan. Coba lagi dalam $seconds detik."

    private fun validateEmail(email: String): String? = when {
        email.isBlank() -> "Email tidak boleh kosong."
        email.length > MAX_EMAIL_LENGTH -> "Format email tidak valid."
        UnsafeInput.containsMatchIn(email) -> "Email mengandung karakter yang tidak diizinkan."
        !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> "Format email tidak valid."
        else -> null
    }

    private fun validatePassword(password: String): String? = when {
        password.length < MIN_PASSWORD_LENGTH -> "Password minimal $MIN_PASSWORD_LENGTH karakter."
        !password.any { it.isUpperCase() } -> "Password harus mengandung huruf besar."
        !password.any { it.isLowerCase() } -> "Password harus mengandung huruf kecil."
        !password.any { it.isDigit() } -> "Password harus mengandung angka."
        else -> null
    }
}
