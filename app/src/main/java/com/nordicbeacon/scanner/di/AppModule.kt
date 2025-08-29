package com.nordicbeacon.scanner.di

import android.content.Context
import android.os.PowerManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * 🔧 Hilt Module - Application Level Dependencies
 * 
 * Provides application-wide dependencies following DI best practices
 * 
 * @author Senior Android Developer
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /**
     * 🔄 Coroutine Dispatchers for different use cases
     */
    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides  
    @MainDispatcher
    fun provideMainDispatcher(): CoroutineDispatcher = Dispatchers.Main

    @Provides
    @DefaultDispatcher  
    fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default

    /**
     * 🔋 Power Management Services
     */
    @Provides
    @Singleton
    fun providePowerManager(
        @ApplicationContext context: Context
    ): PowerManager {
        return context.getSystemService(Context.POWER_SERVICE) as PowerManager
    }
}

// ========== QUALIFIER ANNOTATIONS ==========

/**
 * Qualifier cho IO operations (network, database, file)
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

/**
 * Qualifier cho Main thread operations (UI updates)  
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MainDispatcher

/**
 * Qualifier cho CPU-intensive operations
 */
@Qualifier
@Retention(AnnotationRetention.BINARY) 
annotation class DefaultDispatcher
