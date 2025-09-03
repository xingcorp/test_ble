package com.nordicbeacon.scanner.core.permissions.strategy

import android.os.Build
import com.nordicbeacon.scanner.core.permissions.PermissionStrategy

/**
 * 🏭 Permission Strategy Factory
 * 
 * Creates appropriate permission strategy based on Android version
 * and device characteristics. Handles manufacturer-specific behaviors.
 * 
 * @author Senior Android Engineer  
 */
object PermissionStrategyFactory {
    
    /**
     * 🎯 Create appropriate strategy for current device
     */
    fun createStrategy(): PermissionStrategy {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                Android13PermissionStrategy()
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                Android12PermissionStrategy()
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                Android10PermissionStrategy()
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                ModernPermissionStrategy()
            }
            else -> {
                LegacyPermissionStrategy()
            }
        }.let { baseStrategy ->
            // Wrap with manufacturer-specific handling if needed
            createManufacturerAwareStrategy(baseStrategy)
        }
    }
    
    /**
     * 🏗️ Wrap base strategy with manufacturer-specific handling
     */
    private fun createManufacturerAwareStrategy(baseStrategy: PermissionStrategy): PermissionStrategy {
        val manufacturer = Build.MANUFACTURER.lowercase()
        
        return when {
            manufacturer.contains("samsung") -> {
                SamsungPermissionStrategyDecorator(baseStrategy)
            }
            manufacturer.contains("xiaomi") -> {
                XiaomiPermissionStrategyDecorator(baseStrategy)
            }
            manufacturer.contains("huawei") -> {
                HuaweiPermissionStrategyDecorator(baseStrategy)
            }
            manufacturer.contains("oneplus") -> {
                OnePlusPermissionStrategyDecorator(baseStrategy)
            }
            manufacturer.contains("oppo") -> {
                OppoPermissionStrategyDecorator(baseStrategy)
            }
            manufacturer.contains("vivo") -> {
                VivoPermissionStrategyDecorator(baseStrategy)
            }
            else -> baseStrategy
        }
    }
    
    /**
     * 📱 Get device info for debugging
     */
    fun getDeviceInfo(): DeviceInfo {
        return DeviceInfo(
            manufacturer = Build.MANUFACTURER,
            model = Build.MODEL,
            androidVersion = Build.VERSION.RELEASE,
            apiLevel = Build.VERSION.SDK_INT,
            brand = Build.BRAND,
            device = Build.DEVICE,
            hardware = Build.HARDWARE,
            securityPatch = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Build.VERSION.SECURITY_PATCH
            } else null
        )
    }
}

/**
 * 📱 Device information for strategy selection
 */
data class DeviceInfo(
    val manufacturer: String,
    val model: String,
    val androidVersion: String,
    val apiLevel: Int,
    val brand: String,
    val device: String,
    val hardware: String,
    val securityPatch: String?
) {
    
    /**
     * Check if device is known to have permission quirks
     */
    fun hasKnownPermissionQuirks(): Boolean {
        val problematicManufacturers = setOf(
            "xiaomi", "huawei", "oppo", "vivo", "oneplus", "meizu", "samsung"
        )
        return manufacturer.lowercase() in problematicManufacturers
    }
    
    /**
     * Get friendly device name
     */
    fun getFriendlyName(): String = "$manufacturer $model (Android $androidVersion)"
}
