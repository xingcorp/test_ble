package com.nordicbeacon.scanner.infrastructure.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.nordicbeacon.scanner.infrastructure.services.BeaconScanningService
import com.nordicbeacon.scanner.infrastructure.oem.coordination.BatteryOptimizationCoordinator
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

/**
 * 📡 System Event Receiver - Auto-restart Capabilities
 * 
 * Handles system events để ensure Nordic beacon scanning persistence
 * Provides auto-restart functionality cho critical scenarios
 * 
 * Key Events Handled:
 * - Device boot completed
 * - Package replaced (app updates)  
 * - Power save mode changes
 * - Doze mode changes
 * - Battery optimization changes
 * 
 * @author Senior Android Developer
 */
@AndroidEntryPoint
class SystemEventReceiver : BroadcastReceiver() {

    companion object {
        private const val ACTION_QUICKBOOT_POWERON = "android.intent.action.QUICKBOOT_POWERON"
    }

    @Inject lateinit var batteryOptimizationCoordinator: BatteryOptimizationCoordinator

    override fun onReceive(context: Context, intent: Intent) {
        
        val action = intent.action
        Timber.i("📡 System event received: $action")
        
        when (action) {
            Intent.ACTION_BOOT_COMPLETED,
            ACTION_QUICKBOOT_POWERON -> {
                handleBootCompleted(context)
            }
            
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_PACKAGE_REPLACED -> {
                if (isOurPackage(intent)) {
                    handlePackageReplaced(context)
                }
            }
            
            android.os.PowerManager.ACTION_POWER_SAVE_MODE_CHANGED -> {
                handlePowerSaveModeChanged(context)
            }
            
            android.os.PowerManager.ACTION_DEVICE_IDLE_MODE_CHANGED -> {
                handleDozeModeChanged(context)
            }
            
            else -> {
                Timber.d("📡 Unhandled system event: $action")
            }
        }
    }

    // ========== EVENT HANDLERS ==========

    /**
     * 🚀 Handle device boot completed
     */
    private fun handleBootCompleted(context: Context) {
        Timber.i("🚀 Device boot completed - checking auto-restart requirements")
        
        try {
            // Check if we should auto-start scanning after boot
            if (shouldAutoStartAfterBoot()) {
                
                // Start beacon scanning service
                val serviceIntent = BeaconScanningService.createStartIntent(context)
                
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
                
                Timber.i("✅ Nordic beacon scanning auto-started after boot")
                
            } else {
                Timber.i("ℹ️ Auto-start disabled hoặc not configured")
            }
            
        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to auto-start after boot")
        }
    }

    /**
     * 🔄 Handle app package replaced (updates)
     */
    private fun handlePackageReplaced(context: Context) {
        Timber.i("🔄 Package replaced - restarting Nordic beacon scanning")
        
        try {
            // Restart scanning service after app update
            val serviceIntent = BeaconScanningService.createStartIntent(context)
            
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
            
            Timber.i("✅ Service restarted after package update")
            
        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to restart service after update")
        }
    }

    /**
     * 🔋 Handle power save mode changes
     */
    private fun handlePowerSaveModeChanged(context: Context) {
        
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
        val isPowerSaveMode = powerManager.isPowerSaveMode
        
        Timber.i("🔋 Power save mode changed: $isPowerSaveMode")
        
        if (isPowerSaveMode) {
            // Device entered power save mode - might need to adapt scanning
            Timber.i("⚡ Device entered power save mode - adapting scan strategy")
            // TODO: Signal service to use conservative scanning periods
            
        } else {
            // Device exited power save mode - can resume normal scanning
            Timber.i("🔋 Device exited power save mode - resuming normal scanning")
            // TODO: Signal service to resume normal scanning periods
        }
    }

    /**
     * 😴 Handle doze mode changes
     */
    private fun handleDozeModeChanged(context: Context) {
        
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
        val isDeviceIdleMode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            powerManager.isDeviceIdleMode
        } else {
            false
        }
        
        Timber.i("😴 Doze mode changed: $isDeviceIdleMode")
        
        if (isDeviceIdleMode) {
            Timber.i("😴 Device entered doze mode - beacon scanning may be limited")
            // Service should continue via foreground service exemption
            
        } else {
            Timber.i("🌅 Device exited doze mode - resuming full beacon scanning")
            // Can resume normal scanning frequency
        }
    }

    // ========== HELPER METHODS ==========

    /**
     * 🔍 Check if intent is cho our package
     */
    private fun isOurPackage(intent: Intent): Boolean {
        val data = intent.data
        return data?.schemeSpecificPart == "com.nordicbeacon.scanner"
    }

    /**
     * ⚙️ Check if should auto-start after boot
     */
    private fun shouldAutoStartAfterBoot(): Boolean {
        // TODO: Read từ SharedPreferences hoặc DataStore
        // For now, default to true cho Nordic beacon scanning
        return true
    }
}
