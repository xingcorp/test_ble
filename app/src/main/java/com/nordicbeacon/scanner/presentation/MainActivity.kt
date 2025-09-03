package com.nordicbeacon.scanner.presentation

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.nordicbeacon.scanner.R
import com.nordicbeacon.scanner.infrastructure.services.BeaconScanningService
import com.nordicbeacon.scanner.presentation.viewmodels.BeaconScanViewModel
import com.nordicbeacon.scanner.infrastructure.oem.coordination.BatteryOptimizationCoordinator
import com.nordicbeacon.scanner.infrastructure.oem.education.UserEducationHelper
import com.nordicbeacon.scanner.infrastructure.workers.BeaconScanWorker
import com.nordicbeacon.scanner.analytics.AnalyticsIntegrationManager
import com.nordicbeacon.scanner.analytics.dashboard.AnalyticsDashboardActivity
import com.nordicbeacon.scanner.core.permissions.PermissionManager
import com.nordicbeacon.scanner.core.permissions.PermissionGroups
import com.nordicbeacon.scanner.core.permissions.PermissionResult
import androidx.work.WorkManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * 📱 Main Activity - Nordic Beacon Scanner
 * 
 * Primary UI entry point cho Nordic beacon scanning application
 * Handles permission flow và service lifecycle management
 * 
 * Key Features:
 * - Multi-version permission handling (API 21 → 34)
 * - Professional permission education flow
 * - Service lifecycle management
 * - Real-time beacon detection display
 * - Error handling với user-friendly messages
 * 
 * @author Senior Android Developer
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // ========== VIEW MODEL & STATE ==========
    
    private val viewModel: BeaconScanViewModel by viewModels()
    
    @Inject lateinit var batteryOptimizationCoordinator: BatteryOptimizationCoordinator
    @Inject lateinit var userEducationHelper: UserEducationHelper
    @Inject lateinit var analyticsIntegrationManager: AnalyticsIntegrationManager
    @Inject lateinit var permissionManager: PermissionManager
    
    // ========== PERMISSION HANDLING (NEW SYSTEM) ==========
    
    // Old permission launcher (deprecated - replaced with new fluent API)
    /*
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        handlePermissionResults(permissions)
    }
    */

    // ========== ACTIVITY LIFECYCLE ==========

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Timber.i("📱 MainActivity created")
        Timber.i("🎯 Target Nordic UUID: ${com.nordicbeacon.scanner.domain.entities.NordicBeacon.NORDIC_UUID}")
        
        // For now, simple content view - will implement proper UI layout later
        setContentView(R.layout.activity_main)
        
        // Initialize analytics system
        initializeAnalytics()
        
        // Initialize UI và observers
        initializeUI()
        observeViewModel()
        
        // Check permissions và start scanning if ready
        checkPermissionsAndStartScanning()
        
        // Handle battery optimization intent action
        handleBatteryOptimizationIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        Timber.d("📱 MainActivity resumed")
        
        // Check if service is still running
        viewModel.checkServiceStatus()
    }

    override fun onPause() {
        super.onPause()
        Timber.d("📱 MainActivity paused")
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.i("📱 MainActivity destroyed")
    }

    // ========== UI INITIALIZATION ==========

    /**
     * 🎨 Initialize UI components
     */
    private fun initializeUI() {
        Timber.d("🎨 Initializing UI components...")
        
        // Initialize button click listeners (layout already set trong onCreate)
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_start_scanning)?.setOnClickListener {
            Timber.i("🚀 Start scanning button clicked")
            startBeaconScanning()
        }
        
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_stop_scanning)?.setOnClickListener {
            Timber.i("⏹️ Stop scanning button clicked") 
            stopBeaconScanning()
        }
        
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_clear_history)?.setOnClickListener {
            Timber.i("🧹 Clear history button clicked")
            clearDetectionHistory()
        }
        
        findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.fab_scan_toggle)?.setOnClickListener {
            Timber.i("📱 FAB scan toggle clicked")
            toggleScanning()
        }
        
        Timber.i("✅ UI components initialized với click listeners")
    }

    /**
     * 👁️ Setup ViewModel observers
     */
    private fun observeViewModel() {
        Timber.d("👁️ Setting up ViewModel observers...")
        
        // Observe scanning state
        lifecycleScope.launch {
            viewModel.scanningState.collect { state ->
                Timber.i("🔄 Scanning state changed: $state")
                handleScanningStateChange(state)
            }
        }
        
        // Observe beacon detections
        lifecycleScope.launch {
            viewModel.beaconDetections.collect { beacons ->
                if (beacons.isNotEmpty()) {
                    Timber.i("🎯 Received ${beacons.size} beacon detection(s)")
                    handleBeaconDetections(beacons)
                }
            }
        }
        
        // Observe permission status
        lifecycleScope.launch {
            viewModel.permissionStatus.collect { status ->
                Timber.d("🔐 Permission status updated: $status")
                handlePermissionStatusChange(status)
            }
        }
        
        // Observe errors
        lifecycleScope.launch {
            viewModel.errors.collect { error ->
                error?.let {
                    Timber.e("❌ ViewModel error: ${it.message}")
                    handleViewModelError(it)
                }
            }
        }
        
        Timber.i("✅ ViewModel observers setup completed")
    }

    // ========== PERMISSION MANAGEMENT ==========

    /**
     * 🔐 Check permissions và start scanning process (New System)
     */
    private fun checkPermissionsAndStartScanning() {
        Timber.i("🔐 Checking required Nordic BLE scanning permissions...")
        
        // Use new fluent permission API
        permissionManager
            .with(this)
            .request(*PermissionGroups.BLE_SCANNING_COMPLETE.toTypedArray())
            .educate(true)
            .rationale(
                "Nordic Beacon Scanning", 
                "This app requires Bluetooth and location permissions to detect Nordic beacons in your area."
            )
            .onGranted { result ->
                Timber.i("✅ All Nordic scanning permissions granted: ${result.permissions.size}")
                startBeaconScanningService()
            }
            .onDenied { result ->
                handlePermissionDenialResult(result)
            }
            .onError { error ->
                Timber.e("❌ Permission request error: ${error.exception}")
                showPermissionErrorDialog(error)
            }
            .check()
    }

    /**
     * 🎯 Request BLE permissions using new system
     */
    private fun requestBlePermissions() {
        permissionManager
            .with(this)
            .request(*PermissionGroups.BLE_SCANNING_ESSENTIAL.toTypedArray())
            .educate(true)
            .rationale("Nordic BLE Scanning", "Required for Nordic beacon detection")
            .onResult { result ->
                handlePermissionResult(result)
            }
            .check()
    }

    /**
     * 🔄 Handle permission result from new system
     */
    private fun handlePermissionResult(result: PermissionResult) {
        when (result) {
            is PermissionResult.Granted -> {
                Timber.i("✅ Permissions granted: ${result.permissions.size}")
                startBeaconScanningService()
            }
            is PermissionResult.Denied -> {
                handlePermissionDenialResult(result)
            }
            is PermissionResult.Cancelled -> {
                Timber.w("⏹️ Permission request cancelled")
                showPermissionImportanceDialog()
            }
            is PermissionResult.Error -> {
                Timber.e("❌ Permission error: ${result.exception}")
                showPermissionErrorDialog(result)
            }
        }
    }

    /**
     * ❌ Handle permission denial with smart responses
     */
    private fun handlePermissionDenialResult(result: PermissionResult.Denied) {
        when (result.getRecommendedAction()) {
            com.nordicbeacon.scanner.core.permissions.PermissionAction.OPEN_SETTINGS -> {
                showPermissionSettingsDialog()
            }
            com.nordicbeacon.scanner.core.permissions.PermissionAction.REQUEST_AGAIN -> {
                showRetryPermissionDialog()
            }
            else -> {
                if (result.hasPartialGrant) {
                    showPartialPermissionDialog(result.grantedPermissions)
                } else {
                    showPermissionSettingsDialog()
                }
            }
        }
    }

    /**
     * ⚠️ Show permission error dialog
     */
    private fun showPermissionErrorDialog(error: PermissionResult.Error) {
        try {
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Permission Error")
                .setMessage("Error requesting permissions: ${error.exception.message}")
                .setPositiveButton("Try Again") { _, _ -> checkPermissionsAndStartScanning() }
                .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
                .show()
        } catch (e: Exception) {
            Timber.e(e, "Failed to show error dialog")
        }
    }

    /**
     * 🔄 Show retry permission dialog
     */
    private fun showRetryPermissionDialog() {
        try {
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("🔐 Nordic Beacon Permissions")
                .setMessage("Nordic beacon detection needs Bluetooth and location permissions.\n\nWould you like to try granting them again?")
                .setPositiveButton("✅ Try Again") { _, _ -> requestBlePermissions() }
                .setNeutralButton("⚙️ Settings") { _, _ -> openAppSettings() }
                .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
                .show()
        } catch (e: Exception) {
            Timber.e(e, "Failed to show retry dialog")
        }
    }

    /**
     * 🎯 Show partial permission dialog
     */
    private fun showPartialPermissionDialog(grantedPermissions: List<com.nordicbeacon.scanner.core.permissions.Permission>) {
        try {
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("⚠️ Partial Permissions")
                .setMessage("Some permissions granted (${grantedPermissions.size}), but Nordic scanning needs all permissions for optimal detection.\n\nContinue with limited functionality?")
                .setPositiveButton("🚀 Continue") { _, _ -> startBeaconScanningService() }
                .setNeutralButton("🔄 Request All") { _, _ -> requestBlePermissions() }
                .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
                .show()
        } catch (e: Exception) {
            Timber.e(e, "Failed to show partial permission dialog")
        }
    }

    /**
     * ℹ️ Show permission importance dialog
     */
    private fun showPermissionImportanceDialog() {
        try {
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("🎯 Nordic Beacon Detection")
                .setMessage("This app is specifically designed for Nordic beacon scanning.\n\nWithout Bluetooth and location permissions, the app cannot function.\n\nWould you like to grant permissions now?")
                .setPositiveButton("✅ Grant") { _, _ -> requestBlePermissions() }
                .setNegativeButton("Exit") { _, _ -> finish() }
                .setCancelable(false)
                .show()
        } catch (e: Exception) {
            Timber.e(e, "Failed to show importance dialog")
        }
    }

    // ========== OLD PERMISSION METHODS (DEPRECATED) ==========
    // These methods are kept commented for reference but replaced with new system
    
    /*
     * 🙏 Request missing permissions từ user (Sequential Strategy) - OLD SYSTEM
     */
    /*
    private fun requestMissingPermissions() {
        val permissionStatus = permissionManager.getSequentialPermissionStatus()
        
        // If can already start basic scanning, inform user
        if (permissionStatus.canStartScanning) {
            Timber.i("✅ Basic scanning permissions available - can start Nordic detection")
            // Continue với available permissions
            return
        }
        
        // Get next permission step to request
        val nextStep = permissionStatus.currentStep
        if (nextStep == null) {
            Timber.i("✅ All permissions granted!")
            return
        }
        
        Timber.i("🔄 Sequential permission request - Step: ${nextStep.description}")
        
        // Request specific step permissions
        val stepPermissions = permissionManager.getPermissionsForStep(nextStep)
        
        // Check if we should show rationale
        val shouldShowRationale = stepPermissions.any { permission ->
            shouldShowRequestPermissionRationale(permission)
        }
        
        if (shouldShowRationale) {
            // Show educational dialog cho this step
            showPermissionStepDialog(nextStep, stepPermissions)
        } else {
            // Permissions likely blocked - direct to settings
            Timber.w("⚠️ Permission dialogs blocked - directing to Settings")
            showPermissionSettingsDialog()
        }
    }
    */

    /*
     * 📚 Show educational dialog cho specific permission step - OLD SYSTEM
     */
    /*
    private fun showPermissionStepDialog(step: PermissionStep, permissions: Array<String>) {
        try {
            val builder = androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("🔐 ${step.description}")
                .setMessage("""
                    ${step.userExplanation}
                    
                    📋 Permissions needed:
                    ${permissions.joinToString("\n") { "• ${permissionManager.getPermissionDisplayName(it)}" }}
                    
                    💡 This step enables Nordic beacon detection functionality.
                """.trimIndent())
                .setPositiveButton("✅ Grant") { _, _ ->
                    Timber.i("🔄 Requesting step permissions: ${permissions.joinToString()}")
                    permissionLauncher.launch(permissions)
                }
                .setNegativeButton("⚙️ Settings") { _, _ ->
                    showPermissionSettingsDialog()
                }
                .setCancelable(true)
            
            builder.create().show()
            
        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to show step dialog")
            permissionLauncher.launch(permissions)
        }
    }
    */

    /*
     * 📝 Handle permission request results - OLD SYSTEM
     */
    /*
    private fun handlePermissionResults(permissions: Map<String, Boolean>) {
        val granted = permissions.filterValues { it }.keys
        val denied = permissions.filterValues { !it }.keys
        
        Timber.i("✅ Permissions granted: $granted")
        Timber.w("❌ Permissions denied: $denied")
        
        handleGrantedPermissions(granted.toList())
        if (denied.isNotEmpty()) {
            handleDeniedPermissions(denied.toList())
        }
        
        // Continue sequential flow - check if more permissions needed
        val permissionStatus = permissionManager.getSequentialPermissionStatus()
        if (permissionStatus.currentStep != null && granted.isNotEmpty()) {
            // Some granted, continue với next step
            Timber.i("🔄 Continuing sequential permission flow...")
            requestMissingPermissions()
        } else if (permissionStatus.hasOptimalPermissions) {
            // All permissions granted - start scanning!
            Timber.i("🎉 All permissions granted - starting Nordic beacon scanning!")
            startBeaconScanningService()
        } else if (permissionStatus.canStartScanning) {
            // Can start với basic permissions
            Timber.i("🚀 Basic permissions sufficient - can start Nordic scanning")
            showBasicScanningDialog()
        }
    }
    */

    /**
     * 🚫 Handle denied permissions scenario
     */
    private fun handlePermissionsDenied(deniedPermissions: List<String>) {
        Timber.w("🚫 Handling denied permissions: $deniedPermissions")
        
        // TODO: Show appropriate user education dialogs
        // TODO: Implement graceful degradation strategies
        
        // For critical permissions, app cannot function
        val criticalPermissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH
        )
        
        val hasCriticalDenials = deniedPermissions.any { it in criticalPermissions }
        
        if (hasCriticalDenials) {
            Timber.e("❌ Critical permissions denied - app cannot function properly")
            showPermissionSettingsDialog()
        }
    }

    /**
     * ⚙️ Show dialog to direct user to Settings cho manual permission grant
     */
    private fun showPermissionSettingsDialog() {
        Timber.i("⚙️ Showing permission settings guidance dialog...")
        
        try {
            val builder = androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("🔐 Nordic Beacon Scanner - Permissions Required")
                .setMessage("""
                    🎯 Nordic beacon detection requires these permissions:
                    
                    📍 Location: "Allow all the time"
                    📡 Bluetooth: "Allow nearby devices"  
                    🔋 Background: "Allow background activity"
                    
                    ⚙️ Please grant manually:
                    Settings → Apps → Nordic Beacon Scanner → Permissions
                    
                    💡 These permissions enable continuous Nordic beacon scanning even when your screen is off.
                """.trimIndent())
                .setPositiveButton("🔧 Open Settings") { _, _ ->
                    openAppSettings()
                }
                .setNegativeButton("📱 Later") { dialog, _ ->
                    dialog.dismiss()
                    Timber.i("⏸️ User chose to configure permissions later")
                }
                .setCancelable(true)
            
            builder.create().show()
            
            Timber.i("✅ Permission guidance dialog displayed")
            
        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to show permission dialog")
            // Fallback: Direct to settings
            openAppSettings()
        }
    }

    /**
     * ⚙️ Open app settings cho manual permission configuration
     */
    private fun openAppSettings() {
        try {
            val intent = android.content.Intent().apply {
                action = android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                data = android.net.Uri.fromParts("package", packageName, null)
                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
            Timber.i("⚙️ Opened app settings cho manual permission configuration")
        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to open app settings")
        }
    }

    /**
     * ✅ Handle granted permissions
     */
    private fun handleGrantedPermissions(grantedPermissions: List<String>) {
        if (grantedPermissions.isNotEmpty()) {
            Timber.i("✅ Successfully granted: ${grantedPermissions.joinToString(", ")}")
            
            // Log permission step completion
            grantedPermissions.forEach { permission ->
                Timber.d("✅ Permission granted: ${permissionManager.getPermissionDisplayName(permission)}")
            }
        }
    }

    /**
     * ❌ Handle denied permissions
     */
    private fun handleDeniedPermissions(deniedPermissions: List<String>) {
        Timber.w("🚫 Handling denied permissions: $deniedPermissions")
        
        // Check if critical permissions are denied
        val criticalPermissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN
        )
        
        val hasCriticalDenials = deniedPermissions.any { it in criticalPermissions }
        
        if (hasCriticalDenials) {
            Timber.e("❌ Critical permissions denied - app cannot function properly")
            // Continue sequential flow will handle this với Settings dialog
        }
    }

    /**
     * 📱 Show dialog when basic scanning permissions available
     */
    private fun showBasicScanningDialog() {
        try {
            val builder = androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("🚀 Nordic Scanning Ready")
                .setMessage("""
                    ✅ Basic Nordic beacon scanning is now available!
                    
                    📍 Current permissions allow foreground detection
                    🔋 For background scanning (screen off), grant background location permission
                    
                    🎯 Start Nordic beacon detection now?
                """.trimIndent())
                .setPositiveButton("🚀 Start Scanning") { _, _ ->
                    startBeaconScanningService()
                }
                .setNegativeButton("⚙️ Get Full Permissions") { _, _ ->
                    requestMissingPermissions() // Continue sequential flow
                }
                .setCancelable(true)
            
            builder.create().show()
            
        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to show basic scanning dialog")
            startBeaconScanningService() // Fallback
        }
    }

    // ========== SERVICE MANAGEMENT ==========

    /**
     * 🚀 Start beacon scanning service
     */
    private fun startBeaconScanningService() {
        try {
            val serviceIntent = BeaconScanningService.createStartIntent(this)
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
            
            Timber.i("🚀 Nordic beacon scanning service started")
            
        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to start beacon scanning service")
            handleServiceStartError(e)
        }
    }

    /**
     * ⏹️ Stop beacon scanning service
     */
    private fun stopBeaconScanningService() {
        try {
            val serviceIntent = BeaconScanningService.createStopIntent(this)
            startService(serviceIntent)
            
            Timber.i("⏹️ Nordic beacon scanning service stop requested")
            
        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to stop beacon scanning service")
        }
    }

    // ========== EVENT HANDLERS ==========

    /**
     * 🔄 Handle scanning state changes
     */
    private fun handleScanningStateChange(state: com.nordicbeacon.scanner.domain.models.ScanningState) {
        when (state) {
            com.nordicbeacon.scanner.domain.models.ScanningState.SCANNING -> {
                Timber.i("✅ Scanning state: ACTIVE")
                // TODO: Update UI to show active scanning state
            }
            com.nordicbeacon.scanner.domain.models.ScanningState.STOPPED -> {
                Timber.i("⏹️ Scanning state: STOPPED")  
                // TODO: Update UI to show stopped state
            }
            com.nordicbeacon.scanner.domain.models.ScanningState.ERROR -> {
                Timber.w("❌ Scanning state: ERROR")
                // TODO: Show error state in UI
            }
            else -> {
                Timber.d("🔄 Scanning state: $state")
            }
        }
    }

    /**
     * 🎯 Handle beacon detections
     */
    private fun handleBeaconDetections(beacons: List<com.nordicbeacon.scanner.domain.entities.NordicBeacon>) {
        Timber.i("🎯 Processing ${beacons.size} Nordic beacon detection(s)")
        
        beacons.forEach { beacon ->
            Timber.i("   📡 ${beacon.uuid.value} | ${beacon.signalStrength.rssi}dBm | ${"%.2f".format(beacon.proximity.meters)}m")
        }
        
        // TODO: Update UI với beacon detection list
    }

    /**
     * 🔐 Handle permission status changes
     */
    private fun handlePermissionStatusChange(status: Any) {
        // TODO: Implement when PermissionStatus model is defined
        Timber.d("🔐 Permission status changed")
    }

    // ========== UI ACTION HANDLERS ==========

    /**
     * 🚀 Start beacon scanning (New System)
     */
    private fun startBeaconScanning() {
        Timber.i("🚀 Starting Nordic beacon scanning...")
        
        // Check permissions using new system
        lifecycleScope.launch {
            val hasPermissions = permissionManager.arePermissionsGranted(
                *PermissionGroups.BLE_SCANNING_ESSENTIAL.toTypedArray()
            )
            
            if (!hasPermissions) {
                Timber.w("⚠️ Missing required permissions - requesting...")
                requestBlePermissions()
                return@launch
            }
            
            // Start the beacon scanning service
            try {
                val intent = BeaconScanningService.createStartIntent(this@MainActivity)
                startService(intent)
                
                // Update UI state
                findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_start_scanning)?.apply {
                    isEnabled = false
                }
                findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_stop_scanning)?.apply {
                    isEnabled = true
                }
                
                Timber.i("✅ Nordic beacon scanning started successfully")
                
            } catch (e: Exception) {
                Timber.e(e, "❌ Failed to start beacon scanning")
            }
        }
    }

    /**
     * ⏹️ Stop beacon scanning  
     */
    private fun stopBeaconScanning() {
        Timber.i("⏹️ Stopping Nordic beacon scanning...")
        
        lifecycleScope.launch {
            try {
                val intent = BeaconScanningService.createStopIntent(this@MainActivity)
                startService(intent)
                
                // Update UI state
                findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_start_scanning)?.apply {
                    isEnabled = true
                }
                findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_stop_scanning)?.apply {
                    isEnabled = false
                }
                
                Timber.i("✅ Nordic beacon scanning stopped")
                
            } catch (e: Exception) {
                Timber.e(e, "❌ Failed to stop beacon scanning")
            }
        }
    }

    /**
     * 🧹 Clear detection history
     */
    private fun clearDetectionHistory() {
        Timber.i("🧹 Clearing detection history...")
        
        lifecycleScope.launch {
            try {
                // Clear via ViewModel
                viewModel.clearDetectionHistory()
                
                // Update UI
                findViewById<android.widget.TextView>(R.id.txt_detection_count)?.text = "0"
                
                Timber.i("✅ Detection history cleared")
                
            } catch (e: Exception) {
                Timber.e(e, "❌ Failed to clear detection history")
            }
        }
    }

    /**
     * 🔄 Toggle scanning state
     */
    private fun toggleScanning() {
        Timber.i("🔄 Toggling scanning state...")
        
        val startButton = findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_start_scanning)
        val isCurrentlyScanning = startButton?.isEnabled == false
        
        if (isCurrentlyScanning) {
            stopBeaconScanning()
        } else {
            startBeaconScanning()
        }
    }

    /**
     * ❌ Handle ViewModel errors
     */
    private fun handleViewModelError(error: Throwable) {
        Timber.e(error, "❌ ViewModel error occurred")
        
        // TODO: Show appropriate error dialog to user
        // TODO: Implement error recovery actions
    }

    /**
     * ❌ Handle service start errors
     */
    private fun handleServiceStartError(error: Exception) {
        Timber.e(error, "❌ Service start error")
        
        // TODO: Show error dialog với retry option
        // TODO: Implement fallback strategies
    }

    /**
     * 🔋 Handle battery optimization intent action
     */
    private fun handleBatteryOptimizationIntent(intent: Intent?) {
        
        if (intent?.action == "ACTION_BATTERY_OPTIMIZATION") {
            Timber.i("🔋 Battery optimization action requested từ notification")
            
            lifecycleScope.launch {
                try {
                    // Execute battery optimization flow
                    val result = batteryOptimizationCoordinator.executeOptimization()
                    
                    when (result) {
                        is com.nordicbeacon.scanner.infrastructure.oem.models.BatteryOptimizationResult.AlreadyOptimized -> {
                            Timber.i("✅ Device already optimized")
                            // TODO: Show success message
                        }
                        
                        is com.nordicbeacon.scanner.infrastructure.oem.models.BatteryOptimizationResult.SettingsOpened -> {
                            Timber.i("⚙️ Settings opened: ${result.settingsType}")
                            // TODO: Show user guidance dialog
                        }
                        
                        is com.nordicbeacon.scanner.infrastructure.oem.models.BatteryOptimizationResult.Failed -> {
                            Timber.w("❌ Optimization failed: ${result.reason}")
                            // TODO: Show fallback instructions
                        }
                        
                        else -> {
                            Timber.d("ℹ️ Optimization result: ${result::class.simpleName}")
                        }
                    }
                    
                } catch (e: Exception) {
                    Timber.e(e, "❌ Battery optimization flow failed")
                    // TODO: Show error dialog
                }
            }
        }
    }

    /**
     * 🔄 Setup WorkManager backup scanning
     */
    private fun setupWorkManagerBackup() {
        try {
            val workRequest = BeaconScanWorker.createPeriodicWorkRequest()
            
            WorkManager.getInstance(this)
                .enqueueUniquePeriodicWork(
                    BeaconScanWorker.WORK_NAME,
                    androidx.work.ExistingPeriodicWorkPolicy.KEEP, // Don't restart if already running
                    workRequest
                )
            
            Timber.i("🔄 WorkManager backup scanning scheduled")
            
        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to setup WorkManager backup")
        }
    }

    /**
     * 🔬 Initialize analytics system
     */
    private fun initializeAnalytics() {
        try {
            analyticsIntegrationManager.initializeAnalytics()
            
            // Setup analytics observers
            setupAnalyticsObservers()
            
            Timber.i("🔬 Analytics system initialized")
            
        } catch (e: Exception) {
            Timber.e(e, "❌ Analytics initialization failed")
        }
    }

    /**
     * 👁️ Setup analytics data observers
     */
    private fun setupAnalyticsObservers() {
        lifecycleScope.launch {
            analyticsIntegrationManager.enhancedBeacons
                .collect { enhancedBeacon ->
                    handleEnhancedBeaconDetection(enhancedBeacon)
                }
        }
        
        lifecycleScope.launch {
            analyticsIntegrationManager.analyticsInsights
                .collect { insights ->
                    insights?.let { handleAnalyticsInsights(it) }
                }
        }
    }

    /**
     * 🎯 Handle enhanced beacon detection với analytics
     */
    private fun handleEnhancedBeaconDetection(enhancedBeacon: com.nordicbeacon.scanner.analytics.EnhancedNordicBeacon) {
        val beacon = enhancedBeacon.originalBeacon
        val analytics = enhancedBeacon.analyticsData
        
        Timber.i("🎯 Enhanced beacon detected với analytics:")
        Timber.i("   📡 Filtered RSSI: ${"%.1f".format(analytics.filteredRssi.filteredValue)}dBm (${(analytics.filteredRssi.improvementFactor * 100).toInt()}% improvement)")
        Timber.i("   📏 Enhanced Distance: ${"%.2f".format(analytics.enhancedDistance.distance)}m (${(analytics.enhancedDistance.confidence * 100).toInt()}% confidence)")
        Timber.i("   🏆 Reliability Score: ${analytics.reliabilityScore.overallScore}%")
        
        // TODO: Update UI với enhanced data
    }

    /**
     * 📊 Handle analytics insights updates
     */
    private fun handleAnalyticsInsights(insights: com.nordicbeacon.scanner.analytics.insights.BeaconInsights) {
        Timber.i("📊 Analytics insights updated:")
        Timber.i("   📈 Total detections: ${insights.totalDetections}")
        Timber.i("   🎯 Unique beacons: ${insights.uniqueBeacons}")
        Timber.i("   📋 Recommendations: ${insights.recommendations.size}")
        
        // TODO: Update UI với insights data
    }

    /**
     * 📊 Open analytics dashboard
     */
    private fun openAnalyticsDashboard() {
        try {
            val intent = Intent(this, AnalyticsDashboardActivity::class.java)
            startActivity(intent)
            
        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to open analytics dashboard")
        }
    }
}
