package app.neon.com.ui.leaderboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.neon.com.data.model.LeaderboardEntry
import app.neon.com.data.repository.SocialRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.timeout
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LeaderboardUiState(
    val allEntries: List<LeaderboardEntry> = emptyList(),
    val top1: LeaderboardEntry? = null,
    val top2: LeaderboardEntry? = null,
    val top3: LeaderboardEntry? = null,
    val otherEntries: List<LeaderboardEntry> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class LeaderboardViewModel @Inject constructor(
    private val socialRepository: SocialRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LeaderboardUiState(isLoading = true))
    val uiState: StateFlow<LeaderboardUiState> = _uiState.asStateFlow()

    init {
        loadLeaderboard()
    }

    fun loadLeaderboard() {
        viewModelScope.launch {
            socialRepository.observeLeaderboard()
                .timeout(5000)
                .catch { e ->
                _uiState.update { it.copy(isLoading = false, errorMessage = e.localizedMessage) }
            }.collect { entries ->
                val top1 = entries.getOrNull(0)
                val top2 = entries.getOrNull(1)
                val top3 = entries.getOrNull(2)
                val others = if (entries.size > 3) entries.subList(3, entries.size) else emptyList()

                _uiState.update {
                    it.copy(
                        allEntries = entries,
                        top1 = top1,
                        top2 = top2,
                        top3 = top3,
                        otherEntries = others,
                        isLoading = false
                    )
                }
            }
        }
    }
}
