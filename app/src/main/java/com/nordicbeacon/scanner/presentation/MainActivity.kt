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
import com.nordicbeacon.scanner.infrastructure.permissions.PermissionManager
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
    
    // ========== PERMISSION HANDLING ==========
    
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        handlePermissionResults(permissions)
    }

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
     * 🔐 Check permissions và start scanning process
     */
    private fun checkPermissionsAndStartScanning() {
        Timber.i("🔐 Checking required permissions...")
        
        val requiredPermissions = getRequiredPermissions()
        val missingPermissions = requiredPermissions.filter { permission ->
            ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED
        }
        
        if (missingPermissions.isEmpty()) {
            Timber.i("✅ All permissions granted - starting beacon scanning")
            startBeaconScanningService()
        } else {
            Timber.w("⚠️ Missing permissions: $missingPermissions")
            requestMissingPermissions(missingPermissions)
        }
    }

    /**
     * 📋 Get required permissions based on Android version
     */
    private fun getRequiredPermissions(): List<String> {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                // Android 12+ permissions
                listOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                )
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                // Android 10-11 permissions
                listOf(
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                )
            }
            else -> {
                // Legacy Android permissions
                listOf(
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            }
        }
    }

    /**
     * 🙏 Request missing permissions từ user
     */
    private fun requestMissingPermissions() {
        val missingPermissions = permissionManager.getMissingPermissions()
        Timber.i("🙏 Requesting ${missingPermissions.size} missing permission(s)")
        
        // TODO: Show educational dialog trước khi request permissions
        // For now, directly request permissions
        
        permissionLauncher.launch(missingPermissions.toTypedArray())
    }

    /**
     * 📝 Handle permission request results
     */
    private fun handlePermissionResults(permissions: Map<String, Boolean>) {
        val granted = permissions.filterValues { it }.keys
        val denied = permissions.filterValues { !it }.keys
        
        Timber.i("✅ Permissions granted: $granted")
        if (denied.isNotEmpty()) {
            Timber.w("❌ Permissions denied: $denied")
        }
        
        if (denied.isEmpty()) {
            // All permissions granted - start scanning
            Timber.i("🎯 All permissions granted - starting beacon scanning")
            startBeaconScanningService()
            
        } else {
            // Some permissions denied - handle gracefully
            handlePermissionsDenied(denied.toList())
        }
    }

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
            // TODO: Show education dialog với retry option
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
     * 🚀 Start beacon scanning
     */
    private fun startBeaconScanning() {
        Timber.i("🚀 Starting Nordic beacon scanning...")
        
        // Check permissions first
        if (!permissionManager.hasRequiredPermissions()) {
            requestMissingPermissions()
            return
        }
        
        // Start the beacon scanning service
        lifecycleScope.launch {
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
