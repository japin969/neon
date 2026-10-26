package app.neon.com.data.network

import app.neon.com.data.model.ModulesApiResponse
import app.neon.com.data.model.MusicSearchApiResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface MusicApiService {

    @GET("search/songs")
    suspend fun searchSongs(
        @Query("query") query: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): MusicSearchApiResponse

    @GET("modules")
    suspend fun getModules(
        @Query("language") language: String = "english,hindi"
    ): ModulesApiResponse
}
