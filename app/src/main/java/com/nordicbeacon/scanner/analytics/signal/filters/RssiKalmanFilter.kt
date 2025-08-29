package com.nordicbeacon.scanner.analytics.signal.filters

import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * 🎯 Kalman Filter for RSSI Signal Smoothing
 * 
 * Advanced signal processing filter cho reducing RSSI noise trong Nordic beacon detection
 * Implements discrete Kalman filter algorithm optimized cho BLE signal characteristics
 * 
 * Key Features:
 * - Adaptive noise reduction based on signal history
 * - Configurable process và measurement noise parameters
 * - Outlier detection và rejection
 * - Real-time filtering với minimal computational overhead
 * 
 * Mathematical Foundation:
 * - State prediction: x_k = A * x_{k-1} + B * u_k + w_k
 * - Measurement update: z_k = H * x_k + v_k  
 * - Kalman gain: K_k = P_k * H^T / (H * P_k * H^T + R)
 * 
 * @author Senior Android Developer
 */
class RssiKalmanFilter(
    private val processNoise: Double = 0.1,      // Q - process noise covariance (system uncertainty)
    private val measurementNoise: Double = 4.0,  // R - measurement noise covariance (sensor noise)
    private val initialUncertainty: Double = 1.0 // P - initial estimation error covariance
) {

    // ========== KALMAN FILTER STATE ==========
    
    private var estimatedRssi: Double = 0.0     // x_k - current state estimate
    private var errorCovariance: Double = initialUncertainty // P_k - error covariance
    private var isInitialized: Boolean = false
    private var measurementCount: Int = 0
    
    // ========== FILTER STATISTICS ==========
    
    private var totalMeasurements: Int = 0
    private var filteredMeasurements: Int = 0
    private var outlierCount: Int = 0
    private var averageError: Double = 0.0

    // ========== CORE KALMAN FILTER IMPLEMENTATION ==========

    /**
     * 📡 Process new RSSI measurement through Kalman filter
     * 
     * @param measurementRssi Raw RSSI measurement từ Nordic beacon
     * @param measurementTimestamp Optional timestamp cho temporal analysis
     * @return Filtered RSSI value với improved accuracy
     */
    fun filterRssiMeasurement(
        measurementRssi: Int,
        measurementTimestamp: Long = System.currentTimeMillis()
    ): FilteredRssiResult {
        
        totalMeasurements++
        
        // Initialize filter với first measurement
        if (!isInitialized) {
            return initializeFilter(measurementRssi, measurementTimestamp)
        }
        
        // Validate measurement (outlier detection)
        if (!isValidMeasurement(measurementRssi)) {
            outlierCount++
            return FilteredRssiResult(
                filteredValue = estimatedRssi,
                confidence = calculateConfidence(),
                isOutlier = true,
                improvementFactor = 0.0,
                filterState = getCurrentFilterState()
            )
        }
        
        // Kalman filter prediction step
        val predictedRssi = estimatedRssi  // For RSSI, assume constant state model
        val predictedErrorCovariance = errorCovariance + processNoise
        
        // Kalman filter update step  
        val kalmanGain = predictedErrorCovariance / (predictedErrorCovariance + measurementNoise)
        val innovation = measurementRssi - predictedRssi
        
        // State update
        estimatedRssi = predictedRssi + kalmanGain * innovation
        errorCovariance = (1.0 - kalmanGain) * predictedErrorCovariance
        
        measurementCount++
        filteredMeasurements++
        
        // Calculate improvement metrics
        val improvementFactor = calculateImprovementFactor(measurementRssi, estimatedRssi)
        updateAverageError(abs(innovation))
        
        return FilteredRssiResult(
            filteredValue = estimatedRssi,
            confidence = calculateConfidence(),
            isOutlier = false,
            improvementFactor = improvementFactor,
            filterState = getCurrentFilterState()
        )
    }

    /**
     * 🔄 Reset filter state (new beacon session)
     */
    fun reset() {
        estimatedRssi = 0.0
        errorCovariance = initialUncertainty
        isInitialized = false
        measurementCount = 0
        totalMeasurements = 0
        filteredMeasurements = 0
        outlierCount = 0
        averageError = 0.0
    }

    /**
     * 📊 Get filter performance statistics
     */
    fun getFilterStatistics(): KalmanFilterStats {
        return KalmanFilterStats(
            totalMeasurements = totalMeasurements,
            filteredMeasurements = filteredMeasurements,
            outlierCount = outlierCount,
            outlierRate = if (totalMeasurements > 0) outlierCount.toDouble() / totalMeasurements else 0.0,
            averageError = averageError,
            currentEstimate = estimatedRssi,
            errorCovariance = errorCovariance,
            confidence = calculateConfidence()
        )
    }

    // ========== PRIVATE IMPLEMENTATION ==========

    /**
     * 🏭 Initialize filter với first valid measurement
     */
    private fun initializeFilter(
        initialRssi: Int, 
        timestamp: Long
    ): FilteredRssiResult {
        
        estimatedRssi = initialRssi.toDouble()
        errorCovariance = initialUncertainty
        isInitialized = true
        measurementCount = 1
        filteredMeasurements = 1
        
        return FilteredRssiResult(
            filteredValue = estimatedRssi,
            confidence = 0.5, // Low confidence với single measurement
            isOutlier = false,
            improvementFactor = 0.0, // No improvement on first measurement
            filterState = getCurrentFilterState()
        )
    }

    /**
     * 🛡️ Validate RSSI measurement cho outlier detection
     */
    private fun isValidMeasurement(measurementRssi: Int): Boolean {
        
        // Basic range validation
        if (measurementRssi < -100 || measurementRssi > 20) {
            return false
        }
        
        // Outlier detection based on current estimate
        if (isInitialized) {
            val deviation = abs(measurementRssi - estimatedRssi)
            val maxDeviation = 3.0 * sqrt(errorCovariance + measurementNoise) // 3-sigma rule
            
            return deviation <= maxDeviation
        }
        
        return true // Accept all measurements before initialization
    }

    /**
     * 📊 Calculate filter confidence based on error covariance
     */
    private fun calculateConfidence(): Double {
        
        // Confidence decreases với higher error covariance
        val maxCovariance = 25.0 // Empirical maximum for RSSI
        val normalizedCovariance = (errorCovariance / maxCovariance).coerceIn(0.0, 1.0)
        
        // Additional confidence boost từ measurement count
        val countConfidence = if (measurementCount > 0) {
            (measurementCount.toDouble() / 10.0).coerceIn(0.0, 0.5) // Max 0.5 boost từ count
        } else 0.0
        
        return ((1.0 - normalizedCovariance) + countConfidence).coerceIn(0.0, 1.0)
    }

    /**
     * 📈 Calculate improvement factor (signal vs noise)
     */
    private fun calculateImprovementFactor(rawValue: Int, filteredValue: Double): Double {
        
        if (measurementCount < 2) return 0.0 // No comparison available
        
        val rawDeviation = abs(rawValue - estimatedRssi)
        val filteredDeviation = abs(filteredValue - estimatedRssi)
        
        return if (rawDeviation > 0) {
            ((rawDeviation - filteredDeviation) / rawDeviation).coerceIn(0.0, 1.0)
        } else 0.0
    }

    /**
     * 🔄 Update running average error
     */
    private fun updateAverageError(currentError: Double) {
        averageError = if (filteredMeasurements <= 1) {
            currentError
        } else {
            ((averageError * (filteredMeasurements - 1)) + currentError) / filteredMeasurements
        }
    }

    /**
     * 📊 Get current filter state cho debugging
     */
    private fun getCurrentFilterState(): KalmanFilterState {
        return KalmanFilterState(
            estimate = estimatedRssi,
            errorCovariance = errorCovariance,
            kalmanGain = if (errorCovariance > 0) errorCovariance / (errorCovariance + measurementNoise) else 0.0,
            processNoise = processNoise,
            measurementNoise = measurementNoise
        )
    }
}

/**
 * 📊 Filtered RSSI Result
 */
data class FilteredRssiResult(
    val filteredValue: Double,
    val confidence: Double,        // 0.0 - 1.0 confidence score
    val isOutlier: Boolean,
    val improvementFactor: Double, // 0.0 - 1.0 improvement over raw signal
    val filterState: KalmanFilterState
) {
    
    /**
     * 🎯 Get filtered RSSI as integer cho compatibility
     */
    fun getFilteredRssiInt(): Int = filteredValue.toInt()
    
    /**
     * ✅ Check if filter result is reliable
     */
    fun isReliable(): Boolean = confidence >= 0.7 && !isOutlier
    
    /**
     * 📈 Check if significant improvement achieved
     */
    fun hasSignificantImprovement(): Boolean = improvementFactor >= 0.15
}

/**
 * 🔧 Kalman Filter State Information
 */
data class KalmanFilterState(
    val estimate: Double,
    val errorCovariance: Double,
    val kalmanGain: Double,
    val processNoise: Double,
    val measurementNoise: Double
) {
    
    /**
     * 📊 Get state summary cho debugging
     */
    fun getStateSummary(): String {
        return "Estimate: ${"%.2f".format(estimate)}dBm | " +
               "Covariance: ${"%.3f".format(errorCovariance)} | " +
               "Gain: ${"%.3f".format(kalmanGain)}"
    }
}

/**
 * 📈 Kalman Filter Performance Statistics
 */
data class KalmanFilterStats(
    val totalMeasurements: Int,
    val filteredMeasurements: Int,
    val outlierCount: Int,
    val outlierRate: Double,
    val averageError: Double,
    val currentEstimate: Double,
    val errorCovariance: Double,
    val confidence: Double
) {
    
    /**
     * 🎯 Calculate filter effectiveness score
     */
    fun getEffectivenessScore(): Double {
        val outlierHandling = 1.0 - outlierRate.coerceIn(0.0, 1.0)
        val confidenceScore = confidence
        val stabilityScore = if (errorCovariance < 5.0) 1.0 else (5.0 / errorCovariance).coerceIn(0.0, 1.0)
        
        return (outlierHandling * 0.3 + confidenceScore * 0.4 + stabilityScore * 0.3)
    }
    
    /**
     * 📊 Get formatted statistics report
     */
    fun getFormattedReport(): String {
        return """
            📊 Kalman Filter Performance:
            📈 Total Measurements: $totalMeasurements
            ✅ Successfully Filtered: $filteredMeasurements  
            🚫 Outliers Rejected: $outlierCount (${"%.1f".format(outlierRate * 100)}%)
            📐 Average Error: ${"%.2f".format(averageError)}dBm
            🎯 Current Estimate: ${"%.2f".format(currentEstimate)}dBm
            📊 Confidence: ${"%.1f".format(confidence * 100)}%
            🏆 Effectiveness: ${"%.1f".format(getEffectivenessScore() * 100)}%
        """.trimIndent()
    }
}
