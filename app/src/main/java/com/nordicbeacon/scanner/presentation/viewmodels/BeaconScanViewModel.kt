package com.nordicbeacon.scanner.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nordicbeacon.scanner.domain.entities.NordicBeacon
import com.nordicbeacon.scanner.domain.models.BeaconScanResult
import com.nordicbeacon.scanner.domain.models.ScanningState
import com.nordicbeacon.scanner.domain.usecases.ScanBeaconUseCase
import com.nordicbeacon.scanner.domain.usecases.SystemCompatibilityUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * 🎯 Beacon Scan ViewModel - Presentation Logic
 * 
 * Manages UI state và coordinates beacon scanning operations
 * Following MVVM architecture với reactive state management
 * 
 * Key Responsibilities:
 * - UI state management với StateFlow/LiveData
 * - Coordinates domain use cases
 * - Error handling với user-friendly messages  
 * - Permission status tracking
 * - Real-time beacon detection updates
 * 
 * @author Senior Android Developer
 */
@HiltViewModel
class BeaconScanViewModel @Inject constructor(
    private val scanBeaconUseCase: ScanBeaconUseCase,
    private val systemCompatibilityUseCase: SystemCompatibilityUseCase
) : ViewModel() {

    // ========== UI STATE MANAGEMENT ==========
    
    private val _scanningState = MutableStateFlow(ScanningState.IDLE)
    val scanningState: StateFlow<ScanningState> = _scanningState.asStateFlow()
    
    private val _beaconDetections = MutableStateFlow<List<NordicBeacon>>(emptyList())
    val beaconDetections: StateFlow<List<NordicBeacon>> = _beaconDetections.asStateFlow()
    
    private val _permissionStatus = MutableStateFlow<PermissionStatus>(PermissionStatus.Unknown)
    val permissionStatus: StateFlow<PermissionStatus> = _permissionStatus.asStateFlow()
    
    private val _errors = MutableStateFlow<Throwable?>(null)
    val errors: StateFlow<Throwable?> = _errors.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // ========== DETECTION STATISTICS ==========
    
    private val _detectionCount = MutableStateFlow(0)
    val detectionCount: StateFlow<Int> = _detectionCount.asStateFlow()
    
    private val _lastDetectionTime = MutableStateFlow(0L)
    val lastDetectionTime: StateFlow<Long> = _lastDetectionTime.asStateFlow()
    
    private val _scanDuration = MutableStateFlow(0L)
    val scanDuration: StateFlow<Long> = _scanDuration.asStateFlow()

    // ========== INITIALIZATION ==========

    init {
        Timber.i("🎯 BeaconScanViewModel initialized")
        
        // Initialize với system compatibility check
        checkSystemCompatibility()
        
        // Start observing use case state changes
        observeUseCaseState()
    }

    // ========== PUBLIC API ==========

    /**
     * 🚀 Start Nordic beacon scanning
     */
    fun startScanning() {
        Timber.i("🚀 ViewModel: Starting Nordic beacon scanning...")
        
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errors.value = null
                
                // Validate system requirements first
                val systemValidation = systemCompatibilityUseCase.validateSystemRequirements()
                
                if (!systemValidation.isFullyCompatible()) {
                    val missingReqs = systemValidation.getMissingRequirements()
                    Timber.w("⚠️ System validation failed: $missingReqs")
                    _errors.value = Exception("System requirements not met: ${missingReqs.joinToString()}")
                    return@launch
                }
                
                // Start scanning use case
                scanBeaconUseCase.startScanning()
                    .catch { exception ->
                        Timber.e(exception, "❌ Scanning use case error")
                        _errors.value = exception
                        _scanningState.value = ScanningState.ERROR
                    }
                    .collect { result ->
                        handleScanResult(result)
                    }
                    
            } catch (exception: Exception) {
                Timber.e(exception, "❌ Failed to start scanning")
                _errors.value = exception
                _scanningState.value = ScanningState.ERROR
                
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * ⏹️ Stop Nordic beacon scanning
     */  
    fun stopScanning() {
        Timber.i("⏹️ ViewModel: Stopping Nordic beacon scanning...")
        
        viewModelScope.launch {
            try {
                val result = scanBeaconUseCase.stopScanning()
                
                if (result.isSuccess) {
                    _scanningState.value = ScanningState.STOPPED
                    Timber.i("✅ Scanning stopped successfully")
                } else {
                    val error = result.exceptionOrNull()
                    Timber.e(error, "❌ Failed to stop scanning")
                    _errors.value = error
                }
                
            } catch (exception: Exception) {
                Timber.e(exception, "❌ Error stopping scanning")
                _errors.value = exception
            }
        }
    }

    /**
     * 🔄 Refresh beacon detection data
     */
    fun refreshDetections() {
        Timber.i("🔄 Refreshing beacon detections...")
        
        viewModelScope.launch {
            try {
                // Get recent beacon history từ use case
                val historyResult = scanBeaconUseCase.getNordicBeaconHistory(limit = 20, sinceHours = 1)
                
                if (historyResult.isSuccess) {
                    val recentBeacons = historyResult.getOrNull() ?: emptyList()
                    _beaconDetections.value = recentBeacons
                    Timber.i("🔄 Refreshed với ${recentBeacons.size} recent beacon(s)")
                } else {
                    val error = historyResult.exceptionOrNull()
                    Timber.w(error, "⚠️ Failed to refresh beacon history")
                }
                
            } catch (exception: Exception) {
                Timber.e(exception, "❌ Error refreshing detections") 
                _errors.value = exception
            }
        }
    }

    /**
     * 🧹 Clear beacon detection history
     */
    fun clearDetectionHistory() {
        Timber.i("🧹 Clearing beacon detection history...")
        
        viewModelScope.launch {
            try {
                val result = scanBeaconUseCase.cleanupOldData(retentionDays = 0) // Clear all
                
                if (result.isSuccess) {
                    _beaconDetections.value = emptyList()
                    _detectionCount.value = 0
                    Timber.i("✅ Detection history cleared")
                } else {
                    val error = result.exceptionOrNull()
                    Timber.e(error, "❌ Failed to clear detection history")
                    _errors.value = error
                }
                
            } catch (exception: Exception) {
                Timber.e(exception, "❌ Error clearing detection history")
                _errors.value = exception
            }
        }
    }

    /**
     * 🔍 Check service status
     */
    fun checkServiceStatus() {
        Timber.d("🔍 Checking beacon service status...")
        
        // TODO: Implement service status check
        // For now, just log the check
    }

    // ========== PRIVATE IMPLEMENTATION ==========

    /**
     * 🛠️ Check system compatibility
     */
    private fun checkSystemCompatibility() {
        viewModelScope.launch {
            try {
                val validation = systemCompatibilityUseCase.validateSystemRequirements()
                
                if (validation.isFullyCompatible()) {
                    Timber.i("✅ System fully compatible với Nordic beacon scanning")
                } else {
                    Timber.w("⚠️ System compatibility issues: ${validation.getMissingRequirements()}")
                }
                
            } catch (exception: Exception) {
                Timber.e(exception, "❌ System compatibility check failed")
                _errors.value = exception
            }
        }
    }

    /**
     * 👁️ Observe use case state changes
     */
    private fun observeUseCaseState() {
        viewModelScope.launch {
            scanBeaconUseCase.getScanningState()
                .catch { exception ->
                    Timber.e(exception, "❌ Error observing use case state")  
                    _errors.value = exception
                }
                .collect { state ->
                    _scanningState.value = state
                }
        }
    }

    /**
     * 📡 Handle individual scan results
     */
    private fun handleScanResult(result: BeaconScanResult) {
        when (result) {
            is BeaconScanResult.Success -> {
                val beacon = result.beacon
                
                // Add to detections list
                val currentDetections = _beaconDetections.value.toMutableList()
                currentDetections.add(0, beacon) // Add to top
                
                // Keep only recent detections (max 50)
                val trimmedDetections = currentDetections.take(50)
                _beaconDetections.value = trimmedDetections
                
                // Update statistics
                _detectionCount.value = _detectionCount.value + 1
                _lastDetectionTime.value = beacon.detectionTime.millis
                
                Timber.i("🎯 ViewModel: Nordic beacon detection #${_detectionCount.value}")
            }
            
            is BeaconScanResult.NoBeaconsFound -> {
                Timber.d("🔍 ViewModel: No beacons found in scan cycle")
            }
            
            is BeaconScanResult.Error -> {
                Timber.e("❌ ViewModel: Scan error - ${result.message}")
                _errors.value = Exception(result.message, result.cause)
                _scanningState.value = ScanningState.ERROR
            }
        }
    }

    // ========== CLEANUP ==========

    override fun onCleared() {
        super.onCleared()
        Timber.i("🔄 BeaconScanViewModel cleared")
        
        // ViewModelScope automatically cancels all coroutines
        // No manual cleanup needed với proper coroutine usage
    }
}

/**
 * 🔐 Permission Status Model
 */
sealed class PermissionStatus {
    object Unknown : PermissionStatus()
    object AllGranted : PermissionStatus()
    data class MissingPermissions(val missing: List<String>) : PermissionStatus()
    data class PermanentlyDenied(val denied: List<String>) : PermissionStatus()
}
