package app.neon.com.ui.music

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.neon.com.data.model.Song
import app.neon.com.data.model.User
import app.neon.com.data.player.MusicController
import app.neon.com.data.repository.AuthRepository
import app.neon.com.data.repository.MusicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class MusicUiState(
    val greeting: String = "Good Morning",
    val currentUser: User? = null,
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,
    val isSearching: Boolean = false,
    val searchResults: List<Song> = emptyList(),
    val trendingSongs: List<Song> = emptyList(),
    val recommendedSongs: List<Song> = emptyList(),
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 1L,
    val lovedSongIds: Set<String> = emptySet(),
    val isCurrentSongLoved: Boolean = false,
    val isLoadingTrending: Boolean = true,
    val errorMessage: String? = null
)

@HiltViewModel
class MusicViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
    private val musicController: MusicController,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MusicUiState())
    val uiState: StateFlow<MusicUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        determineGreeting()
        loadUserAndLovedSongs()
        loadTrendingAndRecommended()
        observePlaybackState()
    }

    private fun determineGreeting() {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val greetingText = when (hour) {
            in 5..11 -> "Good Morning"
            in 12..16 -> "Good Afternoon"
            in 17..20 -> "Good Evening"
            else -> "Good Night"
        }
        _uiState.update { it.copy(greeting = greetingText) }
    }

    private fun loadUserAndLovedSongs() {
        val user = authRepository.getCachedUser() ?: authRepository.getCurrentFirebaseUser()?.let {
            User(uid = it.uid, email = it.email ?: "")
        }
        _uiState.update { it.copy(currentUser = user) }

        if (user != null) {
            viewModelScope.launch {
                musicRepository.observeLovedSongIds(user.uid).collect { lovedSet ->
                    _uiState.update { current ->
                        current.copy(
                            lovedSongIds = lovedSet,
                            isCurrentSongLoved = current.currentSong?.id?.let { lovedSet.contains(it) } == true
                        )
                    }
                }
            }
        }
    }

    private fun loadTrendingAndRecommended() {
        viewModelScope.launch {
            try {
                val list = musicRepository.getTrendingSongs()
                _uiState.update { it.copy(trendingSongs = list, isLoadingTrending = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoadingTrending = false) }
            }
        }

        viewModelScope.launch {
            try {
                val list = musicRepository.getPopularSongs()
                _uiState.update { it.copy(recommendedSongs = list) }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun observePlaybackState() {
        viewModelScope.launch {
            musicController.currentSong.collect { song ->
                _uiState.update { current ->
                    current.copy(
                        currentSong = song,
                        isCurrentSongLoved = song?.id?.let { current.lovedSongIds.contains(it) } == true
                    )
                }
            }
        }

        viewModelScope.launch {
            musicController.isPlaying.collect { playing ->
                _uiState.update { it.copy(isPlaying = playing) }
            }
        }

        viewModelScope.launch {
            musicController.currentPositionMs.collect { pos ->
                _uiState.update { it.copy(currentPositionMs = pos) }
            }
        }

        viewModelScope.launch {
            musicController.durationMs.collect { dur ->
                _uiState.update { it.copy(durationMs = dur.coerceAtLeast(1L)) }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update {
            it.copy(
                searchQuery = query,
                isSearchActive = query.isNotBlank()
            )
        }

        searchJob?.cancel()
        if (query.isBlank()) {
            _uiState.update { it.copy(searchResults = emptyList(), isSearching = false) }
            return
        }

        searchJob = viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true) }
            delay(400) // Debounce typing

            try {
                val songs = musicRepository.searchSongsUnified(query)
                _uiState.update { it.copy(searchResults = songs, isSearching = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSearching = false, errorMessage = e.localizedMessage) }
            }
        }
    }

    fun clearSearch() {
        _uiState.update {
            it.copy(
                searchQuery = "",
                isSearchActive = false,
                searchResults = emptyList(),
                isSearching = false
            )
        }
    }

    fun playSong(song: Song, playlist: List<Song>) {
        musicController.playSong(song, playlist)
    }

    fun togglePlayPause() {
        musicController.togglePlayPause()
    }

    fun seekTo(positionMs: Long) {
        musicController.seekTo(positionMs)
    }

    fun next() {
        musicController.next()
    }

    fun previous() {
        musicController.previous()
    }

    fun toggleLoveSong(song: Song) {
        val user = _uiState.value.currentUser ?: return
        val currentLoved = _uiState.value.lovedSongIds.contains(song.id)

        viewModelScope.launch {
            musicRepository.toggleLoveSong(user.uid, song, currentLoved).collect { result ->
                result.onSuccess { newLoved ->
                    _uiState.update { current ->
                        val updatedSet = if (newLoved) current.lovedSongIds + song.id else current.lovedSongIds - song.id
                        current.copy(
                            lovedSongIds = updatedSet,
                            isCurrentSongLoved = current.currentSong?.id?.let { updatedSet.contains(it) } == true
                        )
                    }
                }
            }
        }
    }

    fun toggleLoveCurrentSong() {
        val song = _uiState.value.currentSong ?: return
        toggleLoveSong(song)
    }
}
