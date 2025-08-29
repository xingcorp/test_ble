package com.nordicbeacon.scanner.di

import android.content.Context
import androidx.room.Room
import com.nordicbeacon.scanner.data.database.BeaconDao
import com.nordicbeacon.scanner.data.database.BeaconDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 💾 Hilt Module - Database Dependencies
 * 
 * Provides Room database và DAO instances với proper configuration
 * 
 * @author Senior Android Developer
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /**
     * 🏗️ Provides Room Database instance
     */
    @Provides
    @Singleton
    fun provideBeaconDatabase(
        @ApplicationContext context: Context
    ): BeaconDatabase {
        return BeaconDatabase.create(context)
    }

    /**
     * 📊 Provides BeaconDao từ database
     */
    @Provides
    fun provideBeaconDao(database: BeaconDatabase): BeaconDao {
        return database.beaconDao()
    }
}
