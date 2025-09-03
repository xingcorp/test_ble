package com.nordicbeacon.scanner.core.permissions.migration

import androidx.fragment.app.FragmentActivity
import com.nordicbeacon.scanner.core.permissions.*
import com.nordicbeacon.scanner.infrastructure.permissions.PermissionManager as LegacyPermissionManager
import timber.log.Timber
import javax.inject.Inject

/**
 * 🔄 Permission Migration Utility
 * 
 * Utility class to help migrate from the old BLE-specific PermissionManager
 * to the new generic, enterprise-grade permission system.
 * 
 * Provides helper functions and migration patterns for smooth transition.
 * 
 * @author Senior Android Engineer
 */
class PermissionMigrationUtility @Inject constructor(
    private val newPermissionManager: PermissionManager,
    private val legacyPermissionManager: LegacyPermissionManager
) {
    
    // ========== MIGRATION HELPERS ==========
    
    /**
     * 🔄 Migrate BLE scanning permissions from legacy system
     * 
     * Converts old BLE-specific permission checking to new generic system.
     */
    suspend fun migrateBlePermissions(activity: FragmentActivity): PermissionResult {
        Timber.i("🔄 Starting BLE permission migration")
        
        return try {
            // Use new system with predefined BLE group
            val result = newPermissionManager
                .with(activity)
                .request(*PermissionGroups.BLE_SCANNING_ESSENTIAL.toTypedArray())
                .educate(true)
                .rationale(
                    "BLE Permissions Required",
                    "This app needs Bluetooth and location permissions to scan for Nordic beacons and other BLE devices."
                )
                .execute()
            
            // Log migration success
            when (result) {
                is PermissionResult.Granted -> {
                    Timber.i("✅ BLE permission migration successful: ${result.permissions.size} permissions granted")
                }
                is PermissionResult.Denied -> {
                    Timber.w("⚠️ BLE permission migration partial: ${result.grantedPermissions.size} granted, ${result.deniedPermissions.size} denied")
                }
                else -> {
                    Timber.e("❌ BLE permission migration failed: $result")
                }
            }
            
            result
            
        } catch (e: Exception) {
            Timber.e(e, "❌ BLE permission migration error")
            PermissionResult.Error(
                PermissionException.RuntimeError(e),
                PermissionGroups.BLE_SCANNING_ESSENTIAL.toList()
            )
        }
    }
    
    /**
     * 🔄 Compare legacy and new permission status
     * 
     * Diagnostic utility to compare results between old and new systems.
     */
    suspend fun comparePermissionStatus(activity: FragmentActivity): MigrationComparisonResult {
        return try {
            // Check legacy system
            val legacyHasPermissions = legacyPermissionManager.hasRequiredPermissions()
            val legacyMissingPermissions = legacyPermissionManager.getMissingPermissions()
            val legacyCanStartScanning = legacyPermissionManager.canStartBasicScanning()
            
            // Check new system
            val blePermissions = PermissionGroups.BLE_SCANNING_ESSENTIAL
            val newPermissionStatus = newPermissionManager.getPermissionsStatus(*blePermissions.toTypedArray())
            val newHasAllPermissions = newPermissionStatus.values.all { it }
            
            MigrationComparisonResult(
                legacyHasAll = legacyHasPermissions,
                legacyMissing = legacyMissingPermissions.size,
                legacyCanScan = legacyCanStartScanning,
                newHasAll = newHasAllPermissions,
                newGrantedCount = newPermissionStatus.values.count { it },
                newTotalCount = newPermissionStatus.size,
                statusMatch = legacyHasPermissions == newHasAllPermissions,
                detailedComparison = createDetailedComparison(legacyMissingPermissions, newPermissionStatus)
            )
            
        } catch (e: Exception) {
            Timber.e(e, "Error comparing permission status")
            MigrationComparisonResult.error(e.message ?: "Unknown error")
        }
    }
    
    private fun createDetailedComparison(
        legacyMissing: List<String>,
        newStatus: Map<Permission, Boolean>
    ): Map<String, PermissionComparisonStatus> {
        val result = mutableMapOf<String, PermissionComparisonStatus>()
        
        // Map legacy permissions to new permissions
        val permissionMapping = mapOf(
            "android.permission.ACCESS_FINE_LOCATION" to Permission.LOCATION_FINE,
            "android.permission.BLUETOOTH" to Permission.BLUETOOTH,
            "android.permission.BLUETOOTH_ADMIN" to Permission.BLUETOOTH_ADMIN,
            "android.permission.BLUETOOTH_SCAN" to Permission.BLUETOOTH_SCAN,
            "android.permission.BLUETOOTH_CONNECT" to Permission.BLUETOOTH_CONNECT
        )
        
        permissionMapping.forEach { (legacyPermission, newPermission) ->
            val legacyMissing = legacyPermission in legacyMissing
            val newGranted = newStatus[newPermission] ?: false
            
            result[legacyPermission] = PermissionComparisonStatus(
                legacyMissing = legacyMissing,
                newGranted = newGranted,
                matches = !legacyMissing == newGranted
            )
        }
        
        return result
    }
    
    // ========== CONVERSION UTILITIES ==========
    
    /**
     * 🔄 Convert legacy permission request to new system
     * 
     * Helper function to convert old-style permission requests.
     */
    fun convertLegacyRequest(activity: FragmentActivity): PermissionRequestBuilder {
        return newPermissionManager
            .with(activity)
            .request(*PermissionGroups.BLE_SCANNING_ESSENTIAL.toTypedArray())
            .educate(true)
    }
    
    /**
     * 🎯 Get equivalent new permissions for legacy check
     * 
     * Maps legacy permission checking logic to new system.
     */
    fun getEquivalentPermissions(): Set<Permission> {
        return PermissionGroups.BLE_SCANNING_ESSENTIAL
    }
    
    /**
     * 📊 Generate migration report
     * 
     * Creates comprehensive report of migration status and recommendations.
     */
    suspend fun generateMigrationReport(activity: FragmentActivity): MigrationReport {
        val comparison = comparePermissionStatus(activity)
        val legacyReport = legacyPermissionManager.generatePermissionReport()
        
        return MigrationReport(
            timestamp = System.currentTimeMillis(),
            comparisonResult = comparison,
            legacyReport = LegacyReportSummary(
                totalRequired = legacyReport.totalRequired,
                grantedCount = legacyReport.grantedCount,
                deniedCount = legacyReport.deniedCount,
                canScanBeacons = legacyReport.canScanBeacons,
                completionPercentage = legacyReport.completionPercentage
            ),
            newSystemStatus = newPermissionManager.getPermissionsStatus(
                *PermissionGroups.BLE_SCANNING_ESSENTIAL.toTypedArray()
            ),
            recommendations = generateRecommendations(comparison)
        )
    }
    
    private fun generateRecommendations(comparison: MigrationComparisonResult): List<String> {
        val recommendations = mutableListOf<String>()
        
        if (!comparison.statusMatch) {
            recommendations.add("⚠️ Permission status mismatch detected between legacy and new systems")
            recommendations.add("🔧 Consider running full permission re-check with new system")
        }
        
        if (comparison.legacyCanScan && !comparison.newHasAll) {
            recommendations.add("🔍 Legacy system reports scanning capability but new system shows missing permissions")
            recommendations.add("🔄 Recommend using new system for more accurate permission checking")
        }
        
        if (!comparison.newHasAll) {
            recommendations.add("📋 Request missing permissions using new PermissionManager.with(activity).request(PermissionGroups.BLE_SCANNING_ESSENTIAL)")
        }
        
        if (comparison.newGrantedCount > 0 && comparison.newGrantedCount < comparison.newTotalCount) {
            recommendations.add("🎯 Partial permissions granted - consider graceful degradation or explain missing permissions")
        }
        
        return recommendations
    }
    
    // ========== MIGRATION WORKFLOW ==========
    
    /**
     * 🚀 Complete migration workflow
     * 
     * Executes full migration from legacy to new permission system.
     */
    suspend fun executeMigration(
        activity: FragmentActivity,
        onProgress: (String) -> Unit = {},
        onComplete: (MigrationResult) -> Unit
    ) {
        try {
            onProgress("🔄 Starting permission system migration...")
            
            // Step 1: Generate pre-migration report
            onProgress("📊 Analyzing current permission status...")
            val preMigrationReport = generateMigrationReport(activity)
            
            // Step 2: Migrate BLE permissions
            onProgress("🔐 Migrating BLE permissions...")
            val migrationResult = migrateBlePermissions(activity)
            
            // Step 3: Generate post-migration report
            onProgress("📊 Generating post-migration report...")
            val postMigrationReport = generateMigrationReport(activity)
            
            // Step 4: Create final result
            val result = MigrationResult(
                success = migrationResult.isSuccess(),
                preMigrationReport = preMigrationReport,
                postMigrationReport = postMigrationReport,
                permissionResult = migrationResult,
                duration = postMigrationReport.timestamp - preMigrationReport.timestamp
            )
            
            onProgress("✅ Migration completed successfully!")
            onComplete(result)
            
        } catch (e: Exception) {
            Timber.e(e, "Migration workflow failed")
            onComplete(MigrationResult.failure(e))
        }
    }
}

// ========== DATA CLASSES ==========

/**
 * 📊 Migration comparison result
 */
data class MigrationComparisonResult(
    val legacyHasAll: Boolean,
    val legacyMissing: Int,
    val legacyCanScan: Boolean,
    val newHasAll: Boolean,
    val newGrantedCount: Int,
    val newTotalCount: Int,
    val statusMatch: Boolean,
    val detailedComparison: Map<String, PermissionComparisonStatus>,
    val error: String? = null
) {
    companion object {
        fun error(message: String) = MigrationComparisonResult(
            false, 0, false, false, 0, 0, false, emptyMap(), message
        )
    }
}

/**
 * 📋 Permission comparison status
 */
data class PermissionComparisonStatus(
    val legacyMissing: Boolean,
    val newGranted: Boolean,
    val matches: Boolean
)

/**
 * 📊 Legacy report summary
 */
data class LegacyReportSummary(
    val totalRequired: Int,
    val grantedCount: Int,
    val deniedCount: Int,
    val canScanBeacons: Boolean,
    val completionPercentage: Int
)

/**
 * 📄 Complete migration report
 */
data class MigrationReport(
    val timestamp: Long,
    val comparisonResult: MigrationComparisonResult,
    val legacyReport: LegacyReportSummary,
    val newSystemStatus: Map<Permission, Boolean>,
    val recommendations: List<String>
)

/**
 * 🎯 Final migration result
 */
data class MigrationResult(
    val success: Boolean,
    val preMigrationReport: MigrationReport,
    val postMigrationReport: MigrationReport,
    val permissionResult: PermissionResult,
    val duration: Long,
    val error: String? = null
) {
    companion object {
        fun failure(exception: Exception) = MigrationResult(
            success = false,
            preMigrationReport = MigrationReport(0, MigrationComparisonResult.error("Failed"), LegacyReportSummary(0, 0, 0, false, 0), emptyMap(), emptyList()),
            postMigrationReport = MigrationReport(0, MigrationComparisonResult.error("Failed"), LegacyReportSummary(0, 0, 0, false, 0), emptyMap(), emptyList()),
            permissionResult = PermissionResult.Error(PermissionException.RuntimeError(exception), emptyList()),
            duration = 0,
            error = exception.message
        )
    }
}
