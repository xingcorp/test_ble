package com.nordicbeacon.scanner.domain.repositories

import com.nordicbeacon.scanner.domain.entities.NordicBeacon
import com.nordicbeacon.scanner.domain.models.BeaconScanResult
import com.nordicbeacon.scanner.domain.models.BeaconDetectionStats
import com.nordicbeacon.scanner.domain.models.ScanConfig
import kotlinx.coroutines.flow.Flow

/**
 * 📊 Domain Repository Interface - Beacon Data Access Contract
 * 
 * Defines the contract for beacon data operations following Repository pattern
 * Abstraction layer between domain logic and data sources
 * 
 * Following SOLID principles:
 * - Interface Segregation: Focused interface for beacon operations only
 * - Dependency Inversion: Domain depends on abstraction, not concrete implementation
 * 
 * @author Senior Android Developer  
 */
interface IBeaconRepository {

    /**
     * 🔍 Start scanning for Nordic beacons
     * 
     * @param config Scanning configuration parameters
     * @return Flow emitting scan results (Success/Error/NoBeaconsFound)
     */
    suspend fun startScanning(config: ScanConfig = ScanConfig()): Flow<BeaconScanResult>

    /**
     * ⏹️ Stop beacon scanning
     * 
     * @return Result indicating success/failure of stop operation
     */
    suspend fun stopScanning(): Result<Unit>

    /**
     * 📈 Get current scanning statistics
     * 
     * @return Current detection statistics and performance metrics
     */
    suspend fun getDetectionStats(): Flow<BeaconDetectionStats>

    /**
     * 💾 Save beacon detection to local storage
     * 
     * @param beacon The Nordic beacon to save
     * @return Result indicating success/failure of save operation
     */
    suspend fun saveBeaconSighting(beacon: NordicBeacon): Result<Unit>

    /**
     * 📋 Get beacon detection history
     * 
     * @param limit Maximum number of records to return
     * @param sinceTimestamp Optional timestamp filter
     * @return List of historical beacon detections
     */
    suspend fun getBeaconHistory(
        limit: Int = 100,
        sinceTimestamp: Long = 0L
    ): Result<List<NordicBeacon>>

    /**
     * 🗑️ Clear beacon detection history
     * 
     * @param olderThanDays Clear records older than specified days (0 = all)
     * @return Result indicating success/failure of cleanup
     */
    suspend fun clearBeaconHistory(olderThanDays: Int = 0): Result<Unit>

    /**
     * ⚙️ Update scanning configuration
     * 
     * @param config New scanning configuration
     * @return Result indicating success/failure of configuration update
     */
    suspend fun updateScanConfig(config: ScanConfig): Result<Unit>

    /**
     * 🔄 Get current scanning state
     * 
     * @return Flow emitting current scanning state changes
     */
    fun getScanningState(): Flow<com.nordicbeacon.scanner.domain.models.ScanningState>
}
