package com.nordicbeacon.scanner.domain.models

import com.nordicbeacon.scanner.domain.entities.NordicBeacon

/**
 * 🎯 Domain Model - Beacon Scan Result
 * 
 * Sealed class representing all possible outcomes of beacon scanning operations
 * Following functional programming error handling patterns
 * 
 * @author Senior Android Developer
 */
sealed class BeaconScanResult {
    
    /**
     * ✅ Successful beacon detection
     */
    data class Success(
        val beacon: NordicBeacon,
        val scanDuration: Long = 0L,
        val contextInfo: String = ""
    ) : BeaconScanResult()
    
    /**
     * ⚠️ No beacons detected in scan period
     */
    data class NoBeaconsFound(
        val scanDuration: Long,
        val lastSuccessfulScan: Long = 0L
    ) : BeaconScanResult()
    
    /**
     * ❌ Scanning errors
     */
    sealed class Error(open val message: String, open val cause: Throwable? = null) : BeaconScanResult() {
        
        data class BluetoothDisabled(
            override val message: String = "Bluetooth is disabled",
            override val cause: Throwable? = null
        ) : Error(message, cause)
        
        data class PermissionDenied(
            val permission: String,
            override val message: String = "Permission denied: $permission",
            override val cause: Throwable? = null
        ) : Error(message, cause)
        
        data class LocationServicesDisabled(
            override val message: String = "Location services are disabled",
            override val cause: Throwable? = null
        ) : Error(message, cause)
        
        data class ScanningFailed(
            override val message: String,
            override val cause: Throwable? = null
        ) : Error(message, cause)
        
        data class ServiceNotAvailable(
            override val message: String = "Beacon scanning service not available",
            override val cause: Throwable? = null
        ) : Error(message, cause)
        
        data class SystemError(
            override val message: String,
            override val cause: Throwable
        ) : Error(message, cause)
    }
}

/**
 * 🔄 Scanning State Representation
 */
enum class ScanningState {
    IDLE,           // Not scanning
    STARTING,       // Initializing scan
    SCANNING,       // Active scanning
    PAUSED,         // Temporarily paused
    STOPPED,        // Stopped by user/system
    ERROR           // Error state
}

/**
 * 📊 Beacon Detection Statistics
 */
data class BeaconDetectionStats(
    val totalDetections: Int = 0,
    val uniqueBeacons: Int = 0,
    val averageRssi: Double = 0.0,
    val averageDistance: Double = 0.0,
    val scanDuration: Long = 0L,
    val scanStartTime: Long = System.currentTimeMillis(),
    val lastDetectionTime: Long = 0L
) {
    
    fun calculateDetectionRate(): Double {
        val scanDurationMinutes = scanDuration / 60000.0
        return if (scanDurationMinutes > 0) totalDetections / scanDurationMinutes else 0.0
    }
    
    fun isHealthyDetection(): Boolean {
        return totalDetections > 0 && averageRssi > -90.0
    }
}

/**
 * ⚙️ Scanning Configuration
 */
data class ScanConfig(
    val targetUuid: String = NordicBeacon.NORDIC_UUID,
    val foregroundScanPeriod: Long = 1100L,
    val foregroundBetweenScanPeriod: Long = 0L,
    val backgroundScanPeriod: Long = 10000L,
    val backgroundBetweenScanPeriod: Long = 60000L,
    val maxDetectionDistance: Double = 50.0,
    val minimumRssi: Int = -95,
    val enableSignalFiltering: Boolean = true,
    val enableDistanceCalculation: Boolean = true
) {
    
    fun isOptimizedForBattery(): Boolean {
        return backgroundBetweenScanPeriod >= 30000L
    }
    
    fun isOptimizedForAccuracy(): Boolean {
        return foregroundScanPeriod <= 1500L && foregroundBetweenScanPeriod <= 100L
    }
}
