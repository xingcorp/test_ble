package com.nordicbeacon.scanner.di

import android.content.Context
import com.nordicbeacon.scanner.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import org.altbeacon.beacon.BeaconManager
import org.altbeacon.beacon.BeaconParser
import timber.log.Timber
import javax.inject.Singleton

/**
 * 🎯 Hilt Module - Beacon Scanning Dependencies
 * 
 * Provides Nordic beacon specific configuration and dependencies
 * Optimized cho production performance và battery efficiency
 * 
 * @author Senior Android Developer
 */
@Module
@InstallIn(SingletonComponent::class)
object BeaconModule {

    /**
     * 🔍 BeaconManager - Core AltBeacon Library Manager
     * 
     * Configured specifically cho Nordic beacon UUID: FDA50693-0000-0000-0000-290995101092
     * Production-optimized settings cho battery efficiency
     */
    @Provides
    @Singleton
    fun provideBeaconManager(
        @ApplicationContext context: Context
    ): BeaconManager {
        
        Timber.i("🔧 Configuring BeaconManager for Nordic beacons...")
        
        return BeaconManager.getInstanceForApplication(context).apply {
            
            // ========== NORDIC BEACON PARSER CONFIGURATION ==========
            beaconParsers.clear() // Clear default parsers
            beaconParsers.add(
                BeaconParser().apply {
                    setBeaconLayout(BeaconParser.IBEACON_LAYOUT)
                    
                    if (BuildConfig.DEBUG) {
                        Timber.d("📡 iBeacon parser layout: ${BeaconParser.IBEACON_LAYOUT}")
                    }
                }
            )
            
            // ========== PRODUCTION OPTIMIZATIONS ==========
            
            // Disable JobScheduler - use Foreground Service instead për better reliability
            setEnableScheduledScanJobs(false)
            
            // Disable region state persistence cho memory optimization  
            isRegionStatePersistenceEnabled = false
            
            // ========== SCANNING PERIOD CONFIGURATION ==========
            // Optimized cho Nordic beacon detection vs battery usage
            
            // Foreground scanning (when app visible)
            foregroundScanPeriod = 1100L        // 1.1 seconds scan
            foregroundBetweenScanPeriod = 0L     // Continuous when foreground
            
            // Background scanning (when app not visible)  
            backgroundScanPeriod = 10000L        // 10 seconds scan
            backgroundBetweenScanPeriod = 60000L // 60 seconds between scans
            
            // ========== SIGNAL PROCESSING CONFIGURATION ==========
            
            // Hardware-specific optimizations
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                // Use Android 5.0+ BLE APIs cho better performance
                Timber.d("📱 Using Android 5.0+ BLE scanning APIs")
            }
            
            Timber.i("✅ BeaconManager configured successfully")
            Timber.i("🎯 Target Nordic UUID: ${BuildConfig.NORDIC_BEACON_UUID}")
            Timber.i("⏱️ Foreground: ${foregroundScanPeriod}ms scan, ${foregroundBetweenScanPeriod}ms between")
            Timber.i("🌙 Background: ${backgroundScanPeriod}ms scan, ${backgroundBetweenScanPeriod}ms between")
        }
    }

    /**
     * 📊 Beacon Configuration Parameters
     * 
     * Centralized configuration cho all beacon-related settings
     */
    @Provides
    @Singleton
    fun provideBeaconConfiguration(): BeaconConfiguration {
        return BeaconConfiguration(
            nordicUuid = BuildConfig.NORDIC_BEACON_UUID,
            maxDetectionDistance = 50.0, // meters
            minimumRssi = -95, // dBm  
            signalFilteringEnabled = true,
            distanceCalculationEnabled = true,
            duplicateDetectionWindow = 2000L, // ms
            beaconExpirationTime = 30000L // ms
        )
    }
}

/**
 * ⚙️ Beacon Configuration Data Class
 * 
 * Centralized configuration cho beacon scanning behavior
 */
data class BeaconConfiguration(
    val nordicUuid: String,
    val maxDetectionDistance: Double,
    val minimumRssi: Int,
    val signalFilteringEnabled: Boolean,
    val distanceCalculationEnabled: Boolean,
    val duplicateDetectionWindow: Long,
    val beaconExpirationTime: Long
) {
    
    /**
     * Validates if beacon meets detection criteria
     */
    fun meetsDetectionCriteria(rssi: Int, distance: Double): Boolean {
        return rssi >= minimumRssi && distance <= maxDetectionDistance
    }
    
    /**
     * Determines if beacon detection is recent enough to be valid
     */
    fun isDetectionValid(detectionTime: Long): Boolean {
        val age = System.currentTimeMillis() - detectionTime
        return age <= beaconExpirationTime
    }
}
