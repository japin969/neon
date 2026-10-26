package app.neon.com.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.neon.com.data.model.ChatItem
import app.neon.com.data.model.ChatMessage
import app.neon.com.data.model.User
import app.neon.com.data.repository.AuthRepository
import app.neon.com.data.repository.ChatMessageRepository
import app.neon.com.data.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatDetailUiState(
    val chatId: String = "",
    val recipientId: String = "",
    val isGroup: Boolean = false,
    val chatName: String = "",
    val currentUser: User? = null,
    val messages: List<ChatMessage> = emptyList(),
    val pinnedMessage: ChatMessage? = null,
    val inputText: String = "",
    val replyingToMessage: ChatMessage? = null,
    val editingMessage: ChatMessage? = null,
    val selectedMessageForOptions: ChatMessage? = null,
    val isOptionsMenuVisible: Boolean = false,
    val isForwardDialogVisible: Boolean = false,
    val availableChatsForForward: List<ChatItem> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class ChatDetailViewModel @Inject constructor(
    private val messageRepository: ChatMessageRepository,
    private val chatRepository: ChatRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatDetailUiState())
    val uiState: StateFlow<ChatDetailUiState> = _uiState.asStateFlow()

    private var messagesObserverJob: Job? = null
    private var chatsForwardObserverJob: Job? = null

    fun initChat(
        chatId: String,
        recipientId: String = "",
        isGroup: Boolean = false,
        chatName: String = ""
    ) {
        val user = authRepository.getCachedUser() ?: authRepository.getCurrentFirebaseUser()?.let {
            User(uid = it.uid, email = it.email ?: "")
        }

        _uiState.update {
            it.copy(
                chatId = chatId,
                recipientId = recipientId,
                isGroup = isGroup,
                chatName = chatName,
                currentUser = user,
                isLoading = true
            )
        }

        if (user != null) {
            observeMessages(chatId, user.uid)
            loadAvailableChatsForForward(user.uid)
        }
    }

    private fun observeMessages(chatId: String, currentUserId: String) {
        messagesObserverJob?.cancel()
        messagesObserverJob = viewModelScope.launch {
            messageRepository.observeMessages(chatId, currentUserId).collect { messageList ->
                val pinned = messageList.lastOrNull { it.isPinned }
                _uiState.update {
                    it.copy(
                        messages = messageList,
                        pinnedMessage = pinned,
                        isLoading = false
                    )
                }
            }
        }
    }

    private fun loadAvailableChatsForForward(currentUserId: String) {
        chatsForwardObserverJob?.cancel()
        chatsForwardObserverJob = viewModelScope.launch {
            chatRepository.observeUserChats(currentUserId).collect { chatList ->
                _uiState.update { it.copy(availableChatsForForward = chatList) }
            }
        }
    }

    fun onInputTextChanged(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun sendMessage() {
        val state = _uiState.value
        val text = state.inputText.trim()
        if (text.isBlank()) return

        val sender = state.currentUser ?: return
        val chatId = state.chatId

        if (state.editingMessage != null) {
            // Edit existing message mode
            val targetMsg = state.editingMessage
            viewModelScope.launch {
                messageRepository.editMessage(chatId, targetMsg.id, text).collect { result ->
                    result.onFailure { error ->
                        _uiState.update { it.copy(errorMessage = error.localizedMessage) }
                    }
                }
            }
            _uiState.update { it.copy(inputText = "", editingMessage = null) }
        } else {
            // Send new message (with optional reply quote)
            val replyTo = state.replyingToMessage
            viewModelScope.launch {
                messageRepository.sendMessage(chatId, sender, text, replyTo).collect { result ->
                    result.onFailure { error ->
                        _uiState.update { it.copy(errorMessage = error.localizedMessage) }
                    }
                }
            }
            _uiState.update { it.copy(inputText = "", replyingToMessage = null) }
        }
    }

    fun onSwipeReply(message: ChatMessage) {
        _uiState.update {
            it.copy(
                replyingToMessage = message,
                editingMessage = null
            )
        }
    }

    fun cancelReply() {
        _uiState.update { it.copy(replyingToMessage = null) }
    }

    fun startEditMessage(message: ChatMessage) {
        _uiState.update {
            it.copy(
                editingMessage = message,
                inputText = message.decryptedText,
                replyingToMessage = null,
                isOptionsMenuVisible = false
            )
        }
    }

    fun cancelEdit() {
        _uiState.update {
            it.copy(
                editingMessage = null,
                inputText = ""
            )
        }
    }

    fun onReactionSelected(message: ChatMessage, emoji: String) {
        val user = _uiState.value.currentUser ?: return
        val chatId = _uiState.value.chatId

        viewModelScope.launch {
            messageRepository.toggleReaction(chatId, message.id, user.uid, emoji).collect { result ->
                result.onFailure { error ->
                    _uiState.update { it.copy(errorMessage = error.localizedMessage) }
                }
            }
        }
        closeMessageOptions()
    }

    fun deleteMessageForMe(message: ChatMessage) {
        val user = _uiState.value.currentUser ?: return
        val chatId = _uiState.value.chatId

        viewModelScope.launch {
            messageRepository.deleteMessageForMe(chatId, message.id, user.uid).collect { result ->
                result.onFailure { error ->
                    _uiState.update { it.copy(errorMessage = error.localizedMessage) }
                }
            }
        }
        closeMessageOptions()
    }

    fun deleteMessageForEveryone(message: ChatMessage) {
        val chatId = _uiState.value.chatId

        viewModelScope.launch {
            messageRepository.deleteMessageForEveryone(chatId, message.id).collect { result ->
                result.onFailure { error ->
                    _uiState.update { it.copy(errorMessage = error.localizedMessage) }
                }
            }
        }
        closeMessageOptions()
    }

    fun togglePinMessage(message: ChatMessage) {
        val chatId = _uiState.value.chatId

        viewModelScope.launch {
            messageRepository.togglePinMessage(chatId, message.id, message.isPinned).collect { result ->
                result.onFailure { error ->
                    _uiState.update { it.copy(errorMessage = error.localizedMessage) }
                }
            }
        }
        closeMessageOptions()
    }

    fun openForwardDialog(message: ChatMessage) {
        _uiState.update {
            it.copy(
                selectedMessageForOptions = message,
                isForwardDialogVisible = true,
                isOptionsMenuVisible = false
            )
        }
    }

    fun closeForwardDialog() {
        _uiState.update { it.copy(isForwardDialogVisible = false) }
    }

    fun forwardMessageToChat(targetChat: ChatItem) {
        val sender = _uiState.value.currentUser ?: return
        val message = _uiState.value.selectedMessageForOptions ?: return

        viewModelScope.launch {
            messageRepository.forwardMessage(
                targetChatId = targetChat.id,
                sender = sender,
                plainText = message.decryptedText,
                originalSenderName = message.senderUsername
            ).collect { result ->
                result.onFailure { error ->
                    _uiState.update { it.copy(errorMessage = error.localizedMessage) }
                }
            }
        }
        closeForwardDialog()
    }

    fun openMessageOptions(message: ChatMessage) {
        _uiState.update {
            it.copy(
                selectedMessageForOptions = message,
                isOptionsMenuVisible = true
            )
        }
    }

    fun closeMessageOptions() {
        _uiState.update {
            it.copy(
                isOptionsMenuVisible = false,
                selectedMessageForOptions = null
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
