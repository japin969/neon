package app.neon.com.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.neon.com.data.model.User
import app.neon.com.data.repository.AuthRepository
import app.neon.com.data.repository.SocialRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FollowListUiState(
    val currentUserId: String = "",
    val targetUserId: String = "",
    val activeTab: String = "followers", // "followers" or "following"
    val followers: List<User> = emptyList(),
    val following: List<User> = emptyList(),
    val followingUids: Set<String> = emptySet(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

@HiltViewModel
class FollowListViewModel @Inject constructor(
    private val socialRepository: SocialRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FollowListUiState())
    val uiState: StateFlow<FollowListUiState> = _uiState.asStateFlow()

    fun init(userId: String, initialTab: String = "followers") {
        val myUid = authRepository.getCachedUser()?.uid
            ?: authRepository.getCurrentFirebaseUser()?.uid
            ?: ""

        _uiState.update {
            it.copy(
                targetUserId = userId,
                currentUserId = myUid,
                activeTab = initialTab,
                isLoading = true
            )
        }

        // Observe my own following set to show Follow/Unfollow state correctly
        if (myUid.isNotBlank()) {
            viewModelScope.launch {
                socialRepository.getFollowing(myUid).collect { myFollowing ->
                    _uiState.update { it.copy(followingUids = myFollowing.map { u -> u.uid }.toSet()) }
                }
            }
        }

        // Observe target's followers
        viewModelScope.launch {
            socialRepository.getFollowers(userId).collect { list ->
                _uiState.update { it.copy(followers = list, isLoading = false) }
            }
        }

        // Observe target's following
        viewModelScope.launch {
            socialRepository.getFollowing(userId).collect { list ->
                _uiState.update { it.copy(following = list) }
            }
        }
    }

    fun selectTab(tab: String) {
        _uiState.update { it.copy(activeTab = tab) }
    }

    fun toggleFollow(targetUser: User) {
        val myUid = _uiState.value.currentUserId
        if (myUid.isBlank() || myUid == targetUser.uid) return

        val isCurrentlyFollowing = _uiState.value.followingUids.contains(targetUser.uid)

        viewModelScope.launch {
            if (isCurrentlyFollowing) {
                socialRepository.unfollowUser(myUid, targetUser.uid).collect { result ->
                    result.onSuccess {
                        _uiState.update { it.copy(followingUids = it.followingUids - targetUser.uid) }
                    }
                }
            } else {
                socialRepository.followUser(myUid, targetUser.uid).collect { result ->
                    result.onSuccess {
                        _uiState.update { it.copy(followingUids = it.followingUids + targetUser.uid) }
                    }
                }
            }
        }
    }
}
