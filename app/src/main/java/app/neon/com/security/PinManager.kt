package app.neon.com.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.inject.Inject
import javax.inject.Singleton

sealed interface PinVerifyResult {
    data object Success : PinVerifyResult
    data class Wrong(val attemptsLeftInRound: Int) : PinVerifyResult
    data class LockedOut(val remainingSeconds: Int) : PinVerifyResult
    data object NotSet : PinVerifyResult
}

@Singleton
class PinManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val PIN_LENGTH = 4
        private const val PREFS_NAME = "neont_app_lock"
        private const val KEY_SALT = "salt"
        private const val KEY_HASH = "hash"
        private const val KEY_ITERATIONS = "iterations"
        private const val KEY_FAILED = "failed_attempts"
        private const val KEY_LOCKED_UNTIL = "locked_until"
        private const val KDF_ALGORITHM = "PBKDF2WithHmacSHA1"
        private const val ITERATIONS = 100_000
        private const val KEY_LENGTH_BITS = 256
        private const val SALT_BYTES = 16
        private const val ATTEMPTS_PER_ROUND = 5
        private val LOCK_SECONDS = longArrayOf(30, 60, 300, 900, 3_600)

        fun isValidPin(pin: String): Boolean = pin.length == PIN_LENGTH && pin.all { it in '0'..'9' }
    }

    private val mutex = Mutex()
    private val prefs: SharedPreferences by lazy { createPrefs() }
    private val _isEnabled = MutableStateFlow(false)
    val isEnabled: StateFlow<Boolean> = _isEnabled.asStateFlow()

    suspend fun isPinSet(): Boolean = withContext(Dispatchers.IO) {
        prefs.contains(KEY_HASH).also { _isEnabled.value = it }
    }

    suspend fun lockoutRemainingSeconds(): Int = withContext(Dispatchers.IO) {
        secondsLeft(prefs.getLong(KEY_LOCKED_UNTIL, 0L) - System.currentTimeMillis())
    }

    suspend fun setPin(pin: String) {
        require(isValidPin(pin)) { "PIN harus $PIN_LENGTH digit angka." }
        mutex.withLock {
            withContext(Dispatchers.Default) {
                val salt = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }
                val hash = derive(pin, salt, ITERATIONS)
                prefs.edit()
                    .putString(KEY_SALT, encode(salt))
                    .putString(KEY_HASH, encode(hash))
                    .putInt(KEY_ITERATIONS, ITERATIONS)
                    .putInt(KEY_FAILED, 0)
                    .putLong(KEY_LOCKED_UNTIL, 0L)
                    .commit()
            }
            _isEnabled.value = true
        }
    }

    suspend fun verify(pin: String): PinVerifyResult = mutex.withLock {
        withContext(Dispatchers.Default) {
            val hashText = prefs.getString(KEY_HASH, null)
            val saltText = prefs.getString(KEY_SALT, null)
            if (hashText == null || saltText == null) {
                PinVerifyResult.NotSet
            } else {
                val now = System.currentTimeMillis()
                val lockedLeft = secondsLeft(prefs.getLong(KEY_LOCKED_UNTIL, 0L) - now)
                if (lockedLeft > 0) {
                    PinVerifyResult.LockedOut(lockedLeft)
                } else {
                    val failed = prefs.getInt(KEY_FAILED, 0) + 1
                    val roundLockSeconds = if (failed % ATTEMPTS_PER_ROUND == 0) lockSecondsFor(failed) else 0L
                    prefs.edit()
                        .putInt(KEY_FAILED, failed)
                        .putLong(KEY_LOCKED_UNTIL, if (roundLockSeconds > 0) now + roundLockSeconds * 1_000L else 0L)
                        .commit()
                    val iterations = prefs.getInt(KEY_ITERATIONS, ITERATIONS)
                    val candidate = derive(pin, decode(saltText), iterations)
                    if (MessageDigest.isEqual(candidate, decode(hashText))) {
                        prefs.edit().putInt(KEY_FAILED, 0).putLong(KEY_LOCKED_UNTIL, 0L).commit()
                        PinVerifyResult.Success
                    } else if (roundLockSeconds > 0) {
                        PinVerifyResult.LockedOut(roundLockSeconds.toInt())
                    } else {
                        PinVerifyResult.Wrong(ATTEMPTS_PER_ROUND - failed % ATTEMPTS_PER_ROUND)
                    }
                }
            }
        }
    }

    suspend fun clearPin() {
        mutex.withLock {
            withContext(Dispatchers.IO) { prefs.edit().clear().commit() }
            _isEnabled.value = false
        }
    }

    private fun createPrefs(): SharedPreferences {
        fun build(): SharedPreferences {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            return EncryptedSharedPreferences.create(
                context, PREFS_NAME, masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }
        return try { build() } catch (e: Exception) {
            context.deleteSharedPreferences(PREFS_NAME); build()
        }
    }

    private fun derive(pin: String, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, iterations, KEY_LENGTH_BITS)
        try {
            return SecretKeyFactory.getInstance(KDF_ALGORITHM).generateSecret(spec).encoded
        } finally { spec.clearPassword() }
    }

    private fun lockSecondsFor(failedAttempts: Int): Long {
        val round = failedAttempts / ATTEMPTS_PER_ROUND
        return LOCK_SECONDS[(round - 1).coerceIn(0, LOCK_SECONDS.lastIndex)]
    }

    private fun secondsLeft(remainingMs: Long): Int {
        if (remainingMs <= 0L) return 0
        val capped = remainingMs.coerceAtMost(LOCK_SECONDS.last() * 1_000L)
        return ((capped + 999L) / 1_000L).toInt()
    }

    private fun encode(bytes: ByteArray): String = Base64.encodeToString(bytes, Base64.NO_WRAP)
    private fun decode(text: String): ByteArray = Base64.decode(text, Base64.NO_WRAP)
}
