package app.neon.com.ui.group

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.neon.com.data.model.Group
import app.neon.com.data.model.GroupPoll
import app.neon.com.data.model.User
import app.neon.com.data.repository.AuthRepository
import app.neon.com.data.repository.ChatRepository
import app.neon.com.data.repository.GroupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GroupUiState(
    val currentUser: User? = null,
    val group: Group? = null,
    val groupNameInput: String = "",
    val selectedColor: String = "#00FF66",
    val availableContacts: List<User> = emptyList(),
    val selectedMembers: Set<User> = emptySet(),
    val polls: List<GroupPoll> = emptyList(),
    val inviteUsernameQuery: String = "",
    val inviteError: String? = null,
    val newPollQuestion: String = "",
    val newPollOptions: List<String> = listOf("", ""),
    val isCreatePollDialogVisible: Boolean = false,
    val isEditGroupDialogVisible: Boolean = false,
    val isInviteDialogVisible: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class GroupViewModel @Inject constructor(
    private val groupRepository: GroupRepository,
    private val chatRepository: ChatRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GroupUiState())
    val uiState: StateFlow<GroupUiState> = _uiState.asStateFlow()

    init {
        loadCurrentUserAndContacts()
    }

    private fun loadCurrentUserAndContacts() {
        val user = authRepository.getCachedUser() ?: authRepository.getCurrentFirebaseUser()?.let {
            User(uid = it.uid, email = it.email ?: "")
        }
        _uiState.update { it.copy(currentUser = user) }

        if (user != null) {
            viewModelScope.launch {
                chatRepository.observeUserChats(user.uid).collect { chats ->
                    val directUsers = chats.filter { !it.isGroup && it.recipientId.isNotBlank() }.map {
                        User(
                            uid = it.recipientId,
                            username = it.recipientUsername,
                            avatarColorHex = it.recipientAvatarColor,
                            avatarUrl = it.recipientAvatarUrl
                        )
                    }.distinctBy { it.uid }

                    _uiState.update { it.copy(availableContacts = directUsers) }
                }
            }
        }
    }

    fun initGroup(groupId: String) {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            groupRepository.observeGroup(groupId).collect { group ->
                _uiState.update {
                    it.copy(
                        group = group,
                        groupNameInput = group?.name ?: "",
                        selectedColor = group?.avatarColor ?: "#00FF66",
                        isLoading = false
                    )
                }
            }
        }
        viewModelScope.launch {
            groupRepository.observeGroupPolls(groupId).collect { polls ->
                _uiState.update { it.copy(polls = polls) }
            }
        }
    }

    fun onGroupNameChanged(name: String) {
        _uiState.update { it.copy(groupNameInput = name) }
    }

    fun onColorSelected(hex: String) {
        _uiState.update { it.copy(selectedColor = hex) }
    }

    fun toggleMemberSelection(user: User) {
        _uiState.update { current ->
            val updated = current.selectedMembers.toMutableSet()
            if (updated.any { it.uid == user.uid }) {
                updated.removeAll { it.uid == user.uid }
            } else {
                updated.add(user)
            }
            current.copy(selectedMembers = updated)
        }
    }

    fun createGroup(onSuccess: (groupId: String, groupName: String) -> Unit) {
        val state = _uiState.value
        val name = state.groupNameInput.trim()
        if (name.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Nama grup tidak boleh kosong!") }
            return
        }

        val creator = state.currentUser ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            groupRepository.createGroup(
                name = name,
                avatarColor = state.selectedColor,
                creator = creator,
                initialMembers = state.selectedMembers.toList()
            ).collect { result ->
                _uiState.update { it.copy(isLoading = false) }
                result.fold(
                    onSuccess = { group ->
                        onSuccess(group.id, group.name)
                    },
                    onFailure = { error ->
                        _uiState.update { it.copy(errorMessage = error.localizedMessage) }
                    }
                )
            }
        }
    }

    fun updateGroupInfo(newName: String, newColor: String) {
        val groupId = _uiState.value.group?.id ?: return
        viewModelScope.launch {
            groupRepository.updateGroupInfo(groupId, newName, newColor).collect { result ->
                result.fold(
                    onSuccess = {
                        _uiState.update { it.copy(isEditGroupDialogVisible = false) }
                    },
                    onFailure = { error ->
                        _uiState.update { it.copy(errorMessage = error.localizedMessage) }
                    }
                )
            }
        }
    }

    fun kickMember(memberUid: String) {
        val groupId = _uiState.value.group?.id ?: return
        viewModelScope.launch {
            groupRepository.kickMember(groupId, memberUid).collect { result ->
                result.onFailure { error ->
                    _uiState.update { it.copy(errorMessage = error.localizedMessage) }
                }
            }
        }
    }

    fun onInviteUsernameChanged(query: String) {
        _uiState.update { it.copy(inviteUsernameQuery = query, inviteError = null) }
    }

    fun inviteMember() {
        val query = _uiState.value.inviteUsernameQuery.trim().lowercase().removePrefix("@")
        val groupId = _uiState.value.group?.id ?: return
        if (query.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            chatRepository.searchContactByUsername(query).collect { searchResult ->
                searchResult.fold(
                    onSuccess = { targetUser ->
                        if (targetUser != null) {
                            groupRepository.inviteMember(groupId, targetUser).collect { inviteResult ->
                                _uiState.update { it.copy(isLoading = false) }
                                inviteResult.fold(
                                    onSuccess = {
                                        _uiState.update {
                                            it.copy(isInviteDialogVisible = false, inviteUsernameQuery = "")
                                        }
                                    },
                                    onFailure = { error ->
                                        _uiState.update { it.copy(inviteError = error.localizedMessage) }
                                    }
                                )
                            }
                        } else {
                            _uiState.update {
                                it.copy(isLoading = false, inviteError = "Pengguna @$query tidak ditemukan")
                            }
                        }
                    },
                    onFailure = { error ->
                        _uiState.update { it.copy(isLoading = false, inviteError = error.localizedMessage) }
                    }
                )
            }
        }
    }

    fun createPoll(question: String, options: List<String>) {
        val groupId = _uiState.value.group?.id ?: return
        val creator = _uiState.value.currentUser ?: return
        val filteredOptions = options.map { it.trim() }.filter { it.isNotBlank() }

        if (question.isBlank() || filteredOptions.size < 2) {
            _uiState.update { it.copy(errorMessage = "Polling membutuhkan pertanyaan dan minimal 2 opsi!") }
            return
        }

        viewModelScope.launch {
            groupRepository.createPoll(groupId, creator, question, filteredOptions).collect { result ->
                result.fold(
                    onSuccess = {
                        _uiState.update { it.copy(isCreatePollDialogVisible = false) }
                    },
                    onFailure = { error ->
                        _uiState.update { it.copy(errorMessage = error.localizedMessage) }
                    }
                )
            }
        }
    }

    fun votePoll(pollId: String, optionId: String) {
        val groupId = _uiState.value.group?.id ?: return
        val user = _uiState.value.currentUser ?: return

        viewModelScope.launch {
            groupRepository.votePoll(groupId, pollId, optionId, user.uid).collect { result ->
                result.onFailure { error ->
                    _uiState.update { it.copy(errorMessage = error.localizedMessage) }
                }
            }
        }
    }

    fun openEditGroupDialog() {
        _uiState.update { it.copy(isEditGroupDialogVisible = true) }
    }

    fun closeEditGroupDialog() {
        _uiState.update { it.copy(isEditGroupDialogVisible = false) }
    }

    fun openInviteDialog() {
        _uiState.update { it.copy(isInviteDialogVisible = true, inviteUsernameQuery = "", inviteError = null) }
    }

    fun closeInviteDialog() {
        _uiState.update { it.copy(isInviteDialogVisible = false) }
    }

    fun openCreatePollDialog() {
        _uiState.update { it.copy(isCreatePollDialogVisible = true) }
    }

    fun closeCreatePollDialog() {
        _uiState.update { it.copy(isCreatePollDialogVisible = false) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
