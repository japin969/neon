package app.neon.com.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.neon.com.data.model.ChatItem
import app.neon.com.data.model.User
import app.neon.com.data.repository.AuthRepository
import app.neon.com.data.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatListUiState(
    val currentUser: User? = null,
    val allChats: List<ChatItem> = emptyList(),
    val activeChats: List<ChatItem> = emptyList(),
    val pinnedChats: List<ChatItem> = emptyList(),
    val unpinnedChats: List<ChatItem> = emptyList(),
    val archivedChats: List<ChatItem> = emptyList(),
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,
    val isShowingArchivedFolder: Boolean = false,
    val isAddContactDialogVisible: Boolean = false,
    val contactSearchQuery: String = "",
    val isSearchingContact: Boolean = false,
    val foundContact: User? = null,
    val contactSearchError: String? = null,
    val selectedChatForAction: ChatItem? = null,
    val isActionMenuVisible: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class ChatListViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatListUiState())
    val uiState: StateFlow<ChatListUiState> = _uiState.asStateFlow()

    private var searchContactJob: Job? = null
    private var chatObserverJob: Job? = null

    init {
        loadCurrentUserAndChats()
    }

    fun loadCurrentUserAndChats() {
        val user = authRepository.getCachedUser() ?: authRepository.getCurrentFirebaseUser()?.let {
            User(uid = it.uid, email = it.email ?: "")
        }
        _uiState.update { it.copy(currentUser = user, isLoading = true) }

        if (user != null) {
            observeChats(user.uid)
        } else {
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private fun observeChats(uid: String) {
        chatObserverJob?.cancel()
        chatObserverJob = viewModelScope.launch {
            chatRepository.observeUserChats(uid).catch { e ->
                _uiState.update { it.copy(isLoading = false, errorMessage = e.localizedMessage) }
            }.collect { chats ->
                _uiState.update { current ->
                    val query = current.searchQuery.trim().lowercase()
                    val filtered = if (query.isNotBlank()) {
                        chats.filter {
                            it.chatName.lowercase().contains(query) ||
                            it.recipientUsername.lowercase().contains(query) ||
                            it.lastMessage.lowercase().contains(query)
                        }
                    } else {
                        chats
                    }

                    val archived = filtered.filter { it.isArchived }
                    val active = filtered.filter { !it.isArchived }
                    val pinned = active.filter { it.isPinned }
                    val unpinned = active.filter { !it.isPinned }

                    current.copy(
                        allChats = chats,
                        activeChats = active,
                        pinnedChats = pinned,
                        unpinnedChats = unpinned,
                        archivedChats = archived,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { current ->
            val cleanQuery = query.trim().lowercase()
            val filtered = if (cleanQuery.isNotBlank()) {
                current.allChats.filter {
                    it.chatName.lowercase().contains(cleanQuery) ||
                    it.recipientUsername.lowercase().contains(cleanQuery) ||
                    it.lastMessage.lowercase().contains(cleanQuery)
                }
            } else {
                current.allChats
            }

            val archived = filtered.filter { it.isArchived }
            val active = filtered.filter { !it.isArchived }
            val pinned = active.filter { it.isPinned }
            val unpinned = active.filter { !it.isPinned }

            current.copy(
                searchQuery = query,
                activeChats = active,
                pinnedChats = pinned,
                unpinnedChats = unpinned,
                archivedChats = archived
            )
        }
    }

    fun toggleSearchActive(active: Boolean) {
        _uiState.update {
            it.copy(
                isSearchActive = active,
                searchQuery = if (!active) "" else it.searchQuery
            )
        }
        if (!active) {
            onSearchQueryChange("")
        }
    }

    fun toggleShowArchivedFolder() {
        _uiState.update { it.copy(isShowingArchivedFolder = !it.isShowingArchivedFolder) }
    }

    fun openActionMenu(chat: ChatItem) {
        _uiState.update {
            it.copy(
                selectedChatForAction = chat,
                isActionMenuVisible = true
            )
        }
    }

    fun closeActionMenu() {
        _uiState.update {
            it.copy(
                selectedChatForAction = null,
                isActionMenuVisible = false
            )
        }
    }

    fun togglePinChat(chat: ChatItem) {
        val uid = _uiState.value.currentUser?.uid ?: return
        viewModelScope.launch {
            chatRepository.togglePinChat(uid, chat.id, chat.isPinned).collect { result ->
                result.onFailure { error ->
                    _uiState.update { it.copy(errorMessage = error.localizedMessage) }
                }
            }
        }
        closeActionMenu()
    }

    fun toggleArchiveChat(chat: ChatItem) {
        val uid = _uiState.value.currentUser?.uid ?: return
        viewModelScope.launch {
            chatRepository.toggleArchiveChat(uid, chat.id, chat.isArchived).collect { result ->
                result.onFailure { error ->
                    _uiState.update { it.copy(errorMessage = error.localizedMessage) }
                }
            }
        }
        closeActionMenu()
    }

    fun deleteChatFromList(chat: ChatItem) {
        val uid = _uiState.value.currentUser?.uid ?: return
        viewModelScope.launch {
            chatRepository.deleteChatFromList(uid, chat.id).collect { result ->
                result.onFailure { error ->
                    _uiState.update { it.copy(errorMessage = error.localizedMessage) }
                }
            }
        }
        closeActionMenu()
    }

    // Add Contact Dialog State & Logic
    fun openAddContactDialog() {
        _uiState.update {
            it.copy(
                isAddContactDialogVisible = true,
                contactSearchQuery = "",
                foundContact = null,
                contactSearchError = null
            )
        }
    }

    fun closeAddContactDialog() {
        _uiState.update {
            it.copy(
                isAddContactDialogVisible = false,
                contactSearchQuery = "",
                foundContact = null,
                contactSearchError = null
            )
        }
    }

    fun onContactSearchQueryChange(query: String) {
        _uiState.update {
            it.copy(
                contactSearchQuery = query,
                contactSearchError = null
            )
        }

        searchContactJob?.cancel()

        val clean = query.trim().lowercase().removePrefix("@")
        if (clean.length < 3) {
            _uiState.update { it.copy(foundContact = null, isSearchingContact = false) }
            return
        }

        searchContactJob = viewModelScope.launch {
            _uiState.update { it.copy(isSearchingContact = true) }
            delay(400) // Debounce typing

            chatRepository.searchContactByUsername(clean).collect { result ->
                result.fold(
                    onSuccess = { user ->
                        val currentUid = _uiState.value.currentUser?.uid
                        if (user != null && user.uid == currentUid) {
                            _uiState.update {
                                it.copy(
                                    isSearchingContact = false,
                                    foundContact = null,
                                    contactSearchError = "Ini adalah username akun kamu sendiri."
                                )
                            }
                        } else {
                            _uiState.update {
                                it.copy(
                                    isSearchingContact = false,
                                    foundContact = user,
                                    contactSearchError = if (user == null) "Pengguna @$clean tidak ditemukan" else null
                                )
                            }
                        }
                    },
                    onFailure = { error ->
                        _uiState.update {
                            it.copy(
                                isSearchingContact = false,
                                foundContact = null,
                                contactSearchError = error.localizedMessage ?: "Gagal mencari kontak"
                            )
                        }
                    }
                )
            }
        }
    }

    fun startChatWithFoundContact(
        onChatStarted: (chatId: String, recipientId: String, isGroup: Boolean, chatName: String) -> Unit
    ) {
        val targetUser = _uiState.value.foundContact ?: return
        val currentUser = _uiState.value.currentUser ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            chatRepository.createOrGetDirectChat(currentUser, targetUser).collect { result ->
                _uiState.update { it.copy(isLoading = false) }
                result.fold(
                    onSuccess = { chatItem ->
                        closeAddContactDialog()
                        onChatStarted(chatItem.id, targetUser.uid, false, targetUser.username)
                    },
                    onFailure = { error ->
                        _uiState.update { it.copy(errorMessage = error.localizedMessage) }
                    }
                )
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
