package com.nordicbeacon.scanner.infrastructure.oem.coordination

import android.content.Context
import com.nordicbeacon.scanner.infrastructure.oem.detection.DeviceDetectionFactory
import com.nordicbeacon.scanner.infrastructure.oem.models.BatteryOptimizationResult
import com.nordicbeacon.scanner.infrastructure.oem.models.DeviceInfo
import com.nordicbeacon.scanner.infrastructure.oem.models.OptimizationStatus
import com.nordicbeacon.scanner.infrastructure.oem.strategies.OemBatteryOptimizationStrategy
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 🎯 Battery Optimization Coordinator
 * 
 * Main orchestration class cho OEM-specific battery optimization
 * Coordinates device detection, strategy selection, và optimization execution
 * 
 * Key Responsibilities:
 * - Automatic OEM detection và strategy selection
 * - Fallback strategies cho unsupported devices
 * - Result aggregation và user guidance
 * - Performance monitoring và analytics
 * 
 * @author Senior Android Developer
 */
@Singleton
class BatteryOptimizationCoordinator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val deviceDetectionFactory: DeviceDetectionFactory,
    private val strategyRegistry: OemStrategyRegistry
) {

    private var cachedDeviceInfo: DeviceInfo? = null
    private var cachedStrategy: OemBatteryOptimizationStrategy? = null

    // ========== PUBLIC API ==========

    /**
     * 🔍 Analyze current device và determine optimization requirements
     */
    suspend fun analyzeDeviceOptimizationNeeds(): DeviceOptimizationAnalysis {
        
        Timber.i("🔍 Analyzing device optimization needs...")
        
        val deviceInfo = getDeviceInfo()
        val detectionResult = deviceDetectionFactory.detectOemType(deviceInfo)
        val strategy = strategyRegistry.getStrategy(detectionResult.oemType)
        
        val currentStatus = strategy?.checkOptimizationStatus(context) ?: OptimizationStatus.CANNOT_DETERMINE
        
        val analysis = DeviceOptimizationAnalysis(
            deviceInfo = deviceInfo,
            detectionResult = detectionResult,
            strategy = strategy,
            currentStatus = currentStatus,
            recommendedActions = determineRecommendedActions(currentStatus, strategy),
            urgencyLevel = determineUrgencyLevel(currentStatus, detectionResult),
            estimatedImpact = estimateOptimizationImpact(detectionResult)
        )
        
        Timber.i("📊 Analysis complete: ${analysis.getSummary()}")
        
        return analysis
    }

    /**
     * 🚀 Execute battery optimization cho current device
     */
    suspend fun executeOptimization(): BatteryOptimizationResult {
        
        Timber.i("🚀 Executing battery optimization...")
        
        return try {
            val analysis = analyzeDeviceOptimizationNeeds()
            
            // Check if optimization is needed
            if (analysis.currentStatus == OptimizationStatus.OPTIMIZED) {
                Timber.i("✅ Device already optimized")
                return BatteryOptimizationResult.AlreadyOptimized
            }
            
            // Execute strategy if available
            val strategy = analysis.strategy
            if (strategy != null) {
                
                Timber.i("🎯 Executing ${strategy.oemName} optimization strategy")
                
                val result = strategy.requestOptimization(context)
                
                // Log result cho analytics
                logOptimizationAttempt(analysis, result)
                
                result
                
            } else {
                Timber.w("⚠️ No strategy available cho ${analysis.detectionResult.oemType}")
                
                BatteryOptimizationResult.UnsupportedOem(
                    deviceInfo = analysis.deviceInfo.getDeviceSummary(),
                    genericInstructions = getGenericOptimizationInstructions()
                )
            }
            
        } catch (e: Exception) {
            Timber.e(e, "❌ Battery optimization execution failed")
            
            BatteryOptimizationResult.Failed(
                reason = "Optimization execution failed: ${e.message}",
                fallbackInstructions = getGenericOptimizationInstructions(),
                cause = e
            )
        }
    }

    /**
     * 📊 Check current optimization status
     */
    suspend fun getCurrentOptimizationStatus(): OptimizationStatus {
        
        return try {
            val analysis = analyzeDeviceOptimizationNeeds()
            analysis.currentStatus
            
        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to get optimization status")
            OptimizationStatus.CANNOT_DETERMINE
        }
    }

    /**
     * 🔄 Force refresh device analysis (clear cache)
     */
    fun refreshDeviceAnalysis() {
        Timber.i("🔄 Refreshing device analysis cache...")
        cachedDeviceInfo = null
        cachedStrategy = null
    }

    /**
     * 📚 Get educational content cho current device
     */
    fun getEducationalContent(): com.nordicbeacon.scanner.infrastructure.oem.strategies.OemEducationContent? {
        return try {
            val deviceInfo = getDeviceInfo()
            val detectionResult = deviceDetectionFactory.detectOemType(deviceInfo)
            val strategy = strategyRegistry.getStrategy(detectionResult.oemType)
            
            strategy?.getUserEducationContent()
            
        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to get educational content")
            null
        }
    }

    // ========== PRIVATE IMPLEMENTATION ==========

    /**
     * 📱 Get device information với caching
     */
    private fun getDeviceInfo(): DeviceInfo {
        return cachedDeviceInfo ?: DeviceInfo().also { cachedDeviceInfo = it }
    }

    /**
     * 📋 Determine recommended actions based on status
     */
    private fun determineRecommendedActions(
        status: OptimizationStatus,
        strategy: OemBatteryOptimizationStrategy?
    ): List<String> {
        
        return when (status) {
            OptimizationStatus.OPTIMIZED -> {
                listOf("✅ Device is properly optimized cho Nordic beacon scanning")
            }
            
            OptimizationStatus.NOT_OPTIMIZED -> {
                strategy?.getOptimizationStrategy()?.userInstructions ?: getGenericOptimizationInstructions()
            }
            
            OptimizationStatus.PARTIALLY_OPTIMIZED -> {
                listOf(
                    "⚠️ Partial optimization detected",
                    "Review all battery management settings",
                    "Ensure background activity is fully enabled"
                ) + (strategy?.getOptimizationStrategy()?.userInstructions?.take(3) ?: emptyList())
            }
            
            OptimizationStatus.CANNOT_DETERMINE -> {
                listOf(
                    "❓ Unable to determine current status",
                    "Follow manual optimization steps",
                    "Monitor app behavior cho background operation"
                )
            }
            
            OptimizationStatus.UNKNOWN -> {
                listOf(
                    "🔍 Optimization status unknown",
                    "Perform manual configuration check"
                )
            }
        }
    }

    /**
     * 🚨 Determine urgency level cho optimization
     */
    private fun determineUrgencyLevel(
        status: OptimizationStatus,
        detectionResult: com.nordicbeacon.scanner.infrastructure.oem.detection.OemDetectionResult
    ): UrgencyLevel {
        
        return when {
            status == OptimizationStatus.OPTIMIZED -> UrgencyLevel.NONE
            
            detectionResult.oemType.hasAggressiveOptimization() && 
            status == OptimizationStatus.NOT_OPTIMIZED -> UrgencyLevel.CRITICAL
            
            status == OptimizationStatus.NOT_OPTIMIZED -> UrgencyLevel.HIGH
            
            status == OptimizationStatus.PARTIALLY_OPTIMIZED -> UrgencyLevel.MODERATE
            
            else -> UrgencyLevel.LOW
        }
    }

    /**
     * 📊 Estimate optimization impact
     */
    private fun estimateOptimizationImpact(
        detectionResult: com.nordicbeacon.scanner.infrastructure.oem.detection.OemDetectionResult
    ): OptimizationImpact {
        
        return when (detectionResult.oemType) {
            com.nordicbeacon.scanner.infrastructure.oem.detection.OemType.XIAOMI,
            com.nordicbeacon.scanner.infrastructure.oem.detection.OemType.HUAWEI,
            com.nordicbeacon.scanner.infrastructure.oem.detection.OemType.OPPO,
            com.nordicbeacon.scanner.infrastructure.oem.detection.OemType.VIVO -> OptimizationImpact.CRITICAL
            
            com.nordicbeacon.scanner.infrastructure.oem.detection.OemType.SAMSUNG,
            com.nordicbeacon.scanner.infrastructure.oem.detection.OemType.ONEPLUS -> OptimizationImpact.HIGH
            
            com.nordicbeacon.scanner.infrastructure.oem.detection.OemType.GOOGLE,
            com.nordicbeacon.scanner.infrastructure.oem.detection.OemType.NOTHING,
            com.nordicbeacon.scanner.infrastructure.oem.detection.OemType.SONY -> OptimizationImpact.MODERATE
            
            else -> OptimizationImpact.LOW
        }
    }

    /**
     * 📝 Log optimization attempt cho analytics
     */
    private fun logOptimizationAttempt(
        analysis: DeviceOptimizationAnalysis,
        result: BatteryOptimizationResult
    ) {
        
        val logData = mapOf(
            "oem_type" to analysis.detectionResult.oemType.name,
            "confidence" to analysis.detectionResult.confidence.toString(),
            "previous_status" to analysis.currentStatus.name,
            "result_type" to result::class.simpleName,
            "urgency_level" to analysis.urgencyLevel.name
        )
        
        Timber.i("📊 Optimization attempt logged: $logData")
        
        // TODO: Send to analytics service (Firebase Analytics)
    }

    /**
     * 📚 Get generic optimization instructions cho unsupported OEMs
     */
    private fun getGenericOptimizationInstructions(): List<String> {
        return listOf(
            "Open device Settings",
            "Navigate to Battery hoặc Power management",
            "Look cho Battery optimization hoặc App power management",
            "Find Nordic Beacon Scanner in app list",
            "Disable battery optimization hoặc select 'Don't optimize'",
            "Enable background activity if option available",
            "Check cho auto-start hoặc startup management settings"
        )
    }
}

/**
 * 📊 Device Optimization Analysis Result
 */
data class DeviceOptimizationAnalysis(
    val deviceInfo: DeviceInfo,
    val detectionResult: com.nordicbeacon.scanner.infrastructure.oem.detection.OemDetectionResult,
    val strategy: OemBatteryOptimizationStrategy?,
    val currentStatus: OptimizationStatus,
    val recommendedActions: List<String>,
    val urgencyLevel: UrgencyLevel,
    val estimatedImpact: OptimizationImpact
) {
    
    /**
     * 📋 Get analysis summary cho display
     */
    fun getSummary(): String {
        return """
            Device: ${detectionResult.getDetectionSummary()}
            Status: $currentStatus
            Urgency: $urgencyLevel  
            Impact: $estimatedImpact
            Strategy Available: ${strategy != null}
        """.trimIndent()
    }
    
    /**
     * 🚨 Check if immediate action required
     */
    fun requiresImmediateAction(): Boolean {
        return urgencyLevel == UrgencyLevel.CRITICAL && 
               currentStatus == OptimizationStatus.NOT_OPTIMIZED
    }
    
    /**
     * 📱 Check if strategy is available cho this device
     */
    fun hasOptimizationStrategy(): Boolean = strategy != null
}

/**
 * 🚨 Optimization Urgency Levels
 */
enum class UrgencyLevel(val description: String) {
    NONE("No optimization needed"),
    LOW("Optional optimization"),  
    MODERATE("Recommended optimization"),
    HIGH("Optimization strongly recommended"),
    CRITICAL("Optimization required cho app functionality")
}

/**
 * 📊 Optimization Impact Assessment
 */
enum class OptimizationImpact(val description: String) {
    LOW("Minimal impact on app performance"),
    MODERATE("Moderate improvement in background operation"),
    HIGH("Significant improvement in app reliability"), 
    CRITICAL("Essential cho app functionality")
}
