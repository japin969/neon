package app.neon.com.ui.chat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import app.neon.com.ui.viewmodel.ChatViewModel

/**
 * Wrapper stateful untuk ChatDetailContent (stateless).
 * Signature sama dengan versi lama -> aman untuk NavGraph.
 */
@Composable
fun ChatDetailScreen(
    chatViewModel: ChatViewModel,
    chatId: String = "chat_aura",
    chatPartnerName: String = "Aura Cantika",
    chatPartnerAvatar: String = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=200&q=80",
    onBackClick: () -> Unit = {},
    onCallClick: (isVideo: Boolean) -> Unit = {}
) {
    val messages by chatViewModel.messages.collectAsState()

    LaunchedEffect(chatId) {
        chatViewModel.loadChat(chatId)
    }

    ChatDetailContent(
        messages = messages,
        currentUserId = chatViewModel.currentUserId,
        chatPartnerName = chatPartnerName,
        onBackClick = onBackClick,
        onCallClick = onCallClick,
        onSendMessage = { text, replyTo ->
            val replyMessage = replyTo?.let { id -> messages.firstOrNull { it.msgId == id } }
            chatViewModel.setReplyMessage(replyMessage)
            chatViewModel.sendMessage(chatId, text)
        },
        onReaction = { message, emoji -> chatViewModel.sendReaction(chatId, message.msgId, emoji) },
        onEditMessage = { message, newText -> chatViewModel.editMessage(chatId, message.msgId, newText) },
        onDeleteMessage = { message, forEveryone -> chatViewModel.deleteMessage(chatId, message.msgId, forEveryone) }
    )
}
