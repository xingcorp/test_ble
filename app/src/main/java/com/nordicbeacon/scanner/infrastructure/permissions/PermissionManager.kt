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
         * 📋 Sequential Permission Request Strategy
         * 
         * Research-based optimal order cho BLE beacon scanning:
         * 1. Basic location first (required cho all BLE scanning)
         * 2. BLE permissions (API 31+ dependency on location)
         * 3. Background location last (least likely to be granted)
         */
        
        // Step 1: Foundation permissions (always required)
        private val STEP_1_FOUNDATION = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        
        // Step 2: BLE permissions (API level dependent)  
        private val STEP_2_BLUETOOTH_LEGACY = arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN
        )
        
        private val STEP_2_BLUETOOTH_MODERN = arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT
        )
        
        // Step 3: Background permissions (most restrictive)
        private val STEP_3_BACKGROUND = arrayOf(
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
        )
        
        /**
         * 🔋 Critical permissions cho basic Nordic scanning
         */
        private val CRITICAL_PERMISSIONS = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.BLUETOOTH_SCAN // Modern Android
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
     * 🔍 Can start basic scanning (foundation + minimum BLE permissions)
     */
    fun canStartBasicScanning(): Boolean {
        val foundationGranted = hasPermissions(STEP_1_FOUNDATION)
        
        val bleGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            hasPermissions(arrayOf(Manifest.permission.BLUETOOTH_SCAN))
        } else {
            hasPermissions(STEP_2_BLUETOOTH_LEGACY)
        }
        
        return foundationGranted && bleGranted
    }

    /**
     * 📊 Get current sequential permission status
     */
    fun getSequentialPermissionStatus(): SequentialPermissionResult {
        val completedSteps = mutableListOf<PermissionStep>()
        val remainingSteps = mutableListOf<PermissionStep>()
        
        // Check each step
        val allSteps = listOf(
            PermissionStep.FOUNDATION,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PermissionStep.BLUETOOTH_MODERN else PermissionStep.BLUETOOTH_LEGACY,
            PermissionStep.BACKGROUND
        )
        
        var currentStep: PermissionStep? = null
        
        for (step in allSteps) {
            val permissions = getPermissionsForStep(step)
            if (hasPermissions(permissions)) {
                completedSteps.add(step)
            } else if (currentStep == null) {
                currentStep = step
                remainingSteps.add(step)
            } else {
                remainingSteps.add(step)
            }
        }
        
        return SequentialPermissionResult(
            completedSteps = completedSteps,
            currentStep = currentStep,
            remainingSteps = remainingSteps.drop(1), // Remaining after current
            canStartScanning = canStartBasicScanning(),
            hasOptimalPermissions = currentStep == null // All steps complete
        )
    }

    /**
     * 📋 Get next permission step that needs to be requested
     */
    fun getNextPermissionStep(): PermissionStep? {
        return getSequentialPermissionStatus().currentStep
    }

    /**
     * 📱 Get permissions for specific step
     */
    fun getPermissionsForStep(step: PermissionStep): Array<String> {
        return when (step) {
            PermissionStep.FOUNDATION -> STEP_1_FOUNDATION
            PermissionStep.BLUETOOTH_LEGACY -> STEP_2_BLUETOOTH_LEGACY  
            PermissionStep.BLUETOOTH_MODERN -> STEP_2_BLUETOOTH_MODERN
            PermissionStep.BACKGROUND -> STEP_3_BACKGROUND
        }
    }

    /**
     * 🎯 Check if specific permission set is granted
     */
    private fun hasPermissions(permissions: Array<String>): Boolean {
        return permissions.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * 📱 Get device-appropriate permissions based on Android version
     */
    private fun getRequiredPermissionsForDevice(): Array<String> {
        val allPermissions = mutableListOf<String>()
        
        // Always add foundation
        allPermissions.addAll(STEP_1_FOUNDATION)
        
        // Add BLE permissions based on API level
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            allPermissions.addAll(STEP_2_BLUETOOTH_MODERN)
        } else {
            allPermissions.addAll(STEP_2_BLUETOOTH_LEGACY)
        }
        
        // Add background
        allPermissions.addAll(STEP_3_BACKGROUND)
        
        return allPermissions.toTypedArray()
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
 * 📋 Permission Request Steps (Sequential Strategy)
 * 
 * Research-based order cho optimal user acceptance:
 * - Foundation first (most accepted)
 * - BLE permissions (medium acceptance)  
 * - Background last (most restrictive)
 */
enum class PermissionStep(val description: String, val userExplanation: String) {
    FOUNDATION(
        "Foundation Location", 
        "📍 Location access is required để detect Nordic beacons nearby"
    ),
    BLUETOOTH_LEGACY(
        "Bluetooth Access", 
        "📡 Bluetooth permission cho scanning Nordic beacon devices"
    ),
    BLUETOOTH_MODERN(
        "Modern Bluetooth", 
        "📱 Enhanced Bluetooth scanning permission (Android 12+)"
    ),
    BACKGROUND(
        "Background Scanning", 
        "🔋 Background permission cho continuous Nordic detection when screen off"
    )
}

/**
 * 📊 Sequential Permission Strategy Result
 */
data class SequentialPermissionResult(
    val completedSteps: List<PermissionStep>,
    val currentStep: PermissionStep?,
    val remainingSteps: List<PermissionStep>,
    val canStartScanning: Boolean, // Can start với basic permissions
    val hasOptimalPermissions: Boolean // Has all permissions cho best experience
)

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
