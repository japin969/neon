package app.neon.com.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.neon.com.data.model.Song
import app.neon.com.data.player.MusicPlayerManager
import app.neon.com.data.repository.MusicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MusicPlayerUiState(
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 1L,
    val lovedSongIds: Set<String> = emptySet()
) {
    val isCurrentSongLoved: Boolean
        get() = currentSong?.id?.let { it in lovedSongIds } == true
}

@HiltViewModel
class MusicViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: MusicRepository
) : ViewModel() {

    private val playerManager = MusicPlayerManager(context)

    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs.asStateFlow()

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(1L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _lovedSongIds = MutableStateFlow<Set<String>>(emptySet())
    val lovedSongIds: StateFlow<Set<String>> = _lovedSongIds.asStateFlow()

    val uiState: StateFlow<MusicPlayerUiState> = combine(
        _currentSong, _isPlaying, _currentPositionMs, _durationMs, _lovedSongIds
    ) { current, playing, pos, dur, loved ->
        MusicPlayerUiState(
            currentSong = current,
            isPlaying = playing,
            currentPositionMs = pos,
            durationMs = dur,
            lovedSongIds = loved
        )
    }.stateIn(
        viewModelScope,
        kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
        MusicPlayerUiState()
    )

    init {
        loadSongs()
        observePlayerState()
    }

    private fun observePlayerState() {
        viewModelScope.launch { playerManager.currentSong.collect { _currentSong.value = it } }
        viewModelScope.launch { playerManager.isPlaying.collect { _isPlaying.value = it } }
        viewModelScope.launch { playerManager.durationMs.collect { _durationMs.value = it } }
        viewModelScope.launch { playerManager.currentPositionMs.collect { _currentPositionMs.value = it } }
    }

    fun loadSongs(query: String = "") {
        viewModelScope.launch {
            try { _songs.value = repository.searchSongs(query) }
            catch (e: Exception) { _songs.value = emptyList() }
        }
    }

    fun playSong(song: Song) { playerManager.playSong(song, _songs.value) }
    fun togglePlayPause() { playerManager.togglePlayPause() }
    fun seekTo(positionMs: Long) { playerManager.seekTo(positionMs) }
    fun previous() { playerManager.previous() }
    fun next() { playerManager.next() }

    fun toggleLoveSong(song: Song) {
        viewModelScope.launch {
            repository.toggleLoveSong("demo_user", song)
            val updated = _songs.value.map {
                if (it.id == song.id) it.copy(isLoved = !it.isLoved) else it
            }
            _songs.value = updated
            _lovedSongIds.value = updated.filter { it.isLoved }.map { it.id }.toSet()
        }
    }

    fun toggleLoveCurrentSong() {
        _currentSong.value?.let { toggleLoveSong(it) }
    }

    override fun onCleared() {
        super.onCleared()
        playerManager.release()
    }
}
