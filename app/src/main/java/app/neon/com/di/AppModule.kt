package app.neon.com.di

import android.content.Context
import app.neon.com.data.local.FileDatabaseManager
import app.neon.com.data.repository.AuthRepository
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
    fun provideChatRepository(
        fileDatabaseManager: FileDatabaseManager
    ): app.neon.com.data.repository.ChatRepository {
        return app.neon.com.data.repository.ChatRepository(fileDatabaseManager)
    }

    @Provides
    @Singleton
    fun provideChatMessageRepository(): app.neon.com.data.repository.ChatMessageRepository {
        return app.neon.com.data.repository.ChatMessageRepository()
    }

    @Provides
    @Singleton
    fun provideGroupRepository(): app.neon.com.data.repository.GroupRepository {
        return app.neon.com.data.repository.GroupRepository()
    }

    @Provides
    @Singleton
    fun provideSocialRepository(
        fileDatabaseManager: FileDatabaseManager
    ): app.neon.com.data.repository.SocialRepository {
        return app.neon.com.data.repository.SocialRepository(fileDatabaseManager)
    }

    @Provides
    @Singleton
    fun provideMusicRepository(): app.neon.com.data.repository.MusicRepository {
        return app.neon.com.data.repository.MusicRepository()
    }

    @Provides
    @Singleton
    fun provideMusicController(
        @ApplicationContext context: Context
    ): app.neon.com.data.player.MusicController {
        return app.neon.com.data.player.MusicController(context)
    }
}
