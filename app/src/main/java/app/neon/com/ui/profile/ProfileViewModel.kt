package app.neon.com.ui.profile

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.neon.com.data.model.User
import app.neon.com.data.model.UserStory
import app.neon.com.data.repository.AuthRepository
import app.neon.com.data.repository.SocialRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val userVideos: List<app.neon.com.data.model.VideoPost> = emptyList(),
    val currentUser: User? = null,
    val stories: List<UserStory> = emptyList(),
    val hasActiveStory: Boolean = false,
    val newStoryCaption: String = "",
    val newStoryColor: String = "#7928CA",
    val isCreateStoryDialogVisible: Boolean = false,
    val cacheSizeBytes: Long = 0L,
    val formattedCacheSize: String = "0 KB",
    val loggedInDeviceName: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val socialRepository: SocialRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfileData()
        loadDeviceInfo()
    }

    fun loadProfileData() {
        val user = authRepository.getCachedUser() ?: authRepository.getCurrentFirebaseUser()?.let {
            User(uid = it.uid, email = it.email ?: "")
        }

        _uiState.update { it.copy(currentUser = user) }

        if (user != null) {
            // Observe real-time profile (stat updates like followers, likesCount)
            viewModelScope.launch {
                socialRepository.observeUserProfile(user.uid).collect { updatedUser ->
                    if (updatedUser != null) {
                        _uiState.update { it.copy(currentUser = updatedUser) }
                    }
                }
            }

            // Observe stories
            viewModelScope.launch {
                socialRepository.observeUserStories(user.uid).collect { storyList ->
                    _uiState.update {
                        it.copy(
                            stories = storyList,
                            hasActiveStory = storyList.isNotEmpty()
                        )
                    }
                }
            }
        }

        refreshCacheSize()
    }

    private fun loadDeviceInfo() {
        val model = Build.MODEL
        val manufacturer = Build.MANUFACTURER.replaceFirstChar { it.uppercase() }
        val release = Build.VERSION.RELEASE
        val deviceString = "$manufacturer $model (Android $release)"
        _uiState.update { it.copy(loggedInDeviceName = deviceString) }
    }

    fun refreshCacheSize() {
        val bytes = socialRepository.getStorageCacheSizeInBytes()
        val formatted = when {
            bytes >= 1024 * 1024 -> String.format("%.2f MB", bytes / (1024f * 1024f))
            bytes >= 1024 -> String.format("%.1f KB", bytes / 1024f)
            else -> "$bytes B"
        }
        _uiState.update { it.copy(cacheSizeBytes = bytes, formattedCacheSize = formatted) }
    }

    fun clearCache() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            socialRepository.clearLocalCache().collect { result ->
                _uiState.update { it.copy(isLoading = false) }
                result.fold(
                    onSuccess = {
                        refreshCacheSize()
                    },
                    onFailure = { error ->
                        _uiState.update { it.copy(errorMessage = error.localizedMessage) }
                    }
                )
            }
        }
    }



    fun togglePrivacyLastSeen() {
        val user = _uiState.value.currentUser ?: return
        val newShowLastSeen = !user.showLastSeen
        viewModelScope.launch {
            socialRepository.updatePrivacySettings(user.uid, newShowLastSeen, user.showProfilePhoto).collect { result ->
                result.onSuccess {
                    _uiState.update { current ->
                        current.copy(currentUser = current.currentUser?.copy(showLastSeen = newShowLastSeen))
                    }
                }
            }
        }
    }

    fun togglePrivacyProfilePhoto() {
        val user = _uiState.value.currentUser ?: return
        val newShowProfilePhoto = !user.showProfilePhoto
        viewModelScope.launch {
            socialRepository.updatePrivacySettings(user.uid, user.showLastSeen, newShowProfilePhoto).collect { result ->
                result.onSuccess {
                    _uiState.update { current ->
                        current.copy(currentUser = current.currentUser?.copy(showProfilePhoto = newShowProfilePhoto))
                    }
                }
            }
        }
    }

    fun onNewStoryCaptionChanged(caption: String) {
        _uiState.update { it.copy(newStoryCaption = caption) }
    }

    fun onNewStoryColorSelected(colorHex: String) {
        _uiState.update { it.copy(newStoryColor = colorHex) }
    }

    fun openCreateStoryDialog() {
        _uiState.update { it.copy(isCreateStoryDialogVisible = true, newStoryCaption = "") }
    }

    fun closeCreateStoryDialog() {
        _uiState.update { it.copy(isCreateStoryDialogVisible = false) }
    }

    fun createStory(onSuccess: (storyId: String) -> Unit) {
        val user = _uiState.value.currentUser ?: return
        val caption = _uiState.value.newStoryCaption.trim()
        val color = _uiState.value.newStoryColor

        if (caption.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            socialRepository.createStory(user.uid, user.username, caption, color).collect { result ->
                _uiState.update { it.copy(isLoading = false) }
                result.fold(
                    onSuccess = { story: app.neon.com.data.model.UserStory ->
                        closeCreateStoryDialog()
                        onSuccess(story.id)
                    },
                    onFailure = { error ->
                        _uiState.update { it.copy(errorMessage = error.localizedMessage) }
                    }
                )
            }
        }
    }

    fun logout(onLoggedOut: () -> Unit) {
        viewModelScope.launch {
            authRepository.signOut()
            onLoggedOut()
        }
    }

    fun canChangeUsername(user: User): Boolean = authRepository.canChangeUsername(user)
    fun canChangeDisplayName(user: User): Boolean = authRepository.canChangeDisplayName(user)
    fun getRemainingDays(lastChange: Long, cooldownDays: Int): Int = authRepository.getRemainingDays(lastChange, cooldownDays)
    fun updateProfile(
        uid: String, 
        newDisplay: String, 
        newUsername: String, 
        bio: String = "", 
        avatarColorHex: String = "#00FF66", 
        avatarUrl: String = "", 
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val current = _uiState.value.currentUser
            if (current != null) {
                if (newDisplay != current.displayName && authRepository.canChangeDisplayName(current)) {
                    authRepository.updateDisplayName(uid, newDisplay)
                }
                if (newUsername != current.username && authRepository.canChangeUsername(current)) {
                    authRepository.updateUsername(uid, newUsername)
                }
                if (bio != current.bio || avatarColorHex != current.avatarColorHex || avatarUrl != current.avatarUrl) {
                    socialRepository.updateProfile(uid, newDisplay, bio, avatarColorHex)
                }
                onSuccess()
            }
        }
    }

    fun loadUserVideos(userId: String) {
        viewModelScope.launch {
            socialRepository.getUserVideos(userId).collect { videos ->
                _uiState.update { it.copy(userVideos = videos) }
            }
        }
    }
}
