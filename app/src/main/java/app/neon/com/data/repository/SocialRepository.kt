package app.neon.com.data.repository

import app.neon.com.data.model.UserStory
import app.neon.com.data.local.FileDatabaseManager
import app.neon.com.data.model.LeaderboardEntry
import app.neon.com.data.model.User
import app.neon.com.data.model.VideoPost
import app.neon.com.data.model.User
import app.neon.com.data.model.VideoPostStory
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SocialRepository @Inject constructor(
    private val localDatabase: FileDatabaseManager
) {
    private val firebaseDatabase: FirebaseDatabase = FirebaseDatabase.getInstance()

    /**
     * Observes real-time profile of a user (followers, following, likesCount).
     */
    fun observeUserProfile(userId: String): Flow<User?> = callbackFlow {
        if (userId.isBlank()) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val ref = firebaseDatabase.getReference("users").child(userId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val user = snapshot.getValue(User::class.java)
                trySend(user)
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(null)
            }
        }

        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    /**
     * Updates user's display name, bio, and avatar color.
     */
    fun updateProfile(
        userId: String,
        displayName: String,
        bio: String,
        avatarColor: String
    ): Flow<Result<Boolean>> = flow {
        try {
            val updates = hashMapOf<String, Any>(
                "/users/$userId/displayName" to displayName.trim(),
                "/users/$userId/bio" to bio.trim(),
                "/users/$userId/avatarColorHex" to avatarColor,
                "/leaderboard/$userId/displayName" to displayName.trim(),
                "/leaderboard/$userId/avatarColorHex" to avatarColor
            )

            firebaseDatabase.reference.updateChildren(updates).await()

            // Update in local cache
            val cached = localDatabase.getCurrentUser()
            if (cached != null && cached.uid == userId) {
                val updated = cached.copy(
                    displayName = displayName.trim(),
                    bio = bio.trim(),
                    avatarColorHex = avatarColor
                )
                localDatabase.saveCurrentUser(updated)
            }

            emit(Result.success(true))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    /**
     * Updates user's display name, bio, avatar color, and optional avatar URL/photo.
     */
    fun updateProfileWithAvatar(
        userId: String,
        displayName: String,
        bio: String,
        avatarColor: String,
        avatarUrl: String
    ): Flow<Result<Boolean>> = flow {
        try {
            val updates = hashMapOf<String, Any>(
                "/users/$userId/displayName" to displayName.trim(),
                "/users/$userId/bio" to bio.trim(),
                "/users/$userId/avatarColorHex" to avatarColor,
                "/users/$userId/avatarUrl" to avatarUrl.trim(),
                "/leaderboard/$userId/displayName" to displayName.trim(),
                "/leaderboard/$userId/avatarColorHex" to avatarColor
            )

            firebaseDatabase.reference.updateChildren(updates).await()

            val cached = localDatabase.getCurrentUser()
            if (cached != null && cached.uid == userId) {
                val updated = cached.copy(
                    displayName = displayName.trim(),
                    bio = bio.trim(),
                    avatarColorHex = avatarColor,
                    avatarUrl = avatarUrl.trim()
                )
                localDatabase.saveCurrentUser(updated)
            }

            emit(Result.success(true))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    /**
     * Updates Privacy Settings (Last Seen, Profile Photo) in Firebase and local database.
     */
    fun updatePrivacySettings(
        userId: String,
        showLastSeen: Boolean,
        showProfilePhoto: Boolean
    ): Flow<Result<Boolean>> = flow {
        try {
            val updates = hashMapOf<String, Any>(
                "/users/$userId/showLastSeen" to showLastSeen,
                "/users/$userId/showProfilePhoto" to showProfilePhoto
            )

            firebaseDatabase.reference.updateChildren(updates).await()

            val cached = localDatabase.getCurrentUser()
            if (cached != null && cached.uid == userId) {
                val updated = cached.copy(
                    showLastSeen = showLastSeen,
                    showProfilePhoto = showProfilePhoto
                )
                localDatabase.saveCurrentUser(updated)
            }

            emit(Result.success(true))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    /**
     * Follows a target user: updates /followers and /following, and increments counters.
     */
    fun followUser(currentUserId: String, targetUserId: String): Flow<Result<Boolean>> = flow {
        try {
            val updates = hashMapOf<String, Any>(
                "/followers/$targetUserId/$currentUserId" to true,
                "/following/$currentUserId/$targetUserId" to true
            )
            firebaseDatabase.reference.updateChildren(updates).await()

            // Update target user's followers count & leaderboard
            val targetRef = firebaseDatabase.getReference("users").child(targetUserId)
            val snapshot = targetRef.get().await()
            val currentFollowers = snapshot.child("followersCount").getValue(Int::class.java) ?: 0
            val currentLikes = snapshot.child("likesCount").getValue(Int::class.java) ?: 0
            val newFollowers = currentFollowers + 1
            val newScore = (newFollowers * 2) + currentLikes

            val countUpdates = hashMapOf<String, Any>(
                "/users/$targetUserId/followersCount" to newFollowers,
                "/leaderboard/$targetUserId/followersCount" to newFollowers,
                "/leaderboard/$targetUserId/score" to newScore
            )
            firebaseDatabase.reference.updateChildren(countUpdates).await()

            // Update current user's following count
            val meRef = firebaseDatabase.getReference("users").child(currentUserId)
            val meSnapshot = meRef.get().await()
            val meFollowing = meSnapshot.child("followingCount").getValue(Int::class.java) ?: 0
            firebaseDatabase.getReference("users")
                .child(currentUserId)
                .child("followingCount")
                .setValue(meFollowing + 1)
                .await()

            emit(Result.success(true))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    /**
     * Unfollows a target user.
     */
    fun unfollowUser(currentUserId: String, targetUserId: String): Flow<Result<Boolean>> = flow {
        try {
            val updates = hashMapOf<String, Any?>(
                "/followers/$targetUserId/$currentUserId" to null,
                "/following/$currentUserId/$targetUserId" to null
            )
            firebaseDatabase.reference.updateChildren(updates).await()

            // Decrement target user's followers count
            val targetRef = firebaseDatabase.getReference("users").child(targetUserId)
            val snapshot = targetRef.get().await()
            val currentFollowers = snapshot.child("followersCount").getValue(Int::class.java) ?: 0
            val currentLikes = snapshot.child("likesCount").getValue(Int::class.java) ?: 0
            val newFollowers = if (currentFollowers > 0) currentFollowers - 1 else 0
            val newScore = (newFollowers * 2) + currentLikes

            val countUpdates = hashMapOf<String, Any>(
                "/users/$targetUserId/followersCount" to newFollowers,
                "/leaderboard/$targetUserId/followersCount" to newFollowers,
                "/leaderboard/$targetUserId/score" to newScore
            )
            firebaseDatabase.reference.updateChildren(countUpdates).await()

            // Decrement current user's following count
            val meRef = firebaseDatabase.getReference("users").child(currentUserId)
            val meSnapshot = meRef.get().await()
            val meFollowing = meSnapshot.child("followingCount").getValue(Int::class.java) ?: 0
            val newFollowing = if (meFollowing > 0) meFollowing - 1 else 0
            firebaseDatabase.getReference("users")
                .child(currentUserId)
                .child("followingCount")
                .setValue(newFollowing)
                .await()

            emit(Result.success(true))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    /**
     * Checks if current user is following a target user.
     */
    fun isFollowing(currentUserId: String, targetUserId: String): Flow<Boolean> = callbackFlow {
        val ref = firebaseDatabase.getReference("following").child(currentUserId).child(targetUserId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(snapshot.exists())
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(false)
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    /**
     * Gets followers user list.
     */
    fun getFollowers(userId: String): Flow<List<User>> = callbackFlow {
        val ref = firebaseDatabase.getReference("followers").child(userId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val followerUids = snapshot.children.mapNotNull { it.key }
                if (followerUids.isEmpty()) {
                    trySend(emptyList())
                    return
                }

                val usersRef = firebaseDatabase.getReference("users")
                usersRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(usersSnap: DataSnapshot) {
                        val users = followerUids.mapNotNull { uid ->
                            usersSnap.child(uid).getValue(User::class.java)
                        }
                        trySend(users)
                    }

                    override fun onCancelled(error: DatabaseError) {
                        trySend(emptyList())
                    }
                })
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(emptyList())
            }
        }

        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    /**
     * Gets following user list.
     */
    fun getFollowing(userId: String): Flow<List<User>> = callbackFlow {
        val ref = firebaseDatabase.getReference("following").child(userId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val followingUids = snapshot.children.mapNotNull { it.key }
                if (followingUids.isEmpty()) {
                    trySend(emptyList())
                    return
                }

                val usersRef = firebaseDatabase.getReference("users")
                usersRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(usersSnap: DataSnapshot) {
                        val users = followingUids.mapNotNull { uid ->
                            usersSnap.child(uid).getValue(User::class.java)
                        }
                        trySend(users)
                    }

                    override fun onCancelled(error: DatabaseError) {
                        trySend(emptyList())
                    }
                })
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(emptyList())
            }
        }

        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    /**
     * Creates a 5-second Story for the user.
     */
    fun createStory(
        userId: String,
        username: String,
        caption: String,
        colorHex: String
    ): Flow<Result<UserStory>> = flow {
        try {
            val storyId = "story_${UUID.randomUUID().toString().take(10)}"
            val story = UserStory(
                id = storyId,
                userId = userId,
                username = username,
                caption = caption.trim(),
                backgroundColorHex = colorHex,
                timestamp = System.currentTimeMillis(),
                durationMs = 5000L
            )

            firebaseDatabase.getReference("stories")
                .child(userId)
                .child(storyId)
                .setValue(story)
                .await()

            emit(Result.success(story))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    /**
     * Observes real-time active Stories for a user.
     */
    fun observeUserStories(userId: String): Flow<List<UserStory>> = callbackFlow {
        if (userId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val ref = firebaseDatabase.getReference("stories").child(userId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val stories = mutableListOf<UserStory>()
                val oneDayAgo = System.currentTimeMillis() - (24 * 60 * 60 * 1000)

                for (child in snapshot.children) {
                    val story = child.getValue(UserStory::class.java)
                    if (story != null && story.timestamp > oneDayAgo) {
                        stories.add(story)
                    }
                }
                trySend(stories.sortedBy { it.timestamp })
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(emptyList())
            }
        }

        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    /**
     * Observes the Leaderboard ranked by popularity score (Followers * 2 + Likes).
     */
    fun observeLeaderboard(): Flow<List<LeaderboardEntry>> = callbackFlow {
        val ref = firebaseDatabase.getReference("leaderboard")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<LeaderboardEntry>()

                for (child in snapshot.children) {
                    val uid = child.child("uid").getValue(String::class.java) ?: child.key ?: continue
                    val username = child.child("username").getValue(String::class.java) ?: "User"
                    val displayName = child.child("displayName").getValue(String::class.java) ?: username
                    val avatarColorHex = child.child("avatarColorHex").getValue(String::class.java) ?: "#00FF66"
                    val followers = child.child("followersCount").getValue(Int::class.java) ?: 0
                    val likes = child.child("likesCount").getValue(Int::class.java) ?: 0
                    val calculatedScore = (followers * 2) + likes

                    list.add(
                        LeaderboardEntry(
                            uid = uid,
                            username = username,
                            displayName = displayName,
                            avatarColorHex = avatarColorHex,
                            followersCount = followers,
                            likesCount = likes,
                            score = calculatedScore
                        )
                    )
                }

                // Sort descending by score, then assign rank 1, 2, 3...
                val sorted = list.sortedByDescending { it.score }.mapIndexed { index, entry ->
                    entry.copy(rank = index + 1)
                }

                trySend(sorted)
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(emptyList())
            }
        }

        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    fun getStorageCacheSizeInBytes(): Long {
        return localDatabase.getCacheSizeInBytes()
    }

    fun clearLocalCache(): Flow<Result<Boolean>> = flow {
        try {
            localDatabase.clearAllCache()
            emit(Result.success(true))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    // --- FITUR VIDEO FEED ---
    fun getUserVideos(userId: String): Flow<List<VideoPost>> = callbackFlow {
        val ref = firebaseDatabase.getReference("videos").orderByChild("userId").equalTo(userId)
        
        val listener = object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                val posts = mutableListOf<VideoPost>()
                for (child in snapshot.children) {
                    val post = child.getValue(VideoPost::class.java)
                    if (post != null) posts.add(post)
                }
                trySend(posts.sortedByDescending { it.timestamp })
            }
            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {}
        }
        
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }
}
