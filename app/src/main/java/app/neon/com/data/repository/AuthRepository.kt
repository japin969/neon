package app.neon.com.data.repository

import app.neon.com.data.local.FileDatabaseManager
import app.neon.com.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val localDatabase: FileDatabaseManager
) {
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firebaseDatabase: FirebaseDatabase = FirebaseDatabase.getInstance()

    private val neonAvatarColors = listOf(
        "#00FF66", // Neon Green
        "#00E5FF", // Neon Cyan
        "#FF007F", // Neon Magenta / Pink
        "#7928CA", // Neon Purple
        "#FF9100", // Neon Orange
        "#00F5D4", // Neon Mint
        "#7B2CBF", // Neon Violet
        "#F72585"  // Neon Rose
    )

    fun getCurrentFirebaseUser() = firebaseAuth.currentUser

    fun getCachedUser(): User? = localDatabase.getCurrentUser()

    fun getCurrentUser(): User? = localDatabase.getCurrentUser()

    /**
     * Sign In existing user with Email and Password.
     */
    fun signInWithEmail(email: String, pass: String): Flow<Result<User>> = callbackFlow {
        try {
            val authResult = firebaseAuth.signInWithEmailAndPassword(email.trim(), pass).await()
            val uid = authResult.user?.uid ?: throw Exception("Autentikasi gagal: UID tidak ditemukan")

            // Fetch user profile from Firebase Realtime Database
            val userRef = firebaseDatabase.getReference("users").child(uid)
            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val user = snapshot.getValue(User::class.java)
                    if (user != null) {
                        // Cache locally
                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launchSilently {
                            localDatabase.saveCurrentUser(user)
                        }
                        trySend(Result.success(user))
                    } else {
                        // If user profile is not completed yet, return partial user to prompt username screen
                        val partialUser = User(uid = uid, email = email)
                        trySend(Result.success(partialUser))
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    trySend(Result.failure(Exception(error.message)))
                }
            }

            userRef.addListenerForSingleValueEvent(listener)
            awaitClose { userRef.removeEventListener(listener) }
        } catch (e: Exception) {
            trySend(Result.failure(e))
            close(e)
        }
    }

    /**
     * Register new user with Email and Password. Returns UID on success.
     */
    fun signUpWithEmail(email: String, pass: String): Flow<Result<String>> = flow {
        try {
            val authResult = firebaseAuth.createUserWithEmailAndPassword(email.trim(), pass).await()
            val uid = authResult.user?.uid ?: throw Exception("Pendaftaran gagal: UID tidak valid")
            emit(Result.success(uid))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    /**
     * Check if username is available in Firebase Realtime Database (/usernames/{username}).
     */
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
                // If it does not exist, username is available!
                val isAvailable = !snapshot.exists()
                trySend(Result.success(isAvailable))
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(Result.failure(Exception(error.message)))
            }
        }

        ref.addListenerForSingleValueEvent(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    /**
     * Create initial user profile with username, auto-generated initials, and random neon avatar color.
     */
    fun createUserProfile(uid: String, email: String, username: String): Flow<Result<User>> = flow {
        try {
            val sanitizedUsername = username.trim().lowercase()
            val randomColor = neonAvatarColors.random()
            val displayName = username.trim()

            val newUser = User(
                uid = uid,
                email = email,
                username = sanitizedUsername,
                displayName = displayName,
                avatarUrl = "",
                avatarColorHex = randomColor,
                bio = "Hai! Saya baru bergabung di Neont.",
                followersCount = 0,
                followingCount = 0,
                likesCount = 0,
                createdAt = System.currentTimeMillis()
            )

            // Atomic Multi-Path Update to Firebase Realtime Database
            val updates = hashMapOf<String, Any>(
                "/users/$uid" to newUser,
                "/usernames/$sanitizedUsername" to uid,
                "/leaderboard/$uid" to mapOf(
                    "uid" to uid,
                    "username" to sanitizedUsername,
                    "displayName" to displayName,
                    "avatarColorHex" to randomColor,
                    "likesCount" to 0,
                    "followersCount" to 0,
                    "score" to 0
                )
            )

            firebaseDatabase.reference.updateChildren(updates).await()

            // Save to local cache
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

    private fun kotlinx.coroutines.CoroutineScope.launchSilently(block: suspend () -> Unit) {
        kotlinx.coroutines.GlobalScope.launch {
            try { block() } catch (_: Exception) {}
        }
    }
}
