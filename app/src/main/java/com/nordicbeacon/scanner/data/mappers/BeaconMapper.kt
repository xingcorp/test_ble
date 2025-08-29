package com.nordicbeacon.scanner.data.mappers

import com.nordicbeacon.scanner.data.database.BeaconEntity
import com.nordicbeacon.scanner.domain.entities.*
import org.altbeacon.beacon.Beacon
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 🔄 Beacon Data Mapper
 * 
 * Handles conversions between different beacon representations:
 * - AltBeacon library Beacon ↔ Domain NordicBeacon
 * - Domain NordicBeacon ↔ Database BeaconEntity
 * 
 * Following Mapper pattern cho clean data transformation
 * 
 * @author Senior Android Developer
 */
@Singleton
class BeaconMapper @Inject constructor() {

    // ========== ALTBEACON → DOMAIN CONVERSION ==========

    /**
     * 🎯 Convert AltBeacon library Beacon → NordicBeacon domain entity
     * 
     * Performs validation và data enrichment during conversion
     */
    fun toDomainModel(altBeacon: Beacon): NordicBeacon {
        try {
            return NordicBeacon(
                uuid = BeaconUUID.from(altBeacon.id1?.toString() ?: ""),
                major = altBeacon.id2?.toInt()?.let { Major(it) },
                minor = altBeacon.id3?.toInt()?.let { Minor(it) },
                signalStrength = SignalStrength(altBeacon.rssi),
                proximity = Proximity(altBeacon.distance),
                detectionTime = Timestamp.now(),
                txPower = altBeacon.txPower?.let { TxPower(it) },
                metadata = BeaconMetadata(
                    manufacturer = "Nordic Semiconductor",
                    beaconType = "iBeacon",
                    detectionCount = 1,
                    lastSeenTimestamp = System.currentTimeMillis(),
                    averageRssi = altBeacon.rssi.toDouble(),
                    signalStabilityScore = calculateInitialStabilityScore(altBeacon)
                )
            )
            
        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to convert AltBeacon to NordicBeacon")
            throw BeaconMappingException("AltBeacon conversion failed", e)
        }
    }

    /**
     * 🎯 Convert multiple AltBeacons → NordicBeacons với filtering
     */
    fun toDomainModels(altBeacons: Collection<Beacon>): List<NordicBeacon> {
        return altBeacons.mapNotNull { beacon ->
            try {
                if (isNordicBeacon(beacon)) {
                    toDomainModel(beacon)
                } else {
                    null // Filter out non-Nordic beacons
                }
            } catch (e: Exception) {
                Timber.w(e, "⚠️ Skipping invalid beacon during batch conversion")
                null
            }
        }
    }

    // ========== DOMAIN → DATABASE CONVERSION ==========

    /**
     * 💾 Convert NordicBeacon domain entity → BeaconEntity database model
     */
    fun toEntity(domainBeacon: NordicBeacon): BeaconEntity {
        try {
            return BeaconEntity(
                uuid = domainBeacon.uuid.value,
                major = domainBeacon.major?.value,
                minor = domainBeacon.minor?.value,
                rssi = domainBeacon.signalStrength.rssi,
                distance = domainBeacon.proximity.meters,
                tx_power = domainBeacon.txPower?.value,
                detection_time = domainBeacon.detectionTime.millis,
                detection_count = domainBeacon.metadata.detectionCount,
                last_seen = domainBeacon.metadata.lastSeenTimestamp,
                average_rssi = domainBeacon.metadata.averageRssi,
                signal_stability_score = domainBeacon.metadata.signalStabilityScore,
                manufacturer = domainBeacon.metadata.manufacturer,
                beacon_type = domainBeacon.metadata.beaconType
            )
            
        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to convert NordicBeacon to BeaconEntity")
            throw BeaconMappingException("Domain to Entity conversion failed", e)
        }
    }

    /**
     * 💾 Convert multiple NordicBeacons → BeaconEntities
     */
    fun toEntities(domainBeacons: List<NordicBeacon>): List<BeaconEntity> {
        return domainBeacons.map { beacon ->
            try {
                toEntity(beacon)
            } catch (e: Exception) {
                Timber.w(e, "⚠️ Skipping invalid beacon during entity conversion")
                throw e // Re-throw để maintain data integrity
            }
        }
    }

    // ========== DATABASE → DOMAIN CONVERSION ==========

    /**
     * 🏗️ Convert BeaconEntity database model → NordicBeacon domain entity
     */
    fun toDomainModel(entity: BeaconEntity): NordicBeacon {
        try {
            return NordicBeacon(
                uuid = BeaconUUID.from(entity.uuid),
                major = entity.major?.let { Major(it) },
                minor = entity.minor?.let { Minor(it) },
                signalStrength = SignalStrength(entity.rssi),
                proximity = Proximity(entity.distance),
                detectionTime = Timestamp(entity.detection_time),
                txPower = entity.tx_power?.let { TxPower(it) },
                metadata = BeaconMetadata(
                    manufacturer = entity.manufacturer,
                    beaconType = entity.beacon_type,
                    detectionCount = entity.detection_count,
                    lastSeenTimestamp = entity.last_seen,
                    averageRssi = entity.average_rssi,
                    signalStabilityScore = entity.signal_stability_score
                )
            )
            
        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to convert BeaconEntity to NordicBeacon")
            throw BeaconMappingException("Entity to Domain conversion failed", e)
        }
    }

    /**
     * 🏗️ Convert multiple BeaconEntities → NordicBeacons
     */
    fun toDomainModels(entities: List<BeaconEntity>): List<NordicBeacon> {
        return entities.mapNotNull { entity ->
            try {
                if (entity.isNordicBeacon()) {
                    toDomainModel(entity)
                } else {
                    Timber.w("⚠️ Non-Nordic beacon in database: ${entity.uuid}")
                    null
                }
            } catch (e: Exception) {
                Timber.w(e, "⚠️ Skipping invalid entity during domain conversion")
                null // Continue với other entities
            }
        }
    }

    // ========== HELPER METHODS ==========

    /**
     * Validates if AltBeacon is Nordic beacon
     */
    private fun isNordicBeacon(beacon: Beacon): Boolean {
        val beaconUuid = beacon.id1?.toString()?.uppercase()
        val nordicUuid = NordicBeacon.NORDIC_UUID.uppercase()
        
        return beaconUuid == nordicUuid
    }

    /**
     * Calculates initial signal stability score từ single beacon reading
     */
    private fun calculateInitialStabilityScore(beacon: Beacon): Double {
        // Initial stability based on signal strength và distance consistency
        val rssiScore = when {
            beacon.rssi > -50 -> 1.0
            beacon.rssi > -70 -> 0.8
            beacon.rssi > -85 -> 0.6
            beacon.rssi > -95 -> 0.4
            else -> 0.2
        }
        
        val distanceScore = when {
            beacon.distance < 1.0 -> 1.0
            beacon.distance < 5.0 -> 0.8
            beacon.distance < 10.0 -> 0.6
            beacon.distance < 20.0 -> 0.4
            else -> 0.2
        }
        
        return (rssiScore + distanceScore) / 2.0
    }
}

/**
 * ❌ Custom Exception cho mapping errors
 */
class BeaconMappingException(
    message: String, 
    cause: Throwable? = null
) : Exception(message, cause)
