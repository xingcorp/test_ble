package com.nordicbeacon.scanner.infrastructure.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.nordicbeacon.scanner.R
import com.nordicbeacon.scanner.domain.entities.NordicBeacon
import com.nordicbeacon.scanner.presentation.MainActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 🔔 Notification Helper - Professional Notification Management
 * 
 * Handles all notification operations cho Nordic beacon scanning service
 * Provides rich, informative notifications với appropriate priorities
 * 
 * Key Features:
 * - Dynamic notification updates based on scan results
 * - Android version compatibility (API 21+)
 * - Professional notification design với actions
 * - Performance-optimized notification updates
 * 
 * @author Senior Android Developer
 */
@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val notificationManager by lazy { 
        NotificationManagerCompat.from(context)
    }

    init {
        // Create notification channels for different types
        createNotificationChannels()
    }

    // ========== NOTIFICATION CREATION ==========

    /**
     * 🚀 Create initial scanning notification
     */
    fun createInitialScanningNotification(): Notification {
        Timber.d("🔔 Creating initial scanning notification")
        
        return NotificationCompat.Builder(context, SCANNING_CHANNEL_ID)
            .setContentTitle("Nordic Beacon Scanner")
            .setContentText("🔍 Initializing beacon scanning...")
            .setSubText("Preparing to scan for Nordic beacons")
            .setSmallIcon(R.drawable.ic_bluetooth_searching)
            .setColor(getNotificationColor())
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setContentIntent(createMainActivityIntent())
            .addAction(createStopAction())
            .build()
    }

    /**
     * 🎯 Create beacon detection notification
     */
    fun createDetectionNotification(
        beacon: NordicBeacon,
        totalDetections: Int,
        scanDuration: Long
    ): Notification {
        
        val durationMinutes = scanDuration / 60000
        val proximityDesc = when {
            beacon.isImmediate() -> "📍 Immediate"
            beacon.isNear() -> "📍 Near" 
            beacon.isFar() -> "📍 Far"
            else -> "📍 Very Far"
        }
        
        return NotificationCompat.Builder(context, SCANNING_CHANNEL_ID)
            .setContentTitle("Nordic Beacon Detected! 🎯")
            .setContentText("$proximityDesc • ${beacon.signalStrength.rssi}dBm • ${"%.1f".format(beacon.proximity.meters)}m")
            .setSubText("Total: $totalDetections detection(s) | Scanning: ${durationMinutes}min")
            .setSmallIcon(R.drawable.ic_bluetooth_connected)
            .setLargeIcon(createBeaconIcon())
            .setColor(getSuccessColor())
            .setOngoing(true)
            .setShowWhen(true)
            .setWhen(beacon.detectionTime.millis)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(createMainActivityIntent())
            .addAction(createViewDetailsAction())
            .addAction(createStopAction())
            .setStyle(createExpandedStyle(beacon, totalDetections))
            .build()
    }

    /**
     * 🔍 Create searching notification (no detections yet)
     */
    fun createSearchingNotification(scanDuration: Long): Notification {
        val durationMinutes = scanDuration / 60000
        
        return NotificationCompat.Builder(context, SCANNING_CHANNEL_ID)
            .setContentTitle("Scanning for Nordic Beacons...")
            .setContentText("🔍 Looking for beacon: ${NordicBeacon.NORDIC_UUID}")
            .setSubText("Scanning for ${durationMinutes}min • No detections yet")
            .setSmallIcon(R.drawable.ic_bluetooth_searching)
            .setColor(getNotificationColor())
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)  
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(createMainActivityIntent())
            .addAction(createStopAction())
            .setProgress(100, 50, true) // Indeterminate progress
            .build()
    }

    // ========== ERROR NOTIFICATIONS ==========

    /**
     * ❌ Show error notification
     */
    fun showErrorNotification(errorMessage: String) {
        val notification = NotificationCompat.Builder(context, ERROR_CHANNEL_ID)
            .setContentTitle("Beacon Scanning Error")
            .setContentText(errorMessage)
            .setSmallIcon(R.drawable.ic_error)
            .setColor(getErrorColor())
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(createMainActivityIntent())
            .addAction(createRetryAction())
            .build()
            
        notificationManager.notify(ERROR_NOTIFICATION_ID, notification)
    }

    /**
     * 🔐 Show permission error notification
     */
    fun showPermissionErrorNotification() {
        val notification = NotificationCompat.Builder(context, ERROR_CHANNEL_ID)
            .setContentTitle("Permissions Required")
            .setContentText("Bluetooth and Location permissions needed for beacon scanning")
            .setSmallIcon(R.drawable.ic_permission_required)
            .setColor(getWarningColor())
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(createMainActivityIntent())
            .addAction(createOpenSettingsAction())
            .build()
            
        notificationManager.notify(PERMISSION_NOTIFICATION_ID, notification)
    }

    /**
     * ⚙️ Show system incompatibility notification
     */
    fun showSystemIncompatibilityNotification(recommendations: List<String>) {
        val recommendationText = recommendations.joinToString(", ")
        
        val notification = NotificationCompat.Builder(context, ERROR_CHANNEL_ID)
            .setContentTitle("System Compatibility Issue")
            .setContentText("Device configuration prevents beacon scanning")
            .setSubText(recommendationText)
            .setSmallIcon(R.drawable.ic_warning)
            .setColor(getWarningColor())
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(createMainActivityIntent())
            .build()
            
        notificationManager.notify(SYSTEM_NOTIFICATION_ID, notification)
    }

    /**
     * 🔋 Show battery optimization needed notification với OEM-specific guidance
     */
    fun showBatteryOptimizationNeeded(
        urgencyMessage: String,
        primaryAction: String,
        timeEstimate: String
    ) {
        val notification = NotificationCompat.Builder(context, ERROR_CHANNEL_ID)
            .setContentTitle("Battery Optimization Required")
            .setContentText("Configure ${android.os.Build.MANUFACTURER} settings cho continuous scanning")
            .setSubText("Estimated time: $timeEstimate")
            .setSmallIcon(R.drawable.ic_warning)
            .setColor(getWarningColor())
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(createMainActivityIntent())
            .addAction(createOptimizeAction())
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText(urgencyMessage)
                .setSummaryText(primaryAction))
            .build()
            
        notificationManager.notify(BATTERY_OPTIMIZATION_NOTIFICATION_ID, notification)
    }

    /**
     * 🔋 Show generic battery optimization notification (fallback)
     */
    fun showGenericBatteryOptimizationNotification() {
        val notification = NotificationCompat.Builder(context, ERROR_CHANNEL_ID)
            .setContentTitle("Battery Settings Required")
            .setContentText("Configure battery optimization cho continuous beacon scanning")
            .setSmallIcon(R.drawable.ic_warning)
            .setColor(getWarningColor())
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(createMainActivityIntent())
            .addAction(createOptimizeAction())
            .build()
            
        notificationManager.notify(BATTERY_OPTIMIZATION_NOTIFICATION_ID, notification)
    }

    // ========== NOTIFICATION UPDATES ==========

    /**
     * 🔄 Update existing notification
     */
    fun updateNotification(notificationId: Int, notification: Notification) {
        try {
            notificationManager.notify(notificationId, notification)
        } catch (e: Exception) {
            Timber.w(e, "⚠️ Failed to update notification $notificationId")
        }
    }

    // ========== NOTIFICATION CHANNELS SETUP ==========

    /**
     * 📺 Create notification channels cho Android 8.0+
     */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            
            // Main scanning channel
            val scanningChannel = NotificationChannel(
                SCANNING_CHANNEL_ID,
                "Nordic Beacon Scanning",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Continuous Nordic beacon scanning notifications"
                setShowBadge(false)
                enableLights(false)
                enableVibration(false)
                setSound(null, null)
            }
            
            // Error notifications channel
            val errorChannel = NotificationChannel(
                ERROR_CHANNEL_ID,
                "Scanning Errors & Issues", 
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Beacon scanning errors and system issues"
                setShowBadge(true)
                enableLights(true)
                lightColor = getErrorColor()
            }
            
            // Register channels
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(scanningChannel)
            manager.createNotificationChannel(errorChannel)
            
            Timber.i("📺 Notification channels created successfully")
        }
    }

    // ========== NOTIFICATION ACTIONS ==========

    /**
     * ⏹️ Create stop scanning action
     */
    private fun createStopAction(): NotificationCompat.Action {
        val stopIntent = Intent(context, com.nordicbeacon.scanner.infrastructure.services.BeaconScanningService::class.java).apply {
            action = com.nordicbeacon.scanner.infrastructure.services.BeaconScanningService.ACTION_STOP_SCANNING
        }
        
        val stopPendingIntent = PendingIntent.getService(
            context, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Action.Builder(
            R.drawable.ic_stop,
            "Stop",
            stopPendingIntent
        ).build()
    }

    /**
     * 👁️ Create view details action
     */
    private fun createViewDetailsAction(): NotificationCompat.Action {
        val viewIntent = createMainActivityIntent()
        
        return NotificationCompat.Action.Builder(
            R.drawable.ic_view_details,
            "View Details", 
            viewIntent
        ).build()
    }

    /**
     * 🔄 Create retry action cho errors
     */
    private fun createRetryAction(): NotificationCompat.Action {
        val retryIntent = Intent(context, com.nordicbeacon.scanner.infrastructure.services.BeaconScanningService::class.java).apply {
            action = com.nordicbeacon.scanner.infrastructure.services.BeaconScanningService.ACTION_START_SCANNING
        }
        
        val retryPendingIntent = PendingIntent.getService(
            context, 1, retryIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE  
        )
        
        return NotificationCompat.Action.Builder(
            R.drawable.ic_retry,
            "Retry",
            retryPendingIntent
        ).build()
    }

    /**
     * ⚙️ Create open settings action
     */
    private fun createOpenSettingsAction(): NotificationCompat.Action {
        val settingsIntent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = android.net.Uri.fromParts("package", context.packageName, null)
        }
        
        val settingsPendingIntent = PendingIntent.getActivity(
            context, 2, settingsIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Action.Builder(
            R.drawable.ic_settings,
            "Settings",
            settingsPendingIntent
        ).build()
    }

    /**
     * 🔋 Create battery optimization action
     */
    private fun createOptimizeAction(): NotificationCompat.Action {
        val optimizeIntent = Intent(context, MainActivity::class.java).apply {
            action = "ACTION_BATTERY_OPTIMIZATION"
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        
        val optimizePendingIntent = PendingIntent.getActivity(
            context, 3, optimizeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Action.Builder(
            R.drawable.ic_settings,
            "Optimize",
            optimizePendingIntent
        ).build()
    }

    // ========== HELPER METHODS ==========

    /**
     * 🎨 Create main activity pending intent
     */
    private fun createMainActivityIntent(): PendingIntent {
        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        
        return PendingIntent.getActivity(
            context, 0, mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * 📄 Create expanded notification style với beacon details
     */
    private fun createExpandedStyle(
        beacon: NordicBeacon, 
        totalDetections: Int
    ): NotificationCompat.Style {
        
        val expandedText = """
            🎯 Nordic Beacon Details:
            📍 UUID: ${beacon.uuid.value}
            🏷️ Major/Minor: ${beacon.major?.value ?: "N/A"}/${beacon.minor?.value ?: "N/A"}
            📶 Signal: ${beacon.signalStrength.rssi}dBm (${getSignalQualityDesc(beacon.signalStrength.rssi)})
            📏 Distance: ${"%.2f".format(beacon.proximity.meters)}m
            🎖️ Reliability: ${beacon.calculateReliabilityScore()}%
            📊 Total Detections: $totalDetections
        """.trimIndent()
        
        return NotificationCompat.BigTextStyle()
            .bigText(expandedText)
            .setSummaryText("Nordic Beacon Scanner")
    }

    /**
     * 🎨 Get notification colors based on state
     */
    private fun getNotificationColor(): Int {
        return ContextCompat.getColor(context, R.color.nordic_blue)
    }

    private fun getSuccessColor(): Int {
        return ContextCompat.getColor(context, R.color.success_green)
    }

    private fun getErrorColor(): Int {
        return ContextCompat.getColor(context, R.color.error_red)
    }

    private fun getWarningColor(): Int {
        return ContextCompat.getColor(context, R.color.warning_orange)
    }

    /**
     * 🎨 Create beacon icon cho large notification icon
     */
    private fun createBeaconIcon(): android.graphics.Bitmap? {
        // TODO: Create bitmap from vector drawable
        return null // For now, will use default
    }

    /**
     * 📶 Get signal quality description
     */
    private fun getSignalQualityDesc(rssi: Int): String {
        return when {
            rssi > -50 -> "Excellent"
            rssi > -70 -> "Good"
            rssi > -85 -> "Fair"  
            rssi > -95 -> "Poor"
            else -> "Very Poor"
        }
    }

    // ========== CONSTANTS ==========

    companion object {
        // Notification channel IDs
        const val SCANNING_CHANNEL_ID = "nordic_beacon_scanning"
        const val ERROR_CHANNEL_ID = "scanning_errors"
        
        // Notification IDs
        const val SCANNING_NOTIFICATION_ID = 1001
        const val ERROR_NOTIFICATION_ID = 1002  
        const val PERMISSION_NOTIFICATION_ID = 1003
        const val SYSTEM_NOTIFICATION_ID = 1004
        const val BATTERY_OPTIMIZATION_NOTIFICATION_ID = 1005
    }
}
