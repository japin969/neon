package app.neon.com.data.network

import retrofit2.http.GET
import retrofit2.http.Query

interface SiputApiService {
    @GET("api/ai/gptoss120b")
    suspend fun askAi(
        @Query("prompt") prompt: String,
        @Query("system") system: String = "You are Neon AI, asisten cerdas di aplikasi chat Neon. Jawab singkat, ramah, dan dalam bahasa yang sama dengan pengguna.",
        @Query("temperature") temperature: Double = 0.7
    ): SiputChatResponse
}

data class SiputChatResponse(
    val status: Boolean = false,
    val data: SiputChatData? = null,
    val timestamp: String = ""
)

data class SiputChatData(
    val response: String = ""
)
