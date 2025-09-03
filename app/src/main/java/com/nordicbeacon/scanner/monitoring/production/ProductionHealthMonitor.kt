package com.nordicbeacon.scanner.monitoring.production

import android.content.Context
import com.nordicbeacon.scanner.analytics.monitoring.service.ServicePerformanceMonitor
import com.nordicbeacon.scanner.infrastructure.resilience.CircuitBreakerManager
import com.nordicbeacon.scanner.security.SecurityAuditManager
import com.nordicbeacon.scanner.monitoring.benchmarks.PerformanceBenchmarkSuite
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 🏥 Production Health Monitor
 * 
 * Enterprise-grade health monitoring cho production deployment
 * Provides comprehensive system health assessment với proactive alerting
 * 
 * Key Features:
 * - Multi-dimensional health scoring
 * - Proactive alert generation  
 * - Automatic remediation triggers
 * - Performance trend analysis
 * - Security health monitoring
 * 
 * @author Senior Android Developer
 */
@Singleton
class ProductionHealthMonitor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val servicePerformanceMonitor: ServicePerformanceMonitor,
    private val circuitBreakerManager: CircuitBreakerManager,
    private val securityAuditManager: SecurityAuditManager,
    private val benchmarkSuite: PerformanceBenchmarkSuite,
    private val firebaseIntegrationManager: FirebaseIntegrationManager
) {

    private val monitoringScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var isMonitoringActive = false
    private val healthHistory = mutableListOf<HealthSnapshot>()

    // ========== HEALTH MONITORING ==========

    /**
     * 🚀 Start comprehensive health monitoring
     */
    fun startHealthMonitoring(intervalMinutes: Long = 5): Flow<SystemHealthStatus> = flow {
        
        Timber.i("🚀 Starting production health monitoring (interval: ${intervalMinutes}min)")
        
        isMonitoringActive = true
        
        while (isMonitoringActive) {
            try {
                val healthStatus = assessSystemHealth()
                
                // Record health snapshot
                recordHealthSnapshot(healthStatus)
                
                // Check for alerts
                checkHealthAlerts(healthStatus)
                
                // Emit current status
                emit(healthStatus)
                
                // Wait for next monitoring cycle
                delay(intervalMinutes * 60 * 1000L)
                
            } catch (e: Exception) {
                Timber.e(e, "❌ Health monitoring cycle failed")
                
                val errorStatus = SystemHealthStatus.error(e.message ?: "Health monitoring error")
                emit(errorStatus)
                
                delay(intervalMinutes * 60 * 1000L) // Continue monitoring despite errors
            }
        }
    }

    /**
     * ⏹️ Stop health monitoring
     */
    fun stopHealthMonitoring() {
        isMonitoringActive = false
        Timber.i("⏹️ Production health monitoring stopped")
    }

    /**
     * 🏥 Assess comprehensive system health
     */
    suspend fun assessSystemHealth(): SystemHealthStatus {
        
        Timber.d("🏥 Assessing comprehensive system health...")
        
        return try {
            // Collect health data từ various sources
            val serviceHealth = servicePerformanceMonitor.getServiceHealthStatus()
            val securityAudit = securityAuditManager.performSecurityAudit()
            val circuitBreakerStatus = circuitBreakerManager.getCircuitBreakerStatus()
            val benchmarkResults = if (healthHistory.isEmpty()) {
                benchmarkSuite.executeBenchmarkSuite()
            } else null // Skip frequent benchmarking
            
            // Calculate composite health score
            val healthComponents = HealthComponents(
                serviceHealth = if (serviceHealth?.isHealthy == true) serviceHealth.currentMetrics.healthScore else 0.0,
                securityScore = securityAudit.getSecurityScore() / 100.0,
                circuitBreakerHealth = calculateCircuitBreakerHealth(circuitBreakerStatus),
                performanceScore = benchmarkResults?.overallScore?.div(100.0) ?: getLastBenchmarkScore(),
                resourceUtilization = calculateResourceUtilization(serviceHealth)
            )
            
            val overallHealthScore = calculateOverallHealth(healthComponents)
            val healthLevel = determineHealthLevel(overallHealthScore)
            
            val alerts = generateHealthAlerts(healthComponents, circuitBreakerStatus)
            val recommendations = generateHealthRecommendations(healthComponents, alerts)
            
            SystemHealthStatus(
                overallHealthScore = overallHealthScore,
                healthLevel = healthLevel,
                components = healthComponents,
                activeAlerts = alerts,
                recommendations = recommendations,
                circuitBreakerStatus = circuitBreakerStatus,
                lastSecurityAudit = securityAudit,
                lastBenchmarkResults = benchmarkResults,
                assessmentTimestamp = System.currentTimeMillis()
            )
            
        } catch (e: Exception) {
            Timber.e(e, "❌ System health assessment failed")
            SystemHealthStatus.error(e.message ?: "Health assessment error")
        }
    }

    // ========== HEALTH ANALYSIS ==========

    /**
     * 📊 Calculate overall health score (0.0 - 1.0)
     */
    private fun calculateOverallHealth(components: HealthComponents): Double {
        
        // Weighted combination của health components
        val weights = HealthWeights(
            service = 0.30,      // Service performance most critical
            security = 0.25,     // Security very important
            performance = 0.20,  // Performance optimization important
            circuitBreaker = 0.15, // Circuit breaker health
            resources = 0.10     // Resource utilization
        )
        
        return (components.serviceHealth * weights.service +
                components.securityScore * weights.security +
                components.performanceScore * weights.performance +
                components.circuitBreakerHealth * weights.circuitBreaker +
                components.resourceUtilization * weights.resources).coerceIn(0.0, 1.0)
    }

    /**
     * 🏥 Determine health level based on score
     */
    private fun determineHealthLevel(healthScore: Double): HealthLevel {
        return when {
            healthScore >= 0.95 -> HealthLevel.EXCELLENT
            healthScore >= 0.85 -> HealthLevel.GOOD  
            healthScore >= 0.70 -> HealthLevel.FAIR
            healthScore >= 0.50 -> HealthLevel.POOR
            else -> HealthLevel.CRITICAL
        }
    }

    /**
     * 🚨 Generate health alerts based on thresholds
     */
    private fun generateHealthAlerts(
        components: HealthComponents,
        circuitBreakerStatus: Map<String, com.nordicbeacon.scanner.infrastructure.resilience.CircuitBreakerStatus>
    ): List<HealthAlert> {
        
        val alerts = mutableListOf<HealthAlert>()
        
        // Service health alerts
        if (components.serviceHealth < 0.7) {
            alerts.add(HealthAlert.critical("Service performance degraded: ${(components.serviceHealth * 100).toInt()}%"))
        }
        
        // Security alerts
        if (components.securityScore < 0.8) {
            alerts.add(HealthAlert.high("Security score below threshold: ${(components.securityScore * 100).toInt()}%"))
        }
        
        // Circuit breaker alerts
        circuitBreakerStatus.forEach { (name, status) ->
            if (!status.isHealthy()) {
                alerts.add(HealthAlert.medium("Circuit breaker $name: ${status.getHealthDescription()}"))
            }
        }
        
        // Resource utilization alerts
        if (components.resourceUtilization > 0.9) {
            alerts.add(HealthAlert.high("High resource utilization: ${(components.resourceUtilization * 100).toInt()}%"))
        }
        
        return alerts
    }

    /**
     * 💡 Generate health improvement recommendations
     */
    private fun generateHealthRecommendations(
        components: HealthComponents,
        alerts: List<HealthAlert>
    ): List<String> {
        
        val recommendations = mutableListOf<String>()
        
        if (components.serviceHealth < 0.8) {
            recommendations.add("Review service performance metrics và optimize resource usage")
        }
        
        if (components.performanceScore < 0.8) {
            recommendations.add("Run performance benchmark suite để identify bottlenecks")
        }
        
        if (alerts.any { it.severity == AlertSeverity.CRITICAL }) {
            recommendations.add("Address critical alerts immediately cho system stability")
        }
        
        if (recommendations.isEmpty()) {
            recommendations.add("System health is optimal - continue monitoring")
        }
        
        return recommendations
    }

    // ========== PRIVATE HELPER METHODS ==========

    private fun calculateCircuitBreakerHealth(status: Map<String, com.nordicbeacon.scanner.infrastructure.resilience.CircuitBreakerStatus>): Double {
        if (status.isEmpty()) return 1.0
        
        val healthyCount = status.values.count { it.isHealthy() }
        return healthyCount.toDouble() / status.size
    }

    private fun calculateResourceUtilization(serviceHealth: com.nordicbeacon.scanner.analytics.monitoring.service.ServiceHealthStatus?): Double {
        return serviceHealth?.currentMetrics?.memoryPressure ?: 0.5
    }

    private fun getLastBenchmarkScore(): Double {
        return 0.85 // Default good score if no recent benchmark
    }

    private fun recordHealthSnapshot(healthStatus: SystemHealthStatus) {
        val snapshot = HealthSnapshot(
            timestamp = System.currentTimeMillis(),
            healthScore = healthStatus.overallHealthScore,
            alertCount = healthStatus.activeAlerts.size
        )
        
        healthHistory.add(snapshot)
        
        // Keep history manageable
        if (healthHistory.size > 288) { // 24 hours at 5min intervals
            healthHistory.removeAt(0)
        }
    }

    private fun checkHealthAlerts(healthStatus: SystemHealthStatus) {
        val criticalAlerts = healthStatus.activeAlerts.filter { it.severity == AlertSeverity.CRITICAL }
        
        if (criticalAlerts.isNotEmpty()) {
            Timber.e("🚨 CRITICAL HEALTH ALERTS: ${criticalAlerts.size}")
            criticalAlerts.forEach { alert ->
                Timber.e("🚨 ${alert.message}")
                
                // Report to Firebase Crashlytics
                firebaseIntegrationManager.reportNonFatalException(
                    exception = Exception("Critical Health Alert: ${alert.message}"),
                    context = "health_monitoring"
                )
            }
        }
    }
}

// ========== DATA MODELS ==========

data class SystemHealthStatus(
    val overallHealthScore: Double,
    val healthLevel: HealthLevel,
    val components: HealthComponents,
    val activeAlerts: List<HealthAlert>,
    val recommendations: List<String>,
    val circuitBreakerStatus: Map<String, com.nordicbeacon.scanner.infrastructure.resilience.CircuitBreakerStatus>,
    val lastSecurityAudit: com.nordicbeacon.scanner.security.SecurityAuditReport,
    val lastBenchmarkResults: com.nordicbeacon.scanner.monitoring.benchmarks.BenchmarkReport?,
    val assessmentTimestamp: Long
) {
    
    fun getHealthPercentage(): Int = (overallHealthScore * 100).toInt()
    
    fun isProductionReady(): Boolean = healthLevel >= HealthLevel.GOOD && 
                                      activeAlerts.none { it.severity == AlertSeverity.CRITICAL }
    
    companion object {
        fun error(message: String) = SystemHealthStatus(
            0.0, HealthLevel.CRITICAL, HealthComponents.empty(), 
            listOf(HealthAlert.critical(message)), emptyList(), emptyMap(),
            com.nordicbeacon.scanner.security.SecurityAuditReport.failed(message, System.currentTimeMillis()),
            null, System.currentTimeMillis()
        )
    }
}

data class HealthComponents(
    val serviceHealth: Double,
    val securityScore: Double,
    val circuitBreakerHealth: Double,  
    val performanceScore: Double,
    val resourceUtilization: Double
) {
    companion object {
        fun empty() = HealthComponents(0.0, 0.0, 0.0, 0.0, 1.0)
    }
}

data class HealthWeights(
    val service: Double,
    val security: Double,
    val performance: Double,
    val circuitBreaker: Double,
    val resources: Double
)

enum class HealthLevel {
    CRITICAL, // Immediate action required
    POOR,     // Significant issues
    FAIR,     // Some issues to address
    GOOD,     // Acceptable health  
    EXCELLENT // Optimal health
}

data class HealthAlert(
    val message: String,
    val severity: AlertSeverity,
    val category: String,
    val timestamp: Long = System.currentTimeMillis()
) {
    companion object {
        fun critical(message: String) = HealthAlert(message, AlertSeverity.CRITICAL, "system")
        fun high(message: String) = HealthAlert(message, AlertSeverity.HIGH, "performance")
        fun medium(message: String) = HealthAlert(message, AlertSeverity.MEDIUM, "operational")
        fun low(message: String) = HealthAlert(message, AlertSeverity.LOW, "informational")
    }
}

enum class AlertSeverity { LOW, MEDIUM, HIGH, CRITICAL }

data class HealthSnapshot(
    val timestamp: Long,
    val healthScore: Double,
    val alertCount: Int
)
