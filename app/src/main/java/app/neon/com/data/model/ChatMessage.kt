package app.neon.com.data.model

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class ChatMessage(
    val id: String = "",
    val chatId: String = "",
    val senderId: String = "",
    val senderUsername: String = "",
    val senderAvatarColor: String = "#00FF66",
    val encryptedText: String = "",
    val decryptedText: String = "",
    val timestamp: Long = 0L,
    val isEdited: Boolean = false,
    val deletedForEveryone: Boolean = false,
    val deletedForUsers: Map<String, Boolean> = emptyMap(),
    val replyToMessageId: String = "",
    val replyToText: String = "",
    val replyToSender: String = "",
    val reactions: Map<String, String> = emptyMap(),
    val isPinned: Boolean = false,
    val forwardedFrom: String = ""
) {
    /**
     * Checks if this message was sent by the provided user ID.
     */
    fun isFromMe(currentUserId: String): Boolean = senderId == currentUserId

    /**
     * Checks if this message was hidden/deleted for the provided user ID.
     */
    fun isDeletedForUser(currentUserId: String): Boolean =
        deletedForUsers[currentUserId] == true

    /**
     * Aggregates reactions into counts, e.g. "❤️" -> 2, "🔥" -> 1.
     */
    fun getReactionSummary(): Map<String, Int> {
        val summary = mutableMapOf<String, Int>()
        reactions.values.forEach { emoji ->
            summary[emoji] = (summary[emoji] ?: 0) + 1
        }
        return summary
    }
}
