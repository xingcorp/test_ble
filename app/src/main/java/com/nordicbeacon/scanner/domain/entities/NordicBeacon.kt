package com.nordicbeacon.scanner.domain.entities

import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue
import android.os.Parcelable

/**
 * 🎯 Core Domain Entity - Nordic Beacon
 * 
 * Represents a Nordic BLE beacon with specific UUID: FDA50693-0000-0000-0000-290995101092
 * Immutable entity following DDD principles
 * 
 * @author Senior Android Developer
 */
@Parcelize
data class NordicBeacon(
    val uuid: @RawValue BeaconUUID,
    val major: @RawValue Major?,
    val minor: @RawValue Minor?,
    val signalStrength: @RawValue SignalStrength,
    val proximity: @RawValue Proximity,
    val detectionTime: @RawValue Timestamp,
    val txPower: @RawValue TxPower?,
    val metadata: @RawValue BeaconMetadata = BeaconMetadata()
) : Parcelable {

    /**
     * Validates if this is a genuine Nordic beacon
     */
    fun isValidNordicBeacon(): Boolean {
        return uuid.value == NORDIC_UUID && signalStrength.isValidSignal()
    }

    /**
     * Determines if beacon is in immediate proximity (< 1 meter)
     */
    fun isImmediate(): Boolean = proximity.meters < 1.0

    /**
     * Determines if beacon is in near range (1-5 meters)
     */
    fun isNear(): Boolean = proximity.meters in 1.0..5.0

    /**
     * Determines if beacon is in far range (5-20 meters)
     */
    fun isFar(): Boolean = proximity.meters in 5.0..20.0

    /**
     * Calculates signal reliability score (0-100)
     */
    fun calculateReliabilityScore(): Int {
        val rssiScore = when {
            signalStrength.rssi > -50 -> 100
            signalStrength.rssi > -70 -> 80
            signalStrength.rssi > -85 -> 60
            signalStrength.rssi > -95 -> 40
            else -> 20
        }
        
        val proximityScore = when {
            proximity.meters < 1.0 -> 100
            proximity.meters < 5.0 -> 85
            proximity.meters < 10.0 -> 70
            proximity.meters < 20.0 -> 50
            else -> 25
        }
        
        return (rssiScore * 0.6 + proximityScore * 0.4).toInt()
    }

    companion object {
        const val NORDIC_UUID = "FDA50693-0000-0000-0000-290995101092"
        
        /**
         * Factory method để create NordicBeacon với validation
         */
        fun create(
            uuid: String,
            major: Int?,
            minor: Int?,
            rssi: Int,
            distance: Double,
            txPower: Int? = null
        ): NordicBeacon? {
            return if (uuid == NORDIC_UUID && rssi > -100 && distance >= 0) {
                NordicBeacon(
                    uuid = BeaconUUID.from(uuid),
                    major = major?.let { Major(it) },
                    minor = minor?.let { Minor(it) },
                    signalStrength = SignalStrength(rssi),
                    proximity = Proximity(distance),
                    detectionTime = Timestamp.now(),
                    txPower = txPower?.let { TxPower(it) }
                )
            } else null
        }
    }
}

// ========== VALUE OBJECTS - Type Safety ==========

/**
 * 🔒 Value Object - Beacon UUID với validation
 */
@JvmInline
value class BeaconUUID private constructor(val value: String) {
    companion object {
        fun from(uuid: String): BeaconUUID {
            require(isValidUUID(uuid)) { "Invalid beacon UUID format: $uuid" }
            return BeaconUUID(uuid.uppercase())
        }
        
        private fun isValidUUID(uuid: String): Boolean {
            val uuidRegex = Regex("[0-9A-Fa-f]{8}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{12}")
            return uuid.matches(uuidRegex)
        }
    }
}

/**
 * 🔒 Value Object - Beacon Major identifier
 */
@JvmInline
value class Major(val value: Int) {
    init {
        require(value in 0..65535) { "Major must be between 0 and 65535, got: $value" }
    }
}

/**
 * 🔒 Value Object - Beacon Minor identifier  
 */
@JvmInline
value class Minor(val value: Int) {
    init {
        require(value in 0..65535) { "Minor must be between 0 and 65535, got: $value" }
    }
}

/**
 * 🔒 Value Object - Signal Strength (RSSI)
 */
@JvmInline
value class SignalStrength(val rssi: Int) {
    init {
        require(rssi in -100..20) { "RSSI must be between -100 and 20 dBm, got: $rssi" }
    }
    
    fun isValidSignal(): Boolean = rssi > -100
    fun isStrongSignal(): Boolean = rssi > -70
    fun isWeakSignal(): Boolean = rssi < -85
}

/**
 * 🔒 Value Object - Beacon Proximity Distance
 */
@JvmInline  
value class Proximity(val meters: Double) {
    init {
        require(meters >= 0) { "Proximity cannot be negative: $meters" }
    }
    
    fun isInRange(maxDistance: Double = 50.0): Boolean = meters <= maxDistance
}

/**
 * 🔒 Value Object - Detection Timestamp
 */
@JvmInline
value class Timestamp(val millis: Long) {
    companion object {
        fun now(): Timestamp = Timestamp(System.currentTimeMillis())
    }
    
    fun toHour(): Int = (millis / 3600000).toInt() % 24
}

/**
 * 🔒 Value Object - Transmission Power
 */
@JvmInline
value class TxPower(val value: Int) {
    init {
        require(value in -30..20) { "TxPower must be between -30 and 20 dBm, got: $value" }
    }
}

/**
 * 📋 Additional metadata for beacon
 */
@Parcelize
data class BeaconMetadata(
    val manufacturer: String = "Nordic Semiconductor",
    val beaconType: String = "iBeacon",
    val detectionCount: Int = 1,
    val lastSeenTimestamp: Long = System.currentTimeMillis(),
    val averageRssi: Double = 0.0,
    val signalStabilityScore: Double = 0.0
) : Parcelable
