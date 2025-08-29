package com.nordicbeacon.scanner.infrastructure.oem.handlers

import android.content.Context
import com.nordicbeacon.scanner.infrastructure.oem.models.*
import com.nordicbeacon.scanner.infrastructure.oem.strategies.BaseOemStrategy
import timber.log.Timber
import javax.inject.Inject

/**
 * 🔋 Google Pixel Battery Optimization Handler
 * 
 * Handles Stock Android (Pixel) battery optimization
 * Simplest optimization strategy due to standard Android behavior
 * 
 * Google Pixel advantages:
 * - Standard Android battery optimization behavior
 * - No aggressive OEM modifications
 * - Predictable settings paths
 * - Better Doze mode compatibility
 * 
 * @author Senior Android Developer
 */
class GooglePixelOptimizationHandler @Inject constructor() : BaseOemStrategy() {

    override val oemName: String = "Google"
    
    override val supportedCriteria = DeviceMatchCriteria(
        manufacturers = listOf("google"),
        minAndroidVersion = 21
    )

    override fun isSupported(deviceInfo: DeviceInfo): Boolean {
        return deviceInfo.matchesCriteria(supportedCriteria)
    }

    override suspend fun checkOptimizationStatus(context: Context): OptimizationStatus {
        return try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                val powerManager = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
                val isIgnored = powerManager.isIgnoringBatteryOptimizations(context.packageName)
                
                if (isIgnored) OptimizationStatus.OPTIMIZED else OptimizationStatus.NOT_OPTIMIZED
            } else {
                OptimizationStatus.OPTIMIZED // No optimization on Android <6.0
            }
        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to check Pixel optimization status")
            OptimizationStatus.CANNOT_DETERMINE
        }
    }

    override fun getOptimizationStrategy(): OptimizationStrategy {
        return OptimizationStrategy(
            oemName = oemName,
            strategyName = "Stock Android Battery Optimization",
            description = "Configure standard Android battery optimization settings",
            requiredSettings = getPixelSettingsPaths(),
            userInstructions = getPixelUserInstructions(),
            successIndicators = getPixelSuccessIndicators(),
            estimatedComplexity = ComplexityLevel.SIMPLE // Pixel is straightforward
        )
    }

    override suspend fun requestOptimization(context: Context): BatteryOptimizationResult {
        return try {
            val intent = android.content.Intent().apply {
                action = android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            if (isIntentSafe(context, intent)) {
                context.startActivity(intent)
                
                BatteryOptimizationResult.SettingsOpened(
                    settingsType = "Android Battery Optimization",
                    instructionsProvided = getPixelUserInstructions()
                )
            } else {
                BatteryOptimizationResult.Failed(
                    reason = "Cannot open battery optimization settings",
                    fallbackInstructions = getPixelFallbackInstructions()
                )
            }
            
        } catch (e: Exception) {
            Timber.e(e, "❌ Pixel optimization failed")
            BatteryOptimizationResult.Failed(
                reason = "Failed to open settings",
                fallbackInstructions = getPixelFallbackInstructions(),
                cause = e
            )
        }
    }

    override fun getUserEducationContent(): OemEducationContent {
        return OemEducationContent(
            title = "Google Pixel Battery Optimization",
            explanation = "Google Pixel devices use standard Android battery optimization.",
            whyNeeded = "Configure battery settings để allow Nordic beacon scanning in background.",
            steps = getPixelUserInstructions()
        )
    }

    override fun getDetectionConfidence(deviceInfo: DeviceInfo): Int = 98 // Pixel detection very reliable

    // ========== CONFIGURATION METHODS ==========

    private fun getPixelSettingsPaths(): List<SettingPath> {
        return listOf(
            SettingPath(
                settingName = "Battery Optimization",
                intentAction = android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS,
                componentName = null
            )
        )
    }

    private fun getPixelUserInstructions(): List<String> {
        return listOf(
            "Find Nordic Beacon Scanner in app list",
            "Tap on the app",
            "Select 'Don't optimize'", 
            "Tap 'Done' to save"
        )
    }

    private fun getPixelSuccessIndicators(): List<String> {
        return listOf(
            "App shows 'Not optimized' status",
            "Nordic Beacon Scanner not in optimized list"
        )
    }

    private fun getPixelFallbackInstructions(): List<String> {
        return listOf(
            "Settings > Apps & notifications",
            "Advanced > Special app access",
            "Battery optimization",
            "Find Nordic Beacon Scanner"
        )
    }
}
