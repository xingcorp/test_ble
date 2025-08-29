package com.nordicbeacon.scanner.analytics.signal.quality

import com.nordicbeacon.scanner.domain.entities.NordicBeacon
import kotlin.math.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 📊 Signal Quality Assessment Engine
 * 
 * Advanced signal quality analysis cho Nordic beacon reliability assessment
 * Provides multi-dimensional quality scoring và stability analysis
 * 
 * Quality Dimensions:
 * - Signal Strength Consistency (RSSI stability over time)
 * - Distance Measurement Reliability
 * - Temporal Signal Stability  
 * - Environmental Interference Assessment
 * - Overall Beacon Reliability Score
 * 
 * @author Senior Android Developer
 */
@Singleton
class SignalQualityAssessment @Inject constructor() {

    // ========== QUALITY ASSESSMENT IMPLEMENTATION ==========

    /**
     * 🎯 Assess comprehensive signal quality cho Nordic beacon
     * 
     * @param signalHistory List of historical signal measurements
     * @param timeWindow Analysis time window trong milliseconds
     * @return Comprehensive signal quality assessment
     */
    fun assessSignalQuality(
        signalHistory: List<SignalMeasurement>,
        timeWindow: Long = 30000L // Default 30 second window
    ): SignalQualityScore {
        
        if (signalHistory.isEmpty()) {
            return SignalQualityScore.empty()
        }
        
        // Filter measurements within time window
        val recentMeasurements = filterRecentMeasurements(signalHistory, timeWindow)
        
        if (recentMeasurements.size < 2) {
            return SignalQualityScore.insufficient(recentMeasurements.size)
        }
        
        // Calculate individual quality components
        val strengthConsistency = calculateSignalStrengthConsistency(recentMeasurements)
        val distanceReliability = calculateDistanceReliability(recentMeasurements)  
        val temporalStability = calculateTemporalStability(recentMeasurements)
        val interferenceLevel = assessInterferenceLevel(recentMeasurements)
        val overallReliability = calculateOverallReliability(recentMeasurements)
        
        return SignalQualityScore(
            strengthConsistency = strengthConsistency,
            distanceReliability = distanceReliability,
            temporalStability = temporalStability,
            interferenceLevel = interferenceLevel,
            overallReliability = overallReliability,
            measurementCount = recentMeasurements.size,
            timeWindowMs = timeWindow,
            analysisTimestamp = System.currentTimeMillis()
        )
    }

    /**
     * 🏆 Calculate beacon reliability score (0-100)
     */
    fun calculateBeaconReliabilityScore(beacon: NordicBeacon): BeaconReliabilityScore {
        
        // Multi-factor reliability assessment
        val signalStrengthScore = calculateSignalStrengthScore(beacon.signalStrength.rssi)
        val proximityScore = calculateProximityScore(beacon.proximity.meters)
        val consistencyScore = beacon.calculateReliabilityScore().toDouble() / 100.0
        
        // Weighted combination
        val overallScore = (signalStrengthScore * 0.4 + proximityScore * 0.3 + consistencyScore * 0.3) * 100
        
        return BeaconReliabilityScore(
            overallScore = overallScore.toInt().coerceIn(0, 100),
            signalStrengthScore = (signalStrengthScore * 100).toInt(),
            proximityScore = (proximityScore * 100).toInt(),
            consistencyScore = (consistencyScore * 100).toInt(),
            riskFactors = identifyRiskFactors(beacon),
            recommendations = generateImprovementRecommendations(beacon)
        )
    }

    // ========== QUALITY COMPONENT CALCULATIONS ==========

    /**
     * 📊 Calculate signal strength consistency
     */
    private fun calculateSignalStrengthConsistency(measurements: List<SignalMeasurement>): Double {
        
        if (measurements.size < 3) return 0.5 // Insufficient data
        
        val rssiValues = measurements.map { it.rssi.toDouble() }
        val mean = rssiValues.average()
        val variance = rssiValues.map { (it - mean).pow(2) }.average()
        val standardDeviation = sqrt(variance)
        
        // Consistency inversely related to standard deviation
        val coefficientOfVariation = if (mean != 0.0) abs(standardDeviation / mean) else 1.0
        
        return (1.0 - coefficientOfVariation.coerceIn(0.0, 1.0)).coerceIn(0.0, 1.0)
    }

    /**
     * 📏 Calculate distance measurement reliability
     */
    private fun calculateDistanceReliability(measurements: List<SignalMeasurement>): Double {
        
        if (measurements.size < 3) return 0.5
        
        val distances = measurements.map { it.distance }
        val mean = distances.average()
        val variance = distances.map { (it - mean).pow(2) }.average()
        val standardDeviation = sqrt(variance)
        
        // Reliability based on distance consistency
        val maxExpectedDeviation = 2.0 // meters
        val deviationRatio = standardDeviation / maxExpectedDeviation
        
        return (1.0 - deviationRatio).coerceIn(0.0, 1.0)
    }

    /**
     * ⏰ Calculate temporal stability
     */
    private fun calculateTemporalStability(measurements: List<SignalMeasurement>): Double {
        
        if (measurements.size < 4) return 0.5
        
        // Analyze trend stability over time
        val sortedMeasurements = measurements.sortedBy { it.timestamp }
        var stabilityScore = 0.0
        var comparisonCount = 0
        
        // Compare adjacent measurements
        for (i in 1 until sortedMeasurements.size) {
            val current = sortedMeasurements[i]
            val previous = sortedMeasurements[i-1]
            
            val rssiChange = abs(current.rssi - previous.rssi)
            val timeInterval = current.timestamp - previous.timestamp
            
            // Penalize rapid changes
            val changeRate = if (timeInterval > 0) rssiChange / (timeInterval / 1000.0) else 0.0
            val stability = 1.0 / (1.0 + changeRate) // Inverse relationship
            
            stabilityScore += stability
            comparisonCount++
        }
        
        return if (comparisonCount > 0) stabilityScore / comparisonCount else 0.5
    }

    /**
     * 📡 Assess environmental interference level
     */
    private fun assessInterferenceLevel(measurements: List<SignalMeasurement>): Double {
        
        if (measurements.size < 5) return 0.5 // Insufficient data cho interference assessment
        
        // Look cho erratic behavior patterns indicating interference
        var interferenceIndicators = 0
        val threshold = 10 // dBm sudden change threshold
        
        for (i in 1 until measurements.size) {
            val rssiChange = abs(measurements[i].rssi - measurements[i-1].rssi)
            
            if (rssiChange > threshold) {
                interferenceIndicators++
            }
        }
        
        val interferenceRate = interferenceIndicators.toDouble() / (measurements.size - 1)
        
        return (1.0 - interferenceRate).coerceIn(0.0, 1.0)
    }

    /**
     * 🏆 Calculate overall reliability combining all factors
     */
    private fun calculateOverallReliability(measurements: List<SignalMeasurement>): Double {
        
        val strengthScore = calculateSignalStrengthConsistency(measurements)
        val distanceScore = calculateDistanceReliability(measurements)
        val stabilityScore = calculateTemporalStability(measurements)
        val interferenceScore = assessInterferenceLevel(measurements)
        
        // Weighted combination of quality factors
        return strengthScore * 0.25 + distanceScore * 0.25 + stabilityScore * 0.3 + interferenceScore * 0.2
    }

    // ========== HELPER METHODS ==========

    private fun filterRecentMeasurements(measurements: List<SignalMeasurement>, timeWindow: Long): List<SignalMeasurement> {
        val cutoffTime = System.currentTimeMillis() - timeWindow
        return measurements.filter { it.timestamp >= cutoffTime }.sortedBy { it.timestamp }
    }

    private fun calculateSignalStrengthScore(rssi: Int): Double {
        return when {
            rssi > -50 -> 1.0
            rssi > -70 -> 0.8  
            rssi > -85 -> 0.6
            rssi > -95 -> 0.4
            else -> 0.2
        }
    }

    private fun calculateProximityScore(distance: Double): Double {
        return when {
            distance < 1.0 -> 1.0
            distance < 5.0 -> 0.8
            distance < 10.0 -> 0.6
            distance < 20.0 -> 0.4
            else -> 0.2
        }
    }

    private fun identifyRiskFactors(beacon: NordicBeacon): List<String> = emptyList()
    private fun generateImprovementRecommendations(beacon: NordicBeacon): List<String> = emptyList()
}

// ========== DATA MODELS ==========

data class SignalMeasurement(
    val rssi: Int,
    val distance: Double,
    val timestamp: Long,
    val txPower: Int? = null
)

data class SignalQualityScore(
    val strengthConsistency: Double,
    val distanceReliability: Double, 
    val temporalStability: Double,
    val interferenceLevel: Double,
    val overallReliability: Double,
    val measurementCount: Int,
    val timeWindowMs: Long,
    val analysisTimestamp: Long
) {
    
    companion object {
        fun empty() = SignalQualityScore(0.0, 0.0, 0.0, 0.0, 0.0, 0, 0L, System.currentTimeMillis())
        fun insufficient(count: Int) = SignalQualityScore(0.5, 0.5, 0.5, 0.5, 0.5, count, 0L, System.currentTimeMillis())
    }
}

data class BeaconReliabilityScore(
    val overallScore: Int,
    val signalStrengthScore: Int,
    val proximityScore: Int,
    val consistencyScore: Int,
    val riskFactors: List<String>,
    val recommendations: List<String>
)
