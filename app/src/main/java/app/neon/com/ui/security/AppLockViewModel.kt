package app.neon.com.ui.security

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.neon.com.data.repository.AuthRepository
import app.neon.com.security.PinManager
import app.neon.com.security.PinVerifyResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class AppLockPhase { Loading, Locked, Unlocked }

data class PinPadState(
    val enteredLength: Int = 0,
    val errorMessage: String? = null,
    val lockoutSeconds: Int = 0,
    val isBusy: Boolean = false
)

data class AppLockUiState(
    val phase: AppLockPhase = AppLockPhase.Loading,
    val pad: PinPadState = PinPadState()
)

@HiltViewModel
class AppLockViewModel @Inject constructor(
    private val pinManager: PinManager,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppLockUiState())
    val uiState: StateFlow<AppLockUiState> = _uiState.asStateFlow()

    private var entered = ""
    private var backgroundedAt = 0L
    private var countdownJob: Job? = null

    init {
        viewModelScope.launch {
            try {
                if (pinManager.isPinSet()) {
                    _uiState.update { it.copy(phase = AppLockPhase.Locked) }
                    syncLockout()
                } else {
                    _uiState.update { it.copy(phase = AppLockPhase.Unlocked) }
                }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                _uiState.update {
                    AppLockUiState(
                        phase = AppLockPhase.Locked,
                        pad = PinPadState(errorMessage = "PIN tidak dapat dibaca. Gunakan \"Lupa PIN\" untuk mengatur ulang.")
                    )
                }
            }
        }
    }

    fun onDigit(digit: Char) {
        val state = _uiState.value
        if (state.phase != AppLockPhase.Locked) return
        if (state.pad.isBusy || state.pad.lockoutSeconds > 0) return
        if (digit !in '0'..'9' || entered.length >= PinManager.PIN_LENGTH) return
        entered += digit
        _uiState.update { it.copy(pad = it.pad.copy(enteredLength = entered.length, errorMessage = null)) }
        if (entered.length == PinManager.PIN_LENGTH) verifyEntered()
    }

    fun onBackspace() {
        val state = _uiState.value
        if (state.phase != AppLockPhase.Locked || state.pad.isBusy || entered.isEmpty()) return
        entered = entered.dropLast(1)
        _uiState.update { it.copy(pad = it.pad.copy(enteredLength = entered.length)) }
    }

    private fun verifyEntered() {
        val pin = entered
        _uiState.update { it.copy(pad = it.pad.copy(isBusy = true)) }
        viewModelScope.launch {
            val result: PinVerifyResult? = try { pinManager.verify(pin) }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { null }
            entered = ""
            when (result) {
                PinVerifyResult.Success, PinVerifyResult.NotSet -> {
                    countdownJob?.cancel()
                    _uiState.update { AppLockUiState(phase = AppLockPhase.Unlocked) }
                }
                is PinVerifyResult.Wrong -> _uiState.update {
                    it.copy(pad = PinPadState(errorMessage = "PIN salah. Sisa percobaan: ${result.attemptsLeftInRound}"))
                }
                is PinVerifyResult.LockedOut -> {
                    _uiState.update { it.copy(pad = PinPadState(lockoutSeconds = result.remainingSeconds)) }
                    startCountdown()
                }
                null -> _uiState.update { it.copy(pad = PinPadState(errorMessage = "PIN tidak dapat diperiksa. Coba lagi.")) }
            }
        }
    }

    fun onAppBackgrounded() {
        if (_uiState.value.phase == AppLockPhase.Unlocked) backgroundedAt = SystemClock.elapsedRealtime()
    }

    fun onAppForegrounded(relockAfterMillis: Long) {
        if (_uiState.value.phase != AppLockPhase.Unlocked || backgroundedAt == 0L) return
        val awayMillis = SystemClock.elapsedRealtime() - backgroundedAt
        backgroundedAt = 0L
        if (pinManager.isEnabled.value && awayMillis >= relockAfterMillis) {
            entered = ""
            _uiState.update { AppLockUiState(phase = AppLockPhase.Locked) }
            viewModelScope.launch {
                try { syncLockout() } catch (e: CancellationException) { throw e } catch (e: Exception) { }
            }
        }
    }

    fun forgotPin(onSignedOut: () -> Unit) {
        viewModelScope.launch {
            try { authRepository.signOut() } catch (e: CancellationException) { throw e } catch (e: Exception) { }
            try { pinManager.clearPin() } catch (e: CancellationException) { throw e } catch (e: Exception) { }
            entered = ""
            countdownJob?.cancel()
            _uiState.update { AppLockUiState(phase = AppLockPhase.Unlocked) }
            onSignedOut()
        }
    }

    private suspend fun syncLockout() {
        val seconds = pinManager.lockoutRemainingSeconds()
        if (seconds > 0) {
            _uiState.update { it.copy(pad = PinPadState(lockoutSeconds = seconds)) }
            startCountdown()
        }
    }

    private fun startCountdown() {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            while (_uiState.value.pad.lockoutSeconds > 0) {
                delay(1_000L)
                _uiState.update { it.copy(pad = it.pad.copy(lockoutSeconds = (it.pad.lockoutSeconds - 1).coerceAtLeast(0))) }
            }
        }
    }
}
