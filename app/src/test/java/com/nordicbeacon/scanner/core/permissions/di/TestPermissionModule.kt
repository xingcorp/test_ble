package com.nordicbeacon.scanner.core.permissions.di

import com.nordicbeacon.scanner.core.permissions.*
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

/**
 * 🧪 Test Permission Module
 * 
 * Replaces the main PermissionModule during tests to provide mock implementations.
 * Uses @TestInstallIn to properly replace production bindings in tests.
 * 
 * @author Senior Android Engineer
 */
@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [PermissionModule::class]
)
object TestPermissionModule {
    
    /**
     * 🎭 Provide mock PermissionStrategy for testing
     */
    @Provides
    @Singleton
    fun provideMockPermissionStrategy(): PermissionStrategy {
        return MockPermissionStrategy()
    }
}

/**
 * 🎭 Mock Permission Strategy for Testing
 * 
 * Allows testing permission flows without actual system interactions.
 * Configurable behavior for different test scenarios.
 */
class MockPermissionStrategy : PermissionStrategy {
    
    // Configuration for test behavior
    var shouldGrantPermissions = true
    var shouldShowRationale = false
    var simulateDelay = false
    var simulatePermanentDenial = false
    
    override fun getRequiredPermissions(permission: Permission): List<String> {
        return if (permission.isApplicableForCurrentVersion()) {
            listOf(permission.manifestPermission)
        } else {
            emptyList()
        }
    }
    
    override fun shouldShowRationale(activity: androidx.fragment.app.FragmentActivity, permission: String): Boolean {
        return shouldShowRationale
    }
    
    override fun isPermissionGranted(activity: androidx.fragment.app.FragmentActivity, permission: String): Boolean {
        return shouldGrantPermissions
    }
    
    override fun requestPermissions(
        activity: androidx.fragment.app.FragmentActivity, 
        permissions: Array<String>, 
        requestCode: Int
    ) {
        // Simulate permission request
        if (simulateDelay) {
            Thread.sleep(1000)
        }
        
        // Mock callback would be triggered here in a real test
        // activity.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
    
    override fun handleSpecialPermission(permission: Permission): PermissionResult? {
        return null // No special handling in mock
    }
    
    override fun isPermanentlyDenied(activity: androidx.fragment.app.FragmentActivity, permission: String): Boolean {
        return simulatePermanentDenial
    }
    
    // ========== TEST CONFIGURATION METHODS ==========
    
    fun configureForGrantedScenario() {
        shouldGrantPermissions = true
        shouldShowRationale = false
        simulatePermanentDenial = false
    }
    
    fun configureForDeniedScenario() {
        shouldGrantPermissions = false
        shouldShowRationale = true
        simulatePermanentDenial = false
    }
    
    fun configureForPermanentlyDeniedScenario() {
        shouldGrantPermissions = false
        shouldShowRationale = false
        simulatePermanentDenial = true
    }
    
    fun reset() {
        shouldGrantPermissions = true
        shouldShowRationale = false
        simulateDelay = false
        simulatePermanentDenial = false
    }
}
