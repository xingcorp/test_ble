package com.nordicbeacon.scanner.core.permissions.impl

import android.content.Context
import android.content.SharedPreferences
import com.nordicbeacon.scanner.core.permissions.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 💾 Permission Repository Implementation
 * 
 * Handles persistence of permission-related data including history,
 * education status, and user preferences. Uses SharedPreferences for
 * lightweight, fast storage.
 * 
 * @author Senior Android Engineer
 */
@Singleton
class PermissionRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : PermissionRepository {
    
    companion object {
        private const val PREF_NAME = "permission_data"
        private const val KEY_EDUCATION_PREFIX = "education_shown_"
        private const val KEY_HISTORY_PREFIX = "history_"
        private const val KEY_STATS_PREFIX = "stats_"
        
        // Statistics keys
        private const val KEY_TOTAL_REQUESTS = "total_requests"
        private const val KEY_TOTAL_GRANTED = "total_granted"
        private const val KEY_TOTAL_DENIED = "total_denied"
        private const val KEY_TOTAL_PERMANENT_DENIED = "total_permanent_denied"
        private const val KEY_AVERAGE_REQUEST_TIME = "average_request_time"
    }
    
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }
    
    // ========== PERMISSION HISTORY ==========
    
    override suspend fun savePermissionHistory(permission: Permission, granted: Boolean) {
        withContext(Dispatchers.IO) {
            try {
                val timestamp = System.currentTimeMillis()
                val historyKey = "${KEY_HISTORY_PREFIX}${permission.manifestPermission}"
                
                // Get existing history
                val existingHistory = getPermissionHistoryInternal(permission)
                val newEntry = PermissionHistoryEntry(
                    permission = permission,
                    granted = granted,
                    timestamp = timestamp,
                    requestSource = "user_request" // Could be enhanced to track source
                )
                
                // Add new entry (keep last 10 entries)
                val updatedHistory = (existingHistory + newEntry).takeLast(10)
                
                // Save updated history
                val historyJson = serializeHistoryEntries(updatedHistory)
                prefs.edit().putString(historyKey, historyJson).apply()
                
                // Update statistics
                updateStatistics(granted)
                
                Timber.d("Saved permission history: ${permission.manifestPermission} = $granted")
                
            } catch (e: Exception) {
                Timber.e(e, "Failed to save permission history for: ${permission.manifestPermission}")
            }
        }
    }
    
    override suspend fun getPermissionHistory(permission: Permission): List<PermissionHistoryEntry> {
        return withContext(Dispatchers.IO) {
            getPermissionHistoryInternal(permission)
        }
    }
    
    private fun getPermissionHistoryInternal(permission: Permission): List<PermissionHistoryEntry> {
        return try {
            val historyKey = "${KEY_HISTORY_PREFIX}${permission.manifestPermission}"
            val historyJson = prefs.getString(historyKey, null)
            
            if (historyJson != null) {
                deserializeHistoryEntries(historyJson, permission)
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to get permission history for: ${permission.manifestPermission}")
            emptyList()
        }
    }
    
    // ========== EDUCATION STATUS ==========
    
    override suspend fun saveEducationShown(permission: Permission) {
        withContext(Dispatchers.IO) {
            try {
                val educationKey = "${KEY_EDUCATION_PREFIX}${permission.manifestPermission}"
                val timestamp = System.currentTimeMillis()
                
                prefs.edit()
                    .putLong(educationKey, timestamp)
                    .apply()
                
                Timber.d("Marked education as shown for: ${permission.manifestPermission}")
                
            } catch (e: Exception) {
                Timber.e(e, "Failed to save education status for: ${permission.manifestPermission}")
            }
        }
    }
    
    override suspend fun wasEducationShown(permission: Permission): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val educationKey = "${KEY_EDUCATION_PREFIX}${permission.manifestPermission}"
                val timestamp = prefs.getLong(educationKey, 0L)
                
                val wasShown = timestamp > 0L
                Timber.v("Education shown status for ${permission.manifestPermission}: $wasShown")
                
                wasShown
            } catch (e: Exception) {
                Timber.e(e, "Failed to check education status for: ${permission.manifestPermission}")
                false
            }
        }
    }
    
    // ========== STATISTICS ==========
    
    override suspend fun getPermissionStats(): PermissionStats {
        return withContext(Dispatchers.IO) {
            try {
                val totalRequests = prefs.getInt(KEY_TOTAL_REQUESTS, 0)
                val totalGranted = prefs.getInt(KEY_TOTAL_GRANTED, 0)
                val totalDenied = prefs.getInt(KEY_TOTAL_DENIED, 0)
                val totalPermanentDenied = prefs.getInt(KEY_TOTAL_PERMANENT_DENIED, 0)
                val averageRequestTime = prefs.getLong(KEY_AVERAGE_REQUEST_TIME, 0L)
                
                PermissionStats(
                    requestedCount = totalRequests,
                    grantedCount = totalGranted,
                    deniedCount = totalDenied,
                    permanentlyDeniedCount = totalPermanentDenied,
                    requestDurationMs = averageRequestTime
                )
            } catch (e: Exception) {
                Timber.e(e, "Failed to get permission statistics")
                PermissionStats.empty()
            }
        }
    }
    
    private fun updateStatistics(granted: Boolean) {
        try {
            val totalRequests = prefs.getInt(KEY_TOTAL_REQUESTS, 0) + 1
            val totalGranted = prefs.getInt(KEY_TOTAL_GRANTED, 0) + if (granted) 1 else 0
            val totalDenied = prefs.getInt(KEY_TOTAL_DENIED, 0) + if (!granted) 1 else 0
            
            prefs.edit()
                .putInt(KEY_TOTAL_REQUESTS, totalRequests)
                .putInt(KEY_TOTAL_GRANTED, totalGranted)
                .putInt(KEY_TOTAL_DENIED, totalDenied)
                .apply()
                
        } catch (e: Exception) {
            Timber.e(e, "Failed to update permission statistics")
        }
    }
    
    fun updateRequestDuration(durationMs: Long) {
        try {
            val currentAverage = prefs.getLong(KEY_AVERAGE_REQUEST_TIME, 0L)
            val totalRequests = prefs.getInt(KEY_TOTAL_REQUESTS, 0)
            
            val newAverage = if (totalRequests > 1) {
                ((currentAverage * (totalRequests - 1)) + durationMs) / totalRequests
            } else {
                durationMs
            }
            
            prefs.edit()
                .putLong(KEY_AVERAGE_REQUEST_TIME, newAverage)
                .apply()
                
        } catch (e: Exception) {
            Timber.e(e, "Failed to update request duration")
        }
    }
    
    // ========== CLEAR DATA ==========
    
    override suspend fun clearAll() {
        withContext(Dispatchers.IO) {
            try {
                prefs.edit().clear().apply()
                Timber.i("Cleared all permission data")
            } catch (e: Exception) {
                Timber.e(e, "Failed to clear permission data")
            }
        }
    }
    
    /**
     * 🧹 Clear education data only (for testing purposes)
     */
    suspend fun clearEducationData() {
        withContext(Dispatchers.IO) {
            try {
                val editor = prefs.edit()
                
                // Remove all education-related keys
                prefs.all.keys
                    .filter { it.startsWith(KEY_EDUCATION_PREFIX) }
                    .forEach { editor.remove(it) }
                
                editor.apply()
                Timber.i("Cleared education data")
            } catch (e: Exception) {
                Timber.e(e, "Failed to clear education data")
            }
        }
    }
    
    /**
     * 🧹 Clear history data only
     */
    suspend fun clearHistoryData() {
        withContext(Dispatchers.IO) {
            try {
                val editor = prefs.edit()
                
                // Remove all history-related keys
                prefs.all.keys
                    .filter { it.startsWith(KEY_HISTORY_PREFIX) }
                    .forEach { editor.remove(it) }
                
                editor.apply()
                Timber.i("Cleared history data")
            } catch (e: Exception) {
                Timber.e(e, "Failed to clear history data")
            }
        }
    }
    
    /**
     * 📊 Get all stored permission keys for debugging
     */
    suspend fun getStoredPermissions(): Set<String> {
        return withContext(Dispatchers.IO) {
            try {
                prefs.all.keys
                    .filter { it.startsWith(KEY_EDUCATION_PREFIX) || it.startsWith(KEY_HISTORY_PREFIX) }
                    .map { key ->
                        when {
                            key.startsWith(KEY_EDUCATION_PREFIX) -> key.removePrefix(KEY_EDUCATION_PREFIX)
                            key.startsWith(KEY_HISTORY_PREFIX) -> key.removePrefix(KEY_HISTORY_PREFIX)
                            else -> key
                        }
                    }
                    .toSet()
            } catch (e: Exception) {
                Timber.e(e, "Failed to get stored permissions")
                emptySet()
            }
        }
    }
    
    // ========== SERIALIZATION HELPERS ==========
    
    /**
     * Simple JSON-like serialization for history entries
     * (In production, you might want to use a proper JSON library)
     */
    private fun serializeHistoryEntries(entries: List<PermissionHistoryEntry>): String {
        return entries.joinToString("|") { entry ->
            "${entry.granted}:${entry.timestamp}:${entry.requestSource}"
        }
    }
    
    /**
     * Deserialize history entries from string format
     */
    private fun deserializeHistoryEntries(data: String, permission: Permission): List<PermissionHistoryEntry> {
        return try {
            data.split("|")
                .filter { it.isNotBlank() }
                .mapNotNull { entryData ->
                    val parts = entryData.split(":")
                    if (parts.size >= 3) {
                        PermissionHistoryEntry(
                            permission = permission,
                            granted = parts[0].toBoolean(),
                            timestamp = parts[1].toLong(),
                            requestSource = parts[2]
                        )
                    } else null
                }
        } catch (e: Exception) {
            Timber.e(e, "Failed to deserialize history entries: $data")
            emptyList()
        }
    }
    
    // ========== MIGRATION HELPERS ==========
    
    /**
     * 🔄 Migrate data from old permission manager
     */
    suspend fun migrateFromLegacyData() {
        withContext(Dispatchers.IO) {
            try {
                // This would handle migration from old PermissionManager data
                // Implementation would depend on the old data structure
                Timber.i("Starting permission data migration")
                
                // Example: migrate old education flags
                // migrateLegacyEducationFlags()
                
                Timber.i("Permission data migration completed")
            } catch (e: Exception) {
                Timber.e(e, "Failed to migrate permission data")
            }
        }
    }
    
    /**
     * 🔍 Health check for data integrity
     */
    suspend fun performHealthCheck(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // Validate data integrity
                val stats = getPermissionStats()
                val storedPermissions = getStoredPermissions()
                
                Timber.i("Permission data health check - Stats: $stats, Stored permissions: ${storedPermissions.size}")
                
                // Basic validation
                stats.requestedCount >= 0 && 
                stats.grantedCount >= 0 && 
                stats.deniedCount >= 0 &&
                stats.requestedCount >= (stats.grantedCount + stats.deniedCount)
                
            } catch (e: Exception) {
                Timber.e(e, "Permission data health check failed")
                false
            }
        }
    }
}
