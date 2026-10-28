package app.neon.com.data.model

import androidx.compose.runtime.Immutable

@Immutable
data class ChatMessage(
    val msgId: String = "",
    val chatId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val text: String = "",
    val time: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isFromMe: Boolean = false,
    val isRead: Boolean = false,
    val isEdited: Boolean = false,
    val isDeleted: Boolean = false,
    val replyTo: String? = null,
    val reactions: Map<String, String> = emptyMap() // userId -> emoji ("❤️", "👍", "😂", "🔥", "🙏")
)
