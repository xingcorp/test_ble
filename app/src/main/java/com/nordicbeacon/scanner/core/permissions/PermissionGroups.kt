package com.nordicbeacon.scanner.core.permissions

/**
 * 📦 Predefined Permission Groups for Common Use Cases
 * 
 * Convenient groupings of permissions for typical application scenarios.
 * Helps reduce boilerplate and provides semantic meaning to permission requests.
 * 
 * @author Senior Android Engineer
 */
object PermissionGroups {
    
    // ========== BLUETOOTH BEACON SCANNING ==========
    
    /**
     * 📡 Nordic BLE Beacon Scanning (Essential)
     * 
     * Minimum permissions required for basic BLE beacon scanning functionality.
     * Covers both legacy and modern Android versions.
     */
    val BLE_SCANNING_ESSENTIAL = setOf(
        Permission.LOCATION_FINE,
        Permission.BLUETOOTH_SCAN,
        Permission.BLUETOOTH, // Fallback for older devices
        Permission.BLUETOOTH_ADMIN // Fallback for older devices
    )
    
    /**
     * 📡 Nordic BLE Beacon Scanning (Complete)
     * 
     * Full permission set for optimal BLE beacon scanning experience
     * including background scanning capabilities and foreground service.
     */
    val BLE_SCANNING_COMPLETE = BLE_SCANNING_ESSENTIAL + setOf(
        Permission.LOCATION_BACKGROUND,
        Permission.BLUETOOTH_CONNECT,
        Permission.BLUETOOTH_ADVERTISE,
        Permission.FOREGROUND_SERVICE_LOCATION // Required for foreground service on Android 14+
    )
    
    /**
     * 📡 BLE Beacon Advertising
     * 
     * Permissions required to advertise as a BLE beacon.
     */
    val BLE_ADVERTISING = setOf(
        Permission.BLUETOOTH_ADVERTISE,
        Permission.BLUETOOTH, // Legacy support
        Permission.BLUETOOTH_ADMIN // Legacy support
    )
    
    // ========== LOCATION SERVICES ==========
    
    /**
     * 📍 Basic Location (Foreground Only)
     * 
     * Essential location permissions for foreground use.
     */
    val LOCATION_BASIC = setOf(
        Permission.LOCATION_FINE,
        Permission.LOCATION_COARSE
    )
    
    /**
     * 📍 Complete Location Services
     * 
     * Full location permission set including background access.
     */
    val LOCATION_COMPLETE = LOCATION_BASIC + setOf(
        Permission.LOCATION_BACKGROUND
    )
    
    // ========== MEDIA AND STORAGE ==========
    
    /**
     * 💾 Legacy Storage Access (Android < 13)
     * 
     * Traditional storage permissions for older Android versions.
     */
    val STORAGE_LEGACY = setOf(
        Permission.STORAGE_READ,
        Permission.STORAGE_WRITE
    )
    
    /**
     * 📱 Modern Media Access (Android 13+)
     * 
     * Granular media permissions for newer Android versions.
     */
    val MEDIA_MODERN = setOf(
        Permission.MEDIA_IMAGES,
        Permission.MEDIA_VIDEO,
        Permission.MEDIA_AUDIO
    )
    
    /**
     * 💾 Complete File Access
     * 
     * Comprehensive file and media access across Android versions.
     */
    val FILE_ACCESS_COMPLETE = STORAGE_LEGACY + MEDIA_MODERN + setOf(
        Permission.STORAGE_MANAGE
    )
    
    // ========== COMMUNICATION ==========
    
    /**
     * 🔔 Notifications and Alerts
     * 
     * Permission for showing notifications to user.
     */
    val NOTIFICATIONS = setOf(
        Permission.POST_NOTIFICATIONS
    )
    
    /**
     * 📞 Phone and Contacts
     * 
     * Communication-related permissions.
     */
    val COMMUNICATION = setOf(
        Permission.READ_CONTACTS,
        Permission.WRITE_CONTACTS,
        Permission.PHONE_STATE,
        Permission.CALL_PHONE,
        Permission.PHONE_NUMBERS
    )
    
    /**
     * 🎤 Audio Recording
     * 
     * Microphone access for audio features.
     */
    val AUDIO_RECORDING = setOf(
        Permission.RECORD_AUDIO
    )
    
    // ========== COMPLETE APPLICATION PERMISSIONS ==========
    
    /**
     * 🏢 Enterprise Application Suite
     * 
     * Comprehensive permission set for enterprise applications with
     * full device integration capabilities.
     */
    val ENTERPRISE_COMPLETE = BLE_SCANNING_COMPLETE + 
                             LOCATION_COMPLETE + 
                             FILE_ACCESS_COMPLETE + 
                             COMMUNICATION + 
                             NOTIFICATIONS + 
                             AUDIO_RECORDING + 
                             setOf(Permission.CAMERA)
    
    /**
     * 📱 Consumer Application Basic
     * 
     * Basic permission set for consumer-facing applications.
     */
    val CONSUMER_BASIC = BLE_SCANNING_ESSENTIAL + 
                        LOCATION_BASIC + 
                        NOTIFICATIONS
    
    // ========== VERSION-SPECIFIC GROUPS ==========
    
    /**
     * 🤖 Android 12+ Bluetooth
     * 
     * Modern Bluetooth permissions for Android 12 and above.
     */
    val BLUETOOTH_MODERN = setOf(
        Permission.BLUETOOTH_SCAN,
        Permission.BLUETOOTH_CONNECT,
        Permission.BLUETOOTH_ADVERTISE
    )
    
    /**
     * 🤖 Android 13+ Media
     * 
     * Granular media permissions for Android 13 and above.
     */
    val MEDIA_ANDROID_13 = setOf(
        Permission.MEDIA_IMAGES,
        Permission.MEDIA_VIDEO,
        Permission.MEDIA_AUDIO,
        Permission.POST_NOTIFICATIONS
    )
    
    // ========== UTILITY FUNCTIONS ==========
    
    /**
     * 🎯 Get applicable permissions from a group for current device
     * 
     * Filters out permissions not applicable to current Android version.
     */
    fun getApplicablePermissions(permissionSet: Set<Permission>): Set<Permission> {
        return permissionSet.filter { it.isApplicableForCurrentVersion() }.toSet()
    }
    
    /**
     * 📱 Get version-appropriate BLE permissions
     * 
     * Returns the correct BLE permission set based on Android version.
     */
    fun getBluetoothPermissionsForCurrentVersion(): Set<Permission> {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            getApplicablePermissions(BLUETOOTH_MODERN + setOf(Permission.LOCATION_FINE))
        } else {
            getApplicablePermissions(setOf(
                Permission.BLUETOOTH,
                Permission.BLUETOOTH_ADMIN,
                Permission.LOCATION_FINE
            ))
        }
    }
    
    /**
     * 💾 Get storage permissions for current version
     * 
     * Returns appropriate storage/media permissions based on Android version.
     */
    fun getStoragePermissionsForCurrentVersion(): Set<Permission> {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            getApplicablePermissions(MEDIA_MODERN)
        } else {
            getApplicablePermissions(STORAGE_LEGACY)
        }
    }
    
    /**
     * 🔍 Check if permission group has critical permissions
     * 
     * Determines if a permission group contains permissions marked as critical.
     */
    fun hasCriticalPermissions(permissionSet: Set<Permission>): Boolean {
        val criticalGroups = setOf(
            PermissionGroup.LOCATION,
            PermissionGroup.BLUETOOTH
        )
        return permissionSet.any { it.group in criticalGroups }
    }
    
    /**
     * 📊 Get permission group statistics
     * 
     * Returns statistics about a permission group.
     */
    fun getGroupStatistics(permissionSet: Set<Permission>): PermissionGroupStats {
        val applicable = getApplicablePermissions(permissionSet)
        val byImportance = applicable.groupBy { it.group.importance }
        
        return PermissionGroupStats(
            totalPermissions = permissionSet.size,
            applicablePermissions = applicable.size,
            highImportance = byImportance[PermissionImportance.HIGH]?.size ?: 0,
            mediumImportance = byImportance[PermissionImportance.MEDIUM]?.size ?: 0,
            lowImportance = byImportance[PermissionImportance.LOW]?.size ?: 0,
            groups = applicable.map { it.group }.toSet()
        )
    }
}

/**
 * 📊 Statistics for a permission group
 */
data class PermissionGroupStats(
    val totalPermissions: Int,
    val applicablePermissions: Int,
    val highImportance: Int,
    val mediumImportance: Int,
    val lowImportance: Int,
    val groups: Set<PermissionGroup>
) {
    val applicabilityRate: Float 
        get() = if (totalPermissions > 0) applicablePermissions.toFloat() / totalPermissions else 0f
        
    val criticalityScore: Float
        get() = (highImportance * 3 + mediumImportance * 2 + lowImportance) / applicablePermissions.toFloat()
}
