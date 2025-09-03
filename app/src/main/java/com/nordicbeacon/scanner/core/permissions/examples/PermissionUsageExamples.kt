package com.nordicbeacon.scanner.core.permissions.examples

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.nordicbeacon.scanner.core.permissions.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * 📚 Permission Manager Usage Examples
 * 
 * Comprehensive examples showing how to use the new enterprise-grade
 * PermissionManager in various scenarios and use cases.
 * 
 * @author Senior Android Engineer
 */
@AndroidEntryPoint
class PermissionUsageExamples : FragmentActivity() {
    
    @Inject
    lateinit var permissionManager: PermissionManager
    
    // ========== BASIC USAGE EXAMPLES ==========
    
    /**
     * 🚀 Example 1: Simple Single Permission Request
     * 
     * Most basic usage - request one permission with callback.
     */
    fun example1_SimpleSinglePermission() {
        permissionManager
            .with(this)
            .request(Permission.CAMERA)
            .onGranted { 
                Timber.i("📸 Camera permission granted - starting camera") 
                startCameraFeature()
            }
            .onDenied { denied ->
                Timber.w("📸 Camera permission denied: ${denied.deniedPermissions}")
                showCameraPermissionExplanation()
            }
            .check()
    }
    
    /**
     * 🚀 Example 2: Multiple Permissions with Rationale
     * 
     * Request multiple permissions with custom explanation.
     */
    fun example2_MultiplePermissionsWithRationale() {
        permissionManager
            .with(this)
            .request(Permission.CAMERA, Permission.STORAGE_READ, Permission.STORAGE_WRITE)
            .rationale(
                "Camera and Storage Access", 
                "We need camera access to take photos and storage access to save them to your device."
            )
            .onComplete { granted, denied ->
                Timber.i("📋 Permission result - Granted: ${granted.size}, Denied: ${denied.size}")
                
                if (granted.contains(Permission.CAMERA)) {
                    enableCameraFeatures()
                }
                
                if (granted.containsAll(listOf(Permission.STORAGE_READ, Permission.STORAGE_WRITE))) {
                    enableStorageFeatures()
                } else {
                    showLimitedStorageMode()
                }
            }
            .check()
    }
    
    /**
     * 🚀 Example 3: BLE Scanning Permissions
     * 
     * Request all permissions needed for BLE beacon scanning.
     */
    fun example3_BlePermissions() {
        permissionManager
            .with(this)
            .request(*PermissionGroups.BLE_SCANNING_ESSENTIAL.toTypedArray())
            .educate(true) // Show educational content
            .rationale("BLE Beacon Scanning", "This app needs Bluetooth and location permissions to detect Nordic beacons.")
            .onGranted { result ->
                Timber.i("🔵 BLE permissions granted: ${result.permissions}")
                startBeaconScanning()
            }
            .onDenied { result ->
                handleBlePermissionDenial(result)
            }
            .check()
    }
    
    /**
     * 🚀 Example 4: Async Permission Checking
     * 
     * Use coroutines for async permission operations.
     */
    fun example4_AsyncPermissionChecking() {
        lifecycleScope.launch {
            try {
                // Check current status first
                val hasCamera = permissionManager.isPermissionGranted(Permission.CAMERA)
                val hasLocation = permissionManager.isPermissionGranted(Permission.LOCATION_FINE)
                
                Timber.i("📊 Current permissions - Camera: $hasCamera, Location: $hasLocation")
                
                if (!hasCamera || !hasLocation) {
                    // Request missing permissions
                    val result = permissionManager
                        .with(this@PermissionUsageExamples)
                        .request(Permission.CAMERA, Permission.LOCATION_FINE)
                        .timeout(30000) // 30 second timeout
                        .execute()
                    
                    handleAsyncResult(result)
                }
            } catch (e: Exception) {
                Timber.e(e, "Error in async permission checking")
            }
        }
    }
    
    // ========== ADVANCED USAGE EXAMPLES ==========
    
    /**
     * 🎯 Example 5: Permission Group with Education
     * 
     * Request permission group with automatic educational content.
     */
    fun example5_PermissionGroupWithEducation() {
        permissionManager
            .with(this)
            .request(PermissionGroup.LOCATION)
            .educate(true)
            .autoRetry(1) // Retry once if initially denied
            .detailed(true) // Get detailed result information
            .onResult { result ->
                when (result) {
                    is PermissionResult.Granted -> {
                        Timber.i("📍 All location permissions granted")
                        enableLocationFeatures()
                    }
                    is PermissionResult.Denied -> {
                        if (result.hasPermanentlyDenied) {
                            showSettingsDialog()
                        } else if (result.canShowRationale()) {
                            showLocationRationale()
                        }
                    }
                    is PermissionResult.Cancelled -> {
                        Timber.w("📍 Location permission request cancelled")
                    }
                    is PermissionResult.Error -> {
                        Timber.e("📍 Location permission request error: ${result.exception}")
                    }
                }
            }
            .check()
    }
    
    /**
     * 🎯 Example 6: Comprehensive Permission Strategy
     * 
     * Complete permission handling with fallback strategies.
     */
    fun example6_ComprehensiveStrategy() {
        lifecycleScope.launch {
            // Step 1: Check current status
            val currentStatus = permissionManager.getPermissionsStatus(
                Permission.LOCATION_FINE,
                Permission.BLUETOOTH_SCAN,
                Permission.CAMERA
            )
            
            val missing = currentStatus.filter { !it.value }.keys.toList()
            
            if (missing.isEmpty()) {
                // All permissions already granted
                Timber.i("✅ All required permissions already granted")
                startAllFeatures()
                return@launch
            }
            
            // Step 2: Request missing permissions with comprehensive handling
            val result = permissionManager
                .with(this@PermissionUsageExamples)
                .request(*missing.toTypedArray())
                .educate(true)
                .rationale("Complete App Experience", "These permissions enable full app functionality.")
                .autoRetry(2)
                .timeout(60000) // 1 minute timeout
                .execute()
            
            // Step 3: Handle comprehensive result
            result.fold(
                onGranted = { granted ->
                    Timber.i("✅ Permissions granted: ${granted.permissions}")
                    startAllFeatures()
                },
                onDenied = { denied ->
                    handleComprehensiveDenial(denied)
                },
                onCancelled = { cancelled ->
                    Timber.w("⏹️ Permission request cancelled: ${cancelled.reason}")
                    showPermissionImportanceDialog()
                },
                onError = { error ->
                    Timber.e("⚠️ Permission request error: ${error.exception}")
                    showErrorDialog(error.exception.message ?: "Unknown error")
                }
            )
        }
    }
    
    /**
     * 🎯 Example 7: Background Permission Handling
     * 
     * Special handling for background location permission.
     */
    fun example7_BackgroundPermissions() {
        // First request foreground location
        permissionManager
            .with(this)
            .request(Permission.LOCATION_FINE, Permission.LOCATION_COARSE)
            .rationale("Location Access", "We need location access to provide proximity-based features.")
            .onGranted { _ ->
                // After foreground granted, request background
                requestBackgroundLocation()
            }
            .onDenied { denied ->
                Timber.w("📍 Foreground location denied, cannot proceed to background")
                showLocationExplanation()
            }
            .check()
    }
    
    private fun requestBackgroundLocation() {
        permissionManager
            .with(this)
            .request(Permission.LOCATION_BACKGROUND)
            .rationale(
                "Background Location", 
                "Allow background location access for continuous beacon monitoring when app is closed."
            )
            .onGranted { _ ->
                Timber.i("🔋 Background location granted - enabling continuous scanning")
                enableContinuousScanning()
            }
            .onDenied { denied ->
                Timber.w("🔋 Background location denied - using foreground-only mode")
                enableForegroundOnlyMode()
            }
            .check()
    }
    
    /**
     * 🎯 Example 8: Permission Status Monitoring
     * 
     * Monitor permission changes over time.
     */
    fun example8_PermissionMonitoring() {
        lifecycleScope.launch {
            // Observe permission changes
            permissionManager.observePermissions(
                Permission.LOCATION_FINE,
                Permission.BLUETOOTH_SCAN,
                Permission.CAMERA
            ).collect { statusMap ->
                Timber.i("📊 Permission status changed: $statusMap")
                
                val granted = statusMap.filter { it.value }.keys
                val denied = statusMap.filter { !it.value }.keys
                
                // Adapt features based on current permissions
                adaptFeaturesBasedOnPermissions(granted, denied)
            }
        }
    }
    
    // ========== HELPER METHODS ==========
    
    private fun startCameraFeature() {
        Timber.i("📸 Starting camera functionality")
    }
    
    private fun showCameraPermissionExplanation() {
        Timber.i("📸 Showing camera permission explanation dialog")
    }
    
    private fun enableCameraFeatures() {
        Timber.i("📸 Enabling camera-related features")
    }
    
    private fun enableStorageFeatures() {
        Timber.i("💾 Enabling full storage features")
    }
    
    private fun showLimitedStorageMode() {
        Timber.i("💾 Switching to limited storage mode")
    }
    
    private fun startBeaconScanning() {
        Timber.i("🔵 Starting BLE beacon scanning")
    }
    
    private fun handleBlePermissionDenial(result: PermissionResult.Denied) {
        if (result.hasPermanentlyDenied) {
            Timber.w("🔵 BLE permissions permanently denied - showing settings dialog")
            showSettingsDialog()
        } else {
            Timber.w("🔵 BLE permissions temporarily denied - showing explanation")
            showBlePermissionExplanation()
        }
    }
    
    private fun handleAsyncResult(result: PermissionResult) {
        when (result) {
            is PermissionResult.Granted -> {
                Timber.i("✅ Async permission request succeeded")
            }
            is PermissionResult.Denied -> {
                Timber.w("❌ Async permission request denied")
            }
            else -> {
                Timber.e("⚠️ Async permission request failed: $result")
            }
        }
    }
    
    private fun enableLocationFeatures() {
        Timber.i("📍 Enabling location-based features")
    }
    
    private fun showSettingsDialog() {
        Timber.i("⚙️ Showing settings dialog for permanently denied permissions")
    }
    
    private fun showLocationRationale() {
        Timber.i("📍 Showing location permission rationale")
    }
    
    private fun startAllFeatures() {
        Timber.i("🚀 Starting all application features")
    }
    
    private fun handleComprehensiveDenial(denied: PermissionResult.Denied) {
        val critical = denied.deniedPermissions.filter { 
            it.group.importance == PermissionImportance.HIGH 
        }
        
        if (critical.isNotEmpty()) {
            Timber.w("⚠️ Critical permissions denied: $critical")
            showCriticalPermissionDialog(critical)
        } else {
            Timber.i("ℹ️ Non-critical permissions denied - continuing with limited features")
            enableLimitedMode()
        }
    }
    
    private fun showPermissionImportanceDialog() {
        Timber.i("ℹ️ Showing permission importance explanation")
    }
    
    private fun showErrorDialog(message: String) {
        Timber.e("⚠️ Showing error dialog: $message")
    }
    
    private fun enableContinuousScanning() {
        Timber.i("🔋 Enabling continuous background scanning")
    }
    
    private fun enableForegroundOnlyMode() {
        Timber.i("📱 Enabling foreground-only scanning mode")
    }
    
    private fun adaptFeaturesBasedOnPermissions(granted: Set<Permission>, denied: Set<Permission>) {
        Timber.i("🔄 Adapting features - Granted: ${granted.size}, Denied: ${denied.size}")
        
        // Enable/disable features based on current permission status
        if (Permission.CAMERA in granted) {
            enableCameraFeatures()
        }
        
        if (Permission.LOCATION_FINE in granted || Permission.LOCATION_COARSE in granted) {
            enableLocationFeatures()
        }
        
        if (Permission.BLUETOOTH_SCAN in granted) {
            startBeaconScanning()
        }
    }
    
    private fun showLocationExplanation() {
        Timber.i("📍 Showing location permission explanation")
    }
    
    private fun showBlePermissionExplanation() {
        Timber.i("🔵 Showing BLE permission explanation")
    }
    
    private fun showCriticalPermissionDialog(critical: List<Permission>) {
        Timber.w("⚠️ Showing critical permission dialog for: $critical")
    }
    
    private fun enableLimitedMode() {
        Timber.i("🔒 Enabling limited functionality mode")
    }
}

/**
 * 📚 Additional Usage Examples in Different Contexts
 */
object PermissionUsagePatterns {
    
    /**
     * 🎯 Pattern 1: Service-based Permission Checking
     */
    suspend fun checkPermissionsInService(permissionManager: PermissionManager): Boolean {
        return permissionManager.arePermissionsGranted(
            Permission.LOCATION_FINE,
            Permission.BLUETOOTH_SCAN
        )
    }
    
    /**
     * 🎯 Pattern 2: Feature Toggle Based on Permissions
     */
    suspend fun configureAppBasedOnPermissions(permissionManager: PermissionManager): AppConfiguration {
        val locationGranted = permissionManager.isPermissionGranted(Permission.LOCATION_FINE)
        val bluetoothGranted = permissionManager.isPermissionGranted(Permission.BLUETOOTH_SCAN)
        val cameraGranted = permissionManager.isPermissionGranted(Permission.CAMERA)
        
        return AppConfiguration(
            enableLocationFeatures = locationGranted,
            enableBluetoothScanning = bluetoothGranted,
            enableCameraFeatures = cameraGranted,
            featureLevel = when {
                locationGranted && bluetoothGranted && cameraGranted -> FeatureLevel.FULL
                locationGranted && bluetoothGranted -> FeatureLevel.CORE
                locationGranted || bluetoothGranted -> FeatureLevel.LIMITED
                else -> FeatureLevel.BASIC
            }
        )
    }
    
    /**
     * 🎯 Pattern 3: Progressive Permission Requests
     */
    fun requestPermissionsProgressively(
        activity: FragmentActivity,
        permissionManager: PermissionManager
    ) {
        // Step 1: Request essential permissions
        permissionManager
            .with(activity)
            .request(*PermissionGroups.BLE_SCANNING_ESSENTIAL.toTypedArray())
            .onGranted { _ ->
                // Step 2: Request enhancement permissions
                requestEnhancementPermissions(activity, permissionManager)
            }
            .onDenied { _ ->
                Timber.w("Essential permissions denied - using basic mode")
            }
            .check()
    }
    
    private fun requestEnhancementPermissions(
        activity: FragmentActivity,
        permissionManager: PermissionManager
    ) {
        permissionManager
            .with(activity)
            .request(Permission.LOCATION_BACKGROUND, Permission.POST_NOTIFICATIONS)
            .rationale("Enhanced Features", "These additional permissions enable premium features.")
            .onComplete { granted, denied ->
                Timber.i("Enhancement permissions - Granted: ${granted.size}, Denied: ${denied.size}")
            }
            .check()
    }
}

// ========== DATA CLASSES FOR EXAMPLES ==========

data class AppConfiguration(
    val enableLocationFeatures: Boolean,
    val enableBluetoothScanning: Boolean,
    val enableCameraFeatures: Boolean,
    val featureLevel: FeatureLevel
)

enum class FeatureLevel {
    BASIC,      // No permissions
    LIMITED,    // Some permissions
    CORE,       // Essential permissions
    FULL        // All permissions
}
