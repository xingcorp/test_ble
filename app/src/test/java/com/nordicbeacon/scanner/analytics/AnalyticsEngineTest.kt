package com.nordicbeacon.scanner.analytics

import com.nordicbeacon.scanner.analytics.signal.filters.RssiKalmanFilter
import com.nordicbeacon.scanner.analytics.signal.filters.MovingAverageFilter  
import com.nordicbeacon.scanner.analytics.signal.distance.AdvancedDistanceCalculator
import com.nordicbeacon.scanner.analytics.signal.quality.SignalQualityAssessment
import com.nordicbeacon.scanner.domain.entities.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.mockito.kotlin.*

/**
 * 🧪 Analytics Engine Unit Tests
 * 
 * Comprehensive testing của analytics processing pipeline
 * Validates signal processing algorithms và performance metrics
 * 
 * @author Senior Android Developer
 */
class AnalyticsEngineTest {

    private lateinit var kalmanFilter: RssiKalmanFilter
    private lateinit var movingAverageFilter: MovingAverageFilter
    private lateinit var distanceCalculator: AdvancedDistanceCalculator
    private lateinit var qualityAssessment: SignalQualityAssessment
    private lateinit var analyticsEngine: BeaconAnalyticsEngine

    @Before
    fun setUp() {
        kalmanFilter = RssiKalmanFilter()
        movingAverageFilter = MovingAverageFilter()
        distanceCalculator = AdvancedDistanceCalculator()
        qualityAssessment = SignalQualityAssessment()
        
        analyticsEngine = BeaconAnalyticsEngine(
            kalmanFilter = kalmanFilter,
            movingAverageFilter = movingAverageFilter,
            distanceCalculator = distanceCalculator,
            qualityAssessment = qualityAssessment,
            ioDispatcher = Dispatchers.Unconfined
        )
    }

    // ========== KALMAN FILTER TESTS ==========

    @Test
    fun `kalman filter should reduce RSSI noise effectively`() {
        // Given - noisy RSSI readings
        val noisyReadings = listOf(-65, -45, -75, -60, -80, -55, -70) // High variance
        
        // When - apply Kalman filtering
        val filteredResults = noisyReadings.map { rssi ->
            kalmanFilter.filterRssiMeasurement(rssi)
        }
        
        // Then - filter should smooth out noise
        val improvementFactors = filteredResults.map { it.improvementFactor }
        val averageImprovement = improvementFactors.drop(2).average() // Skip first few measurements
        
        assertTrue("Kalman filter should show improvement", averageImprovement > 0.1)
        assertTrue("Final confidence should be high", filteredResults.last().confidence > 0.7)
    }

    @Test
    fun `kalman filter should reject outliers correctly`() {
        // Given - normal readings with outlier
        val readings = listOf(-65, -67, -63, -66, -25, -64, -68) // -25 is obvious outlier
        
        // When  
        val results = readings.map { kalmanFilter.filterRssiMeasurement(it) }
        
        // Then - outlier should be detected
        val outlierResults = results.filter { it.isOutlier }
        assertTrue("Should detect at least one outlier", outlierResults.isNotEmpty())
        
        val outlierReading = results[4] // The -25 reading
        assertTrue("Outlier should be flagged", outlierReading.isOutlier)
    }

    // ========== DISTANCE CALCULATION TESTS ==========

    @Test
    fun `advanced distance calculator should improve accuracy over standard model`() {
        // Given - test RSSI values
        val testRssi = -70
        val testTxPower = -59
        
        // When - calculate với enhanced model
        val result = distanceCalculator.calculateEnhancedDistance(
            rssi = testRssi,
            txPower = testTxPower
        )
        
        // Then - should provide reasonable distance với confidence
        assertTrue("Distance should be reasonable", result.distance in 0.1..50.0)
        assertTrue("Should have decent confidence", result.confidence > 0.5)
        assertTrue("Should use enhanced model", result.model.name.contains("ENHANCED"))
    }

    @Test
    fun `multi-model distance calculation should provide best estimate`() {
        // Given
        val testRssi = -75
        val testTxPower = -59
        
        // When
        val result = distanceCalculator.calculateMultiModelDistance(
            rssi = testRssi,
            txPower = testTxPower
        )
        
        // Then
        assertTrue("Should have multiple estimates", result.allEstimates.size >= 2)
        assertTrue("Best estimate should have highest confidence", 
            result.bestEstimate.confidence >= result.allEstimates.map { it.confidence }.average())
        assertTrue("Model agreement should be reasonable", result.modelAgreement > 0.5)
    }

    // ========== SIGNAL QUALITY TESTS ==========

    @Test
    fun `signal quality assessment should identify stable signals`() {
        // Given - stable signal measurements
        val stableReadings = (1..10).map { i ->
            com.nordicbeacon.scanner.analytics.signal.quality.SignalMeasurement(
                rssi = -65 + (i % 3 - 1), // Small variations around -65
                distance = 3.0 + (i % 3 - 1) * 0.1, // Small distance variations
                timestamp = System.currentTimeMillis() + i * 1000L
            )
        }
        
        // When
        val qualityScore = qualityAssessment.assessSignalQuality(stableReadings)
        
        // Then
        assertTrue("Should detect high consistency", qualityScore.strengthConsistency > 0.7)
        assertTrue("Should detect good distance reliability", qualityScore.distanceReliability > 0.7)
        assertTrue("Should have good overall reliability", qualityScore.overallReliability > 0.7)
    }

    @Test
    fun `signal quality should detect poor signal conditions`() {
        // Given - unstable signal measurements
        val unstableReadings = listOf(
            com.nordicbeacon.scanner.analytics.signal.quality.SignalMeasurement(-50, 1.0, System.currentTimeMillis()),
            com.nordicbeacon.scanner.analytics.signal.quality.SignalMeasurement(-90, 15.0, System.currentTimeMillis() + 1000L),
            com.nordicbeacon.scanner.analytics.signal.quality.SignalMeasurement(-45, 0.8, System.currentTimeMillis() + 2000L),
            com.nordicbeacon.scanner.analytics.signal.quality.SignalMeasurement(-95, 20.0, System.currentTimeMillis() + 3000L)
        )
        
        // When
        val qualityScore = qualityAssessment.assessSignalQuality(unstableReadings)
        
        // Then  
        assertTrue("Should detect poor consistency", qualityScore.strengthConsistency < 0.5)
        assertTrue("Should detect poor reliability", qualityScore.distanceReliability < 0.5)
    }

    // ========== INTEGRATION TESTS ==========

    @Test  
    fun `analytics engine should process beacon end-to-end`() = runTest {
        // Given
        val testBeacon = createTestNordicBeacon()
        
        // When
        val enhancedBeacon = analyticsEngine.processBeacon(testBeacon)
        
        // Then
        assertNotNull("Enhanced beacon should not be null", enhancedBeacon)
        assertEquals("Original beacon should be preserved", testBeacon, enhancedBeacon.originalBeacon)
        assertNotNull("Analytics data should be present", enhancedBeacon.analyticsData)
        
        val analytics = enhancedBeacon.analyticsData
        assertTrue("Filtered RSSI should be reasonable", analytics.filteredRssi.filteredValue.toInt() in -100..20)
        assertTrue("Distance should be positive", analytics.enhancedDistance.distance >= 0)
        assertTrue("Quality score should be valid", analytics.qualityScore.overallReliability in 0.0..1.0)
    }

    // ========== HELPER METHODS ==========

    private fun createTestNordicBeacon(): NordicBeacon {
        return NordicBeacon(
            uuid = BeaconUUID.from("FDA50693-0000-0000-0000-290995101092"),
            major = Major(1234),
            minor = Minor(5678),
            signalStrength = SignalStrength(-65),
            proximity = Proximity(3.5),
            detectionTime = Timestamp.now(),
            txPower = TxPower(-59)
        )
    }
}
