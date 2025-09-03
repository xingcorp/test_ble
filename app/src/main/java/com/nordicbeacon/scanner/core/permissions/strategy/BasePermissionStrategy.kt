package com.nordicbeacon.scanner.core.permissions.strategy

import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.nordicbeacon.scanner.core.permissions.Permission
import com.nordicbeacon.scanner.core.permissions.PermissionResult
import com.nordicbeacon.scanner.core.permissions.PermissionStrategy
import timber.log.Timber

/**
 * 🏗️ Base Permission Strategy Implementation
 * 
 * Provides common permission handling logic that can be extended
 * by version-specific strategies. Follows Template Method pattern.
 * 
 * @author Senior Android Engineer
 */
abstract class BasePermissionStrategy : PermissionStrategy {
    
    /**
     * ✅ Check if permission is granted
     */
    override fun isPermissionGranted(activity: FragmentActivity, permission: String): Boolean {
        return try {
            ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED
        } catch (e: Exception) {
            Timber.e(e, "Error checking permission: $permission")
            false
        }
    }
    
    /**
     * 📚 Check if should show rationale
     */
    override fun shouldShowRationale(activity: FragmentActivity, permission: String): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
            } catch (e: Exception) {
                Timber.e(e, "Error checking rationale for permission: $permission")
                false
            }
        } else {
            false
        }
    }
    
    /**
     * 🚀 Request permissions from system
     */
    override fun requestPermissions(
        activity: FragmentActivity,
        permissions: Array<String>,
        requestCode: Int
    ) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                activity.requestPermissions(permissions, requestCode)
            } else {
                Timber.d("Permissions auto-granted on pre-M devices: ${permissions.contentToString()}")
            }
        } catch (e: Exception) {
            Timber.e(e, "Error requesting permissions: ${permissions.contentToString()}")
        }
    }
    
    /**
     * 🔒 Check if permission is permanently denied
     */
    override fun isPermanentlyDenied(activity: FragmentActivity, permission: String): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            !isPermissionGranted(activity, permission) && 
            !shouldShowRationale(activity, permission) &&
            wasPermissionRequested(activity, permission)
        } else {
            false
        }
    }
    
    /**
     * 🏷️ Get required permissions for current version (template method)
     */
    override fun getRequiredPermissions(permission: Permission): List<String> {
        return if (permission.isApplicableForCurrentVersion()) {
            getVersionSpecificPermissions(permission)
        } else {
            emptyList()
        }
    }
    
    /**
     * 🎯 Handle special permissions (template method)
     */
    override fun handleSpecialPermission(permission: Permission): PermissionResult? {
        return handleVersionSpecificSpecialPermission(permission)
    }
    
    // ========== TEMPLATE METHODS ==========
    
    /**
     * Get version-specific permissions (to be implemented by subclasses)
     */
    protected abstract fun getVersionSpecificPermissions(permission: Permission): List<String>
    
    /**
     * Handle version-specific special permissions (optional override)
     */
    protected open fun handleVersionSpecificSpecialPermission(permission: Permission): PermissionResult? {
        return null
    }
    
    /**
     * Check if permission was previously requested (helper method)
     */
    protected open fun wasPermissionRequested(activity: FragmentActivity, permission: String): Boolean {
        // This is a simplified check - in real implementation you might want to use SharedPreferences
        // or a more sophisticated tracking mechanism
        return try {
            // Check if the app has been installed for some time
            val packageInfo = activity.packageManager.getPackageInfo(activity.packageName, 0)
            val installTime = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
                packageInfo.firstInstallTime
            } else {
                0L
            }
            System.currentTimeMillis() - installTime > 60000 // More than 1 minute
        } catch (e: Exception) {
            Timber.e(e, "Error checking if permission was requested")
            false
        }
    }
    
    // ========== UTILITY METHODS ==========
    
    /**
     * 📊 Log permission status for debugging
     */
    protected fun logPermissionStatus(permission: String, status: String) {
        Timber.d("Permission $permission: $status")
    }
    
    /**
     * 🛡️ Safe permission check with error handling
     */
    protected fun safeCheckPermission(activity: FragmentActivity, permission: String): Boolean {
        return try {
            ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED
        } catch (e: SecurityException) {
            Timber.w(e, "SecurityException checking permission: $permission")
            false
        } catch (e: Exception) {
            Timber.e(e, "Unexpected error checking permission: $permission")
            false
        }
    }
    
    /**
     * 🔧 Get system feature availability
     */
    protected fun hasSystemFeature(activity: FragmentActivity, feature: String): Boolean {
        return try {
            activity.packageManager.hasSystemFeature(feature)
        } catch (e: Exception) {
            Timber.e(e, "Error checking system feature: $feature")
            false
        }
    }
}

/**
 * 📱 Legacy Permission Strategy (API < 23)
 * 
 * For devices running Android versions before runtime permissions.
 * All permissions are granted at install time.
 */
class LegacyPermissionStrategy : BasePermissionStrategy() {
    
    override fun getVersionSpecificPermissions(permission: Permission): List<String> {
        // On legacy devices, all permissions are install-time permissions
        return listOf(permission.manifestPermission)
    }
    
    override fun isPermissionGranted(activity: FragmentActivity, permission: String): Boolean {
        // On pre-M devices, permissions are granted at install time
        return true
    }
    
    override fun shouldShowRationale(activity: FragmentActivity, permission: String): Boolean {
        // No runtime permissions on legacy devices
        return false
    }
    
    override fun requestPermissions(activity: FragmentActivity, permissions: Array<String>, requestCode: Int) {
        // No runtime permission requests on legacy devices
        Timber.d("Auto-granted permissions on legacy device: ${permissions.contentToString()}")
    }
    
    override fun isPermanentlyDenied(activity: FragmentActivity, permission: String): Boolean {
        // Cannot be permanently denied on legacy devices
        return false
    }
}
