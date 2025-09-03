package com.nordicbeacon.scanner.core.permissions.impl

import com.nordicbeacon.scanner.core.permissions.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 📚 Permission Education Content Provider
 * 
 * Provides comprehensive, contextual educational content to help users
 * understand why permissions are needed and encourage grants.
 * 
 * @author Senior Android Engineer
 */
@Singleton
class PermissionEducatorImpl @Inject constructor(
    private val repository: PermissionRepository
) : PermissionEducator {
    
    override fun getEducationalContent(permission: Permission): PermissionEducationContent {
        return when (permission) {
            // ========== LOCATION PERMISSIONS ==========
            Permission.LOCATION_FINE -> PermissionEducationContent(
                permission = permission,
                title = "Precise Location Access",
                description = "We need access to your precise location to provide accurate distance measurements and proximity-based features.",
                rationale = "Location access allows the app to detect nearby devices and provide location-based services with high accuracy.",
                benefits = listOf(
                    "Accurate device detection and ranging",
                    "Proximity-based automation",
                    "Location-based reminders",
                    "Enhanced user experience"
                ),
                icon = "location_on",
                priority = PermissionImportance.HIGH
            )
            
            Permission.LOCATION_COARSE -> PermissionEducationContent(
                permission = permission,
                title = "Approximate Location Access",
                description = "We need access to your approximate location for general proximity features.",
                rationale = "Approximate location helps us provide location-based services while preserving battery life.",
                benefits = listOf(
                    "Basic proximity detection",
                    "Regional content customization",
                    "Energy-efficient location services"
                ),
                icon = "location_city",
                priority = PermissionImportance.MEDIUM
            )
            
            Permission.LOCATION_BACKGROUND -> PermissionEducationContent(
                permission = permission,
                title = "Background Location Access",
                description = "Allow the app to access location even when not actively using it for continuous monitoring.",
                rationale = "Background location enables automated features and continuous device monitoring when the app is closed.",
                benefits = listOf(
                    "Continuous device monitoring",
                    "Automated location-based actions",
                    "Background sync and updates",
                    "Seamless user experience"
                ),
                icon = "gps_fixed",
                priority = PermissionImportance.HIGH
            )
            
            // ========== BLUETOOTH PERMISSIONS ==========
            Permission.BLUETOOTH_SCAN -> PermissionEducationContent(
                permission = permission,
                title = "Bluetooth Device Scanning",
                description = "Scan for nearby Bluetooth devices including beacons, sensors, and other connected devices.",
                rationale = "Bluetooth scanning is essential for discovering and connecting to nearby devices.",
                benefits = listOf(
                    "Discover nearby Bluetooth devices",
                    "Automatic device pairing",
                    "Enhanced connectivity features",
                    "Real-time device monitoring"
                ),
                icon = "bluetooth_searching",
                priority = PermissionImportance.HIGH
            )
            
            Permission.BLUETOOTH_CONNECT -> PermissionEducationContent(
                permission = permission,
                title = "Bluetooth Device Connection",
                description = "Connect to previously discovered Bluetooth devices for data exchange.",
                rationale = "Connection permission allows the app to establish communication with Bluetooth devices.",
                benefits = listOf(
                    "Connect to paired devices",
                    "Exchange data with devices",
                    "Control connected devices",
                    "Sync device settings"
                ),
                icon = "bluetooth_connected",
                priority = PermissionImportance.HIGH
            )
            
            Permission.BLUETOOTH_ADVERTISE -> PermissionEducationContent(
                permission = permission,
                title = "Bluetooth Advertisement",
                description = "Make this device discoverable to other Bluetooth devices.",
                rationale = "Advertisement permission allows other devices to find and connect to this device.",
                benefits = listOf(
                    "Enable device discovery",
                    "Share data with other devices",
                    "Act as a beacon or hub",
                    "Enable peer-to-peer features"
                ),
                icon = "bluetooth",
                priority = PermissionImportance.MEDIUM
            )
            
            Permission.BLUETOOTH -> PermissionEducationContent(
                permission = permission,
                title = "Basic Bluetooth Access",
                description = "Access basic Bluetooth functionality for device communication.",
                rationale = "Basic Bluetooth access is required for any Bluetooth-related features.",
                benefits = listOf(
                    "Enable Bluetooth features",
                    "Device connectivity",
                    "Legacy device support"
                ),
                icon = "bluetooth",
                priority = PermissionImportance.HIGH
            )
            
            Permission.BLUETOOTH_ADMIN -> PermissionEducationContent(
                permission = permission,
                title = "Bluetooth Administration",
                description = "Manage Bluetooth settings and connections.",
                rationale = "Administrative access allows the app to manage Bluetooth state and connections.",
                benefits = listOf(
                    "Manage Bluetooth settings",
                    "Control connection states",
                    "Optimize connectivity"
                ),
                icon = "bluetooth_settings",
                priority = PermissionImportance.MEDIUM
            )
            
            // ========== CAMERA PERMISSIONS ==========
            Permission.CAMERA -> PermissionEducationContent(
                permission = permission,
                title = "Camera Access",
                description = "Access the camera to capture photos and videos or scan QR codes.",
                rationale = "Camera access enables photo capture, video recording, and barcode/QR code scanning features.",
                benefits = listOf(
                    "Capture photos and videos",
                    "Scan QR codes and barcodes",
                    "Document scanning",
                    "Visual recognition features"
                ),
                icon = "camera_alt",
                priority = PermissionImportance.MEDIUM
            )
            
            // ========== STORAGE PERMISSIONS ==========
            Permission.STORAGE_READ -> PermissionEducationContent(
                permission = permission,
                title = "Read Storage Access",
                description = "Read files from your device's storage to access saved documents, photos, and other files.",
                rationale = "Storage read access allows the app to access and use files you've saved on your device.",
                benefits = listOf(
                    "Access saved files and documents",
                    "Import photos and media",
                    "Backup and restore data",
                    "Share files with other apps"
                ),
                icon = "folder_open",
                priority = PermissionImportance.MEDIUM
            )
            
            Permission.STORAGE_WRITE -> PermissionEducationContent(
                permission = permission,
                title = "Write Storage Access",
                description = "Save files to your device's storage for backup, export, and sharing.",
                rationale = "Storage write access allows the app to save data, exports, and backups to your device.",
                benefits = listOf(
                    "Save and export data",
                    "Create backups",
                    "Download and store files",
                    "Cache content for offline use"
                ),
                icon = "save",
                priority = PermissionImportance.MEDIUM
            )
            
            Permission.STORAGE_MANAGE -> PermissionEducationContent(
                permission = permission,
                title = "Full Storage Management",
                description = "Manage all files on your device for advanced file operations.",
                rationale = "Full storage management provides comprehensive file access for advanced features.",
                benefits = listOf(
                    "Advanced file management",
                    "System-wide file operations",
                    "Complete backup solutions",
                    "Professional file handling"
                ),
                icon = "storage",
                priority = PermissionImportance.LOW
            )
            
            // ========== MEDIA PERMISSIONS ==========
            Permission.MEDIA_IMAGES -> PermissionEducationContent(
                permission = permission,
                title = "Photo Access",
                description = "Access photos in your gallery for viewing, editing, or sharing.",
                rationale = "Photo access allows the app to work with images stored on your device.",
                benefits = listOf(
                    "View and organize photos",
                    "Edit and enhance images",
                    "Share photos easily",
                    "Create photo albums"
                ),
                icon = "image",
                priority = PermissionImportance.MEDIUM
            )
            
            Permission.MEDIA_VIDEO -> PermissionEducationContent(
                permission = permission,
                title = "Video Access",
                description = "Access videos in your gallery for playback, editing, or sharing.",
                rationale = "Video access allows the app to work with video files stored on your device.",
                benefits = listOf(
                    "Play and organize videos",
                    "Edit video content",
                    "Share videos easily",
                    "Create video libraries"
                ),
                icon = "video_library",
                priority = PermissionImportance.MEDIUM
            )
            
            Permission.MEDIA_AUDIO -> PermissionEducationContent(
                permission = permission,
                title = "Audio Access",
                description = "Access audio files in your music library for playback and management.",
                rationale = "Audio access allows the app to work with music and audio files on your device.",
                benefits = listOf(
                    "Play music and audio",
                    "Organize audio libraries",
                    "Create playlists",
                    "Audio file management"
                ),
                icon = "library_music",
                priority = PermissionImportance.MEDIUM
            )
            
            // ========== NOTIFICATION PERMISSIONS ==========
            Permission.POST_NOTIFICATIONS -> PermissionEducationContent(
                permission = permission,
                title = "Notification Permission",
                description = "Send notifications to keep you informed about important updates and events.",
                rationale = "Notification access allows the app to alert you about important information even when not actively using the app.",
                benefits = listOf(
                    "Receive important alerts",
                    "Stay updated on events",
                    "Get timely reminders",
                    "Real-time communication"
                ),
                icon = "notifications",
                priority = PermissionImportance.MEDIUM
            )
            
            // ========== FOREGROUND SERVICE PERMISSIONS ==========
            Permission.FOREGROUND_SERVICE_LOCATION -> PermissionEducationContent(
                permission = permission,
                title = "Background Service Location",
                description = "Allow the app to run location-based services in the background for continuous Nordic beacon scanning.",
                rationale = "Foreground service location permission is required by Android 14+ for continuous beacon monitoring when the app is not actively in use.",
                benefits = listOf(
                    "Continuous Nordic beacon scanning",
                    "Background location-based services",
                    "Persistent beacon monitoring",
                    "Reliable detection even when screen is off"
                ),
                icon = "location_on",
                priority = PermissionImportance.HIGH
            )
            
            // ========== MICROPHONE PERMISSIONS ==========
            Permission.RECORD_AUDIO -> PermissionEducationContent(
                permission = permission,
                title = "Microphone Access",
                description = "Access the microphone to record audio, enable voice features, or communicate with others.",
                rationale = "Microphone access enables audio recording, voice commands, and communication features.",
                benefits = listOf(
                    "Record audio and voice memos",
                    "Voice commands and control",
                    "Audio communication",
                    "Voice-to-text features"
                ),
                icon = "mic",
                priority = PermissionImportance.MEDIUM
            )
            
            // ========== CONTACT PERMISSIONS ==========
            Permission.READ_CONTACTS -> PermissionEducationContent(
                permission = permission,
                title = "Contacts Access",
                description = "Access your contacts to enable sharing, communication, and contact-based features.",
                rationale = "Contact access allows the app to integrate with your address book for enhanced functionality.",
                benefits = listOf(
                    "Easy contact sharing",
                    "Contact-based features",
                    "Quick communication",
                    "Social integration"
                ),
                icon = "contacts",
                priority = PermissionImportance.LOW
            )
            
            Permission.WRITE_CONTACTS -> PermissionEducationContent(
                permission = permission,
                title = "Contact Modification",
                description = "Modify your contacts to add information, sync data, or update contact details.",
                rationale = "Contact modification allows the app to update and sync contact information.",
                benefits = listOf(
                    "Sync contact information",
                    "Add contact details",
                    "Update contact data",
                    "Contact backup and restore"
                ),
                icon = "person_add",
                priority = PermissionImportance.LOW
            )
            
            // ========== PHONE PERMISSIONS ==========
            Permission.PHONE_STATE -> PermissionEducationContent(
                permission = permission,
                title = "Phone State Access",
                description = "Access basic phone information and call state for enhanced functionality.",
                rationale = "Phone state access allows the app to respond to calls and access device information.",
                benefits = listOf(
                    "Respond to incoming calls",
                    "Access device information",
                    "Call state management",
                    "Enhanced phone integration"
                ),
                icon = "phone",
                priority = PermissionImportance.LOW
            )
            
            Permission.PHONE_NUMBERS -> PermissionEducationContent(
                permission = permission,
                title = "Phone Number Access",
                description = "Access phone numbers for identification and communication features.",
                rationale = "Phone number access enables caller identification and phone-based features.",
                benefits = listOf(
                    "Caller identification",
                    "Phone-based authentication",
                    "Contact management",
                    "Communication features"
                ),
                icon = "phone",
                priority = PermissionImportance.LOW
            )
            
            Permission.CALL_PHONE -> PermissionEducationContent(
                permission = permission,
                title = "Make Phone Calls",
                description = "Make phone calls directly from the app for quick communication.",
                rationale = "Call permission allows the app to initiate phone calls on your behalf.",
                benefits = listOf(
                    "Quick call functionality",
                    "One-tap calling",
                    "Emergency calling",
                    "Integrated communication"
                ),
                icon = "call",
                priority = PermissionImportance.LOW
            )
        }
    }
    
    override fun getGroupEducationalContent(group: PermissionGroup): PermissionEducationContent {
        return when (group) {
            PermissionGroup.LOCATION -> PermissionEducationContent(
                permission = Permission.LOCATION_FINE, // Representative permission
                title = "Location Services",
                description = "Location access enables proximity-based features, navigation, and location-aware functionality.",
                rationale = "Location permissions are essential for providing accurate, context-aware services based on where you are.",
                benefits = listOf(
                    "Accurate proximity detection",
                    "Location-based automation",
                    "Navigation and directions",
                    "Contextual information"
                ),
                icon = "location_on",
                priority = PermissionImportance.HIGH
            )
            
            PermissionGroup.BLUETOOTH -> PermissionEducationContent(
                permission = Permission.BLUETOOTH_SCAN,
                title = "Bluetooth Connectivity",
                description = "Bluetooth access enables device discovery, connection, and communication with nearby devices.",
                rationale = "Bluetooth permissions are required for all device scanning, pairing, and communication features.",
                benefits = listOf(
                    "Device discovery and pairing",
                    "Wireless data exchange",
                    "IoT device control",
                    "Seamless connectivity"
                ),
                icon = "bluetooth",
                priority = PermissionImportance.HIGH
            )
            
            PermissionGroup.CAMERA -> PermissionEducationContent(
                permission = Permission.CAMERA,
                title = "Camera Features",
                description = "Camera access enables photo capture, video recording, and visual recognition features.",
                rationale = "Camera permission is required for all photography, scanning, and visual feature functionality.",
                benefits = listOf(
                    "Photo and video capture",
                    "QR code scanning",
                    "Document digitization",
                    "Visual recognition"
                ),
                icon = "camera_alt",
                priority = PermissionImportance.MEDIUM
            )
            
            PermissionGroup.STORAGE -> PermissionEducationContent(
                permission = Permission.STORAGE_READ,
                title = "File Access",
                description = "Storage access enables file operations, data backup, and content management.",
                rationale = "Storage permissions allow the app to save, load, and manage files on your device.",
                benefits = listOf(
                    "File backup and restore",
                    "Data import and export",
                    "Offline content access",
                    "File sharing capabilities"
                ),
                icon = "folder",
                priority = PermissionImportance.MEDIUM
            )
            
            PermissionGroup.MEDIA -> PermissionEducationContent(
                permission = Permission.MEDIA_IMAGES,
                title = "Media Access",
                description = "Media access enables working with photos, videos, and audio files in your library.",
                rationale = "Media permissions provide access to your multimedia content for editing and sharing.",
                benefits = listOf(
                    "Photo and video editing",
                    "Media organization",
                    "Content sharing",
                    "Multimedia management"
                ),
                icon = "perm_media",
                priority = PermissionImportance.MEDIUM
            )
            
            PermissionGroup.NOTIFICATIONS -> PermissionEducationContent(
                permission = Permission.POST_NOTIFICATIONS,
                title = "Notifications",
                description = "Notification permission enables timely alerts and important information delivery.",
                rationale = "Notification access keeps you informed about important events and updates.",
                benefits = listOf(
                    "Real-time alerts",
                    "Important notifications",
                    "Event reminders",
                    "Status updates"
                ),
                icon = "notifications",
                priority = PermissionImportance.MEDIUM
            )
            
            PermissionGroup.MICROPHONE -> PermissionEducationContent(
                permission = Permission.RECORD_AUDIO,
                title = "Audio Features",
                description = "Microphone access enables audio recording, voice commands, and communication features.",
                rationale = "Microphone permission is required for all audio input and voice-related functionality.",
                benefits = listOf(
                    "Voice recording",
                    "Voice commands",
                    "Audio communication",
                    "Sound analysis"
                ),
                icon = "mic",
                priority = PermissionImportance.MEDIUM
            )
            
            PermissionGroup.CONTACTS -> PermissionEducationContent(
                permission = Permission.READ_CONTACTS,
                title = "Contact Integration",
                description = "Contact access enables integration with your address book for enhanced social features.",
                rationale = "Contact permissions allow the app to work with your existing contacts for better functionality.",
                benefits = listOf(
                    "Contact integration",
                    "Social features",
                    "Easy sharing",
                    "Contact sync"
                ),
                icon = "contacts",
                priority = PermissionImportance.LOW
            )
            
            PermissionGroup.PHONE -> PermissionEducationContent(
                permission = Permission.PHONE_STATE,
                title = "Phone Integration",
                description = "Phone access enables call management, device identification, and phone-based features.",
                rationale = "Phone permissions provide integration with your device's calling capabilities.",
                benefits = listOf(
                    "Call management",
                    "Device identification",
                    "Phone integration",
                    "Communication features"
                ),
                icon = "phone",
                priority = PermissionImportance.LOW
            )
        }
    }
    
    override suspend fun shouldShowEducation(permission: Permission): Boolean {
        return !repository.wasEducationShown(permission)
    }
    
    override suspend fun markEducationShown(permission: Permission) {
        repository.saveEducationShown(permission)
    }
}
