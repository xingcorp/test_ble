package com.nordicbeacon.scanner.core.permissions

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.flow.Flow

/**
 * 🔐 Enterprise Permission Management System
 * 
 * Main interface for handling all permission-related operations across the application.
 * Follows SOLID principles and provides a fluent API for ease of use.
 * 
 * Design Principles Applied:
 * - Single Responsibility: Only handles permissions
 * - Open/Closed: Extensible through strategy pattern
 * - Liskov Substitution: Interface-based design
 * - Interface Segregation: Focused, minimal interface
 * - Dependency Inversion: Depends on abstractions
 * 
 * @author Senior Android Engineer
 */
interface PermissionManager {
    
    /**
     * 🏗️ Create a new permission request builder
     * 
     * @param activity The activity to request permissions from
     * @return A fluent request builder
     */
    fun with(activity: FragmentActivity): PermissionRequestBuilder
    
    /**
     * 🏗️ Create a new permission request builder
     * 
     * @param fragment The fragment to request permissions from  
     * @return A fluent request builder
     */
    fun with(fragment: Fragment): PermissionRequestBuilder
    
    /**
     * ✅ Check if permissions are granted synchronously
     * 
     * @param permissions The permissions to check
     * @return true if all permissions are granted
     */
    suspend fun arePermissionsGranted(vararg permissions: Permission): Boolean
    
    /**
     * ✅ Check if a specific permission is granted
     * 
     * @param permission The permission to check
     * @return true if permission is granted
     */
    suspend fun isPermissionGranted(permission: Permission): Boolean
    
    /**
     * 📊 Get current status of multiple permissions
     * 
     * @param permissions The permissions to check status for
     * @return Map of permission to grant status
     */
    suspend fun getPermissionsStatus(vararg permissions: Permission): Map<Permission, Boolean>
    
    /**
     * 📋 Get missing permissions from a list
     * 
     * @param permissions The permissions to check
     * @return List of permissions that are not granted
     */
    suspend fun getMissingPermissions(vararg permissions: Permission): List<Permission>
    
    /**
     * 🔄 Observe permission changes over time
     * 
     * @param permissions The permissions to observe
     * @return Flow that emits when permission status changes
     */
    fun observePermissions(vararg permissions: Permission): Flow<Map<Permission, Boolean>>
    
    /**
     * 🧹 Clear any cached permission state
     */
    fun clearCache()
    
    /**
     * 📊 Get permission request statistics
     */
    suspend fun getPermissionStats(): PermissionStats
    
    companion object {
        
        /**
         * 🏭 Factory method to create PermissionManager instance
         * Uses default implementation with dependency injection
         */
        fun create(): PermissionManager {
            return PermissionManagerFactory.create()
        }
    }
}

/**
 * 🏗️ Fluent Permission Request Builder
 * 
 * Provides a clean, readable API for building permission requests
 * with optional configuration like rationale, timeouts, etc.
 */
interface PermissionRequestBuilder {
    
    /**
     * 📋 Request a single permission
     */
    fun request(permission: Permission): PermissionRequestBuilder
    
    /**
     * 📋 Request multiple permissions
     */
    fun request(vararg permissions: Permission): PermissionRequestBuilder
    
    /**
     * 📋 Request all permissions from a group
     */
    fun request(group: PermissionGroup): PermissionRequestBuilder
    
    /**
     * 📚 Show educational content before requesting
     */
    fun educate(shouldEducate: Boolean = true): PermissionRequestBuilder
    
    /**
     * 📚 Provide custom rationale text
     */
    fun rationale(text: String): PermissionRequestBuilder
    
    /**
     * 📚 Provide custom rationale with title and message
     */
    fun rationale(title: String, message: String): PermissionRequestBuilder
    
    /**
     * ⏰ Set request timeout
     */
    fun timeout(timeoutMs: Long): PermissionRequestBuilder
    
    /**
     * 🔄 Allow automatic retry on temporary denial
     */
    fun autoRetry(maxRetries: Int = 1): PermissionRequestBuilder
    
    /**
     * 📊 Enable detailed result information
     */
    fun detailed(enabled: Boolean = true): PermissionRequestBuilder
    
    // ========== EXECUTION METHODS ==========
    
    /**
     * ✅ Execute request and handle success
     */
    fun onGranted(handler: (PermissionResult.Granted) -> Unit): PermissionRequestBuilder
    
    /**
     * ❌ Handle permission denial
     */
    fun onDenied(handler: (PermissionResult.Denied) -> Unit): PermissionRequestBuilder
    
    /**
     * ⏹️ Handle request cancellation
     */
    fun onCancelled(handler: (PermissionResult.Cancelled) -> Unit): PermissionRequestBuilder
    
    /**
     * ⚠️ Handle errors
     */
    fun onError(handler: (PermissionResult.Error) -> Unit): PermissionRequestBuilder
    
    /**
     * 📊 Handle any result type
     */
    fun onResult(handler: (PermissionResult) -> Unit): PermissionRequestBuilder
    
    /**
     * 🎯 Simple handler for success/failure
     */
    fun onComplete(handler: (granted: List<Permission>, denied: List<Permission>) -> Unit): PermissionRequestBuilder
    
    /**
     * 🚀 Execute the permission request (fire and forget)
     */
    fun check()
    
    /**
     * 🚀 Execute the permission request and return result
     */
    suspend fun execute(): PermissionResult
}

/**
 * 🔧 Permission Strategy Interface
 * 
 * Handles version-specific and manufacturer-specific permission logic
 * using the Strategy pattern for clean separation of concerns.
 */
interface PermissionStrategy {
    
    /**
     * Get required permissions for current Android version
     */
    fun getRequiredPermissions(permission: Permission): List<String>
    
    /**
     * Check if should show rationale
     */
    fun shouldShowRationale(activity: FragmentActivity, permission: String): Boolean
    
    /**
     * Check if permission is granted
     */
    fun isPermissionGranted(activity: FragmentActivity, permission: String): Boolean
    
    /**
     * Request permissions from system
     */
    fun requestPermissions(
        activity: FragmentActivity, 
        permissions: Array<String>, 
        requestCode: Int
    )
    
    /**
     * Handle special permissions (like SYSTEM_ALERT_WINDOW)
     */
    fun handleSpecialPermission(permission: Permission): PermissionResult?
    
    /**
     * Check if permission is permanently denied
     */
    fun isPermanentlyDenied(activity: FragmentActivity, permission: String): Boolean
}

/**
 * 📚 Permission Education Provider
 * 
 * Provides educational content and rationale for permissions
 * to improve user understanding and grant rates.
 */
interface PermissionEducator {
    
    /**
     * Get educational content for permission
     */
    fun getEducationalContent(permission: Permission): PermissionEducationContent
    
    /**
     * Get educational content for permission group
     */
    fun getGroupEducationalContent(group: PermissionGroup): PermissionEducationContent
    
    /**
     * Check if should show education for permission
     */
    suspend fun shouldShowEducation(permission: Permission): Boolean
    
    /**
     * Mark education as shown for permission
     */
    suspend fun markEducationShown(permission: Permission)
}

/**
 * 📊 Permission Repository
 * 
 * Handles persistence of permission-related data like
 * user preferences, education history, etc.
 */
interface PermissionRepository {
    
    /**
     * Save permission grant history
     */
    suspend fun savePermissionHistory(permission: Permission, granted: Boolean)
    
    /**
     * Get permission grant history
     */
    suspend fun getPermissionHistory(permission: Permission): List<PermissionHistoryEntry>
    
    /**
     * Save education shown status
     */
    suspend fun saveEducationShown(permission: Permission)
    
    /**
     * Check if education was shown
     */
    suspend fun wasEducationShown(permission: Permission): Boolean
    
    /**
     * Clear all permission data
     */
    suspend fun clearAll()
    
    /**
     * Get permission request statistics
     */
    suspend fun getPermissionStats(): PermissionStats
}

/**
 * 🏭 Factory for creating PermissionManager instances
 */
object PermissionManagerFactory {
    
    private var instance: PermissionManager? = null
    
    /**
     * Create or get singleton instance
     */
    fun create(): PermissionManager {
        return instance ?: synchronized(this) {
            instance ?: createInstance().also { instance = it }
        }
    }
    
    /**
     * Set custom implementation for testing
     */
    fun setInstance(manager: PermissionManager) {
        instance = manager
    }
    
    /**
     * Clear instance for testing
     */
    fun clearInstance() {
        instance = null
    }
    
    private fun createInstance(): PermissionManager {
        // This will be implemented with proper DI
        throw NotImplementedError("Use dependency injection to provide PermissionManager implementation")
    }
}

/**
 * 📚 Educational content data class
 */
data class PermissionEducationContent(
    val permission: Permission,
    val title: String,
    val description: String,
    val rationale: String,
    val benefits: List<String>,
    val icon: String? = null,
    val priority: PermissionImportance = PermissionImportance.MEDIUM
)

/**
 * 📊 Permission history entry
 */
data class PermissionHistoryEntry(
    val permission: Permission,
    val granted: Boolean,
    val timestamp: Long,
    val requestSource: String
)
