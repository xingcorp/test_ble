package com.nordicbeacon.scanner.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * 📊 Data Access Object - Nordic Beacon Database Operations
 * 
 * Room DAO với optimized queries cho beacon data access
 * Designed cho high-performance beacon detection storage và retrieval
 * 
 * @author Senior Android Developer
 */
@Dao
interface BeaconDao {

    // ========== INSERT OPERATIONS ==========

    /**
     * 💾 Insert single beacon detection
     * 
     * Uses REPLACE strategy để update existing detections with same UUID/major/minor
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBeaconSighting(beacon: BeaconEntity): Long

    /**
     * 💾 Insert multiple beacon detections (batch operation)
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBeaconSightings(beacons: List<BeaconEntity>): List<Long>

    /**
     * 🔄 Update existing beacon detection với new signal data
     */
    @Update
    suspend fun updateBeaconSighting(beacon: BeaconEntity): Int

    // ========== QUERY OPERATIONS ==========

    /**
     * 📋 Get all Nordic beacon detections
     */
    @Query("SELECT * FROM beacon_sightings WHERE uuid = :nordicUuid ORDER BY detection_time DESC")
    suspend fun getAllNordicBeacons(nordicUuid: String = "FDA50693-0000-0000-0000-290995101092"): List<BeaconEntity>

    /**
     * ⏰ Get recent beacon detections với time filtering
     */
    @Query("""
        SELECT * FROM beacon_sightings 
        WHERE uuid = :nordicUuid 
        AND detection_time >= :sinceTimestamp 
        ORDER BY detection_time DESC 
        LIMIT :limit
    """)
    suspend fun getRecentBeaconSightings(
        nordicUuid: String = "FDA50693-0000-0000-0000-290995101092",
        sinceTimestamp: Long,
        limit: Int = 100
    ): List<BeaconEntity>

    /**
     * 🔄 Get live beacon detections stream (reactive queries)
     */
    @Query("""
        SELECT * FROM beacon_sightings 
        WHERE uuid = :nordicUuid 
        ORDER BY detection_time DESC 
        LIMIT :limit
    """)
    fun getLiveBeaconSightings(
        nordicUuid: String = "FDA50693-0000-0000-0000-290995101092", 
        limit: Int = 50
    ): Flow<List<BeaconEntity>>

    /**
     * 📊 Get beacon detection statistics
     */
    @Query("""
        SELECT 
            COUNT(*) as total_detections,
            COUNT(DISTINCT major || '-' || minor) as unique_beacons,
            AVG(rssi) as average_rssi,
            AVG(distance) as average_distance,
            MIN(detection_time) as first_detection,
            MAX(detection_time) as last_detection
        FROM beacon_sightings 
        WHERE uuid = :nordicUuid 
        AND detection_time >= :sinceTimestamp
    """)
    suspend fun getDetectionStatistics(
        nordicUuid: String = "FDA50693-0000-0000-0000-290995101092",
        sinceTimestamp: Long
    ): BeaconStatistics?

    /**
     * 🎯 Get strongest signal detections
     */
    @Query("""
        SELECT * FROM beacon_sightings 
        WHERE uuid = :nordicUuid 
        AND rssi >= :minRssi 
        ORDER BY rssi DESC, detection_time DESC 
        LIMIT :limit
    """)
    suspend fun getStrongestSignals(
        nordicUuid: String = "FDA50693-0000-0000-0000-290995101092",
        minRssi: Int = -70,
        limit: Int = 20
    ): List<BeaconEntity>

    /**
     * 📍 Get closest beacon detections  
     */
    @Query("""
        SELECT * FROM beacon_sightings 
        WHERE uuid = :nordicUuid 
        AND distance <= :maxDistance 
        ORDER BY distance ASC, detection_time DESC 
        LIMIT :limit
    """)
    suspend fun getClosestBeacons(
        nordicUuid: String = "FDA50693-0000-0000-0000-290995101092",
        maxDistance: Double = 10.0,
        limit: Int = 20
    ): List<BeaconEntity>

    // ========== DELETE OPERATIONS ==========

    /**
     * 🗑️ Delete old beacon detections (privacy compliance)
     */
    @Query("DELETE FROM beacon_sightings WHERE detection_time < :cutoffTimestamp")
    suspend fun deleteOldSightings(cutoffTimestamp: Long): Int

    /**
     * 🗑️ Delete all beacon sightings (complete reset)
     */
    @Query("DELETE FROM beacon_sightings")
    suspend fun deleteAllSightings(): Int

    /**
     * 🗑️ Delete specific beacon sightings by UUID/major/minor
     */
    @Query("DELETE FROM beacon_sightings WHERE uuid = :uuid AND major = :major AND minor = :minor")
    suspend fun deleteBeaconSightings(uuid: String, major: Int?, minor: Int?): Int

    // ========== MAINTENANCE OPERATIONS ==========

    /**
     * 📊 Get database size information
     */
    @Query("SELECT COUNT(*) FROM beacon_sightings")
    suspend fun getTotalRecordCount(): Int

    /**
     * 📊 Get database storage usage estimation  
     */
    @Query("""
        SELECT 
            COUNT(*) * 
            (LENGTH(uuid) + 8 + 8 + 8 + 8 + 8 + 8 + 8 + 8 + LENGTH(manufacturer) + LENGTH(beacon_type)) 
        as estimated_bytes
        FROM beacon_sightings
    """)
    suspend fun getEstimatedStorageUsage(): Long

    /**
     * 🧹 Database maintenance - remove duplicates within time window
     */
    @Query("""
        DELETE FROM beacon_sightings 
        WHERE id NOT IN (
            SELECT MIN(id) 
            FROM beacon_sightings 
            GROUP BY uuid, major, minor, 
                     (detection_time / :timeWindowMs)
        )
    """)
    suspend fun removeDuplicateDetections(timeWindowMs: Long = 2000L): Int
}

/**
 * 📈 Database Statistics Model
 */
data class BeaconStatistics(
    val total_detections: Int,
    val unique_beacons: Int, 
    val average_rssi: Double,
    val average_distance: Double,
    val first_detection: Long,
    val last_detection: Long
) {
    
    /**
     * Calculates detection rate per hour
     */
    fun getDetectionRatePerHour(): Double {
        val durationHours = (last_detection - first_detection) / 3600000.0
        return if (durationHours > 0) total_detections / durationHours else 0.0
    }
    
    /**
     * Determines if statistics represent healthy detection pattern
     */
    fun isHealthyDetectionPattern(): Boolean {
        return total_detections > 0 && 
               average_rssi > -90.0 && 
               average_distance < 50.0 &&
               unique_beacons > 0
    }
}
