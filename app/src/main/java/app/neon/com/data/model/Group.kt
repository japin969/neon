package app.neon.com.data.model

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class Group(
    val id: String = "",
    val name: String = "",
    val avatarColor: String = "#00E5FF",
    val creatorId: String = "",
    val admins: Map<String, Boolean> = emptyMap(),
    val members: Map<String, Boolean> = emptyMap(),
    val memberProfiles: Map<String, User> = emptyMap(),
    val pinnedMessageId: String = "",
    val pinnedMessageText: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    fun isAdmin(userId: String): Boolean = admins[userId] == true
    fun isMember(userId: String): Boolean = members[userId] == true
}
