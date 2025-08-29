package com.nordicbeacon.scanner.data.repositories

import com.nordicbeacon.scanner.di.IoDispatcher
import com.nordicbeacon.scanner.domain.entities.NordicBeacon
import com.nordicbeacon.scanner.domain.models.BeaconScanResult
import com.nordicbeacon.scanner.domain.models.BeaconDetectionStats
import com.nordicbeacon.scanner.domain.models.ScanConfig
import com.nordicbeacon.scanner.domain.models.ScanningState
import com.nordicbeacon.scanner.domain.repositories.IBeaconRepository
import com.nordicbeacon.scanner.data.datasources.BleDataSource
import com.nordicbeacon.scanner.data.datasources.LocalBeaconDataSource
import com.nordicbeacon.scanner.data.mappers.BeaconMapper
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 📊 Repository Implementation - Beacon Data Access Layer
 * 
 * Concrete implementation of IBeaconRepository following Clean Architecture
 * Coordinates between BLE hardware access and local storage
 * 
 * Key Responsibilities:
 * - Orchestrates BLE scanning operations
 * - Manages scanning state và lifecycle  
 * - Handles data persistence và retrieval
 * - Provides error handling và recovery
 * 
 * @author Senior Android Developer
 */
@Singleton
class BeaconRepositoryImpl @Inject constructor(
    private val bleDataSource: BleDataSource,
    private val localDataSource: LocalBeaconDataSource, 
    private val beaconMapper: BeaconMapper,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : IBeaconRepository {

    // ========== STATE MANAGEMENT ==========
    
    private val _scanningState = MutableStateFlow(ScanningState.IDLE)
    private val _detectionStats = MutableStateFlow(BeaconDetectionStats())
    
    private var currentScanConfig: ScanConfig = ScanConfig()
    private var scanStartTime: Long = 0L

    override fun getScanningState(): Flow<ScanningState> = _scanningState.asStateFlow()

    override suspend fun getDetectionStats(): Flow<BeaconDetectionStats> = _detectionStats.asStateFlow()

    // ========== CORE SCANNING OPERATIONS ==========

    override suspend fun startScanning(config: ScanConfig): Flow<BeaconScanResult> {
        
        Timber.i("🔍 Starting Nordic beacon scanning...")
        Timber.d("⚙️ Scan Config: $config")
        
        return try {
            // Update state và configuration
            _scanningState.value = ScanningState.STARTING
            currentScanConfig = config
            scanStartTime = System.currentTimeMillis()
            
            // Reset detection stats
            _detectionStats.value = BeaconDetectionStats(scanStartTime = scanStartTime)
            
            // Start BLE scanning via data source
            bleDataSource.startScanning(config)
                .filter { beacon -> isNordicBeacon(beacon) }
                .map { beacon -> beaconMapper.toDomainModel(beacon) }
                .filter { beacon -> meetsQualityCriteria(beacon) }
                .onEach { beacon -> 
                    // Update state cho successful scan
                    _scanningState.value = ScanningState.SCANNING
                    updateDetectionStats(beacon)
                    
                    // Persist detection locally
                    saveBeaconSighting(beacon)
                }
                .map { beacon -> BeaconScanResult.Success(beacon, calculateScanDuration()) }
                .catch { throwable ->
                    Timber.e(throwable, "❌ Beacon scanning failed")
                    _scanningState.value = ScanningState.ERROR
                    emit(mapToScanError(throwable))
                }
                .flowOn(ioDispatcher)
                
        } catch (exception: Exception) {
            Timber.e(exception, "❌ Failed to start beacon scanning")
            _scanningState.value = ScanningState.ERROR
            throw exception
        }
    }

    override suspend fun stopScanning(): Result<Unit> {
        return withContext(ioDispatcher) {
            try {
                Timber.i("⏹️ Stopping Nordic beacon scanning...")
                
                bleDataSource.stopScanning()
                _scanningState.value = ScanningState.STOPPED
                
                val finalStats = _detectionStats.value.copy(
                    scanDuration = calculateScanDuration()
                )
                _detectionStats.value = finalStats
                
                Timber.i("✅ Scanning stopped successfully")
                Timber.i("📊 Final Stats: ${finalStats.totalDetections} detections in ${finalStats.scanDuration/1000}s")
                
                Result.success(Unit)
                
            } catch (exception: Exception) {
                Timber.e(exception, "❌ Failed to stop scanning")
                Result.failure(exception)
            }
        }
    }

    // ========== DATA PERSISTENCE OPERATIONS ==========

    override suspend fun saveBeaconSighting(beacon: NordicBeacon): Result<Unit> {
        return withContext(ioDispatcher) {
            try {
                localDataSource.saveBeaconSighting(beacon)
                Timber.d("💾 Beacon sighting saved: ${beacon.uuid.value}")
                Result.success(Unit)
                
            } catch (exception: Exception) {
                Timber.e(exception, "❌ Failed to save beacon sighting")
                Result.failure(exception)
            }
        }
    }

    override suspend fun getBeaconHistory(limit: Int, sinceTimestamp: Long): Result<List<NordicBeacon>> {
        return withContext(ioDispatcher) {
            try {
                val history = localDataSource.getBeaconHistory(limit, sinceTimestamp)
                Timber.d("📋 Retrieved ${history.size} beacon records from history")
                Result.success(history)
                
            } catch (exception: Exception) {
                Timber.e(exception, "❌ Failed to retrieve beacon history")  
                Result.failure(exception)
            }
        }
    }

    override suspend fun clearBeaconHistory(olderThanDays: Int): Result<Unit> {
        return withContext(ioDispatcher) {
            try {
                val cutoffTime = if (olderThanDays > 0) {
                    System.currentTimeMillis() - (olderThanDays * 24 * 3600000L)
                } else {
                    0L
                }
                
                localDataSource.clearBeaconHistory(cutoffTime)
                Timber.i("🗑️ Beacon history cleaned (older than $olderThanDays days)")
                Result.success(Unit)
                
            } catch (exception: Exception) {
                Timber.e(exception, "❌ Failed to clear beacon history")
                Result.failure(exception)  
            }
        }
    }

    // ========== CONFIGURATION MANAGEMENT ==========

    override suspend fun updateScanConfig(config: ScanConfig): Result<Unit> {
        return withContext(ioDispatcher) {
            try {
                currentScanConfig = config
                
                // If currently scanning, restart với new config
                if (_scanningState.value == ScanningState.SCANNING) {
                    bleDataSource.updateScanConfig(config)
                }
                
                Timber.i("⚙️ Scan configuration updated: $config")
                Result.success(Unit)
                
            } catch (exception: Exception) {
                Timber.e(exception, "❌ Failed to update scan configuration")
                Result.failure(exception)
            }
        }
    }

    // ========== PRIVATE HELPER METHODS ==========

    /**
     * Validates if raw beacon is Nordic beacon
     */
    private fun isNordicBeacon(rawBeacon: org.altbeacon.beacon.Beacon): Boolean {
        val beaconUuid = rawBeacon.id1?.toString()?.uppercase()
        return beaconUuid == NordicBeacon.NORDIC_UUID.uppercase()
    }

    /**
     * Validates beacon meets quality criteria 
     */
    private fun meetsQualityCriteria(beacon: NordicBeacon): Boolean {
        return beacon.isValidNordicBeacon() && 
               beacon.signalStrength.rssi >= currentScanConfig.minimumRssi &&
               beacon.proximity.meters <= currentScanConfig.maxDetectionDistance
    }

    /**
     * Updates detection statistics với new beacon
     */
    private fun updateDetectionStats(beacon: NordicBeacon) {
        val current = _detectionStats.value
        val updated = current.copy(
            totalDetections = current.totalDetections + 1,
            lastDetectionTime = beacon.detectionTime.millis,
            averageRssi = calculateRunningAverage(current.averageRssi, beacon.signalStrength.rssi.toDouble(), current.totalDetections),
            averageDistance = calculateRunningAverage(current.averageDistance, beacon.proximity.meters, current.totalDetections)
        )
        
        _detectionStats.value = updated
    }

    /**
     * Calculates running average cho statistics  
     */
    private fun calculateRunningAverage(currentAvg: Double, newValue: Double, count: Int): Double {
        return if (count <= 1) {
            newValue
        } else {
            ((currentAvg * (count - 1)) + newValue) / count
        }
    }

    /**
     * Calculates current scan duration
     */
    private fun calculateScanDuration(): Long {
        return if (scanStartTime > 0) {
            System.currentTimeMillis() - scanStartTime
        } else 0L
    }

    /**
     * Maps exceptions to appropriate BeaconScanResult.Error types
     */
    private fun mapToScanError(throwable: Throwable): BeaconScanResult.Error {
        return when (throwable) {
            is SecurityException -> BeaconScanResult.Error.PermissionDenied(
                permission = "Unknown permission",
                cause = throwable
            )
            is IllegalStateException -> BeaconScanResult.Error.ServiceNotAvailable(
                cause = throwable
            )
            else -> BeaconScanResult.Error.SystemError(
                message = throwable.message ?: "Unknown scanning error",
                cause = throwable
            )
        }
    }
}
