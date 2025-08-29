package com.nordicbeacon.scanner.infrastructure.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nordicbeacon.scanner.domain.usecases.ScanBeaconUseCase
import com.nordicbeacon.scanner.domain.usecases.SystemCompatibilityUseCase
import com.nordicbeacon.scanner.domain.models.BeaconScanResult
import com.nordicbeacon.scanner.domain.models.ScanConfig
import com.nordicbeacon.scanner.infrastructure.oem.coordination.BatteryOptimizationCoordinator
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber

/**
 * 🔄 Beacon Scan Worker - WorkManager Backup Strategy
 * 
 * Fallback scanning mechanism khi Foreground Service bị killed bởi system
 * Provides periodic Nordic beacon detection với minimal battery impact
 * 
 * Key Features:
 * - Periodic background scanning (minimum 15-minute intervals)
 * - Battery-conscious scan periods
 * - Quick scan bursts với timeout protection
 * - OEM compatibility checking
 * - Automatic service restart attempts
 * 
 * Usage Scenarios:
 * - Foreground service killed by OEM optimization
 * - Extreme battery saver mode active
 * - System resource pressure
 * - Fallback cho unsupported OEM configurations
 * 
 * @author Senior Android Developer
 */
@HiltWorker
class BeaconScanWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val scanBeaconUseCase: ScanBeaconUseCase,
    private val systemCompatibilityUseCase: SystemCompatibilityUseCase,
    private val batteryOptimizationCoordinator: BatteryOptimizationCoordinator
) : CoroutineWorker(context, params) {

    // ========== WORK EXECUTION ==========

    override suspend fun doWork(): Result {
        
        Timber.i("🔄 BeaconScanWorker started (attempt ${runAttemptCount + 1})")
        
        return try {
            // Step 1: Validate system readiness
            if (!isSystemReadyForBackgroundScan()) {
                Timber.w("⚠️ System not ready cho background scan")
                return Result.retry()
            }
            
            // Step 2: Perform quick beacon scan
            val scanResult = performQuickScan()
            
            // Step 3: Handle scan results
            processScanResult(scanResult)
            
            // Step 4: Attempt service restart if needed
            attemptServiceRestart()
            
            Timber.i("✅ BeaconScanWorker completed successfully")
            Result.success()
            
        } catch (e: Exception) {
            Timber.e(e, "❌ BeaconScanWorker failed")
            
            // Retry logic với exponential backoff
            if (runAttemptCount < MAX_RETRY_ATTEMPTS) {
                Timber.i("🔄 Will retry (${runAttemptCount + 1}/$MAX_RETRY_ATTEMPTS)")
                Result.retry()
            } else {
                Timber.e("❌ Max retries exceeded, giving up")
                Result.failure()
            }
        }
    }

    // ========== PRIVATE IMPLEMENTATION ==========

    /**
     * 🔍 Check if system is ready cho background scanning
     */
    private suspend fun isSystemReadyForBackgroundScan(): Boolean {
        
        return try {
            // Check system compatibility
            val systemValidation = systemCompatibilityUseCase.validateSystemRequirements()
            if (!systemValidation.isFullyCompatible()) {
                Timber.w("⚠️ System validation failed: ${systemValidation.getMissingRequirements()}")
                return false
            }
            
            // Check battery optimization status
            val optimizationStatus = batteryOptimizationCoordinator.getCurrentOptimizationStatus()
            if (optimizationStatus == com.nordicbeacon.scanner.infrastructure.oem.models.OptimizationStatus.NOT_OPTIMIZED) {
                Timber.w("⚠️ Battery optimization not configured - background scan may fail")
                // Continue anyway - WorkManager might still work
            }
            
            // Check battery level (avoid scanning on very low battery)
            val batteryManager = applicationContext.getSystemService(Context.BATTERY_SERVICE) as android.os.BatteryManager
            val batteryLevel = batteryManager.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY)
            
            if (batteryLevel < LOW_BATTERY_THRESHOLD) {
                Timber.w("🔋 Battery too low ($batteryLevel%) - skipping background scan")
                return false
            }
            
            true
            
        } catch (e: Exception) {
            Timber.e(e, "❌ System readiness check failed")
            false
        }
    }

    /**
     * ⚡ Perform quick beacon scan với timeout protection
     */
    private suspend fun performQuickScan(): BeaconScanResult? {
        
        Timber.i("⚡ Starting quick Nordic beacon scan...")
        
        return try {
            // Use conservative scan config cho WorkManager
            val conservativeScanConfig = ScanConfig(
                foregroundScanPeriod = 2000L,    // 2 second scan
                foregroundBetweenScanPeriod = 1000L, // 1 second between
                backgroundScanPeriod = 5000L,     // 5 second background scan
                backgroundBetweenScanPeriod = 10000L, // 10 second between
                enableSignalFiltering = true,
                minimumRssi = -90 // More permissive cho quick scan
            )
            
            // Perform scan với timeout protection
            withTimeoutOrNull(SCAN_TIMEOUT_MS) {
                scanBeaconUseCase.startScanning(conservativeScanConfig)
                    .first() // Take first result only
            }
            
        } catch (e: Exception) {
            Timber.e(e, "❌ Quick scan failed")
            null
        }
    }

    /**
     * 📊 Process scan result và update statistics
     */
    private fun processScanResult(result: BeaconScanResult?) {
        
        when (result) {
            is BeaconScanResult.Success -> {
                val beacon = result.beacon
                
                Timber.i("🎯 WorkManager detected Nordic beacon:")
                Timber.i("   📍 RSSI: ${beacon.signalStrength.rssi}dBm")  
                Timber.i("   📏 Distance: ${"%.2f".format(beacon.proximity.meters)}m")
                Timber.i("   🎖️ Reliability: ${beacon.calculateReliabilityScore()}%")
                
                // TODO: Update statistics và send to analytics
                updateWorkerStatistics(true)
            }
            
            is BeaconScanResult.NoBeaconsFound -> {
                Timber.d("🔍 No Nordic beacons found in WorkManager scan")
                updateWorkerStatistics(false)
            }
            
            is BeaconScanResult.Error -> {
                Timber.w("❌ WorkManager scan error: ${result.message}")
                updateWorkerStatistics(false)
            }
            
            null -> {
                Timber.w("⏱️ WorkManager scan timed out")
                updateWorkerStatistics(false)
            }
        }
    }

    /**
     * 🚀 Attempt to restart main scanning service
     */
    private suspend fun attemptServiceRestart() {
        
        try {
            Timber.i("🚀 Attempting to restart BeaconScanningService từ WorkManager...")
            
            val serviceIntent = com.nordicbeacon.scanner.infrastructure.services.BeaconScanningService.createStartIntent(applicationContext)
            
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                applicationContext.startForegroundService(serviceIntent)
            } else {
                applicationContext.startService(serviceIntent)
            }
            
            Timber.i("✅ Service restart initiated từ WorkManager")
            
        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to restart service từ WorkManager")
            // Not critical - WorkManager can continue as backup
        }
    }

    /**
     * 📊 Update worker execution statistics
     */
    private fun updateWorkerStatistics(detectionSuccess: Boolean) {
        
        // TODO: Implement worker statistics tracking
        // - Execution frequency
        // - Detection success rate
        // - Battery impact metrics
        // - Service restart success rate
        
        Timber.d("📊 Worker stats updated: Detection=$detectionSuccess, Attempt=${runAttemptCount + 1}")
    }

    // ========== CONSTANTS ==========

    companion object {
        const val WORK_NAME = "nordic_beacon_scan_backup"
        const val MAX_RETRY_ATTEMPTS = 3
        const val SCAN_TIMEOUT_MS = 30_000L // 30 seconds max scan time
        const val LOW_BATTERY_THRESHOLD = 15 // Skip scan if battery < 15%

        /**
         * 🏭 Create periodic work request cho backup scanning
         */
        fun createPeriodicWorkRequest(): androidx.work.PeriodicWorkRequest {
            
            val constraints = androidx.work.Constraints.Builder()
                .setRequiredNetworkType(androidx.work.NetworkType.NOT_REQUIRED) // No network needed
                .setRequiresBatteryNotLow(true) // Skip if battery low
                .setRequiresDeviceIdle(false) // Can run when device active
                .build()
            
            return androidx.work.PeriodicWorkRequestBuilder<BeaconScanWorker>(
                repeatInterval = 15, // Minimum WorkManager interval
                repeatIntervalTimeUnit = java.util.concurrent.TimeUnit.MINUTES
            )
            .setConstraints(constraints)
            .setBackoffCriteria(
                androidx.work.BackoffPolicy.EXPONENTIAL,
                1, // Initial delay
                java.util.concurrent.TimeUnit.MINUTES
            )
            .addTag("nordic_beacon_backup")
            .addTag("battery_optimization_fallback")
            .build()
        }
    }
}
