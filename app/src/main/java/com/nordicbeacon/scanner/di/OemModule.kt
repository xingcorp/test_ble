package com.nordicbeacon.scanner.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * 🔧 Hilt Module - OEM Battery Optimization Dependencies
 * 
 * Provides OEM-specific optimization strategies và coordination services
 * All OEM handlers are automatically discovered và registered
 * 
 * @author Senior Android Developer
 */
@Module
@InstallIn(SingletonComponent::class)
object OemModule {
    
    // All OEM handlers are automatically injected by Hilt
    // No explicit provides methods needed due to @Inject constructors
    
    // Registry và Coordinator are @Singleton và auto-injected
    // DeviceDetectionFactory is @Singleton và auto-injected
    // UserEducationHelper is @Singleton và auto-injected
}
