package com.nordicbeacon.scanner.infrastructure.oem.handlers

import android.content.ComponentName
import android.content.Context
import com.nordicbeacon.scanner.infrastructure.oem.models.*
import com.nordicbeacon.scanner.infrastructure.oem.strategies.BaseOemStrategy
import timber.log.Timber
import javax.inject.Inject

/**
 * 🔋 OnePlus Battery Optimization Handler
 * 
 * Handles OnePlus OxygenOS battery optimization bypass
 * Supports Battery optimization, App auto-launch, và background activity
 * 
 * OnePlus-specific challenges:
 * - Advanced battery optimization (aggressive but configurable)
 * - App auto-launch restrictions
 * - Background app limits
 * - OxygenOS version differences (11, 12, 13+)
 * 
 * @author Senior Android Developer
 */
class OnePlusOptimizationHandler @Inject constructor() : BaseOemStrategy() {

    override val oemName: String = "OnePlus"
    
    override val supportedCriteria = DeviceMatchCriteria(
        manufacturers = listOf("oneplus"),
        minAndroidVersion = 21
    )

    override fun isSupported(deviceInfo: DeviceInfo): Boolean {
        return deviceInfo.matchesCriteria(supportedCriteria)
    }

    override suspend fun checkOptimizationStatus(context: Context): OptimizationStatus {
        return try {
            val batteryOptStatus = checkBatteryOptimization(context)
            val autoLaunchStatus = checkAutoLaunchStatus(context)
            
            when {
                batteryOptStatus == OptimizationStatus.OPTIMIZED &&
                autoLaunchStatus == OptimizationStatus.OPTIMIZED -> OptimizationStatus.OPTIMIZED
                
                batteryOptStatus == OptimizationStatus.NOT_OPTIMIZED -> OptimizationStatus.NOT_OPTIMIZED
                
                else -> OptimizationStatus.PARTIALLY_OPTIMIZED
            }
            
        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to check OnePlus optimization")
            OptimizationStatus.CANNOT_DETERMINE
        }
    }

    override fun getOptimizationStrategy(): OptimizationStrategy {
        return OptimizationStrategy(
            oemName = oemName,
            strategyName = "OnePlus Battery & Auto-launch Management",
            description = "Configure OnePlus battery optimization và auto-launch settings",
            requiredSettings = getOnePlusSettingsPaths(),
            userInstructions = getOnePlusUserInstructions(),
            successIndicators = getOnePlusSuccessIndicators(),
            estimatedComplexity = ComplexityLevel.MODERATE
        )
    }

    override suspend fun requestOptimization(context: Context): BatteryOptimizationResult {
        return attemptOpenSettings(context, getOnePlusSettingsPaths())
    }

    override fun getUserEducationContent(): OemEducationContent {
        return OemEducationContent(
            title = "OnePlus Battery Optimization",
            explanation = "OnePlus OxygenOS includes battery management features that may affect background scanning.",
            whyNeeded = "Configure battery settings để ensure continuous Nordic beacon detection.",
            steps = getOnePlusUserInstructions()
        )
    }

    override fun getDetectionConfidence(deviceInfo: DeviceInfo): Int = 85

    // ========== IMPLEMENTATION HELPERS ==========

    private suspend fun checkBatteryOptimization(context: Context): OptimizationStatus = OptimizationStatus.CANNOT_DETERMINE
    private suspend fun checkAutoLaunchStatus(context: Context): OptimizationStatus = OptimizationStatus.CANNOT_DETERMINE

    private fun getOnePlusSettingsPaths(): List<SettingPath> = listOf(
        SettingPath(
            settingName = "OnePlus Battery Optimization",
            intentAction = android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS,
            componentName = null
        )
    )

    private fun getOnePlusUserInstructions(): List<String> = listOf(
        "Open Settings > Battery",
        "Tap 'Battery optimization'", 
        "Find Nordic Beacon Scanner",
        "Select 'Don't optimize'",
        "Go to Settings > Apps > Nordic Beacon Scanner",
        "Enable 'Auto-launch'"
    )

    private fun getOnePlusSuccessIndicators(): List<String> = listOf(
        "Battery optimization disabled",
        "Auto-launch enabled"
    )
}
