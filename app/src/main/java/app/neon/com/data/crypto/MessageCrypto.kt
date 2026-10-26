package app.neon.com.data.crypto

import android.util.Base64
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object MessageCrypto {

    private const val ALGORITHM_NAME = "AES"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_LENGTH_BITS = 128
    private const val IV_LENGTH_BYTES = 12
    private const val KEY_LENGTH_BYTES = 32

    private const val PEPPER_SALT = "Neont_Secure_V1_2026_AEAD_Secret"

    private val secureRandom: SecureRandom by lazy {
        SecureRandom()
    }

    fun deriveKey(chatIdOrSecret: String): SecretKey {
        val messageDigest = MessageDigest.getInstance("SHA-256")
        val combinedInput = "$chatIdOrSecret:$PEPPER_SALT"
        val keyBytes = messageDigest.digest(combinedInput.toByteArray(StandardCharsets.UTF_8))
        return SecretKeySpec(keyBytes, 0, KEY_LENGTH_BYTES, ALGORITHM_NAME)
    }

    fun encrypt(plainText: String, chatIdOrSecret: String): String {
        if (plainText.isEmpty()) return ""

        return try {
            val key = deriveKey(chatIdOrSecret)
            val iv = ByteArray(IV_LENGTH_BYTES)
            secureRandom.nextBytes(iv)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            val parameterSpec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
            cipher.init(Cipher.ENCRYPT_MODE, key, parameterSpec)

            val plainBytes = plainText.toByteArray(StandardCharsets.UTF_8)
            val cipherBytes = cipher.doFinal(plainBytes)

            val byteBuffer = ByteBuffer.allocate(iv.size + cipherBytes.size)
            byteBuffer.put(iv)
            byteBuffer.put(cipherBytes)

            val combinedPayload = byteBuffer.array()
            Base64.encodeToString(combinedPayload, Base64.NO_WRAP)
        } catch (e: Exception) {
            e.printStackTrace()
            "__ENC_ERR__:$plainText"
        }
    }

    fun decrypt(encryptedPayload: String, chatIdOrSecret: String): String {
        if (encryptedPayload.isEmpty()) return ""

        if (encryptedPayload.startsWith("__ENC_ERR__:")) {
            return encryptedPayload.removePrefix("__ENC_ERR__:")
        }

        return try {
            val combinedPayload = Base64.decode(encryptedPayload, Base64.NO_WRAP)

            if (combinedPayload.size <= IV_LENGTH_BYTES) {
                return encryptedPayload
            }

            val byteBuffer = ByteBuffer.wrap(combinedPayload)
            val iv = ByteArray(IV_LENGTH_BYTES)
            byteBuffer.get(iv)

            val cipherBytes = ByteArray(byteBuffer.remaining())
            byteBuffer.get(cipherBytes)

            val key = deriveKey(chatIdOrSecret)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val parameterSpec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
            cipher.init(Cipher.DECRYPT_MODE, key, parameterSpec)

            val decryptedBytes = cipher.doFinal(cipherBytes)
            String(decryptedBytes, StandardCharsets.UTF_8)
        } catch (e: Exception) {
            encryptedPayload
        }
    }

    fun isLikelyEncrypted(payload: String): Boolean {
        if (payload.length < 24) return false
        return try {
            val decoded = Base64.decode(payload, Base64.NO_WRAP)
            decoded.size > IV_LENGTH_BYTES + 4
        } catch (e: IllegalArgumentException) {
            false
        }
    }
}
