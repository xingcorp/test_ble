package com.nordicbeacon.scanner.infrastructure.oem.detection

import com.nordicbeacon.scanner.infrastructure.oem.models.DeviceInfo
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 🏭 Device Detection Factory
 * 
 * Identifies Android device manufacturer và determines appropriate OEM strategy
 * Handles edge cases và provides confidence scoring cho detection accuracy
 * 
 * Key Features:
 * - Multi-criteria device identification
 * - Confidence scoring cho detection accuracy
 * - Edge case handling (generic brands, custom ROMs)
 * - Comprehensive OEM database
 * 
 * @author Senior Android Developer
 */
@Singleton
class DeviceDetectionFactory @Inject constructor() {

    /**
     * 🔍 Detect OEM type với comprehensive analysis
     * 
     * @param deviceInfo Device information to analyze
     * @return Detected OEM type với confidence score
     */
    fun detectOemType(deviceInfo: DeviceInfo = DeviceInfo()): OemDetectionResult {
        
        Timber.i("🔍 Detecting OEM type cho device: ${deviceInfo.getDeviceSummary()}")
        
        // Primary detection based on manufacturer
        val primaryDetection = detectByManufacturer(deviceInfo)
        
        // Secondary validation với additional criteria
        val validatedDetection = validateDetectionWithSecondaryMarkers(deviceInfo, primaryDetection)
        
        // Calculate final confidence score
        val finalConfidence = calculateDetectionConfidence(deviceInfo, validatedDetection)
        
        val result = OemDetectionResult(
            oemType = validatedDetection,
            confidence = finalConfidence,
            deviceInfo = deviceInfo,
            detectionReasons = buildDetectionReasons(deviceInfo, validatedDetection)
        )
        
        Timber.i("✅ OEM Detection Result: ${result.oemType} (${result.confidence}% confidence)")
        
        return result
    }

    /**
     * 🏭 Primary detection based on manufacturer string
     */
    private fun detectByManufacturer(deviceInfo: DeviceInfo): OemType {
        
        return when {
            // Samsung detection
            deviceInfo.matchesManufacturer("samsung") -> OemType.SAMSUNG
            
            // Xiaomi ecosystem (including sub-brands)
            deviceInfo.matchesManufacturer("xiaomi") ||
            deviceInfo.matchesManufacturer("redmi") ||
            deviceInfo.matchesManufacturer("poco") ||
            deviceInfo.matchesManufacturer("blackshark") -> OemType.XIAOMI
            
            // Huawei ecosystem
            deviceInfo.matchesManufacturer("huawei") ||
            deviceInfo.matchesManufacturer("honor") -> OemType.HUAWEI
            
            // OnePlus
            deviceInfo.matchesManufacturer("oneplus") -> OemType.ONEPLUS
            
            // Oppo ecosystem
            deviceInfo.matchesManufacturer("oppo") ||
            deviceInfo.matchesManufacturer("realme") -> OemType.OPPO
            
            // Vivo ecosystem  
            deviceInfo.matchesManufacturer("vivo") ||
            deviceInfo.matchesManufacturer("iqoo") -> OemType.VIVO
            
            // Nothing
            deviceInfo.matchesManufacturer("nothing") -> OemType.NOTHING
            
            // Sony
            deviceInfo.matchesManufacturer("sony") -> OemType.SONY
            
            // LG (legacy support)
            deviceInfo.matchesManufacturer("lg") ||
            deviceInfo.matchesManufacturer("lge") -> OemType.LG
            
            // Motorola
            deviceInfo.matchesManufacturer("motorola") ||
            deviceInfo.matchesManufacturer("lenovo") -> OemType.MOTOROLA
            
            // Google Pixel
            deviceInfo.matchesManufacturer("google") -> OemType.GOOGLE
            
            // Generic/Unknown
            else -> OemType.GENERIC
        }
    }

    /**
     * ✅ Validate detection với secondary markers
     */
    private fun validateDetectionWithSecondaryMarkers(
        deviceInfo: DeviceInfo, 
        primaryDetection: OemType
    ): OemType {
        
        // Additional validation logic
        return when (primaryDetection) {
            OemType.XIAOMI -> {
                // Validate MIUI presence
                if (deviceInfo.buildFingerprint.contains("miui", ignoreCase = true) ||
                    hasXiaomiSystemApps(deviceInfo)) {
                    OemType.XIAOMI
                } else {
                    // Might be generic Android on Xiaomi hardware
                    OemType.XIAOMI_GENERIC
                }
            }
            
            OemType.SAMSUNG -> {
                // Validate One UI presence  
                if (hasOneUiMarkers(deviceInfo)) {
                    OemType.SAMSUNG
                } else {
                    OemType.SAMSUNG_GENERIC
                }
            }
            
            OemType.HUAWEI -> {
                // Check for EMUI/HarmonyOS
                if (deviceInfo.buildFingerprint.contains("emui", ignoreCase = true) ||
                    deviceInfo.buildFingerprint.contains("harmonyos", ignoreCase = true)) {
                    OemType.HUAWEI  
                } else {
                    OemType.HUAWEI_GENERIC
                }
            }
            
            else -> primaryDetection
        }
    }

    /**
     * 📊 Calculate detection confidence score
     */
    private fun calculateDetectionConfidence(
        deviceInfo: DeviceInfo,
        detectedType: OemType
    ): Int {
        
        var confidence = when (detectedType) {
            OemType.SAMSUNG -> if (hasOneUiMarkers(deviceInfo)) 95 else 70
            OemType.XIAOMI -> if (deviceInfo.buildFingerprint.contains("miui", ignoreCase = true)) 95 else 75
            OemType.HUAWEI -> if (deviceInfo.buildFingerprint.contains("emui", ignoreCase = true)) 90 else 70
            OemType.GOOGLE -> 98 // Very reliable detection
            OemType.ONEPLUS -> 85 // Usually reliable  
            OemType.OPPO, OemType.VIVO -> 80 // Good reliability
            OemType.NOTHING, OemType.SONY -> 75 // Moderate reliability
            else -> 60 // Lower confidence cho less common brands
        }
        
        // Boost confidence cho exact manufacturer matches
        if (deviceInfo.standardizedManufacturer == detectedType.name.lowercase()) {
            confidence = minOf(100, confidence + 10)
        }
        
        return confidence
    }

    /**
     * 📋 Build detection reasoning for debugging
     */
    private fun buildDetectionReasons(deviceInfo: DeviceInfo, oemType: OemType): List<String> {
        val reasons = mutableListOf<String>()
        
        reasons.add("Manufacturer: ${deviceInfo.manufacturer}")
        reasons.add("Brand: ${deviceInfo.brand}")
        reasons.add("Model: ${deviceInfo.model}")
        reasons.add("Build fingerprint analysis")
        
        // Add specific markers found
        when (oemType) {
            OemType.XIAOMI -> {
                if (deviceInfo.buildFingerprint.contains("miui", ignoreCase = true)) {
                    reasons.add("MIUI detected in build fingerprint")
                }
            }
            OemType.SAMSUNG -> {
                if (hasOneUiMarkers(deviceInfo)) {
                    reasons.add("One UI markers detected")
                }
            }
            // Add other OEM-specific markers...
            else -> {
                reasons.add("Primary manufacturer match")
            }
        }
        
        return reasons
    }

    // ========== HELPER DETECTION METHODS ==========

    /**
     * 🔍 Check cho One UI markers on Samsung devices
     */
    private fun hasOneUiMarkers(deviceInfo: DeviceInfo): Boolean {
        return deviceInfo.buildFingerprint.contains("samsung", ignoreCase = true) &&
               (deviceInfo.product.startsWith("dream") || 
                deviceInfo.product.startsWith("beyond") ||
                deviceInfo.model.startsWith("SM-"))
    }

    /**
     * 🔍 Check cho MIUI system apps presence  
     */
    private fun hasXiaomiSystemApps(deviceInfo: DeviceInfo): Boolean {
        // In real implementation, would check for MIUI system package signatures
        // For now, use build fingerprint analysis
        return deviceInfo.buildFingerprint.contains("miui", ignoreCase = true) ||
               deviceInfo.buildFingerprint.contains("xiaomi", ignoreCase = true)
    }
}

/**
 * 🏷️ OEM Type Enumeration
 * 
 * Comprehensive list của major Android OEM manufacturers
 */
enum class OemType(val displayName: String, val marketShare: Double) {
    SAMSUNG("Samsung", 22.0),
    XIAOMI("Xiaomi", 13.0), 
    XIAOMI_GENERIC("Xiaomi (Generic)", 1.0),
    HUAWEI("Huawei", 8.0),
    HUAWEI_GENERIC("Huawei (Generic)", 1.0),
    ONEPLUS("OnePlus", 3.0),
    OPPO("Oppo", 11.0),
    VIVO("Vivo", 9.0),
    GOOGLE("Google Pixel", 4.0),
    NOTHING("Nothing", 0.5),
    SONY("Sony", 1.0),
    LG("LG", 1.0),
    MOTOROLA("Motorola", 2.0),
    SAMSUNG_GENERIC("Samsung (Generic)", 1.0),
    GENERIC("Generic Android", 25.0); // Stock Android, custom ROMs, etc.
    
    /**
     * 📊 Check if OEM is major manufacturer (>5% market share)
     */
    fun isMajorOem(): Boolean = marketShare >= 5.0
    
    /**
     * 🔍 Check if OEM typically has aggressive battery optimization
     */
    fun hasAggressiveOptimization(): Boolean {
        return when (this) {
            XIAOMI, HUAWEI, OPPO, VIVO -> true
            SAMSUNG, ONEPLUS -> true  
            else -> false
        }
    }
}

/**
 * 📊 OEM Detection Result
 */
data class OemDetectionResult(
    val oemType: OemType,
    val confidence: Int, // 0-100 confidence score
    val deviceInfo: DeviceInfo,
    val detectionReasons: List<String>
) {
    
    /**
     * 🎯 Check if detection is reliable enough to use
     */
    fun isReliable(): Boolean = confidence >= 70
    
    /**
     * 🔍 Check if high confidence detection
     */  
    fun isHighConfidence(): Boolean = confidence >= 90
    
    /**
     * 📋 Get detection summary cho logging
     */
    fun getDetectionSummary(): String {
        return "${oemType.displayName} (${confidence}% confidence) - ${deviceInfo.getDeviceSummary()}"
    }
}
