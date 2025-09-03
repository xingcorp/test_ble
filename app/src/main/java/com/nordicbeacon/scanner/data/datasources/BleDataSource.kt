package com.nordicbeacon.scanner.data.datasources

import android.content.Context
import android.os.RemoteException
import com.nordicbeacon.scanner.BuildConfig
import com.nordicbeacon.scanner.di.IoDispatcher
import com.nordicbeacon.scanner.domain.models.ScanConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import org.altbeacon.beacon.Beacon
import org.altbeacon.beacon.BeaconConsumer
import org.altbeacon.beacon.BeaconManager
import org.altbeacon.beacon.Identifier
import org.altbeacon.beacon.RangeNotifier
import org.altbeacon.beacon.Region
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 📡 BLE Data Source - AltBeacon Library Integration
 * 
 * Bridges AltBeacon library callbacks với modern Flow-based architecture
 * Handles Nordic beacon specific scanning với UUID: FDA50693-0000-0000-0000-290995101092
 * 
 * Key Responsibilities:
 * - BeaconConsumer lifecycle management
 * - Callback → Flow conversion
 * - Nordic beacon filtering at hardware level
 * - BLE stack error handling và recovery
 * 
 * @author Senior Android Developer
 */
@Singleton
class BleDataSource @Inject constructor(
    @ApplicationContext private val context: Context,
    private val beaconManager: BeaconManager,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {

    // ========== STATE MANAGEMENT ==========
    
    private var isScanning = false
    private var currentScanConfig: ScanConfig? = null
    private val nordicRegion = createNordicRegion()
    private var currentBeaconConsumer: BeaconConsumer? = null

    // ========== CORE SCANNING OPERATIONS ==========

    /**
     * 🔍 Start BLE scanning cho Nordic beacons
     * 
     * Converts AltBeacon callback-based API → modern Flow-based stream
     * Implements proper resource management và error handling
     */
    suspend fun startScanning(config: ScanConfig): Flow<Beacon> = callbackFlow {
        
        Timber.i("📡 Starting BLE scanning for Nordic beacons...")
        Timber.d("🎯 Target UUID: ${BuildConfig.NORDIC_BEACON_UUID}")
        
        try {
            // Store configuration
            currentScanConfig = config
            
            // Apply scan configuration to BeaconManager
            applyScanConfiguration(config)
            
            // Create range notifier cho Nordic beacon detection
            val rangeNotifier = RangeNotifier { beacons, region ->
                try {
                    Timber.d("📶 Beacon range callback received: ${beacons.size} beacon(s)")
                    
                    beacons?.forEach { beacon ->
                        if (isNordicBeacon(beacon) && meetsQualityCriteria(beacon, config)) {
                            
                            Timber.i("🎯 Nordic beacon detected: ${beacon.id1} | RSSI: ${beacon.rssi}dBm | Distance: ${"%.2f".format(beacon.distance)}m")
                            
                            // Emit beacon to Flow stream
                            val sendResult = trySend(beacon)
                            if (sendResult.isFailure) {
                                Timber.w("⚠️ Failed to emit beacon to flow: ${sendResult.exceptionOrNull()}")
                            }
                        } else {
                            Timber.d("🚫 Beacon filtered out: UUID=${beacon.id1} | RSSI=${beacon.rssi}")
                        }
                    }
                    
                } catch (e: Exception) {
                    Timber.e(e, "❌ Error in range notifier callback")
                    close(e)
                }
            }
            
            // Create beacon consumer pentru service binding
            val beaconConsumer = object : BeaconConsumer {
                
                override fun onBeaconServiceConnect() {
                    try {
                        Timber.i("🔗 BeaconManager service connected")
                        
                        beaconManager.addRangeNotifier(rangeNotifier)
                        beaconManager.startRangingBeaconsInRegion(nordicRegion)
                        
                        isScanning = true
                        Timber.i("✅ Nordic beacon ranging started successfully")
                        
                    } catch (e: RemoteException) {
                        Timber.e(e, "❌ Failed to start beacon ranging")
                        close(e)
                    } catch (e: Exception) {
                        Timber.e(e, "❌ Unexpected error in beacon service connect")
                        close(e)
                    }
                }
                
                override fun getApplicationContext(): Context = context
                
                override fun unbindService(connection: android.content.ServiceConnection) {
                    try {
                        context.unbindService(connection)
                        Timber.d("🔓 BeaconManager service unbound")
                    } catch (e: Exception) {
                        Timber.w(e, "⚠️ Error unbinding beacon service")
                    }
                }
                
                override fun bindService(intent: android.content.Intent, connection: android.content.ServiceConnection, mode: Int): Boolean {
                    return try {
                        val bound = context.bindService(intent, connection, mode)
                        Timber.d("🔗 BeaconManager service binding: $bound")
                        bound
                    } catch (e: Exception) {
                        Timber.e(e, "❌ Failed to bind beacon service")
                        false
                    }
                }
            }
            
            // Store consumer reference cho state checking
            currentBeaconConsumer = beaconConsumer
            
            // Bind BeaconManager service
            beaconManager.bind(beaconConsumer)
            
            // Handle Flow closure và cleanup
            awaitClose {
                try {
                    Timber.i("🔄 Cleaning up BLE scanning resources...")
                    
                    if (isScanning) {
                        beaconManager.stopRangingBeaconsInRegion(nordicRegion)
                        beaconManager.removeRangeNotifier(rangeNotifier)
                        isScanning = false
                    }
                    
                    beaconManager.unbind(beaconConsumer)
                    
                    Timber.i("✅ BLE scanning cleanup completed")
                    
                } catch (e: Exception) {
                    Timber.e(e, "❌ Error during BLE scanning cleanup")
                }
            }
            
        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to initialize BLE scanning")
            close(e)
        }
        
    }.flowOn(ioDispatcher)

    /**
     * ⏹️ Stop BLE scanning operations
     */
    suspend fun stopScanning() = withContext(ioDispatcher) {
        try {
            if (isScanning) {
                beaconManager.stopRangingBeaconsInRegion(nordicRegion)
                isScanning = false
                Timber.i("⏹️ BLE scanning stopped")
            }
        } catch (e: Exception) {
            Timber.e(e, "❌ Error stopping BLE scanning")
            throw e
        }
    }

    /**
     * ⚙️ Update scan configuration dynamically
     */
    suspend fun updateScanConfig(config: ScanConfig) = withContext(ioDispatcher) {
        try {
            currentScanConfig = config
            applyScanConfiguration(config)
            Timber.i("⚙️ BLE scan configuration updated")
            
        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to update scan configuration")
            throw e
        }
    }

    // ========== PRIVATE IMPLEMENTATION METHODS ==========

    /**
     * Creates Nordic-specific region cho targeted scanning
     */
    private fun createNordicRegion(): Region {
        val nordicUuid = Identifier.parse(BuildConfig.NORDIC_BEACON_UUID)
        
        return Region(
            "nordic-beacon-region",
            nordicUuid,
            null, // Any major
            null  // Any minor
        ).also { region ->
            Timber.d("🎯 Created Nordic beacon region: ${region.uniqueId}")
            Timber.d("📍 UUID filter: ${region.id1}")
        }
    }

    /**
     * Applies scan configuration to BeaconManager
     */
    private fun applyScanConfiguration(config: ScanConfig) {
        beaconManager.apply {
            foregroundScanPeriod = config.foregroundScanPeriod
            foregroundBetweenScanPeriod = config.foregroundBetweenScanPeriod
            backgroundScanPeriod = config.backgroundScanPeriod  
            backgroundBetweenScanPeriod = config.backgroundBetweenScanPeriod
            
            Timber.d("⚙️ Applied scan config - FG: ${config.foregroundScanPeriod}ms/${config.foregroundBetweenScanPeriod}ms | BG: ${config.backgroundScanPeriod}ms/${config.backgroundBetweenScanPeriod}ms")
        }
    }

    /**
     * Validates if beacon is Nordic beacon
     */
    private fun isNordicBeacon(beacon: Beacon): Boolean {
        val beaconUuid = beacon.id1?.toString()?.uppercase()
        val nordicUuid = BuildConfig.NORDIC_BEACON_UUID.uppercase()
        
        val isNordic = beaconUuid == nordicUuid
        
        if (BuildConfig.DEBUG && !isNordic) {
            Timber.d("🚫 Non-Nordic beacon filtered: $beaconUuid")
        }
        
        return isNordic
    }

    /**
     * Validates beacon meets quality criteria
     */
    private fun meetsQualityCriteria(beacon: Beacon, config: ScanConfig): Boolean {
        val meetsRssiThreshold = beacon.rssi >= config.minimumRssi
        val meetsDistanceThreshold = beacon.distance <= config.maxDetectionDistance
        val hasValidSignal = beacon.rssi > -100 // Reasonable signal floor
        
        val meetsCriteria = meetsRssiThreshold && meetsDistanceThreshold && hasValidSignal
        
        if (BuildConfig.DEBUG && !meetsCriteria) {
            Timber.d("📊 Beacon quality check failed: RSSI=${beacon.rssi} (min: ${config.minimumRssi}) | Distance=${"%.2f".format(beacon.distance)} (max: ${config.maxDetectionDistance})")
        }
        
        return meetsCriteria
    }

    /**
     * 🔍 Get current BLE scanning status
     */
    fun isScanningActive(): Boolean = isScanning

    /**
     * 📊 Get BeaconManager internal state cho debugging
     */
    fun getBeaconManagerState(): BeaconManagerState {
        return BeaconManagerState(
            isBound = currentBeaconConsumer?.let { beaconManager.isBound(it) } ?: false, // Use stored consumer reference
            isScanning = isScanning,
            foregroundScanPeriod = beaconManager.foregroundScanPeriod,
            backgroundScanPeriod = beaconManager.backgroundScanPeriod,
            rangedRegions = beaconManager.rangedRegions?.size ?: 0
        )
    }
}

/**
 * 📊 BeaconManager State Information
 */
data class BeaconManagerState(
    val isBound: Boolean,
    val isScanning: Boolean, 
    val foregroundScanPeriod: Long,
    val backgroundScanPeriod: Long,
    val rangedRegions: Int
) {
    
    fun isHealthy(): Boolean {
        return isBound && (isScanning || rangedRegions > 0)
    }
    
    override fun toString(): String {
        return "BeaconManagerState(bound=$isBound, scanning=$isScanning, regions=$rangedRegions, FG=${foregroundScanPeriod}ms, BG=${backgroundScanPeriod}ms)"
    }
}
