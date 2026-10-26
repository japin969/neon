package app.neon.com.data.model

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class ChatItem(
    val id: String = "",
    val isGroup: Boolean = false,
    val chatName: String = "",
    val recipientId: String = "",
    val recipientUsername: String = "",
    val recipientAvatarColor: String = "#00FF66",
    val recipientAvatarUrl: String = "",
    val lastMessage: String = "",
    val lastMessageTimestamp: Long = 0L,
    val unreadCount: Int = 0,
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val participants: Map<String, Boolean> = emptyMap(),
    val pinnedMessageId: String = ""
) {
    fun getDisplayInitials(): String {
        val title = if (chatName.isNotBlank()) chatName else recipientUsername
        return when {
            title.length >= 2 -> title.take(2).uppercase()
            title.length == 1 -> title.take(1).uppercase()
            else -> "CH"
        }
    }
}
