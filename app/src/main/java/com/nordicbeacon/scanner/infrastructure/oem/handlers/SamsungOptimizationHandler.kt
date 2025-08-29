package com.nordicbeacon.scanner.infrastructure.oem.handlers

import android.content.ComponentName
import android.content.Context
import com.nordicbeacon.scanner.infrastructure.oem.models.*
import com.nordicbeacon.scanner.infrastructure.oem.strategies.BaseOemStrategy
import com.nordicbeacon.scanner.infrastructure.oem.detection.OemType
import timber.log.Timber
import javax.inject.Inject

/**
 * 🔋 Samsung Battery Optimization Handler
 * 
 * Handles Samsung One UI battery optimization bypass
 * Supports Device Care, Sleeping Apps, và Battery optimization settings
 * 
 * Samsung-specific challenges:
 * - Device Care automatic app optimization
 * - Sleeping Apps feature (One UI 4.0+)  
 * - Battery optimization với different paths per One UI version
 * - Game Optimizer conflicts với background apps
 * 
 * @author Senior Android Developer
 */
class SamsungOptimizationHandler @Inject constructor() : BaseOemStrategy() {

    override val oemName: String = "Samsung"
    
    override val supportedCriteria = DeviceMatchCriteria(
        manufacturers = listOf("samsung"),
        minAndroidVersion = 21 // Support Galaxy devices từ Android 5.0+
    )

    // ========== CORE IMPLEMENTATION ==========

    override fun isSupported(deviceInfo: DeviceInfo): Boolean {
        return deviceInfo.matchesCriteria(supportedCriteria) &&
               hasOneUiFeatures(deviceInfo)
    }

    override suspend fun checkOptimizationStatus(context: Context): OptimizationStatus {
        return try {
            // Check multiple Samsung optimization states
            val deviceCareStatus = checkDeviceCareOptimization(context)
            val batteryOptimizationStatus = checkBatteryOptimization(context)
            val sleepingAppsStatus = checkSleepingAppsStatus(context)
            
            when {
                deviceCareStatus == OptimizationStatus.OPTIMIZED &&
                batteryOptimizationStatus == OptimizationStatus.OPTIMIZED &&
                sleepingAppsStatus == OptimizationStatus.OPTIMIZED -> OptimizationStatus.OPTIMIZED
                
                deviceCareStatus == OptimizationStatus.NOT_OPTIMIZED ||
                batteryOptimizationStatus == OptimizationStatus.NOT_OPTIMIZED ||
                sleepingAppsStatus == OptimizationStatus.NOT_OPTIMIZED -> OptimizationStatus.NOT_OPTIMIZED
                
                else -> OptimizationStatus.PARTIALLY_OPTIMIZED
            }
            
        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to check Samsung optimization status")
            OptimizationStatus.CANNOT_DETERMINE
        }
    }

    override fun getOptimizationStrategy(): OptimizationStrategy {
        return OptimizationStrategy(
            oemName = oemName,
            strategyName = "Samsung One UI Battery Optimization",
            description = "Configure Samsung Device Care và Battery settings cho continuous beacon scanning",
            requiredSettings = getSamsungSettingsPaths(),
            userInstructions = getSamsungUserInstructions(),
            successIndicators = getSamsungSuccessIndicators(),
            estimatedComplexity = ComplexityLevel.MODERATE
        )
    }

    override suspend fun requestOptimization(context: Context): BatteryOptimizationResult {
        Timber.i("🔋 Starting Samsung battery optimization process...")
        
        return try {
            // Attempt multiple Samsung-specific settings paths
            val settingsPaths = getSamsungSettingsPaths()
            
            // Try Device Care first (most comprehensive)
            val deviceCareResult = attemptOpenDeviceCare(context)
            if (deviceCareResult is BatteryOptimizationResult.SettingsOpened) {
                return deviceCareResult
            }
            
            // Fallback to battery optimization settings
            val batteryResult = attemptOpenBatteryOptimization(context)
            if (batteryResult is BatteryOptimizationResult.SettingsOpened) {
                return batteryResult
            }
            
            // Final fallback to generic settings
            attemptOpenSettings(context, settingsPaths)
            
        } catch (e: Exception) {
            Timber.e(e, "❌ Samsung optimization request failed")
            BatteryOptimizationResult.Failed(
                reason = "Failed to open Samsung battery settings",
                fallbackInstructions = getSamsungFallbackInstructions(),
                cause = e
            )
        }
    }

    override fun getUserEducationContent(): OemEducationContent {
        return OemEducationContent(
            title = "Samsung Battery Optimization Setup",
            explanation = "Samsung One UI has multiple battery management features that may stop Nordic beacon scanning when your screen is off.",
            whyNeeded = """
                Samsung devices use Device Care and Sleeping Apps features to manage battery usage. 
                These can prevent our Nordic beacon scanner from running continuously in the background.
                Configuring these settings ensures reliable beacon detection.
            """.trimIndent(),
            steps = getSamsungUserInstructions(),
            troubleshooting = mapOf(
                "App still stops working" to "Check if app is in Sleeping Apps list",
                "Settings won't open" to "Try opening Settings > Device Care manually", 
                "No Device Care option" to "Look for Battery > More battery settings"
            )
        )
    }

    override fun getDetectionConfidence(deviceInfo: DeviceInfo): Int {
        var confidence = 70 // Base confidence cho Samsung detection
        
        // Boost confidence với specific markers
        if (deviceInfo.standardizedManufacturer == "samsung") confidence += 20
        if (hasOneUiFeatures(deviceInfo)) confidence += 10
        if (deviceInfo.model.startsWith("SM-")) confidence += 5 // Samsung model prefix
        
        return minOf(100, confidence)
    }

    // ========== SAMSUNG-SPECIFIC IMPLEMENTATIONS ==========

    /**
     * 🏥 Attempt to open Samsung Device Care
     */
    private suspend fun attemptOpenDeviceCare(context: Context): BatteryOptimizationResult {
        
        val deviceCareIntents = listOf(
            // One UI 4.0+ Device Care
            ComponentName("com.samsung.android.lool", "com.samsung.android.sm.ui.battery.BatteryActivity"),
            
            // One UI 3.x Device Care  
            ComponentName("com.samsung.android.sm", "com.samsung.android.sm.ui.battery.BatteryActivity"),
            
            // Legacy Samsung Smart Manager
            ComponentName("com.samsung.android.sm", "com.samsung.android.sm.ui.cstyleboard.SmartManagerMainActivity")
        )
        
        for (componentName in deviceCareIntents) {
            try {
                val intent = android.content.Intent().apply {
                    component = componentName
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                
                if (isIntentSafe(context, intent)) {
                    context.startActivity(intent)
                    
                    return BatteryOptimizationResult.SettingsOpened(
                        settingsType = "Samsung Device Care",
                        instructionsProvided = getSamsungDeviceCareInstructions()
                    )
                }
                
            } catch (e: Exception) {
                Timber.d("❌ Failed Device Care intent: $componentName")
                continue
            }
        }
        
        return BatteryOptimizationResult.Failed(
            reason = "Cannot open Samsung Device Care",
            fallbackInstructions = getSamsungFallbackInstructions()
        )
    }

    /**
     * 🔋 Attempt to open battery optimization settings
     */
    private suspend fun attemptOpenBatteryOptimization(context: Context): BatteryOptimizationResult {
        
        try {
            // Samsung battery optimization intent
            val intent = android.content.Intent().apply {
                action = android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            if (isIntentSafe(context, intent)) {
                context.startActivity(intent)
                
                return BatteryOptimizationResult.SettingsOpened(
                    settingsType = "Battery Optimization",
                    instructionsProvided = getSamsungBatteryOptimizationInstructions()
                )
            }
            
        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to open Samsung battery optimization")
        }
        
        return BatteryOptimizationResult.Failed(
            reason = "Cannot open battery optimization settings",
            fallbackInstructions = getSamsungFallbackInstructions()
        )
    }

    // ========== STATUS CHECK METHODS ==========

    /**
     * 🏥 Check Device Care optimization status
     */
    private suspend fun checkDeviceCareOptimization(context: Context): OptimizationStatus {
        // Samsung Device Care status checking is complex and requires system-level access
        // For production app, would need to guide user through manual verification
        return OptimizationStatus.CANNOT_DETERMINE
    }

    /**
     * 🔋 Check battery optimization whitelist status
     */
    private suspend fun checkBatteryOptimization(context: Context): OptimizationStatus {
        return try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                val powerManager = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
                val isIgnored = powerManager.isIgnoringBatteryOptimizations(context.packageName)
                
                if (isIgnored) OptimizationStatus.OPTIMIZED else OptimizationStatus.NOT_OPTIMIZED
            } else {
                OptimizationStatus.OPTIMIZED // No optimization on older Android
            }
        } catch (e: Exception) {
            OptimizationStatus.CANNOT_DETERMINE
        }
    }

    /**
     * 😴 Check Sleeping Apps status (One UI 4.0+)
     */
    private suspend fun checkSleepingAppsStatus(context: Context): OptimizationStatus {
        // Sleeping Apps status requires system-level checking
        // Would need user verification in production
        return OptimizationStatus.CANNOT_DETERMINE
    }

    // ========== CONFIGURATION METHODS ==========

    /**
     * ⚙️ Get Samsung settings paths
     */
    private fun getSamsungSettingsPaths(): List<SettingPath> {
        return listOf(
            // Device Care path
            SettingPath(
                settingName = "Device Care",
                intentAction = null,
                componentName = ComponentName("com.samsung.android.lool", "com.samsung.android.sm.ui.battery.BatteryActivity")
            ),
            
            // Battery optimization path
            SettingPath(
                settingName = "Battery Optimization",
                intentAction = android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS,
                componentName = null
            ),
            
            // Generic Samsung settings
            SettingPath(
                settingName = "Samsung Settings",
                intentAction = android.provider.Settings.ACTION_SETTINGS,
                componentName = null
            )
        )
    }

    /**
     * 📚 Samsung user instructions
     */
    private fun getSamsungUserInstructions(): List<String> {
        return listOf(
            "Open Samsung Device Care (if available)",
            "Go to Battery section", 
            "Tap 'App power management'",
            "Find 'Nordic Beacon Scanner' in the list",
            "Disable optimization cho this app",
            "Go to Settings > Apps > Nordic Beacon Scanner",
            "Tap 'Battery' và select 'Optimize battery usage'", 
            "Change từ 'Optimize' to 'Don't optimize'",
            "Check 'Sleeping apps' section và remove app if present"
        )
    }

    /**
     * 🏥 Device Care specific instructions
     */
    private fun getSamsungDeviceCareInstructions(): List<String> {
        return listOf(
            "In Device Care, tap 'Battery'",
            "Tap 'More battery settings'",
            "Select 'Optimize battery usage'",
            "Find Nordic Beacon Scanner",
            "Toggle OFF battery optimization",
            "Go back và check 'Sleeping apps'",
            "Remove Nordic Beacon Scanner if present"
        )
    }

    /**
     * 🔋 Battery optimization specific instructions
     */
    private fun getSamsungBatteryOptimizationInstructions(): List<String> {
        return listOf(
            "Find Nordic Beacon Scanner in the list",
            "Tap on the app name",
            "Select 'Don't optimize'",
            "Tap 'Done' to confirm",
            "Return to home screen"
        )
    }

    /**
     * 📋 Samsung success indicators
     */
    private fun getSamsungSuccessIndicators(): List<String> {
        return listOf(
            "App removed từ 'Sleeping apps' list",
            "Battery optimization disabled cho Nordic Beacon Scanner",
            "Device Care không shows app in optimized list",
            "Background activity allowed cho app"
        )
    }

    /**
     * 🔄 Samsung fallback instructions
     */
    private fun getSamsungFallbackInstructions(): List<String> {
        return listOf(
            "Open Settings app manually",
            "Search cho 'Battery'",
            "Look cho 'Device Care' hoặc 'Battery optimization'",
            "Find Nordic Beacon Scanner app",
            "Disable optimization hoặc add to whitelist",
            "If using One UI 4.0+, check 'Sleeping apps' section"
        )
    }

    // ========== HELPER METHODS ==========

    /**
     * 🔍 Check if device has One UI features
     */
    private fun hasOneUiFeatures(deviceInfo: DeviceInfo): Boolean {
        return deviceInfo.standardizedManufacturer == "samsung" &&
               (deviceInfo.model.startsWith("SM-") || 
                deviceInfo.product.contains("samsung") ||
                deviceInfo.buildFingerprint.contains("samsung", ignoreCase = true))
    }
}
