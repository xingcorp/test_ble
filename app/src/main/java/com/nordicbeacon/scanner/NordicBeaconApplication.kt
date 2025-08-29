package com.nordicbeacon.scanner

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

/**
 * 🚀 Application Class - Nordic Beacon Scanner
 * 
 * Main application entry point with Hilt dependency injection
 * Initializes core services and global configuration
 * 
 * @author Senior Android Developer
 */
@HiltAndroidApp
class NordicBeaconApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        
        // Initialize logging system
        initializeLogging()
        
        // Log application startup
        Timber.i("🎯 Nordic Beacon Scanner Application started")
        Timber.i("📱 App Version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
        Timber.i("🤖 Android Version: ${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})")
        Timber.i("📟 Device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
        
        // Initialize core systems
        initializeCoreServices()
    }

    /**
     * Initialize Timber logging với appropriate configuration
     */
    private fun initializeLogging() {
        if (BuildConfig.DEBUG) {
            // Debug logging với detailed formatting
            Timber.plant(object : Timber.DebugTree() {
                override fun createStackElementTag(element: StackTraceElement): String {
                    return "🔍 ${super.createStackElementTag(element)}:${element.lineNumber}"
                }
                
                override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
                    val timestamp = java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.getDefault())
                        .format(java.util.Date())
                    super.log(priority, "[$timestamp] $tag", message, t)
                }
            })
        } else {
            // Production logging - errors only với crash reporting
            Timber.plant(object : Timber.Tree() {
                override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
                    if (priority >= android.util.Log.ERROR) {
                        // Send to crash reporting service (Firebase Crashlytics)
                        // TODO: Implement Crashlytics integration
                    }
                }
            })
        }
        
        Timber.i("🌲 Timber logging initialized (Debug: ${BuildConfig.DEBUG})")
    }

    /**
     * Initialize core application services
     */
    private fun initializeCoreServices() {
        try {
            // Pre-warm critical dependencies
            // Will be injected by Hilt when needed
            
            Timber.i("✅ Core services initialization completed")
            
        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to initialize core services")
            
            // Critical error - app cannot function
            // In production, might want to show user a recovery dialog
            if (!BuildConfig.DEBUG) {
                // TODO: Send to crash reporting
                android.os.Process.killProcess(android.os.Process.myPid())
            }
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        Timber.w("⚠️ Low memory warning received")
        // Trigger memory cleanup in services
        // TODO: Broadcast memory pressure event to services
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        
        val levelDescription = when (level) {
            TRIM_MEMORY_RUNNING_MODERATE -> "Running Moderate"
            TRIM_MEMORY_RUNNING_LOW -> "Running Low"  
            TRIM_MEMORY_RUNNING_CRITICAL -> "Running Critical"
            TRIM_MEMORY_UI_HIDDEN -> "UI Hidden"
            TRIM_MEMORY_BACKGROUND -> "Background"
            TRIM_MEMORY_MODERATE -> "Moderate" 
            TRIM_MEMORY_COMPLETE -> "Complete"
            else -> "Unknown ($level)"
        }
        
        Timber.w("🔄 Memory trim requested: $levelDescription")
        
        // Implement memory management strategy based on trim level
        when (level) {
            TRIM_MEMORY_RUNNING_CRITICAL -> {
                // Critical memory situation - aggressive cleanup
                Timber.w("🚨 Critical memory pressure - triggering aggressive cleanup")
                // TODO: Broadcast aggressive cleanup event
            }
            TRIM_MEMORY_BACKGROUND -> {
                // App moved to background - moderate cleanup
                Timber.i("📱 App backgrounded - moderate memory cleanup")  
                // TODO: Broadcast moderate cleanup event
            }
        }
    }
}
