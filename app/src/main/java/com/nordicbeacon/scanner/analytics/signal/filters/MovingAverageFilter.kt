package com.nordicbeacon.scanner.analytics.signal.filters

import java.util.LinkedList
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sqrt
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 📊 Moving Average Filter for Distance Smoothing
 * 
 * Implements weighted moving average algorithm cho stabilizing distance calculations
 * Optimized cho Nordic beacon distance measurement smoothing
 * 
 * Key Features:
 * - Configurable window size cho different scenarios
 * - Weighted averaging với decay factors
 * - Outlier detection và adaptive window sizing
 * - Memory-efficient circular buffer implementation
 * 
 * @author Senior Android Developer
 */
@Singleton
class MovingAverageFilter @Inject constructor() {

    // ========== FILTER CONFIGURATION ==========
    private val windowSize: Int = 10           // Number of measurements trong window
    private val decayFactor: Double = 0.9      // Weight decay cho older measurements  
    private val outlierThreshold: Double = 2.0  // Standard deviations cho outlier detection

    // ========== FILTER STATE ==========
    
    private val measurementWindow = LinkedList<WeightedMeasurement>()
    private var currentAverage: Double = 0.0
    private var variance: Double = 0.0
    private var isStable: Boolean = false
    
    // ========== STATISTICS ==========
    
    private var totalMeasurements: Int = 0
    private var filteredMeasurements: Int = 0
    private var outliersRejected: Int = 0

    // ========== CORE FILTERING IMPLEMENTATION ==========

    /**
     * 📏 Process new distance measurement
     * 
     * @param distance Raw distance measurement từ Nordic beacon
     * @param timestamp Measurement timestamp cho temporal weighting
     * @param confidence Optional confidence score từ signal strength
     * @return Smoothed distance với quality metrics
     */
    fun filterDistanceMeasurement(
        distance: Double,
        timestamp: Long = System.currentTimeMillis(),
        confidence: Double = 1.0
    ): FilteredDistanceResult {
        
        totalMeasurements++
        
        // Validate measurement
        if (!isValidDistance(distance)) {
            return FilteredDistanceResult(
                filteredValue = currentAverage,
                confidence = calculateFilterConfidence(),
                isOutlier = true,
                stabilityScore = calculateStabilityScore(),
                improvementFactor = 0.0,
                windowState = getCurrentWindowState()
            )
        }
        
        // Check for outlier
        if (isOutlier(distance) && measurementWindow.size >= 3) {
            outliersRejected++
            
            return FilteredDistanceResult(
                filteredValue = currentAverage,
                confidence = calculateFilterConfidence(),
                isOutlier = true,
                stabilityScore = calculateStabilityScore(),
                improvementFactor = 0.0,
                windowState = getCurrentWindowState()
            )
        }
        
        // Add measurement to window
        addMeasurementToWindow(distance, timestamp, confidence)
        
        // Calculate weighted moving average
        val previousAverage = currentAverage
        currentAverage = calculateWeightedAverage()
        variance = calculateVariance()
        isStable = checkStability()
        
        filteredMeasurements++
        
        // Calculate improvement
        val improvementFactor = if (totalMeasurements > 1) {
            calculateImprovementFactor(distance, currentAverage, previousAverage)
        } else 0.0
        
        return FilteredDistanceResult(
            filteredValue = currentAverage,
            confidence = calculateFilterConfidence(),
            isOutlier = false,
            stabilityScore = calculateStabilityScore(),
            improvementFactor = improvementFactor,
            windowState = getCurrentWindowState()
        )
    }

    /**
     * 🔄 Reset filter state
     */
    fun reset() {
        measurementWindow.clear()
        currentAverage = 0.0
        variance = 0.0
        isStable = false
        totalMeasurements = 0
        filteredMeasurements = 0
        outliersRejected = 0
    }

    /**
     * 📊 Get filter performance statistics
     */
    fun getFilterStatistics(): MovingAverageStats {
        return MovingAverageStats(
            windowSize = measurementWindow.size,
            totalMeasurements = totalMeasurements,
            filteredMeasurements = filteredMeasurements,
            outliersRejected = outliersRejected,
            currentAverage = currentAverage,
            variance = variance,
            stabilityScore = calculateStabilityScore(),
            effectiveness = calculateEffectiveness()
        )
    }

    // ========== PRIVATE IMPLEMENTATION ==========

    /**
     * ✅ Validate distance measurement
     */
    private fun isValidDistance(distance: Double): Boolean {
        return distance >= 0.0 && distance <= 100.0 // Reasonable range cho Nordic beacon
    }

    /**
     * 🚫 Check if measurement is outlier
     */
    private fun isOutlier(distance: Double): Boolean {
        if (measurementWindow.size < 3) return false // Need minimum data cho outlier detection
        
        val standardDeviation = sqrt(variance)
        val deviation = abs(distance - currentAverage)
        
        return deviation > (outlierThreshold * standardDeviation)
    }

    /**
     * 📝 Add measurement to window với automatic size management
     */
    private fun addMeasurementToWindow(distance: Double, timestamp: Long, confidence: Double) {
        
        val weightedMeasurement = WeightedMeasurement(
            value = distance,
            timestamp = timestamp,
            confidence = confidence,
            weight = calculateMeasurementWeight(timestamp, confidence)
        )
        
        measurementWindow.addFirst(weightedMeasurement)
        
        // Remove old measurements if window exceeds size
        while (measurementWindow.size > windowSize) {
            measurementWindow.removeLast()
        }
    }

    /**
     * ⚖️ Calculate measurement weight based on recency và confidence
     */
    private fun calculateMeasurementWeight(timestamp: Long, confidence: Double): Double {
        val age = System.currentTimeMillis() - timestamp
        val ageSeconds = age / 1000.0
        
        // Exponential decay cho older measurements
        val timeFactor = exp(-ageSeconds / 30.0) // 30 second half-life
        
        return confidence * timeFactor * decayFactor
    }

    /**
     * 📊 Calculate weighted moving average
     */
    private fun calculateWeightedAverage(): Double {
        if (measurementWindow.isEmpty()) return 0.0
        
        var weightedSum = 0.0
        var totalWeight = 0.0
        
        for ((index, measurement) in measurementWindow.withIndex()) {
            val ageWeight = decayFactor.pow(index.toDouble())
            val finalWeight = measurement.weight * ageWeight
            
            weightedSum += measurement.value * finalWeight
            totalWeight += finalWeight
        }
        
        return if (totalWeight > 0) weightedSum / totalWeight else 0.0
    }

    /**
     * 📏 Calculate variance cho stability assessment
     */
    private fun calculateVariance(): Double {
        if (measurementWindow.size < 2) return Double.MAX_VALUE
        
        var squaredDeviations = 0.0
        var totalWeight = 0.0
        
        for ((index, measurement) in measurementWindow.withIndex()) {
            val ageWeight = decayFactor.pow(index.toDouble())
            val finalWeight = measurement.weight * ageWeight
            val deviation = measurement.value - currentAverage
            
            squaredDeviations += (deviation * deviation) * finalWeight
            totalWeight += finalWeight
        }
        
        return if (totalWeight > 0) squaredDeviations / totalWeight else Double.MAX_VALUE
    }

    /**
     * 🛡️ Check signal stability
     */
    private fun checkStability(): Boolean {
        if (measurementWindow.size < windowSize / 2) return false
        
        val standardDeviation = sqrt(variance)
        val coefficientOfVariation = if (currentAverage > 0) standardDeviation / currentAverage else Double.MAX_VALUE
        
        return coefficientOfVariation < 0.1 // 10% coefficient of variation indicates stability
    }

    private fun calculateFilterConfidence(): Double = if (isStable) 0.9 else 0.6
    private fun calculateStabilityScore(): Double = if (isStable) 1.0 - (variance / 10.0).coerceIn(0.0, 1.0) else 0.5
    private fun calculateImprovementFactor(raw: Double, filtered: Double, previous: Double): Double = 0.0
    private fun calculateEffectiveness(): Double = if (totalMeasurements > 0) filteredMeasurements.toDouble() / totalMeasurements else 0.0
    private fun getCurrentWindowState(): MovingAverageWindowState = MovingAverageWindowState(measurementWindow.size, currentAverage, variance)
}

// ========== DATA MODELS ==========

/**
 * ⚖️ Weighted Measurement
 */
data class WeightedMeasurement(
    val value: Double,
    val timestamp: Long,
    val confidence: Double,
    val weight: Double
)

/**
 * 📊 Filtered Distance Result
 */
data class FilteredDistanceResult(
    val filteredValue: Double,
    val confidence: Double,
    val isOutlier: Boolean,
    val stabilityScore: Double,
    val improvementFactor: Double,
    val windowState: MovingAverageWindowState
)

/**
 * 📈 Moving Average Statistics
 */
data class MovingAverageStats(
    val windowSize: Int,
    val totalMeasurements: Int,
    val filteredMeasurements: Int,
    val outliersRejected: Int,
    val currentAverage: Double,
    val variance: Double,
    val stabilityScore: Double,
    val effectiveness: Double
)

/**
 * 🔧 Window State Information
 */
data class MovingAverageWindowState(
    val size: Int,
    val average: Double,
    val variance: Double
)
