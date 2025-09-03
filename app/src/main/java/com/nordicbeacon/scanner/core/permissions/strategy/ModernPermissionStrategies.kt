package com.nordicbeacon.scanner.core.permissions.strategy

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.fragment.app.FragmentActivity
import com.nordicbeacon.scanner.core.permissions.Permission
import com.nordicbeacon.scanner.core.permissions.PermissionResult
import com.nordicbeacon.scanner.core.permissions.PermissionStrategy
import timber.log.Timber

/**
 * 📱 Modern Permission Strategy (API 23+)
 * 
 * Handles runtime permissions introduced in Android M.
 * Base implementation for modern Android versions.
 */
open class ModernPermissionStrategy : BasePermissionStrategy() {
    
    override fun getVersionSpecificPermissions(permission: Permission): List<String> {
        return when (permission) {
            // Location permissions
            Permission.LOCATION_FINE -> listOf(permission.manifestPermission)
            Permission.LOCATION_COARSE -> listOf(permission.manifestPermission)
            Permission.LOCATION_BACKGROUND -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    listOf(permission.manifestPermission)
                } else {
                    emptyList() // Not available before Q
                }
            }
            
            // Bluetooth permissions (legacy)
            Permission.BLUETOOTH -> listOf(permission.manifestPermission)
            Permission.BLUETOOTH_ADMIN -> listOf(permission.manifestPermission)
            
            // Modern Bluetooth permissions (not available in this API level)
            Permission.BLUETOOTH_SCAN,
            Permission.BLUETOOTH_CONNECT,
            Permission.BLUETOOTH_ADVERTISE -> emptyList()
            
            // Standard permissions
            Permission.CAMERA -> listOf(permission.manifestPermission)
            Permission.RECORD_AUDIO -> listOf(permission.manifestPermission)
            Permission.READ_CONTACTS -> listOf(permission.manifestPermission)
            Permission.WRITE_CONTACTS -> listOf(permission.manifestPermission)
            Permission.PHONE_STATE -> listOf(permission.manifestPermission)
            Permission.CALL_PHONE -> listOf(permission.manifestPermission)
            
            // Storage permissions
            Permission.STORAGE_READ -> listOf(permission.manifestPermission)
            Permission.STORAGE_WRITE -> listOf(permission.manifestPermission)
            
            // Not available in this API level
            else -> emptyList()
        }
    }
}

/**
 * 📱 Android 10 Permission Strategy (API 29+)
 * 
 * Handles scoped storage and background location permission separation.
 */
@RequiresApi(Build.VERSION_CODES.Q)
open class Android10PermissionStrategy : ModernPermissionStrategy() {
    
    override fun getVersionSpecificPermissions(permission: Permission): List<String> {
        return when (permission) {
            // Background location now separate
            Permission.LOCATION_BACKGROUND -> listOf(permission.manifestPermission)
            
            // Storage permissions behavior change
            Permission.STORAGE_WRITE -> {
                // WRITE_EXTERNAL_STORAGE has limited effect on Q+
                listOf(permission.manifestPermission)
            }
            
            Permission.STORAGE_MANAGE -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    listOf(permission.manifestPermission)
                } else {
                    emptyList()
                }
            }
            
            else -> super.getVersionSpecificPermissions(permission)
        }
    }
    
    override fun handleVersionSpecificSpecialPermission(permission: Permission): PermissionResult? {
        return when (permission) {
            Permission.STORAGE_MANAGE -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    handleManageExternalStoragePermission()
                } else null
            }
            else -> null
        }
    }
    
    @RequiresApi(Build.VERSION_CODES.R)
    private fun handleManageExternalStoragePermission(): PermissionResult? {
        return if (Environment.isExternalStorageManager()) {
            PermissionResult.Granted(listOf(Permission.STORAGE_MANAGE))
        } else {
            null // Let normal flow handle it
        }
    }
}

/**
 * 📱 Android 12 Permission Strategy (API 31+)
 * 
 * Handles new Bluetooth permissions and privacy dashboard.
 */
@RequiresApi(Build.VERSION_CODES.S)
open class Android12PermissionStrategy : Android10PermissionStrategy() {
    
    override fun getVersionSpecificPermissions(permission: Permission): List<String> {
        return when (permission) {
            // New Bluetooth permissions
            Permission.BLUETOOTH_SCAN -> listOf(permission.manifestPermission)
            Permission.BLUETOOTH_CONNECT -> listOf(permission.manifestPermission)
            Permission.BLUETOOTH_ADVERTISE -> listOf(permission.manifestPermission)
            
            // Legacy Bluetooth permissions now limited
            Permission.BLUETOOTH,
            Permission.BLUETOOTH_ADMIN -> {
                // These are now install-time permissions
                listOf(permission.manifestPermission)
            }
            
            // Storage changes
            Permission.STORAGE_READ -> {
                // READ_EXTERNAL_STORAGE deprecated, use media permissions
                emptyList()
            }
            
            else -> super.getVersionSpecificPermissions(permission)
        }
    }
}

/**
 * 📱 Android 13 Permission Strategy (API 33+)
 * 
 * Handles granular media permissions and notification permission.
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
class Android13PermissionStrategy : Android12PermissionStrategy() {
    
    override fun getVersionSpecificPermissions(permission: Permission): List<String> {
        return when (permission) {
            // New media permissions
            Permission.MEDIA_IMAGES -> listOf(permission.manifestPermission)
            Permission.MEDIA_VIDEO -> listOf(permission.manifestPermission)
            Permission.MEDIA_AUDIO -> listOf(permission.manifestPermission)
            
            // New notification permission
            Permission.POST_NOTIFICATIONS -> listOf(permission.manifestPermission)
            
            // Phone number permission
            Permission.PHONE_NUMBERS -> listOf(permission.manifestPermission)
            
            // Storage read is now handled by media permissions
            Permission.STORAGE_READ -> {
                // Return media permissions instead
                listOf(
                    Permission.MEDIA_IMAGES.manifestPermission,
                    Permission.MEDIA_VIDEO.manifestPermission,
                    Permission.MEDIA_AUDIO.manifestPermission
                )
            }
            
            else -> super.getVersionSpecificPermissions(permission)
        }
    }
    
    override fun handleVersionSpecificSpecialPermission(permission: Permission): PermissionResult? {
        return when (permission) {
            Permission.POST_NOTIFICATIONS -> {
                handleNotificationPermission()
            }
            else -> super.handleSpecialPermission(permission)
        }
    }
    
    private fun handleNotificationPermission(): PermissionResult? {
        // Special handling for notification permission if needed
        return null
    }
}

/**
 * 🏗️ Manufacturer-Specific Strategy Decorator
 * 
 * Base class for manufacturer-specific permission handling decorators.
 * Uses Decorator pattern to add manufacturer-specific behavior.
 */
abstract class ManufacturerPermissionStrategyDecorator(
    private val baseStrategy: PermissionStrategy
) : PermissionStrategy {
    
    override fun getRequiredPermissions(permission: Permission): List<String> {
        return baseStrategy.getRequiredPermissions(permission)
    }
    
    override fun shouldShowRationale(activity: FragmentActivity, permission: String): Boolean {
        return baseStrategy.shouldShowRationale(activity, permission)
    }
    
    override fun isPermissionGranted(activity: FragmentActivity, permission: String): Boolean {
        return baseStrategy.isPermissionGranted(activity, permission)
    }
    
    override fun requestPermissions(activity: FragmentActivity, permissions: Array<String>, requestCode: Int) {
        baseStrategy.requestPermissions(activity, permissions, requestCode)
    }
    
    override fun handleSpecialPermission(permission: Permission): PermissionResult? {
        return baseStrategy.handleSpecialPermission(permission)
    }
    
    override fun isPermanentlyDenied(activity: FragmentActivity, permission: String): Boolean {
        return baseStrategy.isPermanentlyDenied(activity, permission)
    }
}

/**
 * 📱 Samsung-Specific Permission Strategy
 */
class SamsungPermissionStrategyDecorator(
    baseStrategy: PermissionStrategy
) : ManufacturerPermissionStrategyDecorator(baseStrategy) {
    
    override fun isPermanentlyDenied(activity: FragmentActivity, permission: String): Boolean {
        // Samsung devices sometimes show different behavior for permanently denied permissions
        val baseResult = super.isPermanentlyDenied(activity, permission)
        
        // Add Samsung-specific checks if needed
        return baseResult
    }
}

/**
 * 📱 Xiaomi-Specific Permission Strategy  
 */
class XiaomiPermissionStrategyDecorator(
    baseStrategy: PermissionStrategy
) : ManufacturerPermissionStrategyDecorator(baseStrategy) {
    
    override fun requestPermissions(activity: FragmentActivity, permissions: Array<String>, requestCode: Int) {
        // MIUI sometimes requires special handling
        try {
            super.requestPermissions(activity, permissions, requestCode)
        } catch (e: Exception) {
            Timber.w(e, "MIUI permission request failed, trying alternative")
            // Could implement MIUI-specific fallback
        }
    }
}

/**
 * 📱 Huawei-Specific Permission Strategy
 */
class HuaweiPermissionStrategyDecorator(
    baseStrategy: PermissionStrategy
) : ManufacturerPermissionStrategyDecorator(baseStrategy) {
    
    override fun shouldShowRationale(activity: FragmentActivity, permission: String): Boolean {
        // EMUI has different rationale behavior
        return super.shouldShowRationale(activity, permission)
    }
}

/**
 * 📱 OnePlus-Specific Permission Strategy
 */
class OnePlusPermissionStrategyDecorator(
    baseStrategy: PermissionStrategy
) : ManufacturerPermissionStrategyDecorator(baseStrategy)

/**
 * 📱 OPPO-Specific Permission Strategy
 */
class OppoPermissionStrategyDecorator(
    baseStrategy: PermissionStrategy
) : ManufacturerPermissionStrategyDecorator(baseStrategy)

/**
 * 📱 Vivo-Specific Permission Strategy
 */
class VivoPermissionStrategyDecorator(
    baseStrategy: PermissionStrategy
) : ManufacturerPermissionStrategyDecorator(baseStrategy)
