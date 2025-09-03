package com.nordicbeacon.scanner.infrastructure.oem.handlers

import android.content.ComponentName
import android.content.Context
import com.nordicbeacon.scanner.infrastructure.oem.models.*
import com.nordicbeacon.scanner.infrastructure.oem.strategies.BaseOemStrategy
import com.nordicbeacon.scanner.infrastructure.oem.strategies.OemEducationContent
import timber.log.Timber
import javax.inject.Inject

/**
 * 🔋 Oppo/Vivo/Realme Battery Optimization Handler
 * 
 * Handles ColorOS (Oppo), Funtouch OS (Vivo), và Realme UI optimization bypass
 * These brands share similar optimization approaches due to common BBK Electronics ownership
 * 
 * Supported brands: Oppo, Vivo, Realme, iQOO, OnePlus (BBK ecosystem)
 * 
 * BBK-specific challenges:
 * - Auto Startup management (critical)
 * - Background app freeze policies
 * - Battery optimization aggressive defaults
 * - Memory cleaning interference
 * 
 * @author Senior Android Developer
 */
class OppoVivoOptimizationHandler @Inject constructor() : BaseOemStrategy() {

    override val oemName: String = "Oppo/Vivo"
    
    override val supportedCriteria = DeviceMatchCriteria(
        manufacturers = listOf("oppo", "vivo", "realme", "iqoo"),
        minAndroidVersion = 21
    )

    override fun isSupported(deviceInfo: DeviceInfo): Boolean {
        return deviceInfo.matchesCriteria(supportedCriteria)
    }

    override suspend fun checkOptimizationStatus(context: Context): OptimizationStatus {
        return try {
            val autoStartStatus = checkAutoStartStatus(context)
            val batteryOptStatus = checkBatteryOptimization(context)
            val backgroundStatus = checkBackgroundAppStatus(context)
            
            when {
                autoStartStatus == OptimizationStatus.OPTIMIZED &&
                batteryOptStatus == OptimizationStatus.OPTIMIZED &&
                backgroundStatus == OptimizationStatus.OPTIMIZED -> OptimizationStatus.OPTIMIZED
                
                autoStartStatus == OptimizationStatus.NOT_OPTIMIZED -> OptimizationStatus.NOT_OPTIMIZED
                
                else -> OptimizationStatus.PARTIALLY_OPTIMIZED
            }
            
        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to check Oppo/Vivo optimization")
            OptimizationStatus.CANNOT_DETERMINE
        }
    }

    override fun getOptimizationStrategy(): OptimizationStrategy {
        val brandName = determineBrandName()
        
        return OptimizationStrategy(
            oemName = brandName,
            strategyName = "$brandName Auto Startup & Battery Management",
            description = "Configure $brandName auto startup và battery optimization settings",
            requiredSettings = getOppoVivoSettingsPaths(),
            userInstructions = getOppoVivoUserInstructions(brandName),
            successIndicators = getOppoVivoSuccessIndicators(),
            estimatedComplexity = ComplexityLevel.MODERATE
        )
    }

    override suspend fun requestOptimization(context: Context): BatteryOptimizationResult {
        val brandName = determineBrandName()
        Timber.i("🔋 Starting $brandName battery optimization process...")
        
        return try {
            // Priority: Auto Startup (most critical for BBK devices)
            val autoStartResult = attemptOpenAutoStartup(context)
            if (autoStartResult is BatteryOptimizationResult.SettingsOpened) {
                return autoStartResult
            }
            
            // Secondary: Battery optimization
            val batteryResult = attemptOpenBatterySettings(context)
            if (batteryResult is BatteryOptimizationResult.SettingsOpened) {
                return batteryResult
            }
            
            // Fallback: Generic settings
            attemptOpenSettings(context, getOppoVivoSettingsPaths())
            
        } catch (e: Exception) {
            Timber.e(e, "❌ $brandName optimization failed")
            BatteryOptimizationResult.Failed(
                reason = "Failed to open $brandName settings",
                fallbackInstructions = getOppoVivoFallbackInstructions(brandName),
                cause = e
            )
        }
    }

    override fun getUserEducationContent(): OemEducationContent {
        val brandName = determineBrandName()
        
        return OemEducationContent(
            title = "$brandName Battery Optimization", 
            explanation = "$brandName devices have strict auto-startup và background app management.",
            whyNeeded = """
                $brandName (BBK Electronics) devices use aggressive battery optimization policies.
                Auto Startup permission is critical cho background services like Nordic beacon scanning.
                Proper configuration ensures the app continues working when screen is off.
            """.trimIndent(),
            steps = getOppoVivoUserInstructions(brandName),
            troubleshooting = """
                Common Issues:
                • App keeps stopping → Check Auto Startup permission - this is mandatory
                • Settings won't open → Try Security/Privacy app manually
                • No auto startup option → Look in App management > Permission management
            """.trimIndent()
        )
    }

    override fun getDetectionConfidence(deviceInfo: DeviceInfo): Int {
        var confidence = 75
        
        when {
            deviceInfo.matchesManufacturer("oppo") -> confidence += 15
            deviceInfo.matchesManufacturer("vivo") -> confidence += 15  
            deviceInfo.matchesManufacturer("realme") -> confidence += 10
            deviceInfo.matchesManufacturer("iqoo") -> confidence += 10
        }
        
        return minOf(100, confidence)
    }

    // ========== BBK-SPECIFIC IMPLEMENTATIONS ==========

    /**
     * 🚀 Attempt to open Auto Startup settings
     */
    private suspend fun attemptOpenAutoStartup(context: Context): BatteryOptimizationResult {
        
        val autoStartupIntents = listOf(
            // Oppo ColorOS Auto Startup
            ComponentName("com.coloros.safecenter", "com.coloros.safecenter.startupapp.StartupAppListActivity"),
            ComponentName("com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity"),
            
            // Vivo Funtouch Auto Start  
            ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity"),
            ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"),
            
            // Realme Auto Start
            ComponentName("com.coloros.safecenter", "com.coloros.safecenter.startupapp.StartupAppListActivity")
        )
        
        for (componentName in autoStartupIntents) {
            try {
                val intent = android.content.Intent().apply {
                    component = componentName
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                
                if (isIntentSafe(context, intent)) {
                    context.startActivity(intent)
                    
                    return BatteryOptimizationResult.SettingsOpened(
                        settingsType = "Auto Startup Settings",
                        instructionsProvided = getAutoStartupInstructions()
                    )
                }
                
            } catch (e: Exception) {
                Timber.d("❌ Failed Auto Startup intent: $componentName")
                continue
            }
        }
        
        return BatteryOptimizationResult.Failed(
            reason = "Cannot open Auto Startup settings",
            fallbackInstructions = getAutoStartupFallback()
        )
    }

    /**
     * 🔋 Attempt to open battery optimization settings
     */
    private suspend fun attemptOpenBatterySettings(context: Context): BatteryOptimizationResult {
        
        try {
            // Generic battery optimization
            val intent = android.content.Intent().apply {
                action = android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            if (isIntentSafe(context, intent)) {
                context.startActivity(intent)
                
                return BatteryOptimizationResult.SettingsOpened(
                    settingsType = "Battery Optimization",
                    instructionsProvided = getBatteryOptimizationInstructions()
                )
            }
            
        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to open battery settings")
        }
        
        return BatteryOptimizationResult.Failed(
            reason = "Cannot open battery optimization",
            fallbackInstructions = getBatteryOptimizationFallback()
        )
    }

    // ========== STATUS CHECK METHODS ==========

    private suspend fun checkAutoStartStatus(context: Context): OptimizationStatus = OptimizationStatus.CANNOT_DETERMINE
    private suspend fun checkBatteryOptimization(context: Context): OptimizationStatus = OptimizationStatus.CANNOT_DETERMINE  
    private suspend fun checkBackgroundAppStatus(context: Context): OptimizationStatus = OptimizationStatus.CANNOT_DETERMINE

    // ========== CONFIGURATION METHODS ==========

    private fun determineBrandName(): String {
        return android.os.Build.MANUFACTURER.lowercase().let { manufacturer ->
            when {
                manufacturer.contains("oppo") -> "Oppo"
                manufacturer.contains("vivo") -> "Vivo"
                manufacturer.contains("realme") -> "Realme"
                manufacturer.contains("iqoo") -> "iQOO"
                else -> "Oppo/Vivo"
            }
        }
    }

    private fun getOppoVivoSettingsPaths(): List<SettingPath> {
        return listOf(
            SettingPath(
                settingName = "Auto Startup",
                intentAction = null,
                componentName = ComponentName("com.coloros.safecenter", "com.coloros.safecenter.startupapp.StartupAppListActivity")
            ),
            SettingPath(
                settingName = "Battery Optimization", 
                intentAction = android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS,
                componentName = null
            )
        )
    }

    private fun getOppoVivoUserInstructions(brandName: String): List<String> {
        return listOf(
            "Open Settings > Apps",
            "Find Nordic Beacon Scanner", 
            "Tap 'Permissions'",
            "Enable 'Auto Startup' permission",
            "Go back to app info",
            "Tap 'Battery'",
            "Select 'Don't optimize'",
            "Enable 'Background activity' if available"
        )
    }

    private fun getOppoVivoSuccessIndicators(): List<String> {
        return listOf(
            "Auto startup enabled cho Nordic Beacon Scanner",
            "Battery optimization disabled",
            "Background activity allowed"
        )
    }

    private fun getOppoVivoFallbackInstructions(brandName: String): List<String> {
        return listOf(
            "Open $brandName Settings manually",
            "Go to Apps/Application management",
            "Find Nordic Beacon Scanner",
            "Enable Auto startup và disable battery optimization"
        )
    }

    private fun getAutoStartupInstructions(): List<String> {
        return listOf(
            "Find Nordic Beacon Scanner in app list",
            "Toggle ON auto startup permission", 
            "Confirm selection"
        )
    }

    private fun getBatteryOptimizationInstructions(): List<String> {
        return listOf(
            "Find Nordic Beacon Scanner",
            "Select 'Don't optimize'",
            "Tap 'Done'"
        )
    }

    private fun getAutoStartupFallback(): List<String> {
        return listOf(
            "Settings > Apps > App management",
            "Find Nordic Beacon Scanner",
            "Look cho Auto startup hoặc Permission management"
        )
    }

    private fun getBatteryOptimizationFallback(): List<String> {
        return listOf(
            "Settings > Battery",
            "Battery optimization", 
            "Find Nordic Beacon Scanner"
        )
    }
}
