package app.neon.com.data.model

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class User(
    val uid: String = "",
    val email: String = "",
    val username: String = "",
    val displayName: String = "",
    val avatarUrl: String = "",
    val avatarColorHex: String = "#00FF66",
    val bio: String = "Hey there! I am using Neont.",
    val followersCount: Int = 0,
    val followingCount: Int = 0,
    val likesCount: Int = 0,
    val showLastSeen: Boolean = true,
    val showProfilePhoto: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
) {
    val id: String get() = uid
    /**
     * Extracts 2-character initials for default avatar generation.
     */
    fun getInitials(): String {
        val cleanName = if (username.isNotBlank()) username else email.substringBefore("@")
        return when {
            cleanName.length >= 2 -> cleanName.take(2).uppercase()
            cleanName.length == 1 -> cleanName.take(1).uppercase()
            else -> "NE"
        }
    }
}
