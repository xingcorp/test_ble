package com.nordicbeacon.scanner.domain.usecases

import com.nordicbeacon.scanner.domain.entities.NordicBeacon
import com.nordicbeacon.scanner.domain.models.BeaconScanResult
import com.nordicbeacon.scanner.domain.models.BeaconDetectionStats
import com.nordicbeacon.scanner.domain.models.ScanConfig
import com.nordicbeacon.scanner.domain.models.ScanningState
import com.nordicbeacon.scanner.domain.repositories.IBeaconRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 🎯 Use Case - Nordic Beacon Scanning Business Logic
 * 
 * Encapsulates Nordic beacon scanning business rules and orchestration
 * Following Single Responsibility Principle - only handles beacon scanning logic
 * 
 * Key Business Rules:
 * - Only scan for Nordic UUID: FDA50693-0000-0000-0000-290995101092
 * - Filter out weak signals (RSSI < -95 dBm)
 * - Validate beacon authenticity and signal quality
 * - Handle scanning state management
 * 
 * @author Senior Android Developer
 */
@Singleton
class ScanBeaconUseCase @Inject constructor(
    private val beaconRepository: IBeaconRepository
) {

    /**
     * 🔍 Start Nordic beacon scanning với business validation
     * 
     * Business Rules Applied:
     * - Nordic UUID filtering mandatory
     * - Signal quality validation
     * - Duplicate detection với time window
     * - Performance monitoring
     */
    suspend fun startScanning(
        config: ScanConfig = ScanConfig()
    ): Flow<BeaconScanResult> {
        return beaconRepository.startScanning(config)
            .filter { result -> isValidScanResult(result) }
            .distinctUntilChanged { old, new -> isDuplicateDetection(old, new) }
            .map { result -> enhanceWithBusinessLogic(result) }
    }

    /**
     * ⏹️ Stop beacon scanning với cleanup
     */
    suspend fun stopScanning(): Result<Unit> {
        return beaconRepository.stopScanning()
    }

    /**
     * 📊 Get enhanced detection statistics với business metrics
     */
    suspend fun getDetectionStatistics(): Flow<BeaconDetectionStats> {
        return beaconRepository.getDetectionStats()
    }

    /**
     * 📱 Get current scanning state
     */
    fun getScanningState(): Flow<ScanningState> {
        return beaconRepository.getScanningState()
    }

    /**
     * 📋 Get filtered beacon history (chỉ Nordic beacons)
     */
    suspend fun getNordicBeaconHistory(
        limit: Int = 50,
        sinceHours: Int = 24
    ): Result<List<NordicBeacon>> {
        val sinceTimestamp = System.currentTimeMillis() - (sinceHours * 3600000L)
        
        return beaconRepository.getBeaconHistory(limit, sinceTimestamp)
            .map { beacons ->
                beacons.filter { it.isValidNordicBeacon() }
                    .sortedByDescending { it.detectionTime.millis }
            }
    }

    /**
     * 🧹 Clean up old beacon data (privacy compliance)
     */
    suspend fun cleanupOldData(retentionDays: Int = 30): Result<Unit> {
        return beaconRepository.clearBeaconHistory(retentionDays)
    }

    // ========== PRIVATE BUSINESS LOGIC ==========

    /**
     * Validates scan result against Nordic beacon business rules
     */
    private fun isValidScanResult(result: BeaconScanResult): Boolean {
        return when (result) {
            is BeaconScanResult.Success -> {
                result.beacon.isValidNordicBeacon() && 
                result.beacon.signalStrength.isValidSignal()
            }
            is BeaconScanResult.NoBeaconsFound -> true // Always valid state
            is BeaconScanResult.Error -> true // Error states need to be handled
        }
    }

    /**
     * Detects duplicate beacon detections within time window
     */
    private fun isDuplicateDetection(
        old: BeaconScanResult, 
        new: BeaconScanResult
    ): Boolean {
        if (old !is BeaconScanResult.Success || new !is BeaconScanResult.Success) {
            return false
        }

        val timeDiff = new.beacon.detectionTime.millis - old.beacon.detectionTime.millis
        val isSameBeacon = old.beacon.uuid == new.beacon.uuid && 
                          old.beacon.major == new.beacon.major &&
                          old.beacon.minor == new.beacon.minor

        // Consider duplicate if same beacon detected within 2 seconds
        return isSameBeacon && timeDiff < 2000L
    }

    /**
     * Enhances scan result với additional business context
     */
    private fun enhanceWithBusinessLogic(result: BeaconScanResult): BeaconScanResult {
        return when (result) {
            is BeaconScanResult.Success -> {
                val enhancedBeacon = result.beacon.copy(
                    metadata = result.beacon.metadata.copy(
                        detectionCount = result.beacon.metadata.detectionCount + 1,
                        lastSeenTimestamp = System.currentTimeMillis(),
                        signalStabilityScore = calculateSignalStability(result.beacon)
                    )
                )
                
                result.copy(
                    beacon = enhancedBeacon,
                    contextInfo = generateContextInfo(enhancedBeacon)
                )
            }
            else -> result
        }
    }

    /**
     * Calculates signal stability score based on historical data
     */
    private fun calculateSignalStability(beacon: NordicBeacon): Double {
        // Business logic cho signal stability assessment
        val reliabilityScore = beacon.calculateReliabilityScore()
        val proximityFactor = when {
            beacon.isImmediate() -> 1.0
            beacon.isNear() -> 0.8
            beacon.isFar() -> 0.6
            else -> 0.3
        }
        
        return (reliabilityScore / 100.0) * proximityFactor
    }

    /**
     * Generates contextual information for beacon detection
     */
    private fun generateContextInfo(beacon: NordicBeacon): String {
        val proximityDesc = when {
            beacon.isImmediate() -> "Immediate (< 1m)"
            beacon.isNear() -> "Near (1-5m)"  
            beacon.isFar() -> "Far (5-20m)"
            else -> "Very Far (> 20m)"
        }
        
        val signalQuality = when {
            beacon.signalStrength.isStrongSignal() -> "Strong"
            beacon.signalStrength.isWeakSignal() -> "Weak"
            else -> "Moderate"
        }
        
        return "📍 $proximityDesc | 📶 $signalQuality | 🎯 Reliability: ${beacon.calculateReliabilityScore()}%"
    }
}

/**
 * 🔧 Use Case - System Compatibility & Requirements
 * 
 * Handles system-level validation and compatibility checking
 */
@Singleton  
class SystemCompatibilityUseCase @Inject constructor(
    private val beaconRepository: IBeaconRepository
) {
    
    /**
     * ✅ Validates system requirements cho Nordic beacon scanning
     */
    suspend fun validateSystemRequirements(): SystemValidationResult {
        // Implementation sẽ check:
        // - Bluetooth LE support
        // - Required permissions
        // - Android version compatibility
        // - Hardware capabilities
        
        return SystemValidationResult(
            hasBluetoothLE = true, // TODO: Implement actual check
            hasRequiredPermissions = false, // TODO: Check permissions
            isAndroidVersionSupported = true, // API 21+ always supported
            canRunBackgroundServices = true, // TODO: Check battery optimization
            recommendations = listOf()
        )
    }
}

/**
 * 📋 System Validation Result
 */
data class SystemValidationResult(
    val hasBluetoothLE: Boolean,
    val hasRequiredPermissions: Boolean,
    val isAndroidVersionSupported: Boolean,
    val canRunBackgroundServices: Boolean,
    val recommendations: List<String>
) {
    
    fun isFullyCompatible(): Boolean {
        return hasBluetoothLE && hasRequiredPermissions && 
               isAndroidVersionSupported && canRunBackgroundServices
    }
    
    fun getMissingRequirements(): List<String> {
        val missing = mutableListOf<String>()
        
        if (!hasBluetoothLE) missing.add("Bluetooth LE not supported")
        if (!hasRequiredPermissions) missing.add("Missing required permissions")
        if (!isAndroidVersionSupported) missing.add("Android version not supported")
        if (!canRunBackgroundServices) missing.add("Background services restricted")
        
        return missing
    }
}
