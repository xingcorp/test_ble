package com.nordicbeacon.scanner.infrastructure.oem.handlers

import android.content.Context
import com.nordicbeacon.scanner.infrastructure.oem.models.*
import com.nordicbeacon.scanner.infrastructure.oem.strategies.BaseOemStrategy
import com.nordicbeacon.scanner.infrastructure.oem.strategies.OemEducationContent
import timber.log.Timber
import javax.inject.Inject

/**
 * 🔋 Nothing & Sony Battery Optimization Handler
 * 
 * Handles Nothing OS và Sony optimization bypass
 * These brands typically have less aggressive optimization than Chinese OEMs
 * 
 * Nothing OS: Near-stock Android với minimal customization
 * Sony: Close to stock Android với some Sony-specific features
 * 
 * @author Senior Android Developer
 */
class NothingSonyOptimizationHandler @Inject constructor() : BaseOemStrategy() {

    override val oemName: String = "Nothing/Sony"
    
    override val supportedCriteria = DeviceMatchCriteria(
        manufacturers = listOf("nothing", "sony"),
        minAndroidVersion = 21
    )

    override fun isSupported(deviceInfo: DeviceInfo): Boolean {
        return deviceInfo.matchesCriteria(supportedCriteria)
    }

    override suspend fun checkOptimizationStatus(context: Context): OptimizationStatus {
        return try {
            // Nothing/Sony typically follow stock Android patterns
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                val powerManager = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
                val isIgnored = powerManager.isIgnoringBatteryOptimizations(context.packageName)
                
                if (isIgnored) OptimizationStatus.OPTIMIZED else OptimizationStatus.NOT_OPTIMIZED
            } else {
                OptimizationStatus.OPTIMIZED
            }
        } catch (e: Exception) {
            OptimizationStatus.CANNOT_DETERMINE
        }
    }

    override fun getOptimizationStrategy(): OptimizationStrategy {
        val brandName = determineBrandName()
        
        return OptimizationStrategy(
            oemName = brandName,
            strategyName = "$brandName Battery Optimization", 
            description = "Configure $brandName battery settings cho background scanning",
            requiredSettings = getNothingSonySettingsPaths(),
            userInstructions = getNothingSonyInstructions(brandName),
            successIndicators = getNothingSonySuccessIndicators(),
            estimatedComplexity = ComplexityLevel.SIMPLE // Usually straightforward
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
                    settingsType = "${determineBrandName()} Battery Settings",
                    instructionsProvided = getNothingSonyInstructions(determineBrandName())
                )
            } else {
                BatteryOptimizationResult.Failed(
                    reason = "Cannot open battery optimization",
                    fallbackInstructions = getNothingSonyFallback()
                )
            }
            
        } catch (e: Exception) {
            BatteryOptimizationResult.Failed(
                reason = "Settings access failed",
                fallbackInstructions = getNothingSonyFallback(),
                cause = e
            )
        }
    }

    override fun getUserEducationContent(): OemEducationContent {
        val brandName = determineBrandName()
        
        return OemEducationContent(
            title = "$brandName Battery Optimization",
            explanation = "$brandName devices use standard Android battery management với minimal customization.",
            whyNeeded = "Configure battery settings để ensure Nordic beacon scanning continues in background.",
            steps = getNothingSonyInstructions(brandName)
        )
    }

    override fun getDetectionConfidence(deviceInfo: DeviceInfo): Int {
        return when {
            deviceInfo.matchesManufacturer("nothing") -> 85
            deviceInfo.matchesManufacturer("sony") -> 80
            else -> 70
        }
    }

    // ========== HELPER METHODS ==========

    private fun determineBrandName(): String {
        return when {
            android.os.Build.MANUFACTURER.contains("nothing", ignoreCase = true) -> "Nothing"
            android.os.Build.MANUFACTURER.contains("sony", ignoreCase = true) -> "Sony"
            else -> "Nothing/Sony"
        }
    }

    private fun getNothingSonySettingsPaths(): List<SettingPath> {
        return listOf(
            SettingPath(
                settingName = "Battery Optimization",
                intentAction = android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS,
                componentName = null
            )
        )
    }

    private fun getNothingSonyInstructions(brandName: String): List<String> {
        return listOf(
            "Find Nordic Beacon Scanner in app list",
            "Tap on the app name",
            "Select 'Don't optimize'",
            "Tap 'Done' to confirm changes"
        )
    }

    private fun getNothingSonySuccessIndicators(): List<String> {
        return listOf(
            "App shows 'Not optimized' status",
            "Background activity unrestricted"
        )
    }

    private fun getNothingSonyFallback(): List<String> {
        return listOf(
            "Settings > Apps",
            "Special access > Battery optimization", 
            "Find Nordic Beacon Scanner"
        )
    }
}
