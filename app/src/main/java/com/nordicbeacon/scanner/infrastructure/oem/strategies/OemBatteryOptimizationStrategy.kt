package com.nordicbeacon.scanner.infrastructure.oem.strategies

import android.content.Context
import com.nordicbeacon.scanner.infrastructure.oem.models.BatteryOptimizationResult
import com.nordicbeacon.scanner.infrastructure.oem.models.DeviceInfo
import com.nordicbeacon.scanner.infrastructure.oem.models.OptimizationStatus
import com.nordicbeacon.scanner.infrastructure.oem.models.OptimizationStrategy

/**
 * 🔋 OEM Battery Optimization Strategy Interface
 * 
 * Defines contract cho OEM-specific battery optimization implementations
 * Following Strategy pattern cho extensible OEM support
 * 
 * Each OEM implementation provides:
 * - Device compatibility checking
 * - Optimization status detection  
 * - User guidance cho whitelist process
 * - Intent creation cho direct settings access
 * 
 * @author Senior Android Developer
 */
interface OemBatteryOptimizationStrategy {

    /**
     * 🏷️ OEM identifier (e.g., "samsung", "xiaomi", "huawei")
     */
    val oemName: String
    
    /**
     * 📱 Supported device criteria
     */
    val supportedCriteria: com.nordicbeacon.scanner.infrastructure.oem.models.DeviceMatchCriteria

    /**
     * 🔍 Check if strategy supports current device
     * 
     * @param deviceInfo Current device information
     * @return true if this strategy can handle the device
     */
    fun isSupported(deviceInfo: DeviceInfo): Boolean

    /**
     * 📊 Check current battery optimization status
     * 
     * @param context Application context
     * @return Current optimization status cho app
     */
    suspend fun checkOptimizationStatus(context: Context): OptimizationStatus

    /**
     * 🎯 Get optimization strategy information
     * 
     * @return Strategy details including user instructions
     */
    fun getOptimizationStrategy(): OptimizationStrategy

    /**
     * 🚀 Attempt to guide user through optimization process
     * 
     * @param context Application context  
     * @return Result of optimization attempt
     */
    suspend fun requestOptimization(context: Context): BatteryOptimizationResult

    /**
     * 📚 Get user education content cho this OEM
     * 
     * @return Educational content explaining why optimization needed
     */
    fun getUserEducationContent(): OemEducationContent

    /**
     * 🔍 Get detection confidence score (0-100)
     * 
     * @param deviceInfo Device to analyze
     * @return Confidence score that this strategy will work
     */
    fun getDetectionConfidence(deviceInfo: DeviceInfo): Int
}

/**
 * 📚 OEM Education Content
 */
data class OemEducationContent(
    val title: String,
    val explanation: String,
    val whyNeeded: String,
    val steps: List<String>,
    val screenshots: List<String> = emptyList(),
    val videoUrl: String? = null,
    val troubleshooting: Map<String, String> = emptyMap()
)

/**
 * 🏭 Abstract Base Strategy Implementation
 * 
 * Provides common functionality cho all OEM strategies
 * Implements shared logic để reduce code duplication
 */
abstract class BaseOemStrategy : OemBatteryOptimizationStrategy {
    
    protected fun createGenericEducationContent(): OemEducationContent {
        return OemEducationContent(
            title = "$oemName Battery Optimization",
            explanation = "To ensure Nordic beacon scanning works continuously, even when your phone's screen is off, you need to configure $oemName's battery optimization settings.",
            whyNeeded = "Your $oemName device has aggressive battery optimization that can stop background apps. Whitelisting our app ensures continuous Nordic beacon detection.",
            steps = getOptimizationStrategy().userInstructions
        )
    }
    
    protected fun isIntentSafe(context: Context, intent: android.content.Intent): Boolean {
        return try {
            val packageManager = context.packageManager
            intent.resolveActivity(packageManager) != null
        } catch (e: Exception) {
            false
        }
    }
    
    protected suspend fun attemptOpenSettings(
        context: Context, 
        settingsPaths: List<com.nordicbeacon.scanner.infrastructure.oem.models.SettingPath>
    ): BatteryOptimizationResult {
        
        for (settingPath in settingsPaths) {
            try {
                val intent = settingPath.createIntent(context)
                
                if (intent != null && isIntentSafe(context, intent)) {
                    intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                    
                    return BatteryOptimizationResult.SettingsOpened(
                        settingsType = settingPath.settingName,
                        instructionsProvided = getOptimizationStrategy().userInstructions
                    )
                }
                
            } catch (e: Exception) {
                timber.log.Timber.w(e, "⚠️ Failed to open ${settingPath.settingName}")
                continue
            }
        }
        
        // All settings paths failed
        return BatteryOptimizationResult.Failed(
            reason = "Unable to open $oemName battery settings",
            fallbackInstructions = getGenericFallbackInstructions()
        )
    }
    
    protected open fun getGenericFallbackInstructions(): List<String> {
        return listOf(
            "Open device Settings app manually",
            "Navigate to Battery or Battery Optimization", 
            "Find Nordic Beacon Scanner in app list",
            "Disable optimization or add to whitelist",
            "Enable auto-start if available"
        )
    }
}
