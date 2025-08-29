package com.nordicbeacon.scanner.analytics.dashboard.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nordicbeacon.scanner.analytics.BeaconAnalyticsEngine
import com.nordicbeacon.scanner.analytics.monitoring.service.ServicePerformanceMonitor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * 📊 Analytics Dashboard ViewModel
 * 
 * Manages analytics dashboard state và coordinates real-time data updates
 * Provides clean separation giữa UI và analytics business logic
 * 
 * @author Senior Android Developer
 */
@HiltViewModel
class AnalyticsDashboardViewModel @Inject constructor(
    private val analyticsEngine: BeaconAnalyticsEngine,
    private val performanceMonitor: ServicePerformanceMonitor
) : ViewModel() {

    private val _analyticsState = MutableStateFlow<AnalyticsDashboardState>(AnalyticsDashboardState.Loading)
    val analyticsState: StateFlow<AnalyticsDashboardState> = _analyticsState.asStateFlow()

    private val _performanceMetrics = MutableStateFlow<com.nordicbeacon.scanner.analytics.monitoring.service.PerformanceMetrics?>(null)
    val performanceMetrics: StateFlow<com.nordicbeacon.scanner.analytics.monitoring.service.PerformanceMetrics?> = _performanceMetrics.asStateFlow()

    fun startAnalyticsCollection() {
        viewModelScope.launch {
            try {
                // Start performance monitoring
                performanceMonitor.startMonitoring()
                    .catch { e -> Timber.e(e, "Performance monitoring error") }
                    .collect { metrics ->
                        _performanceMetrics.value = metrics
                    }
                    
            } catch (e: Exception) {
                Timber.e(e, "Analytics collection failed")
                _analyticsState.value = AnalyticsDashboardState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun pauseRealTimeUpdates() {
        // Implementation cho pausing updates
    }

    fun resumeRealTimeUpdates() {
        // Implementation cho resuming updates
    }

    override fun onCleared() {
        super.onCleared()
        performanceMonitor.stopMonitoring()
    }
}

sealed class AnalyticsDashboardState {
    object Loading : AnalyticsDashboardState()
    data class Success(val data: Any) : AnalyticsDashboardState()
    data class Error(val message: String) : AnalyticsDashboardState()
}
