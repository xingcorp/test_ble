package com.nordicbeacon.scanner.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

/**
 * 💾 Room Entity - Beacon Database Model  
 * 
 * Database representation của Nordic beacon detections
 * Optimized cho query performance với proper indexing
 * 
 * @author Senior Android Developer
 */
@Entity(
    tableName = "beacon_sightings",
    indices = [
        Index(value = ["uuid"]),
        Index(value = ["detection_time"]),
        Index(value = ["uuid", "major", "minor"], unique = false),
        Index(value = ["detection_time", "rssi"]) // Query optimization cho recent strong signals
    ]
)
data class BeaconEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    // Nordic beacon identification
    val uuid: String,
    val major: Int?,
    val minor: Int?,
    
    // Signal characteristics
    val rssi: Int,
    val distance: Double,
    val tx_power: Int?,
    
    // Detection metadata  
    val detection_time: Long,
    val detection_count: Int = 1,
    val last_seen: Long = System.currentTimeMillis(),
    
    // Signal quality metrics
    val average_rssi: Double = rssi.toDouble(),
    val signal_stability_score: Double = 0.0,
    
    // Beacon metadata
    val manufacturer: String = "Nordic Semiconductor",
    val beacon_type: String = "iBeacon"
) {
    
    /**
     * Validates if this is a Nordic beacon entity
     */
    fun isNordicBeacon(): Boolean {
        return uuid.equals("FDA50693-0000-0000-0000-290995101092", ignoreCase = true)
    }
    
    /**
     * Calculates age of detection in milliseconds
     */
    fun getDetectionAge(): Long {
        return System.currentTimeMillis() - detection_time
    }
    
    /**
     * Determines if detection is recent (< 30 seconds)
     */
    fun isRecentDetection(): Boolean {
        return getDetectionAge() < 30000L
    }
}
