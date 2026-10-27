package app.neon.com.data.repository

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.tasks.await


class AuthRepository(
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
) {

    val currentUser: FirebaseUser?
        get() = firebaseAuth.currentUser

    fun isUserLoggedIn(): Boolean {
        return firebaseAuth.currentUser != null
    }

    /**
     * Login pengguna menggunakan Email dan Password
     * @param email Email yang sudah dibersihkan dengan Utils.sanitizeInput
     * @param password Password akun pengguna
     */
    suspend fun signInWithEmail(email: String, password: String): AuthResult<FirebaseUser> {
        return try {
            val authResult = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val user = authResult.user
            if (user != null) {
                AuthResult.Success(user)
            } else {
                AuthResult.Error("Gagal memuat sesi pengguna.")
            }
        } catch (e: Exception) {
            val localizedMessage = mapFirebaseExceptionToIndonesian(e)
            AuthResult.Error(localizedMessage, e)
        }
    }

    /**
     * Mendaftarkan akun pengguna baru
     * @param email Email yang sudah dibersihkan dengan Utils.sanitizeInput
     * @param password Password kuat yang lolos validasi Regex
     */
    suspend fun signUpWithEmail(email: String, password: String): AuthResult<FirebaseUser> {
        return try {
            val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val user = authResult.user
            if (user != null) {
                AuthResult.Success(user)
            } else {
                AuthResult.Error("Gagal membuat akun baru.")
            }
        } catch (e: Exception) {
            val localizedMessage = mapFirebaseExceptionToIndonesian(e)
            AuthResult.Error(localizedMessage, e)
        }
    }

    fun signOut() {
        firebaseAuth.signOut()
    }

    /**
     * Mapping error kode Firebase ke bahasa Indonesia yang ramah pengguna
     * tanpa mengekspos celah enumerasi email ke pihak luar.
     */
    private fun mapFirebaseExceptionToIndonesian(exception: Throwable): String {
        return when (exception) {
            is FirebaseAuthInvalidCredentialsException -> {
                // Email Enumeration Protection:
                // Menyatu dengan penanganan password salah demi perlindungan akun
                "Kombinasi email atau password salah. Silakan periksa kembali."
            }
            is FirebaseAuthInvalidUserException -> {
                when (exception.errorCode) {
                    "ERROR_USER_NOT_FOUND" -> "Akun tidak ditemukan. Silakan lakukan pendaftaran terlebih dahulu."
                    "ERROR_USER_DISABLED" -> "Akun ini telah dinonaktifkan oleh administrator sistem."
                    else -> "Akun tidak ditemukan atau telah dinonaktifkan."
                }
            }
            is FirebaseAuthUserCollisionException -> {
                "Email sudah terdaftar. Silakan gunakan email lain atau masuk ke akun Anda."
            }
            is FirebaseAuthException -> {
                when (exception.errorCode) {
                    "ERROR_WRONG_PASSWORD" -> "Password salah. Silakan coba lagi."
                    "ERROR_EMAIL_ALREADY_IN_USE" -> "Email sudah terdaftar. Silakan masuk ke akun Anda."
                    "ERROR_INVALID_EMAIL" -> "Format email tidak valid."
                    "ERROR_WEAK_PASSWORD" -> "Password terlalu lemah. Gunakan minimal 8 karakter dengan angka dan huruf kapital."
                    "ERROR_TOO_MANY_REQUESTS" -> "Terlalu banyak percobaan gagal. Akses dibatasi sementara demi keamanan akun."
                    "ERROR_OPERATION_NOT_ALLOWED" -> "Autentikasi email & password belum diaktifkan di Firebase Console."
                    else -> exception.localizedMessage ?: "Terjadi kesalahan pada layanan autentikasi."
                }
            }
            is FirebaseNetworkException -> {
                "Koneksi internet tidak stabil. Periksa jaringan Anda dan coba kembali."
            }
            else -> {
                exception.localizedMessage ?: "Terjadi kendala internal. Silakan coba lagi nanti."
            }
        }
    }
}
