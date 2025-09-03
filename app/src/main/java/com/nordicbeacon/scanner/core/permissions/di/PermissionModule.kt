package com.nordicbeacon.scanner.core.permissions.di

import com.nordicbeacon.scanner.core.permissions.*
import com.nordicbeacon.scanner.core.permissions.impl.*
import com.nordicbeacon.scanner.core.permissions.strategy.PermissionStrategyFactory
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 🏗️ Hilt Dependency Injection Module for Permission System
 * 
 * Configures all permission-related dependencies with proper scoping
 * and lifecycle management. Follows clean architecture principles.
 * 
 * @author Senior Android Engineer
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class PermissionModule {
    
    // ========== INTERFACE BINDINGS ==========
    
    /**
     * 🔐 Bind PermissionManager implementation
     */
    @Binds
    @Singleton
    abstract fun bindPermissionManager(
        implementation: PermissionManagerImpl
    ): PermissionManager
    
    /**
     * 📚 Bind PermissionEducator implementation
     */
    @Binds
    @Singleton
    abstract fun bindPermissionEducator(
        implementation: PermissionEducatorImpl
    ): PermissionEducator
    
    /**
     * 💾 Bind PermissionRepository implementation
     */
    @Binds
    @Singleton
    abstract fun bindPermissionRepository(
        implementation: PermissionRepositoryImpl
    ): PermissionRepository
    
    companion object {
        
        /**
         * 🎯 Provide PermissionStrategy based on current device
         */
        @Provides
        @Singleton
        fun providePermissionStrategy(): PermissionStrategy {
            return PermissionStrategyFactory.createStrategy()
        }
        
        /**
         * 📱 Provide DeviceInfo for debugging and strategy selection
         */
        @Provides
        @Singleton
        fun provideDeviceInfo(): com.nordicbeacon.scanner.core.permissions.strategy.DeviceInfo {
            return PermissionStrategyFactory.getDeviceInfo()
        }
    }
}


