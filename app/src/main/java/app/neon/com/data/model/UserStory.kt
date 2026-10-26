package app.neon.com.data.model

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class UserStory(
    val id: String = "",
    val userId: String = "",
    val username: String = "",
    val caption: String = "",
    val backgroundColorHex: String = "#7928CA",
    val timestamp: Long = System.currentTimeMillis(),
    val durationMs: Long = 5000L
)
