package app.neon.com.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.neon.com.data.model.ChatMessage
import app.neon.com.data.repository.ChatRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * ChatViewModel - Menghubungkan UI Chat ke ChatRepository & Firebase
 * - sendMessage(chatId, text)
 * - sendReaction(chatId, msgId, emoji)
 * - deleteMessage(chatId, msgId)
 * - editMessage(chatId, msgId, newText)
 */
class ChatViewModel(
    private val repository: ChatRepository = ChatRepository()
) : ViewModel() {

    private val auth: FirebaseAuth? = runCatching { FirebaseAuth.getInstance() }.getOrNull()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _replyMessage = MutableStateFlow<ChatMessage?>(null)
    val replyMessage: StateFlow<ChatMessage?> = _replyMessage.asStateFlow()

    private val _messageToEdit = MutableStateFlow<ChatMessage?>(null)
    val messageToEdit: StateFlow<ChatMessage?> = _messageToEdit.asStateFlow()

    private var currentChatId: String = ""

    val currentUserId: String
        get() = auth?.currentUser?.uid ?: "my_uid"

    val currentUserName: String
        get() = auth?.currentUser?.displayName ?: "Saya"

    /**
     * Mengamati pesan secara realtime dengan debounce(100L) dari repository
     */
    fun loadChat(chatId: String) {
        currentChatId = chatId
        viewModelScope.launch {
            _isLoading.value = true
            repository.observeMessages(chatId, currentUserId).collectLatest { list ->
                _messages.value = list
                _isLoading.value = false
            }
        }
    }

    /**
     * Tombol Kirim di ComposerBar
     */
    fun sendMessage(chatId: String, text: String) {
        if (text.trim().isEmpty()) return
        val targetChat = chatId.ifEmpty { currentChatId }
        val replyId = _replyMessage.value?.msgId

        viewModelScope.launch {
            repository.sendMessage(
                chatId = targetChat,
                text = text,
                currentUserId = currentUserId,
                currentUserName = currentUserName,
                replyTo = replyId
            )
            _replyMessage.value = null
        }
    }

    /**
     * Fitur Reaksi (Long-press bubble)
     */
    fun sendReaction(chatId: String, msgId: String, emoji: String) {
        val targetChat = chatId.ifEmpty { currentChatId }
        viewModelScope.launch {
            repository.sendReaction(
                chatId = targetChat,
                msgId = msgId,
                userId = currentUserId,
                emoji = emoji
            )
        }
    }

    /**
     * Fitur Hapus Pesan di Firebase
     */
    fun deleteMessage(chatId: String, msgId: String, deleteForEveryone: Boolean = true) {
        val targetChat = chatId.ifEmpty { currentChatId }
        viewModelScope.launch {
            repository.deleteMessage(
                chatId = targetChat,
                msgId = msgId,
                deleteForEveryone = deleteForEveryone
            )
        }
    }

    /**
     * Fitur Edit Pesan di Firebase
     */
    fun editMessage(chatId: String, msgId: String, newText: String) {
        val targetChat = chatId.ifEmpty { currentChatId }
        viewModelScope.launch {
            repository.editMessage(
                chatId = targetChat,
                msgId = msgId,
                newText = newText
            )
            _messageToEdit.value = null
        }
    }

    fun setReplyMessage(message: ChatMessage?) {
        _replyMessage.value = message
    }

    fun setMessageToEdit(message: ChatMessage?) {
        _messageToEdit.value = message
    }
}
