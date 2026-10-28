package app.neon.com.di

import android.content.Context
import app.neon.com.data.local.FileDatabaseManager
import app.neon.com.data.repository.AuthRepository
import app.neon.com.data.repository.ChatRepository
import app.neon.com.data.repository.GroupRepository
import app.neon.com.data.repository.MusicRepository
import app.neon.com.data.repository.SocialRepository
import app.neon.com.data.webrtc.WebRTCClient
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
}
