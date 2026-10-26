package app.neon.com.data.model

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class LeaderboardEntry(
    val uid: String = "",
    val username: String = "",
    val displayName: String = "",
    val avatarColorHex: String = "#00FF66",
    val followersCount: Int = 0,
    val likesCount: Int = 0,
    val score: Int = 0,
    val rank: Int = 0
) {
    fun getInitials(): String {
        val title = if (username.isNotBlank()) username else displayName
        return when {
            title.length >= 2 -> title.take(2).uppercase()
            title.length == 1 -> title.take(1).uppercase()
            else -> "US"
        }
    }
}
