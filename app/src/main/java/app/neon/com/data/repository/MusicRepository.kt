package app.neon.com.data.repository

import app.neon.com.data.model.Song
import app.neon.com.data.network.MusicApiService
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicRepository @Inject constructor() {

    private val firebaseDatabase: FirebaseDatabase = FirebaseDatabase.getInstance()

    private val apiService: MusicApiService by lazy {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        Retrofit.Builder()
            .baseUrl("https://neont-music.vercel.app/api/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(MusicApiService::class.java)
    }

    // High Quality Curated Offline Fallbacks (Cyber / Neon Pop / Synthwave)
    private val fallbackTrendingSongs = listOf(
        Song(
            id = "neon_track_1",
            title = "Midnight City Lights",
            artist = "Neon Dreamer",
            album = "Cyber Horizon",
            coverUrl = "https://images.unsplash.com/photo-1614613535308-eb5fbd3d2c17?w=500&auto=format&fit=crop&q=80",
            audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
            durationSeconds = 372
        ),
        Song(
            id = "neon_track_2",
            title = "Electric Pulse",
            artist = "Synthetix",
            album = "Voltage 2026",
            coverUrl = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=500&auto=format&fit=crop&q=80",
            audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3",
            durationSeconds = 423
        ),
        Song(
            id = "neon_track_3",
            title = "Neon Skyline",
            artist = "Retrowave Collective",
            album = "After Midnight",
            coverUrl = "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=500&auto=format&fit=crop&q=80",
            audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3",
            durationSeconds = 345
        ),
        Song(
            id = "neon_track_4",
            title = "Cybernetic Heart",
            artist = "Aura 99",
            album = "Genesis",
            coverUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=500&auto=format&fit=crop&q=80",
            audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3",
            durationSeconds = 289
        ),
        Song(
            id = "neon_track_5",
            title = "Infinity Drive",
            artist = "Vektor",
            album = "Subsonic",
            coverUrl = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=500&auto=format&fit=crop&q=80",
            audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-5.mp3",
            durationSeconds = 380
        )
    )

    /**
     * Searches songs from the API (GET /search/songs?query={query}).
     * Falls back to filtered catalog if network errors occur.
     */
    fun searchSongs(query: String): Flow<Result<List<Song>>> = flow {
        try {
            val response = apiService.searchSongs(query.trim())
            val results = response.data?.results?.map { it.toSong() } ?: emptyList()
            if (results.isNotEmpty()) {
                emit(Result.success(results))
            } else {
                // Filter fallback
                val filtered = fallbackTrendingSongs.filter {
                    it.title.contains(query, ignoreCase = true) ||
                    it.artist.contains(query, ignoreCase = true)
                }
                emit(Result.success(filtered))
            }
        } catch (e: Exception) {
            val filtered = fallbackTrendingSongs.filter {
                it.title.contains(query, ignoreCase = true) ||
                it.artist.contains(query, ignoreCase = true)
            }
            emit(Result.success(filtered.ifEmpty { fallbackTrendingSongs }))
        }
    }

    /**
     * Gets Trending Songs from the API (GET /modules).
     */
    fun getTrendingSongs(): Flow<Result<List<Song>>> = flow {
        try {
            val response = apiService.getModules()
            val songs = response.data?.trending?.songs?.map { it.toSong() } ?: emptyList()
            if (songs.isNotEmpty()) {
                emit(Result.success(songs))
            } else {
                emit(Result.success(fallbackTrendingSongs))
            }
        } catch (e: Exception) {
            emit(Result.success(fallbackTrendingSongs))
        }
    }

    /**
     * Gets Recommended Songs for the user.
     */
    fun getRecommendedSongs(): Flow<Result<List<Song>>> = flow {
        try {
            val response = apiService.searchSongs("top hits", 1, 15)
            val songs = response.data?.results?.map { it.toSong() } ?: emptyList()
            if (songs.isNotEmpty()) {
                emit(Result.success(songs))
            } else {
                emit(Result.success(fallbackTrendingSongs.shuffled()))
            }
        } catch (e: Exception) {
            emit(Result.success(fallbackTrendingSongs.shuffled()))
        }
    }

    /**
     * Toggles loving a song and stores/removes it under /users/{uid}/lovedSongs/{songId}.
     */
    fun toggleLoveSong(userId: String, song: Song, currentLoved: Boolean): Flow<Result<Boolean>> = flow {
        try {
            val newLoved = !currentLoved
            val ref = firebaseDatabase.getReference("users")
                .child(userId)
                .child("lovedSongs")
                .child(song.id)

            if (newLoved) {
                ref.setValue(song).await()
            } else {
                ref.removeValue().await()
            }

            emit(Result.success(newLoved))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    /**
     * Observes the user's loved song IDs in Realtime.
     */
    fun observeLovedSongIds(userId: String): Flow<Set<String>> = callbackFlow {
        if (userId.isBlank()) {
            trySend(emptySet())
            close()
            return@callbackFlow
        }

        val ref = firebaseDatabase.getReference("users").child(userId).child("lovedSongs")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val ids = snapshot.children.mapNotNull { it.key }.toSet()
                trySend(ids)
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(emptySet())
            }
        }

        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }
}
