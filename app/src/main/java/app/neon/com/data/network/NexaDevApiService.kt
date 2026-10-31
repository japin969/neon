package app.neon.com.data.network

import retrofit2.http.GET
import retrofit2.http.Query

interface NexaDevApiService {
    @GET("api/spotifyplay")
    suspend fun searchSpotify(@Query("q") query: String): NexaDevResponse

    @GET("api/ytplay")
    suspend fun searchYoutube(@Query("q") query: String): NexaDevResponse

    @GET("ai/gpt4")
    suspend fun askAi(@Query("q") query: String): NexaDevChatResponse
}

data class NexaDevResponse(
    val status: Boolean = false,
    val data: NexaDevData? = null
)

data class NexaDevData(
    val title: String = "",
    val artist: String = "",
    val thumbnail: String = "",
    val url: String = "",
    val duration: String = ""
)

data class NexaDevChatResponse(
    val author: String = "",
    val status: Boolean = false,
    val data: NexaDevChatData? = null
)

data class NexaDevChatData(
    val message: String = ""
)
