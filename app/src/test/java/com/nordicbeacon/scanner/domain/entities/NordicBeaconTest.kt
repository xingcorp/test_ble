package com.nordicbeacon.scanner.domain.entities

import org.junit.Test
import org.junit.Assert.*

/**
 * 🧪 Unit Tests - NordicBeacon Entity
 * 
 * Validates Nordic beacon entity behavior và business logic
 * Tests UUID validation, signal quality assessment, proximity calculation
 * 
 * @author Senior Android Developer
 */
class NordicBeaconTest {

    // ========== UUID VALIDATION TESTS ==========

    @Test
    fun `should validate Nordic UUID correctly`() {
        // Given
        val validUuid = "FDA50693-0000-0000-0000-290995101092"
        val invalidUuid = "12345678-1234-1234-1234-123456789012"
        
        // When & Then
        assertTrue("Valid Nordic UUID should be accepted", 
            BeaconUUID.from(validUuid).value == validUuid.uppercase())
            
        assertThrows("Invalid UUID should be rejected", IllegalArgumentException::class.java) {
            BeaconUUID.from("invalid-uuid")
        }
    }

    @Test  
    fun `should create valid Nordic beacon with factory method`() {
        // Given
        val nordicUuid = "FDA50693-0000-0000-0000-290995101092"
        val major = 1234
        val minor = 5678  
        val rssi = -65
        val distance = 2.5
        
        // When
        val beacon = NordicBeacon.create(nordicUuid, major, minor, rssi, distance)
        
        // Then
        assertNotNull("Valid Nordic beacon should be created", beacon)
        beacon?.let {
            assertTrue("Should be valid Nordic beacon", it.isValidNordicBeacon())
            assertEquals("Major should match", major, it.major?.value)
            assertEquals("Minor should match", minor, it.minor?.value)
            assertEquals("RSSI should match", rssi, it.signalStrength.rssi)
            assertEquals("Distance should match", distance, it.proximity.meters, 0.01)
        }
    }

    @Test
    fun `should reject invalid beacon creation parameters`() {
        // Given
        val invalidUuid = "12345678-1234-1234-1234-123456789012"  
        val nordicUuid = "FDA50693-0000-0000-0000-290995101092"
        
        // When & Then - Invalid UUID
        val invalidBeacon = NordicBeacon.create(invalidUuid, 1, 1, -50, 1.0)
        assertNull("Invalid UUID should result in null beacon", invalidBeacon)
        
        // When & Then - Invalid RSSI
        val weakSignalBeacon = NordicBeacon.create(nordicUuid, 1, 1, -150, 1.0)
        assertNull("Too weak signal should result in null beacon", weakSignalBeacon)
        
        // When & Then - Negative distance
        val negativeDistanceBeacon = NordicBeacon.create(nordicUuid, 1, 1, -50, -1.0)
        assertNull("Negative distance should result in null beacon", negativeDistanceBeacon)
    }

    // ========== PROXIMITY TESTS ==========

    @Test
    fun `should correctly categorize beacon proximity`() {
        // Given
        val immediateBeacon = createTestBeacon(distance = 0.5)
        val nearBeacon = createTestBeacon(distance = 3.0)
        val farBeacon = createTestBeacon(distance = 15.0) 
        val veryFarBeacon = createTestBeacon(distance = 25.0)
        
        // When & Then
        assertTrue("0.5m should be immediate", immediateBeacon.isImmediate())
        assertFalse("0.5m should not be near", immediateBeacon.isNear())
        
        assertTrue("3m should be near", nearBeacon.isNear())
        assertFalse("3m should not be immediate", nearBeacon.isImmediate())
        
        assertTrue("15m should be far", farBeacon.isFar())
        assertFalse("15m should not be near", farBeacon.isNear())
        
        assertFalse("25m should not be far", veryFarBeacon.isFar())
        assertFalse("25m should not be immediate/near", veryFarBeacon.isImmediate() || veryFarBeacon.isNear())
    }

    // ========== RELIABILITY SCORE TESTS ==========

    @Test
    fun `should calculate reliability score correctly`() {
        // Given - Strong signal, close distance
        val strongBeacon = createTestBeacon(rssi = -45, distance = 1.0)
        
        // When  
        val strongScore = strongBeacon.calculateReliabilityScore()
        
        // Then
        assertTrue("Strong beacon should have high reliability", strongScore >= 80)
        
        // Given - Weak signal, far distance
        val weakBeacon = createTestBeacon(rssi = -90, distance = 18.0)
        
        // When
        val weakScore = weakBeacon.calculateReliabilityScore()
        
        // Then 
        assertTrue("Weak beacon should have low reliability", weakScore <= 50)
    }

    @Test
    fun `should handle edge case reliability calculations`() {
        // Given - Edge case values
        val edgeBeacon = createTestBeacon(rssi = -95, distance = 50.0)
        
        // When
        val score = edgeBeacon.calculateReliabilityScore()
        
        // Then
        assertTrue("Edge case should have valid score range", score in 0..100)
    }

    // ========== VALUE OBJECT TESTS ==========

    @Test
    fun `should validate SignalStrength constraints`() {
        // Valid RSSI values
        assertDoesNotThrow { SignalStrength(-50) }
        assertDoesNotThrow { SignalStrength(-100) }
        assertDoesNotThrow { SignalStrength(20) }
        
        // Invalid RSSI values
        assertThrows(IllegalArgumentException::class.java) { SignalStrength(-150) }
        assertThrows(IllegalArgumentException::class.java) { SignalStrength(50) }
    }

    @Test 
    fun `should validate Major and Minor constraints`() {
        // Valid values
        assertDoesNotThrow { Major(0) }
        assertDoesNotThrow { Major(65535) }
        assertDoesNotThrow { Minor(1234) }
        
        // Invalid values
        assertThrows(IllegalArgumentException::class.java) { Major(-1) }
        assertThrows(IllegalArgumentException::class.java) { Major(65536) }
        assertThrows(IllegalArgumentException::class.java) { Minor(-1) }
        assertThrows(IllegalArgumentException::class.java) { Minor(65536) }
    }

    // ========== HELPER METHODS ==========

    /**
     * 🏭 Create test Nordic beacon với specified parameters
     */
    private fun createTestBeacon(
        rssi: Int = -65,
        distance: Double = 5.0,
        major: Int = 1234,
        minor: Int = 5678
    ): NordicBeacon {
        return NordicBeacon.create(
            uuid = "FDA50693-0000-0000-0000-290995101092",
            major = major,
            minor = minor, 
            rssi = rssi,
            distance = distance
        ) ?: throw IllegalStateException("Failed to create test beacon")
    }

    /**
     * 🔧 Assert that operation does not throw exception
     */
    private fun assertDoesNotThrow(operation: () -> Unit) {
        try {
            operation()
        } catch (e: Exception) {
            fail("Operation should not throw exception: ${e.message}")
        }
    }
}
