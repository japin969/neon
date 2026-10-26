package app.neon.com.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.neon.com.data.model.UserStory
import app.neon.com.data.repository.SocialRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StoryViewerUiState(
    val stories: List<UserStory> = emptyList(),
    val currentIndex: Int = 0,
    val progress: Float = 0f,
    val isFinished: Boolean = false,
    val isLoading: Boolean = true
)

@HiltViewModel
class StoryViewerViewModel @Inject constructor(
    private val socialRepository: SocialRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StoryViewerUiState())
    val uiState: StateFlow<StoryViewerUiState> = _uiState.asStateFlow()

    private var progressJob: Job? = null
    private val storyDurationMs = 5000L
    private val stepIntervalMs = 50L

    fun initStories(userId: String) {
        viewModelScope.launch {
            socialRepository.observeUserStories(userId).collect { list ->
                _uiState.update {
                    it.copy(
                        stories = list,
                        currentIndex = 0,
                        progress = 0f,
                        isFinished = list.isEmpty(),
                        isLoading = false
                    )
                }
                if (list.isNotEmpty()) {
                    startStoryProgress()
                }
            }
        }
    }

    private fun startStoryProgress() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            val totalSteps = (storyDurationMs / stepIntervalMs).toInt()
            for (step in 1..totalSteps) {
                delay(stepIntervalMs)
                _uiState.update { it.copy(progress = step.toFloat() / totalSteps) }
            }
            nextStory()
        }
    }

    fun nextStory() {
        val state = _uiState.value
        if (state.currentIndex < state.stories.size - 1) {
            _uiState.update {
                it.copy(
                    currentIndex = it.currentIndex + 1,
                    progress = 0f
                )
            }
            startStoryProgress()
        } else {
            progressJob?.cancel()
            _uiState.update { it.copy(isFinished = true) }
        }
    }

    fun previousStory() {
        val state = _uiState.value
        if (state.currentIndex > 0) {
            _uiState.update {
                it.copy(
                    currentIndex = it.currentIndex - 1,
                    progress = 0f
                )
            }
            startStoryProgress()
        } else {
            _uiState.update { it.copy(progress = 0f) }
            startStoryProgress()
        }
    }

    override fun onCleared() {
        super.onCleared()
        progressJob?.cancel()
    }
}
