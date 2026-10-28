package app.neon.com.data.player

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import app.neon.com.data.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * MusicPlayerManager - ExoPlayer (Media3) Engine
 * Menangani streaming audio lagu sungguhan dari URL (Spotify/SoundCloud/YouTube/Deezer preview stream)
 */
class MusicPlayerManager(private val context: Context) {

    private var exoPlayer: ExoPlayer? = null
    private val scope = CoroutineScope(Dispatchers.Main)
    private var progressJob: Job? = null

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(1L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    init {
        runCatching {
            exoPlayer = ExoPlayer.Builder(context).build().apply {
                addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(playing: Boolean) {
                        _isPlaying.value = playing
                        if (playing) {
                            startProgressTicker()
                        } else {
                            progressJob?.cancel()
                        }
                    }

                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_READY) {
                            _durationMs.value = duration.coerceAtLeast(1L)
                        } else if (playbackState == Player.STATE_ENDED) {
                            _isPlaying.value = false
                            _currentPositionMs.value = 0L
                            progressJob?.cancel()
                        }
                    }
                })
            }
        }
    }

    private fun startProgressTicker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive && _isPlaying.value) {
                exoPlayer?.let { player ->
                    _currentPositionMs.value = player.currentPosition
                    if (player.duration > 0) {
                        _durationMs.value = player.duration
                    }
                }
                delay(500)
            }
        }
    }

    /**
     * Memutar lagu dengan ExoPlayer
     */
    fun playSong(song: Song) {
        _currentSong.value = song
        val player = exoPlayer ?: return

        try {
            val mediaItem = MediaItem.fromUri(song.streamUrl)
            player.setMediaItem(mediaItem)
            player.prepare()
            player.play()
            _isPlaying.value = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Play/Pause Toggle
     */
    fun togglePlayPause() {
        val player = exoPlayer ?: return
        if (player.isPlaying) {
            player.pause()
            _isPlaying.value = false
        } else {
            player.play()
            _isPlaying.value = true
        }
    }

    fun seekTo(positionMs: Long) {
        exoPlayer?.seekTo(positionMs)
        _currentPositionMs.value = positionMs
    }

    fun release() {
        progressJob?.cancel()
        exoPlayer?.release()
        exoPlayer = null
    }
}
