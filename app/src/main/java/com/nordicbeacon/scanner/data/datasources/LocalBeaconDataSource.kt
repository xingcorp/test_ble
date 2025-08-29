package com.nordicbeacon.scanner.data.datasources

import com.nordicbeacon.scanner.data.database.BeaconDao
import com.nordicbeacon.scanner.data.database.BeaconEntity
import com.nordicbeacon.scanner.data.mappers.BeaconMapper
import com.nordicbeacon.scanner.di.IoDispatcher
import com.nordicbeacon.scanner.domain.entities.NordicBeacon
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 💾 Local Data Source - Room Database Operations
 * 
 * Wrapper cho BeaconDao providing domain-level operations  
 * Handles data persistence và retrieval cho Nordic beacon detections
 * 
 * Key Features:
 * - Domain model ↔ Database entity conversion
 * - Optimized database operations
 * - Error handling và logging
 * - Data validation before persistence
 * 
 * @author Senior Android Developer
 */
@Singleton
class LocalBeaconDataSource @Inject constructor(
    private val beaconDao: BeaconDao,
    private val beaconMapper: BeaconMapper,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {

    // ========== WRITE OPERATIONS ==========

    /**
     * 💾 Save Nordic beacon detection to local storage
     * 
     * Performs validation và conversion before database insertion
     */
    suspend fun saveBeaconSighting(beacon: NordicBeacon) = withContext(ioDispatcher) {
        try {
            // Validate Nordic beacon before saving
            require(beacon.isValidNordicBeacon()) { 
                "Invalid Nordic beacon - UUID: ${beacon.uuid.value}" 
            }
            
            // Convert domain model to database entity
            val entity = beaconMapper.toEntity(beacon)
            
            // Insert into database
            val insertedId = beaconDao.insertBeaconSighting(entity)
            
            Timber.d("💾 Nordic beacon saved - ID: $insertedId | UUID: ${beacon.uuid.value} | RSSI: ${beacon.signalStrength.rssi}dBm")
            
        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to save beacon sighting")
            throw e
        }
    }

    /**
     * 💾 Batch save multiple beacon detections
     */
    suspend fun saveBeaconSightings(beacons: List<NordicBeacon>) = withContext(ioDispatcher) {
        try {
            // Validate all beacons
            val validBeacons = beacons.filter { it.isValidNordicBeacon() }
            
            if (validBeacons.size < beacons.size) {
                Timber.w("⚠️ Filtered out ${beacons.size - validBeacons.size} invalid beacons")
            }
            
            // Convert và batch insert
            val entities = validBeacons.map { beaconMapper.toEntity(it) }
            val insertedIds = beaconDao.insertBeaconSightings(entities)
            
            Timber.i("💾 Batch saved ${insertedIds.size} Nordic beacon sightings")
            
        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to batch save beacon sightings")
            throw e
        }
    }

    // ========== READ OPERATIONS ==========

    /**
     * 📋 Get Nordic beacon detection history
     */
    suspend fun getBeaconHistory(
        limit: Int = 100,
        sinceTimestamp: Long = 0L
    ): List<NordicBeacon> = withContext(ioDispatcher) {
        try {
            val entities = if (sinceTimestamp > 0) {
                beaconDao.getRecentBeaconSightings(
                    sinceTimestamp = sinceTimestamp,
                    limit = limit
                )
            } else {
                beaconDao.getAllNordicBeacons().take(limit)
            }
            
            val domainBeacons = entities.map { beaconMapper.toDomainModel(it) }
            
            Timber.d("📋 Retrieved ${domainBeacons.size} Nordic beacons from local storage")
            
            domainBeacons
            
        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to retrieve beacon history")
            throw e
        }
    }

    /**
     * 🔄 Get live beacon detection stream
     */
    fun getLiveBeaconStream(limit: Int = 50): Flow<List<NordicBeacon>> {
        return beaconDao.getLiveBeaconSightings(limit = limit)
            .map { entities -> 
                entities.map { beaconMapper.toDomainModel(it) }
            }
    }

    /**
     * 📊 Get detection statistics từ local storage
     */
    suspend fun getLocalDetectionStats(
        sinceTimestamp: Long = System.currentTimeMillis() - 86400000L // Last 24 hours
    ): LocalDetectionStats = withContext(ioDispatcher) {
        try {
            val dbStats = beaconDao.getDetectionStatistics(sinceTimestamp = sinceTimestamp)
            
            return@withContext if (dbStats != null) {
                LocalDetectionStats(
                    totalDetections = dbStats.total_detections,
                    uniqueBeacons = dbStats.unique_beacons,
                    averageRssi = dbStats.average_rssi,
                    averageDistance = dbStats.average_distance,
                    firstDetection = dbStats.first_detection,
                    lastDetection = dbStats.last_detection,
                    detectionTimespan = dbStats.last_detection - dbStats.first_detection
                )
            } else {
                LocalDetectionStats() // Empty stats
            }
            
        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to get local detection statistics")
            LocalDetectionStats() // Return empty stats on error
        }
    }

    // ========== DELETE OPERATIONS ==========

    /**
     * 🗑️ Clear old beacon detection data
     */
    suspend fun clearBeaconHistory(cutoffTimestamp: Long) = withContext(ioDispatcher) {
        try {
            val deletedCount = beaconDao.deleteOldSightings(cutoffTimestamp)
            Timber.i("🗑️ Cleaned up $deletedCount old beacon records")
            
        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to clear beacon history")
            throw e
        }
    }

    /**
     * 🗑️ Complete data wipe (GDPR compliance)
     */
    suspend fun clearAllData() = withContext(ioDispatcher) {
        try {
            val deletedCount = beaconDao.deleteAllSightings()
            Timber.i("🗑️ Cleared all beacon data: $deletedCount records deleted")
            
        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to clear all beacon data")
            throw e
        }
    }

    // ========== MAINTENANCE OPERATIONS ==========

    /**
     * 🧹 Database maintenance operations
     */
    suspend fun performMaintenance() = withContext(ioDispatcher) {
        try {
            Timber.i("🧹 Starting database maintenance...")
            
            // Remove duplicate detections within 2-second window
            val duplicatesRemoved = beaconDao.removeDuplicateDetections(timeWindowMs = 2000L)
            
            // Get storage usage stats
            val totalRecords = beaconDao.getTotalRecordCount()
            val storageUsage = beaconDao.getEstimatedStorageUsage()
            
            Timber.i("✅ Maintenance completed - Removed $duplicatesRemoved duplicates | Total records: $totalRecords | Storage: ${storageUsage / 1024}KB")
            
        } catch (e: Exception) {
            Timber.e(e, "❌ Database maintenance failed")
            throw e
        }
    }
}

/**
 * 📈 Local Detection Statistics
 */
data class LocalDetectionStats(
    val totalDetections: Int = 0,
    val uniqueBeacons: Int = 0,
    val averageRssi: Double = 0.0,
    val averageDistance: Double = 0.0,
    val firstDetection: Long = 0L,
    val lastDetection: Long = 0L,
    val detectionTimespan: Long = 0L
) {
    
    fun isEmpty(): Boolean = totalDetections == 0
    
    fun getDetectionRatePerHour(): Double {
        val hours = detectionTimespan / 3600000.0
        return if (hours > 0) totalDetections / hours else 0.0
    }
}
