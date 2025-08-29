package com.nordicbeacon.scanner.analytics

import com.nordicbeacon.scanner.analytics.insights.BeaconInsightsGenerator
import com.nordicbeacon.scanner.analytics.monitoring.service.ServicePerformanceMonitor
import com.nordicbeacon.scanner.debug.logging.AdvancedLogger
import com.nordicbeacon.scanner.domain.entities.NordicBeacon
import com.nordicbeacon.scanner.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 🎯 Analytics Integration Manager
 * 
 * Coordinates all analytics components và integrates với existing beacon scanning system
 * Provides unified interface cho analytics capabilities
 * 
 * Key Responsibilities:
 * - Coordinate analytics engine, insights generator, và performance monitor
 * - Integrate analytics với existing BeaconScanningService
 * - Manage analytics data flow và processing pipeline
 * - Provide unified analytics API cho UI components
 * 
 * @author Senior Android Developer
 */
@Singleton
class AnalyticsIntegrationManager @Inject constructor(
    private val analyticsEngine: BeaconAnalyticsEngine,
    private val insightsGenerator: BeaconInsightsGenerator,
    private val performanceMonitor: ServicePerformanceMonitor,
    private val advancedLogger: AdvancedLogger,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {

    // ========== ANALYTICS COORDINATION ==========
    
    private val analyticsScope = CoroutineScope(SupervisorJob() + ioDispatcher)
    private var isAnalyticsEnabled = true
    
    // Real-time data streams
    private val _enhancedBeacons = MutableSharedFlow<EnhancedNordicBeacon>(
        replay = 1,
        extraBufferCapacity = 32
    )
    val enhancedBeacons: SharedFlow<EnhancedNordicBeacon> = _enhancedBeacons.asSharedFlow()
    
    private val _analyticsInsights = MutableStateFlow<com.nordicbeacon.scanner.analytics.insights.BeaconInsights?>(null)
    val analyticsInsights: StateFlow<com.nordicbeacon.scanner.analytics.insights.BeaconInsights?> = _analyticsInsights.asStateFlow()

    // ========== PUBLIC API ==========

    /**
     * 🚀 Initialize analytics system
     */
    fun initializeAnalytics() {
        try {
            Timber.i("🚀 Initializing analytics integration...")
            
            // Initialize advanced logging
            advancedLogger.initialize()
            
            // Start performance monitoring
            startPerformanceMonitoring()
            
            // Setup analytics data flow
            setupAnalyticsFlow()
            
            Timber.i("✅ Analytics integration initialized successfully")
            
        } catch (e: Exception) {
            Timber.e(e, "❌ Analytics initialization failed")
            isAnalyticsEnabled = false
        }
    }

    /**
     * 📊 Process beacon với full analytics pipeline
     */
    suspend fun processBeaconWithAnalytics(beacon: NordicBeacon): EnhancedNordicBeacon {
        
        return if (isAnalyticsEnabled) {
            try {
                // Process through analytics engine
                val enhancedBeacon = analyticsEngine.processBeacon(beacon)
                
                // Log beacon detection với analytics data
                advancedLogger.logBeaconDetection(beacon, "analytics_pipeline")
                
                // Log signal processing details
                logSignalProcessingDetails(enhancedBeacon)
                
                // Emit to analytics flow
                _enhancedBeacons.emit(enhancedBeacon)
                
                enhancedBeacon
                
            } catch (e: Exception) {
                Timber.e(e, "❌ Analytics processing failed cho beacon")
                
                // Fallback: Return beacon với minimal analytics
                EnhancedNordicBeacon(
                    originalBeacon = beacon,
                    analyticsData = BeaconAnalyticsData.minimal(beacon)
                )
            }
        } else {
            // Analytics disabled - return beacon với minimal data
            EnhancedNordicBeacon(
                originalBeacon = beacon,
                analyticsData = BeaconAnalyticsData.minimal(beacon)
            )
        }
    }

    /**
     * 📈 Generate current insights từ detection history
     */
    suspend fun generateCurrentInsights(
        detectionHistory: List<NordicBeacon>,
        timeRange: com.nordicbeacon.scanner.analytics.insights.TimeRange = com.nordicbeacon.scanner.analytics.insights.TimeRange.LAST_24_HOURS
    ) {
        
        if (!isAnalyticsEnabled) return
        
        analyticsScope.launch {
            try {
                val insights = insightsGenerator.generateInsights(detectionHistory, timeRange)
                _analyticsInsights.value = insights
                
                Timber.i("📈 Insights generated: ${insights.recommendations.size} recommendation(s)")
                
            } catch (e: Exception) {
                Timber.e(e, "❌ Insights generation failed")
            }
        }
    }

    /**
     * 🏥 Get current system health status
     */
    suspend fun getCurrentHealthStatus(): com.nordicbeacon.scanner.analytics.monitoring.service.ServiceHealthStatus? {
        
        return if (isAnalyticsEnabled) {
            try {
                performanceMonitor.getServiceHealthStatus()
            } catch (e: Exception) {
                Timber.e(e, "❌ Health status check failed")
                null
            }
        } else null
    }

    /**
     * 📊 Enable/disable analytics processing
     */
    fun setAnalyticsEnabled(enabled: Boolean) {
        if (enabled == isAnalyticsEnabled) return
        
        isAnalyticsEnabled = enabled
        
        if (enabled) {
            Timber.i("✅ Analytics enabled")
            startPerformanceMonitoring()
        } else {
            Timber.i("⏸️ Analytics disabled")
            stopPerformanceMonitoring()
        }
    }

    // ========== PRIVATE IMPLEMENTATION ==========

    /**
     * 📊 Start performance monitoring
     */
    private fun startPerformanceMonitoring() {
        analyticsScope.launch {
            try {
                performanceMonitor.startMonitoring(intervalMs = 15000L) // Every 15 seconds
                    .catch { e -> 
                        Timber.e(e, "❌ Performance monitoring stream error")
                    }
                    .collect { metrics ->
                        // Log performance metrics
                        advancedLogger.logServicePerformance(metrics, "integration_manager")
                        
                        // Check for performance issues
                        if (metrics.healthScore < 0.6) {
                            Timber.w("⚠️ Performance degradation detected: Health=${(metrics.healthScore * 100).toInt()}%")
                        }
                    }
                    
            } catch (e: Exception) {
                Timber.e(e, "❌ Performance monitoring setup failed")
            }
        }
    }

    /**
     * ⏹️ Stop performance monitoring  
     */
    private fun stopPerformanceMonitoring() {
        performanceMonitor.stopMonitoring()
    }

    /**
     * 🔄 Setup analytics data flow processing
     */
    private fun setupAnalyticsFlow() {
        analyticsScope.launch {
            analyticsEngine.analyticsFlow
                .catch { e -> Timber.e(e, "❌ Analytics flow error") }
                .collect { event ->
                    handleAnalyticsEvent(event)
                }
        }
    }

    /**
     * 📡 Handle analytics events
     */
    private fun handleAnalyticsEvent(event: AnalyticsEvent) {
        when (event) {
            is AnalyticsEvent.BeaconProcessed -> {
                // Additional processing if needed
                Timber.d("📊 Analytics event: Beacon processed")
            }
            
            is AnalyticsEvent.QualityAlert -> {
                Timber.w("⚠️ Quality alert: ${event.message} (${event.severity})")
            }
            
            is AnalyticsEvent.PerformanceMetric -> {
                Timber.d("📈 Performance metric: ${event.metric} = ${event.value}")
            }
        }
    }

    /**
     * 🔬 Log signal processing details
     */
    private fun logSignalProcessingDetails(enhancedBeacon: EnhancedNordicBeacon) {
        val analytics = enhancedBeacon.analyticsData
        
        advancedLogger.logSignalProcessing(
            originalRssi = enhancedBeacon.originalBeacon.signalStrength.rssi,
            filteredRssi = analytics.filteredRssi,
            distanceResult = analytics.enhancedDistance,
            qualityScore = analytics.qualityScore
        )
    }
}
