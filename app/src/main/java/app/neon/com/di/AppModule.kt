package app.neon.com.di

import android.content.Context
import app.neon.com.data.local.FileDatabaseManager
import app.neon.com.data.repository.AuthRepository
import app.neon.com.data.repository.ChatRepository
import app.neon.com.data.repository.GroupRepository
import app.neon.com.data.repository.MusicRepository
import app.neon.com.data.repository.SocialRepository
import app.neon.com.data.webrtc.WebRTCClient
import app.neon.com.data.network.NexaDevApiService
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideFileDatabaseManager(
        @ApplicationContext context: Context
    ): FileDatabaseManager {
        return FileDatabaseManager(context)
    }

    @Provides
    @Singleton
    fun provideAuthRepository(
        fileDatabaseManager: FileDatabaseManager
    ): AuthRepository {
        return AuthRepository(fileDatabaseManager)
    }

    @Provides
    @Singleton
    fun provideChatRepository(): ChatRepository {
        return ChatRepository()
    }

    @Provides
    @Singleton
    fun provideGroupRepository(): GroupRepository {
        return GroupRepository()
    }

    @Provides
    @Singleton
    fun provideSocialRepository(
        fileDatabaseManager: FileDatabaseManager
    ): SocialRepository {
        return SocialRepository(fileDatabaseManager)
    }

    @Provides
    @Singleton
    fun provideMusicRepository(): MusicRepository {
        return MusicRepository()
    }

    @Provides
    @Singleton
    fun provideWebRTCClient(
        @ApplicationContext context: Context
    ): WebRTCClient {
        return WebRTCClient(context)
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .callTimeout(90, TimeUnit.SECONDS)
            // API NexaDev memblokir user-agent okhttp. Pakai UA browser.
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header(
                        "User-Agent",
                        "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
                    )
                    .header("Accept", "application/json")
                    .build()
                chain.proceed(request)
            }
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://api.nexadev.my.id/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideNexaDevApiService(retrofit: Retrofit): NexaDevApiService {
        return retrofit.create(NexaDevApiService::class.java)
    }
}
