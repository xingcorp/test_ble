package com.nordicbeacon.scanner.di

import com.nordicbeacon.scanner.data.repositories.BeaconRepositoryImpl
import com.nordicbeacon.scanner.domain.repositories.IBeaconRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 📊 Hilt Module - Repository Dependencies  
 * 
 * Binds repository interfaces to concrete implementations
 * Following Dependency Inversion Principle
 * 
 * @author Senior Android Developer
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    /**
     * 🔗 Bind IBeaconRepository → BeaconRepositoryImpl
     * 
     * Enables dependency injection of repository interface
     * Domain layer depends on abstraction, not concrete implementation
     */
    @Binds
    @Singleton
    abstract fun bindBeaconRepository(
        beaconRepositoryImpl: BeaconRepositoryImpl
    ): IBeaconRepository
}
