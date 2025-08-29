package com.nordicbeacon.scanner.infrastructure.oem.models

/**
 * 🔋 Battery Optimization Result Models
 * 
 * Represents outcomes của OEM battery optimization operations
 * Used để track success/failure và provide user guidance
 * 
 * @author Senior Android Developer
 */
sealed class BatteryOptimizationResult {
    
    /**
     * ✅ Already optimized - no action needed
     */
    object AlreadyOptimized : BatteryOptimizationResult()
    
    /**
     * 🚀 Successfully guided user to settings
     */
    data class SettingsOpened(
        val settingsType: String,
        val instructionsProvided: List<String>
    ) : BatteryOptimizationResult()
    
    /**
     * ⚠️ Partial success - some settings opened, manual steps required
     */
    data class PartialSuccess(
        val completedSteps: List<String>,
        val manualSteps: List<String>,
        val reason: String
    ) : BatteryOptimizationResult()
    
    /**
     * ❌ Failed to open settings - fallback required
     */
    data class Failed(
        val reason: String,
        val fallbackInstructions: List<String>,
        val cause: Throwable? = null
    ) : BatteryOptimizationResult()
    
    /**
     * ❓ OEM not supported - generic optimization
     */
    data class UnsupportedOem(
        val deviceInfo: String,
        val genericInstructions: List<String>
    ) : BatteryOptimizationResult()
}

/**
 * 📊 Battery Optimization Status
 */
enum class OptimizationStatus {
    UNKNOWN,           // Status not yet determined
    OPTIMIZED,         // App is whitelisted/optimized
    NOT_OPTIMIZED,     // App needs optimization
    PARTIALLY_OPTIMIZED, // Some optimization applied
    CANNOT_DETERMINE   // Unable to check status
}

/**
 * 🎯 Battery Optimization Strategy Info
 */
data class OptimizationStrategy(
    val oemName: String,
    val strategyName: String,
    val description: String,
    val requiredSettings: List<SettingPath>,
    val userInstructions: List<String>,
    val successIndicators: List<String>,
    val estimatedComplexity: ComplexityLevel
) {
    
    /**
     * 📋 Get formatted instructions cho user display
     */
    fun getFormattedInstructions(): String {
        return userInstructions.joinToString("\n") { "• $it" }
    }
}

/**
 * ⚙️ Settings Path Information
 */
data class SettingPath(
    val settingName: String,
    val intentAction: String?,
    val componentName: android.content.ComponentName?,
    val extraData: Map<String, String> = emptyMap(),
    val isSecure: Boolean = false,
    val fallbackInstructions: List<String> = emptyList()
) {
    
    /**
     * 🔗 Create intent cho opening settings
     */
    fun createIntent(context: android.content.Context): android.content.Intent? {
        return try {
            when {
                componentName != null -> {
                    android.content.Intent().apply {
                        component = componentName
                        extraData.forEach { (key, value) -> putExtra(key, value) }
                    }
                }
                intentAction != null -> {
                    android.content.Intent(intentAction).apply {
                        extraData.forEach { (key, value) -> putExtra(key, value) }
                    }
                }
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }
}

/**
 * 📊 Optimization Complexity Levels
 */
enum class ComplexityLevel(val description: String) {
    SIMPLE("1-2 steps, straightforward"),
    MODERATE("3-4 steps, some navigation required"),
    COMPLEX("5+ steps, multiple settings screens"),
    EXPERT("Advanced users only, potential risks")
}
