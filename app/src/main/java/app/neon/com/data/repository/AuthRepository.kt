package app.neon.com.data.repository

import android.util.Log
import app.neon.com.data.local.FileDatabaseManager
import app.neon.com.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val localDatabase: FileDatabaseManager
) {
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firebaseDatabase: FirebaseDatabase = FirebaseDatabase.getInstance()

    private companion object {
        const val TAG = "NeontAuth"
        const val PROFILE_TIMEOUT_MS = 5_000L
    }

    private val neonAvatarColors = listOf(
        "#00FF66", "#00E5FF", "#FF007F", "#7928CA",
        "#FF9100", "#00F5D4", "#7B2CBF", "#F72585"
    )

    fun getCurrentFirebaseUser() = firebaseAuth.currentUser
    fun getCachedUser(): User? = localDatabase.getCurrentUser()
    fun getCurrentUser(): User? = localDatabase.getCurrentUser()

    fun signInWithEmail(email: String, pass: String): Flow<Result<User>> = flow {
        val cleanEmail = email.trim()
        Log.d(TAG, "1. Memulai signInWithEmailAndPassword")
        val authResult = firebaseAuth.signInWithEmailAndPassword(cleanEmail, pass).await()
        val uid = authResult.user?.uid ?: throw IllegalStateException("Autentikasi gagal: UID tidak ditemukan")
        Log.d(TAG, "2. Auth sukses, uid=$uid")

        val profile: User? = try {
            withTimeoutOrNull(PROFILE_TIMEOUT_MS) {
                Log.d(TAG, "3. Mengambil profil RTDB")
                firebaseDatabase.getReference("users").child(uid).get().await()
                    .getValue(User::class.java)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "3. Gagal ambil profil RTDB: ${e.javaClass.simpleName}: ${e.message}")
            null
        }

        Log.d(TAG, "4. Profil ${if (profile != null) "ditemukan" else "TIDAK didapat (pakai fallback)"}")
        val user = profile ?: User(uid = uid, email = cleanEmail)

        if (profile != null) {
            try {
                localDatabase.saveCurrentUser(profile)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Gagal simpan cache lokal: ${e.message}")
            }
        }

        Log.d(TAG, "5. Emit sukses")
        emit(Result.success(user))
    }.catch { e ->
        if (e is CancellationException) throw e
        Log.e(TAG, "Login gagal: ${e.javaClass.simpleName}: ${e.message}", e)
        emit(Result.failure(e))
    }

    fun signUpWithEmail(email: String, pass: String): Flow<Result<String>> = flow {
        try {
            val authResult = firebaseAuth.createUserWithEmailAndPassword(email.trim(), pass).await()
            val uid = authResult.user?.uid ?: throw Exception("Pendaftaran gagal: UID tidak valid")
            emit(Result.success(uid))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    fun checkUsernameAvailability(username: String): Flow<Result<Boolean>> = callbackFlow {
        val sanitized = username.trim().lowercase()
        if (sanitized.length < 3) {
            trySend(Result.success(false))
            close()
            return@callbackFlow
        }
        val ref = firebaseDatabase.getReference("usernames").child(sanitized)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(Result.success(!snapshot.exists()))
            }
            override fun onCancelled(error: DatabaseError) {
                trySend(Result.failure(Exception(error.message)))
            }
        }
        ref.addListenerForSingleValueEvent(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    fun createUserProfile(uid: String, email: String, username: String): Flow<Result<User>> = flow {
        try {
            val sanitizedUsername = username.trim().lowercase()
            val randomColor = neonAvatarColors.random()
            val newUser = User(
                uid = uid, email = email,
                username = sanitizedUsername, displayName = username.trim(),
                avatarColorHex = randomColor, bio = "Hai! Saya baru bergabung di Neont."
            )
            val updates = hashMapOf<String, Any>(
                "/users/$uid" to newUser,
                "/usernames/$sanitizedUsername" to uid,
                "/leaderboard/$uid" to mapOf(
                    "uid" to uid, "username" to sanitizedUsername,
                    "displayName" to username.trim(), "avatarColorHex" to randomColor,
                    "likesCount" to 0, "followersCount" to 0, "score" to 0
                )
            )
            firebaseDatabase.reference.updateChildren(updates).await()
            localDatabase.saveCurrentUser(newUser)
            emit(Result.success(newUser))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    suspend fun signOut() {
        firebaseAuth.signOut()
        localDatabase.clearSession()
    }

    fun canChangeDisplayName(user: User): Boolean =
        (System.currentTimeMillis() - user.lastDisplayNameChange) >= (7L * 24 * 60 * 60 * 1000)

    fun canChangeUsername(user: User): Boolean =
        (System.currentTimeMillis() - user.lastUsernameChange) >= (30L * 24 * 60 * 60 * 1000)

    fun getRemainingDays(lastChange: Long, cooldownDays: Int): Int {
        val cooldownMs = cooldownDays.toLong() * 24 * 60 * 60 * 1000
        val elapsed = System.currentTimeMillis() - lastChange
        return if (elapsed >= cooldownMs) 0 else ((cooldownMs - elapsed) / (24 * 60 * 60 * 1000)).toInt() + 1
    }
}
