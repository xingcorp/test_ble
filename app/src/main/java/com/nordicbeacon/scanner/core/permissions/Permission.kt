package com.nordicbeacon.scanner.core.permissions

import android.Manifest
import android.os.Build

/**
 * 🔐 Comprehensive Permission Definitions
 * 
 * Enterprise-grade permission management supporting all Android versions
 * and device manufacturers. Follows SOLID principles for scalability.
 * 
 * @author Senior Android Engineer
 */
enum class Permission(
    val manifestPermission: String,
    val group: PermissionGroup,
    val minSdkVersion: Int = 1,
    val maxSdkVersion: Int = Int.MAX_VALUE,
    val isRuntime: Boolean = true
) {
    
    // ========== LOCATION PERMISSIONS ==========
    LOCATION_FINE(
        Manifest.permission.ACCESS_FINE_LOCATION,
        PermissionGroup.LOCATION,
        minSdkVersion = 1
    ),
    
    LOCATION_COARSE(
        Manifest.permission.ACCESS_COARSE_LOCATION,
        PermissionGroup.LOCATION,
        minSdkVersion = 1
    ),
    
    LOCATION_BACKGROUND(
        Manifest.permission.ACCESS_BACKGROUND_LOCATION,
        PermissionGroup.LOCATION,
        minSdkVersion = Build.VERSION_CODES.Q
    ),
    
    // ========== BLUETOOTH PERMISSIONS ==========
    BLUETOOTH(
        Manifest.permission.BLUETOOTH,
        PermissionGroup.BLUETOOTH,
        maxSdkVersion = Build.VERSION_CODES.R,
        isRuntime = false
    ),
    
    BLUETOOTH_ADMIN(
        Manifest.permission.BLUETOOTH_ADMIN,
        PermissionGroup.BLUETOOTH,
        maxSdkVersion = Build.VERSION_CODES.R,
        isRuntime = false
    ),
    
    BLUETOOTH_SCAN(
        Manifest.permission.BLUETOOTH_SCAN,
        PermissionGroup.BLUETOOTH,
        minSdkVersion = Build.VERSION_CODES.S
    ),
    
    BLUETOOTH_CONNECT(
        Manifest.permission.BLUETOOTH_CONNECT,
        PermissionGroup.BLUETOOTH,
        minSdkVersion = Build.VERSION_CODES.S
    ),
    
    BLUETOOTH_ADVERTISE(
        Manifest.permission.BLUETOOTH_ADVERTISE,
        PermissionGroup.BLUETOOTH,
        minSdkVersion = Build.VERSION_CODES.S
    ),
    
    // ========== CAMERA PERMISSIONS ==========
    CAMERA(
        Manifest.permission.CAMERA,
        PermissionGroup.CAMERA,
        minSdkVersion = 1
    ),
    
    // ========== STORAGE PERMISSIONS ==========
    STORAGE_READ(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        PermissionGroup.STORAGE,
        maxSdkVersion = Build.VERSION_CODES.S_V2
    ),
    
    STORAGE_WRITE(
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        PermissionGroup.STORAGE,
        maxSdkVersion = Build.VERSION_CODES.Q
    ),
    
    STORAGE_MANAGE(
        Manifest.permission.MANAGE_EXTERNAL_STORAGE,
        PermissionGroup.STORAGE,
        minSdkVersion = Build.VERSION_CODES.R
    ),
    
    // ========== MEDIA PERMISSIONS (Android 13+) ==========
    MEDIA_IMAGES(
        Manifest.permission.READ_MEDIA_IMAGES,
        PermissionGroup.MEDIA,
        minSdkVersion = Build.VERSION_CODES.TIRAMISU
    ),
    
    MEDIA_VIDEO(
        Manifest.permission.READ_MEDIA_VIDEO,
        PermissionGroup.MEDIA,
        minSdkVersion = Build.VERSION_CODES.TIRAMISU
    ),
    
    MEDIA_AUDIO(
        Manifest.permission.READ_MEDIA_AUDIO,
        PermissionGroup.MEDIA,
        minSdkVersion = Build.VERSION_CODES.TIRAMISU
    ),
    
    // ========== NOTIFICATION PERMISSIONS ==========
    POST_NOTIFICATIONS(
        Manifest.permission.POST_NOTIFICATIONS,
        PermissionGroup.NOTIFICATIONS,
        minSdkVersion = Build.VERSION_CODES.TIRAMISU
    ),
    
    // ========== FOREGROUND SERVICE PERMISSIONS ==========
    FOREGROUND_SERVICE_LOCATION(
        "android.permission.FOREGROUND_SERVICE_LOCATION",
        PermissionGroup.BLUETOOTH,  // Group with BLE since it's location-based service
        minSdkVersion = 34 // Android 14 (API 34)
    ),
    
    // ========== MICROPHONE PERMISSIONS ==========
    RECORD_AUDIO(
        Manifest.permission.RECORD_AUDIO,
        PermissionGroup.MICROPHONE,
        minSdkVersion = 1
    ),
    
    // ========== CONTACTS PERMISSIONS ==========
    READ_CONTACTS(
        Manifest.permission.READ_CONTACTS,
        PermissionGroup.CONTACTS,
        minSdkVersion = 1
    ),
    
    WRITE_CONTACTS(
        Manifest.permission.WRITE_CONTACTS,
        PermissionGroup.CONTACTS,
        minSdkVersion = 1
    ),
    
    // ========== PHONE PERMISSIONS ==========
    PHONE_STATE(
        Manifest.permission.READ_PHONE_STATE,
        PermissionGroup.PHONE,
        minSdkVersion = 1
    ),
    
    PHONE_NUMBERS(
        Manifest.permission.READ_PHONE_NUMBERS,
        PermissionGroup.PHONE,
        minSdkVersion = Build.VERSION_CODES.O
    ),
    
    CALL_PHONE(
        Manifest.permission.CALL_PHONE,
        PermissionGroup.PHONE,
        minSdkVersion = 1
    );
    
    /**
     * Check if permission is applicable for current Android version
     */
    fun isApplicableForCurrentVersion(): Boolean {
        return Build.VERSION.SDK_INT >= minSdkVersion && Build.VERSION.SDK_INT <= maxSdkVersion
    }
    
    /**
     * Check if permission requires runtime request
     */
    fun requiresRuntimeRequest(): Boolean {
        return isRuntime && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && isApplicableForCurrentVersion()
    }
    
    companion object {
        
        /**
         * Get all permissions for a specific group
         */
        fun getPermissionsForGroup(group: PermissionGroup): List<Permission> {
            return values().filter { it.group == group && it.isApplicableForCurrentVersion() }
        }
        
        /**
         * Get permission by manifest string
         */
        fun fromManifestPermission(manifestPermission: String): Permission? {
            return values().find { it.manifestPermission == manifestPermission }
        }
        
        /**
         * Get current version appropriate permissions for a group
         */
        fun getCurrentVersionPermissions(group: PermissionGroup): List<Permission> {
            return getPermissionsForGroup(group).filter { it.requiresRuntimeRequest() }
        }
    }
}

/**
 * 📂 Permission Groups for logical organization
 */
enum class PermissionGroup(
    val displayName: String,
    val description: String,
    val importance: PermissionImportance
) {
    
    LOCATION(
        "Location Access",
        "Access to device location for proximity and navigation features",
        PermissionImportance.HIGH
    ),
    
    BLUETOOTH(
        "Bluetooth Access", 
        "Access to Bluetooth functionality for device scanning and connection",
        PermissionImportance.HIGH
    ),
    
    CAMERA(
        "Camera Access",
        "Access to camera for photo and video capture",
        PermissionImportance.MEDIUM
    ),
    
    STORAGE(
        "Storage Access",
        "Access to device storage for file operations",
        PermissionImportance.MEDIUM
    ),
    
    MEDIA(
        "Media Access",
        "Access to photos, videos and audio files",
        PermissionImportance.MEDIUM
    ),
    
    NOTIFICATIONS(
        "Notification Permission",
        "Permission to show notifications to user",
        PermissionImportance.LOW
    ),
    
    MICROPHONE(
        "Microphone Access",
        "Access to microphone for audio recording",
        PermissionImportance.MEDIUM
    ),
    
    CONTACTS(
        "Contacts Access",
        "Access to device contacts",
        PermissionImportance.LOW
    ),
    
    PHONE(
        "Phone Access", 
        "Access to phone state and calling functionality",
        PermissionImportance.LOW
    )
}

/**
 * 📊 Permission importance levels for UX prioritization
 */
enum class PermissionImportance {
    HIGH,    // Critical for core app functionality
    MEDIUM,  // Important for enhanced features
    LOW      // Optional or convenience features
}
