package app.neon.com.ui.security

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

enum class PinFlowType { Enable, Change, Disable }
enum class PinStep { VerifyCurrent, Create, Confirm }

data class PinFlowUiState(
    val step: PinStep? = null,
    val pad: PinPadState = PinPadState(),
    val isFinished: Boolean = false
)

@HiltViewModel
class PinFlowViewModel @Inject constructor(
    private val pinManager: PinManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(PinFlowUiState())
    val uiState: StateFlow<PinFlowUiState> = _uiState.asStateFlow()

    private var type: PinFlowType? = null
    private var entered = ""
    private var firstPin = ""
    private var countdownJob: Job? = null

    fun start(flowType: PinFlowType) {
        if (type == flowType) return
        type = flowType
        entered = ""
        firstPin = ""
        _uiState.value = PinFlowUiState(step = if (flowType == PinFlowType.Enable) PinStep.Create else PinStep.VerifyCurrent)
        if (flowType != PinFlowType.Enable) {
            viewModelScope.launch {
                try {
                    val seconds = pinManager.lockoutRemainingSeconds()
                    if (seconds > 0) {
                        _uiState.update { it.copy(pad = PinPadState(lockoutSeconds = seconds)) }
                        startCountdown()
                    }
                } catch (e: CancellationException) { throw e } catch (e: Exception) { }
            }
        }
    }

    fun onDigit(digit: Char) {
        val state = _uiState.value
        if (state.step == null || state.isFinished) return
        if (state.pad.isBusy || state.pad.lockoutSeconds > 0) return
        if (digit !in '0'..'9' || entered.length >= PinManager.PIN_LENGTH) return
        entered += digit
        _uiState.update { it.copy(pad = it.pad.copy(enteredLength = entered.length, errorMessage = null)) }
        if (entered.length == PinManager.PIN_LENGTH) onPinComplete()
    }

    fun onBackspace() {
        val state = _uiState.value
        if (state.step == null || state.pad.isBusy || entered.isEmpty()) return
        entered = entered.dropLast(1)
        _uiState.update { it.copy(pad = it.pad.copy(enteredLength = entered.length)) }
    }

    private fun onPinComplete() {
        val pin = entered
        entered = ""
        when (_uiState.value.step) {
            PinStep.VerifyCurrent -> verifyCurrent(pin)
            PinStep.Create -> {
                firstPin = pin
                _uiState.update { it.copy(step = PinStep.Confirm, pad = PinPadState()) }
            }
            PinStep.Confirm -> {
                if (pin == firstPin) save(pin)
                else {
                    firstPin = ""
                    _uiState.update { it.copy(step = PinStep.Create, pad = PinPadState(errorMessage = "PIN tidak cocok. Buat PIN dari awal.")) }
                }
            }
            null -> Unit
        }
    }

    private fun verifyCurrent(pin: String) {
        _uiState.update { it.copy(pad = it.pad.copy(isBusy = true)) }
        viewModelScope.launch {
            val result: PinVerifyResult? = try { pinManager.verify(pin) }
            catch (e: CancellationException) { throw e } catch (e: Exception) { null }
            when (result) {
                PinVerifyResult.Success, PinVerifyResult.NotSet -> {
                    if (type == PinFlowType.Disable) {
                        try {
                            pinManager.clearPin()
                            _uiState.update { it.copy(pad = PinPadState(), isFinished = true) }
                        } catch (e: CancellationException) { throw e }
                        catch (e: Exception) { _uiState.update { it.copy(pad = PinPadState(errorMessage = "Gagal menonaktifkan PIN. Coba lagi.")) } }
                    } else {
                        _uiState.update { it.copy(step = PinStep.Create, pad = PinPadState()) }
                    }
                }
                is PinVerifyResult.Wrong -> _uiState.update { it.copy(pad = PinPadState(errorMessage = "PIN salah. Sisa percobaan: ${result.attemptsLeftInRound}")) }
                is PinVerifyResult.LockedOut -> {
                    _uiState.update { it.copy(pad = PinPadState(lockoutSeconds = result.remainingSeconds)) }
                    startCountdown()
                }
                null -> _uiState.update { it.copy(pad = PinPadState(errorMessage = "PIN tidak dapat diperiksa. Coba lagi.")) }
            }
        }
    }

    private fun save(pin: String) {
        _uiState.update { it.copy(pad = it.pad.copy(isBusy = true)) }
        viewModelScope.launch {
            try {
                pinManager.setPin(pin)
                firstPin = ""
                _uiState.update { it.copy(pad = PinPadState(), isFinished = true) }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                firstPin = ""
                _uiState.update { it.copy(step = PinStep.Create, pad = PinPadState(errorMessage = "Gagal menyimpan PIN. Coba lagi.")) }
            }
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

@HiltViewModel
class AppLockSettingsViewModel @Inject constructor(
    private val pinManager: PinManager
) : ViewModel() {
    val isEnabled: StateFlow<Boolean> = pinManager.isEnabled

    fun refresh() {
        viewModelScope.launch {
            try { pinManager.isPinSet() } catch (e: CancellationException) { throw e } catch (e: Exception) { }
        }
    }
}
