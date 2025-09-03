package com.nordicbeacon.scanner.core.permissions

/**
 * 🎯 Type-safe Permission Results
 * 
 * Comprehensive result handling for permission requests using sealed classes
 * for compile-time safety and exhaustive when handling.
 * 
 * @author Senior Android Engineer
 */
sealed class PermissionResult {
    
    /**
     * ✅ All requested permissions were granted
     */
    data class Granted(
        val permissions: List<Permission>
    ) : PermissionResult() {
        
        val permissionStrings: List<String> 
            get() = permissions.map { it.manifestPermission }
            
        fun hasPermission(permission: Permission): Boolean = permission in permissions
        
        fun hasAllPermissions(vararg permissions: Permission): Boolean = 
            permissions.all { it in this.permissions }
    }
    
    /**
     * ❌ Some or all permissions were denied
     */
    data class Denied(
        val deniedPermissions: List<Permission>,
        val permanentlyDenied: List<Permission> = emptyList(),
        val grantedPermissions: List<Permission> = emptyList()
    ) : PermissionResult() {
        
        val hasPermanentlyDenied: Boolean get() = permanentlyDenied.isNotEmpty()
        val hasTemporaryDenied: Boolean get() = deniedPermissions.isNotEmpty()
        val hasPartialGrant: Boolean get() = grantedPermissions.isNotEmpty()
        
        /**
         * Check if specific permission was permanently denied
         */
        fun isPermanentlyDenied(permission: Permission): Boolean = permission in permanentlyDenied
        
        /**
         * Check if can show rationale for any denied permissions
         */
        fun canShowRationale(): Boolean = hasTemporaryDenied && !hasPermanentlyDenied
        
        /**
         * Get the next best action user should take
         */
        fun getRecommendedAction(): PermissionAction {
            return when {
                hasPermanentlyDenied -> PermissionAction.OPEN_SETTINGS
                hasTemporaryDenied -> PermissionAction.REQUEST_AGAIN
                else -> PermissionAction.NONE
            }
        }
    }
    
    /**
     * ⏹️ Permission request was cancelled by user or system
     */
    data class Cancelled(
        val reason: CancellationReason,
        val requestedPermissions: List<Permission>
    ) : PermissionResult()
    
    /**
     * ⚠️ Error occurred during permission request
     */
    data class Error(
        val exception: PermissionException,
        val requestedPermissions: List<Permission>
    ) : PermissionResult()
    
    // ========== COMMON OPERATIONS ==========
    
    /**
     * Check if result indicates success (all permissions granted)
     */
    fun isSuccess(): Boolean = this is Granted
    
    /**
     * Check if result indicates failure
     */
    fun isFailure(): Boolean = this is Denied || this is Error || this is Cancelled
    
    /**
     * Get all permissions that were involved in this result
     */
    fun getAllPermissions(): List<Permission> = when (this) {
        is Granted -> permissions
        is Denied -> deniedPermissions + grantedPermissions + permanentlyDenied
        is Cancelled -> requestedPermissions
        is Error -> requestedPermissions
    }
    
    /**
     * Get granted permissions from result
     */
    fun getAllGrantedPermissions(): List<Permission> = when (this) {
        is Granted -> permissions
        is Denied -> grantedPermissions
        else -> emptyList()
    }
    
    /**
     * Transform result using provided handlers
     */
    inline fun <T> fold(
        onGranted: (Granted) -> T,
        onDenied: (Denied) -> T,
        onCancelled: (Cancelled) -> T,
        onError: (Error) -> T
    ): T = when (this) {
        is Granted -> onGranted(this)
        is Denied -> onDenied(this)
        is Cancelled -> onCancelled(this)
        is Error -> onError(this)
    }
}

/**
 * 🎬 Cancellation reasons for better UX handling
 */
enum class CancellationReason(val description: String) {
    USER_CANCELLED("User cancelled permission request"),
    ACTIVITY_DESTROYED("Activity was destroyed during request"),
    SYSTEM_CANCELLED("System cancelled the request"),
    TIMEOUT("Request timed out"),
    INVALID_REQUEST("Invalid permission request configuration")
}

/**
 * 🎯 Recommended actions based on permission result
 */
enum class PermissionAction(
    val actionText: String,
    val description: String
) {
    NONE("Continue", "No action needed"),
    REQUEST_AGAIN("Try Again", "Request permission again with explanation"),
    OPEN_SETTINGS("Open Settings", "Open app settings to grant permission"),
    USE_ALTERNATIVE("Continue Anyway", "Continue with limited functionality"),
    EXPLAIN_IMPORTANCE("Learn More", "Show detailed explanation why permission is needed")
}

/**
 * ⚡ Comprehensive Permission Exceptions
 */
sealed class PermissionException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    
    /**
     * Activity reference is null or destroyed
     */
    object ActivityNotFound : PermissionException("Activity not found or destroyed")
    
    /**
     * Unsupported Android API level
     */
    data class UnsupportedApiLevel(
        val requiredApi: Int,
        val currentApi: Int
    ) : PermissionException("Permission requires API $requiredApi, current: $currentApi")
    
    /**
     * Invalid permission specified
     */
    data class InvalidPermission(
        val permission: String
    ) : PermissionException("Invalid permission: $permission")
    
    /**
     * Another permission request is already in progress
     */
    data class RequestInProgress(
        val activePermissions: List<Permission>
    ) : PermissionException("Permission request already in progress for: ${activePermissions.joinToString()}")
    
    /**
     * Request timed out
     */
    data class RequestTimeout(
        val timeoutMs: Long
    ) : PermissionException("Permission request timed out after ${timeoutMs}ms")
    
    /**
     * System denied permission request
     */
    data class SystemDenied(
        val reason: String
    ) : PermissionException("System denied permission request: $reason")
    
    /**
     * Permission not declared in manifest
     */
    data class PermissionNotDeclared(
        val permission: Permission
    ) : PermissionException("Permission ${permission.manifestPermission} not declared in AndroidManifest.xml")
    
    /**
     * Generic runtime error
     */
    data class RuntimeError(
        val originalException: Throwable
    ) : PermissionException("Runtime error during permission request", originalException)
}

/**
 * 📊 Permission request statistics for monitoring
 */
data class PermissionStats(
    val requestedCount: Int,
    val grantedCount: Int,
    val deniedCount: Int,
    val permanentlyDeniedCount: Int,
    val requestDurationMs: Long
) {
    val grantRate: Float get() = if (requestedCount > 0) grantedCount.toFloat() / requestedCount else 0f
    val denyRate: Float get() = if (requestedCount > 0) deniedCount.toFloat() / requestedCount else 0f
    val permanentDenyRate: Float get() = if (requestedCount > 0) permanentlyDeniedCount.toFloat() / requestedCount else 0f
    
    companion object {
        fun empty() = PermissionStats(0, 0, 0, 0, 0L)
    }
}
