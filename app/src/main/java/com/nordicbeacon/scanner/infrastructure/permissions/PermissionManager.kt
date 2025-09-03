package com.nordicbeacon.scanner.infrastructure.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 🔐 Permission Management System
 * 
 * Centralized permission handling cho Nordic beacon scanning functionality
 * Implements clean separation của permission logic từ UI components
 * 
 * Key Responsibilities:
 * - Required permission checking
 * - Permission status analysis
 * - Missing permission identification
 * - Educational content provision
 * 
 * Following Single Responsibility Principle cho permission management
 * 
 * @author Senior Android Developer
 */
@Singleton
class PermissionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        /**
         * 📋 Nordic Beacon Scanner Required Permissions
         */
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
        )
        
        /**
         * 📱 Modern Android permissions (API 31+)
         */
        private val MODERN_BLE_PERMISSIONS = arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT
        )
        
        /**
         * 🔋 Critical permissions cho background operation
         */
        private val CRITICAL_PERMISSIONS = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION,
            Manifest.permission.BLUETOOTH_SCAN
        )
    }

    // ========== PERMISSION CHECKING ==========

    /**
     * ✅ Check if all required permissions are granted
     */
    fun hasRequiredPermissions(): Boolean {
        val allPermissions = getRequiredPermissionsForDevice()
        
        return allPermissions.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }.also { hasAll ->
            Timber.d("🔐 Required permissions check: ${if (hasAll) "✅ All granted" else "❌ Missing permissions"}")
        }
    }

    /**
     * 📋 Get list of missing permissions
     */
    fun getMissingPermissions(): List<String> {
        val allPermissions = getRequiredPermissionsForDevice()
        
        return allPermissions.filter { permission ->
            ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED
        }.also { missing ->
            if (missing.isNotEmpty()) {
                Timber.w("⚠️ Missing permissions: ${missing.joinToString(", ")}")
            }
        }
    }

    /**
     * 🎯 Check if critical permissions for Nordic scanning are granted
     */
    fun hasCriticalPermissions(): Boolean {
        return CRITICAL_PERMISSIONS.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * 📱 Get device-appropriate permissions based on Android version
     */
    private fun getRequiredPermissionsForDevice(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+ (API 31+) - new BLE permissions
            REQUIRED_PERMISSIONS + MODERN_BLE_PERMISSIONS
        } else {
            // Legacy Android - traditional permissions
            REQUIRED_PERMISSIONS
        }
    }

    // ========== PERMISSION ANALYSIS ==========

    /**
     * 📊 Generate permission status report
     */
    fun generatePermissionReport(): PermissionReport {
        val allPermissions = getRequiredPermissionsForDevice()
        val grantedPermissions = mutableListOf<String>()
        val deniedPermissions = mutableListOf<String>()

        allPermissions.forEach { permission ->
            if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
                grantedPermissions.add(permission)
            } else {
                deniedPermissions.add(permission)
            }
        }

        return PermissionReport(
            totalRequired = allPermissions.size,
            granted = grantedPermissions,
            denied = deniedPermissions,
            hasCriticalMissing = deniedPermissions.any { it in CRITICAL_PERMISSIONS },
            canScanBeacons = hasCriticalPermissions(),
            deviceApiLevel = Build.VERSION.SDK_INT
        )
    }

    /**
     * 📚 Get permission educational content
     */
    fun getPermissionEducationalContent(permission: String): PermissionEducationContent {
        return when (permission) {
            Manifest.permission.ACCESS_FINE_LOCATION -> PermissionEducationContent(
                permission = permission,
                title = "Location Access Required",
                explanation = "Nordic beacon detection requires precise location access to identify nearby BLE beacons.",
                whyNeeded = "Bluetooth beacon scanning is classified as location-sensitive by Android for privacy protection.",
                userBenefit = "Enables accurate Nordic beacon detection và distance calculation."
            )
            
            Manifest.permission.ACCESS_BACKGROUND_LOCATION -> PermissionEducationContent(
                permission = permission,
                title = "Background Location Access",
                explanation = "Allow Nordic beacon scanning when the app is closed hoặc screen is off.",
                whyNeeded = "Continuous beacon monitoring requires background location access on modern Android versions.",
                userBenefit = "Ensures Nordic beacon detection continues even when phone is locked hoặc app is minimized."
            )
            
            Manifest.permission.BLUETOOTH_SCAN -> PermissionEducationContent(
                permission = permission,
                title = "Bluetooth Scanning Permission",
                explanation = "Required cho scanning nearby Bluetooth devices including Nordic beacons.",
                whyNeeded = "Android 12+ requires explicit permission cho Bluetooth Low Energy scanning operations.",
                userBenefit = "Enables detection của Nordic beacons trong your vicinity."
            )
            
            else -> PermissionEducationContent.generic(permission)
        }
    }

    // ========== PERMISSION HELPERS ==========

    /**
     * 🎯 Check specific permission status
     */
    fun isPermissionGranted(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * 📋 Get permission display name cho UI
     */
    fun getPermissionDisplayName(permission: String): String {
        return when (permission) {
            Manifest.permission.ACCESS_FINE_LOCATION -> "Precise Location"
            Manifest.permission.ACCESS_BACKGROUND_LOCATION -> "Background Location"
            Manifest.permission.BLUETOOTH -> "Bluetooth Access"
            Manifest.permission.BLUETOOTH_ADMIN -> "Bluetooth Administration"
            Manifest.permission.BLUETOOTH_SCAN -> "Bluetooth Scanning"
            Manifest.permission.BLUETOOTH_CONNECT -> "Bluetooth Connection"
            else -> permission.substringAfterLast(".")
        }
    }
}

// ========== DATA MODELS ==========

/**
 * 📊 Permission Status Report
 */
data class PermissionReport(
    val totalRequired: Int,
    val granted: List<String>,
    val denied: List<String>,
    val hasCriticalMissing: Boolean,
    val canScanBeacons: Boolean,
    val deviceApiLevel: Int
) {
    val grantedCount: Int get() = granted.size
    val deniedCount: Int get() = denied.size
    val completionPercentage: Int get() = ((grantedCount.toFloat() / totalRequired) * 100).toInt()
    
    fun isFullyGranted(): Boolean = denied.isEmpty()
    fun hasCriticalIssues(): Boolean = hasCriticalMissing
}

/**
 * 📚 Permission Education Content
 */
data class PermissionEducationContent(
    val permission: String,
    val title: String,
    val explanation: String,
    val whyNeeded: String,
    val userBenefit: String
) {
    companion object {
        fun generic(permission: String) = PermissionEducationContent(
            permission = permission,
            title = "Permission Required",
            explanation = "This permission is needed cho Nordic beacon scanning functionality.",
            whyNeeded = "Required by Android cho accessing device capabilities.",
            userBenefit = "Enables full Nordic beacon detection features."
        )
    }
}
