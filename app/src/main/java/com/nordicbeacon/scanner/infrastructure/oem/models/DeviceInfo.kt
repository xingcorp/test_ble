package com.nordicbeacon.scanner.infrastructure.oem.models

import android.os.Build

/**
 * 📱 Device Information Model
 * 
 * Comprehensive device identification cho OEM-specific optimizations
 * Contains all necessary information để determine appropriate battery bypass strategy
 * 
 * @author Senior Android Developer
 */
data class DeviceInfo(
    val manufacturer: String = Build.MANUFACTURER,
    val brand: String = Build.BRAND,
    val model: String = Build.MODEL,
    val device: String = Build.DEVICE,
    val product: String = Build.PRODUCT,
    val androidVersion: Int = Build.VERSION.SDK_INT,
    val androidRelease: String = Build.VERSION.RELEASE,
    val buildFingerprint: String = Build.FINGERPRINT
) {
    
    /**
     * 🏭 Get standardized manufacturer name
     */
    val standardizedManufacturer: String
        get() = manufacturer.lowercase().trim()
    
    /**
     * 🏷️ Get standardized brand name  
     */
    val standardizedBrand: String
        get() = brand.lowercase().trim()
    
    /**
     * 🔍 Check if device matches specific criteria
     */
    fun matchesManufacturer(targetManufacturer: String): Boolean {
        return standardizedManufacturer.contains(targetManufacturer.lowercase()) ||
               standardizedBrand.contains(targetManufacturer.lowercase())
    }
    
    /**
     * 📋 Get device summary cho logging
     */
    fun getDeviceSummary(): String {
        return "$manufacturer $model (Android $androidRelease, API $androidVersion)"
    }
    
    /**
     * 🔍 Advanced device detection với multiple criteria
     */
    fun matchesCriteria(criteria: DeviceMatchCriteria): Boolean {
        return criteria.manufacturers.any { matchesManufacturer(it) } &&
               (criteria.minAndroidVersion == null || androidVersion >= criteria.minAndroidVersion) &&
               (criteria.maxAndroidVersion == null || androidVersion <= criteria.maxAndroidVersion) &&
               (criteria.modelPatterns.isEmpty() || criteria.modelPatterns.any { 
                   model.contains(it, ignoreCase = true) 
               })
    }
}

/**
 * 🔍 Device Matching Criteria
 */
data class DeviceMatchCriteria(
    val manufacturers: List<String>,
    val minAndroidVersion: Int? = null,
    val maxAndroidVersion: Int? = null,
    val modelPatterns: List<String> = emptyList(),
    val buildPatterns: List<String> = emptyList()
)
