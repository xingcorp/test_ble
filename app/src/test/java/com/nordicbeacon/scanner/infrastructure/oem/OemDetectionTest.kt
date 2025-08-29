package com.nordicbeacon.scanner.infrastructure.oem

import com.nordicbeacon.scanner.infrastructure.oem.detection.DeviceDetectionFactory
import com.nordicbeacon.scanner.infrastructure.oem.detection.OemType
import com.nordicbeacon.scanner.infrastructure.oem.models.DeviceInfo
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

/**
 * 🧪 Unit Tests - OEM Device Detection
 * 
 * Validates OEM detection logic cho major Android manufacturers
 * Tests edge cases và detection confidence scoring
 * 
 * @author Senior Android Developer
 */
class OemDetectionTest {

    private lateinit var deviceDetectionFactory: DeviceDetectionFactory

    @Before
    fun setUp() {
        deviceDetectionFactory = DeviceDetectionFactory()
    }

    // ========== SAMSUNG DETECTION TESTS ==========

    @Test
    fun `should detect Samsung Galaxy devices correctly`() {
        // Given
        val samsungDevice = DeviceInfo(
            manufacturer = "samsung", 
            brand = "samsung",
            model = "SM-G998B",
            device = "r9s",
            product = "r9sxx",
            buildFingerprint = "samsung/r9sxx/r9s:12/SP1A.210812.016/G998BXXS5CVID:user/release-keys"
        )
        
        // When
        val result = deviceDetectionFactory.detectOemType(samsungDevice)
        
        // Then
        assertEquals("Should detect Samsung", OemType.SAMSUNG, result.oemType)
        assertTrue("Should have high confidence", result.confidence >= 90)
        assertTrue("Should be reliable detection", result.isReliable())
    }

    @Test
    fun `should detect Xiaomi ecosystem devices correctly`() {
        // Given - Xiaomi device
        val xiaomiDevice = DeviceInfo(
            manufacturer = "Xiaomi",
            brand = "xiaomi", 
            model = "Mi 11",
            buildFingerprint = "Xiaomi/venus_global/venus:12/RKQ1.200826.002/V13.0.3.0.SKBMIXM:user/release-keys"
        )
        
        // When
        val xiaomiResult = deviceDetectionFactory.detectOemType(xiaomiDevice)
        
        // Then
        assertEquals("Should detect Xiaomi", OemType.XIAOMI, xiaomiResult.oemType)
        assertTrue("Should have high confidence", xiaomiResult.confidence >= 90)
        
        // Given - Redmi device
        val redmiDevice = DeviceInfo(
            manufacturer = "Redmi",
            brand = "redmi",
            model = "Redmi Note 11",
            buildFingerprint = "Redmi/spes_global/spes:12/SKQ1.211103.001/V13.0.6.0.SGCMIXM:user/release-keys"
        )
        
        // When  
        val redmiResult = deviceDetectionFactory.detectOemType(redmiDevice)
        
        // Then
        assertEquals("Should detect Redmi as Xiaomi ecosystem", OemType.XIAOMI, redmiResult.oemType)
    }

    @Test
    fun `should detect Huawei and Honor devices correctly`() {
        // Given - Huawei device
        val huaweiDevice = DeviceInfo(
            manufacturer = "HUAWEI",
            brand = "huawei",
            model = "ELS-NX9", 
            buildFingerprint = "HUAWEI/hwELS-H/HWELS:10/HUAWEIELS-H/10.0.0.175C00E3R2P13:user/release-keys"
        )
        
        // When
        val huaweiResult = deviceDetectionFactory.detectOemType(huaweiDevice)
        
        // Then
        assertEquals("Should detect Huawei", OemType.HUAWEI, huaweiResult.oemType)
        assertTrue("Should have good confidence", huaweiResult.confidence >= 85)
        
        // Given - Honor device  
        val honorDevice = DeviceInfo(
            manufacturer = "HONOR",
            brand = "honor",
            model = "HLK-AL00"
        )
        
        // When
        val honorResult = deviceDetectionFactory.detectOemType(honorDevice)
        
        // Then
        assertEquals("Should detect Honor as Huawei ecosystem", OemType.HUAWEI, honorResult.oemType)
    }

    @Test
    fun `should detect Google Pixel devices correctly`() {
        // Given
        val pixelDevice = DeviceInfo(
            manufacturer = "Google",
            brand = "google",
            model = "Pixel 7",
            device = "panther",
            product = "panther",
            buildFingerprint = "google/panther/panther:13/TQ3A.230805.001/10316531:user/release-keys"
        )
        
        // When  
        val result = deviceDetectionFactory.detectOemType(pixelDevice)
        
        // Then
        assertEquals("Should detect Google Pixel", OemType.GOOGLE, result.oemType)
        assertTrue("Should have very high confidence", result.confidence >= 95)
        assertTrue("Should be high confidence detection", result.isHighConfidence())
    }

    @Test
    fun `should detect OnePlus devices correctly`() {
        // Given
        val onePlusDevice = DeviceInfo(
            manufacturer = "OnePlus",
            brand = "oneplus",
            model = "LE2123",
            buildFingerprint = "OnePlus/OnePlus9Pro_EEA/OnePlus9Pro:12/RKQ1.201105.002/2204262302:user/release-keys"
        )
        
        // When
        val result = deviceDetectionFactory.detectOemType(onePlusDevice)
        
        // Then
        assertEquals("Should detect OnePlus", OemType.ONEPLUS, result.oemType)
        assertTrue("Should have good confidence", result.confidence >= 80)
    }

    @Test
    fun `should handle BBK ecosystem devices (Oppo, Vivo, Realme)`() {
        // Given - Oppo device
        val oppoDevice = DeviceInfo(
            manufacturer = "OPPO",
            brand = "oppo", 
            model = "CPH2173"
        )
        
        // When
        val oppoResult = deviceDetectionFactory.detectOemType(oppoDevice)
        
        // Then
        assertEquals("Should detect Oppo", OemType.OPPO, oppoResult.oemType)
        
        // Given - Vivo device
        val vivoDevice = DeviceInfo(
            manufacturer = "vivo",
            brand = "vivo",
            model = "V2040"
        )
        
        // When
        val vivoResult = deviceDetectionFactory.detectOemType(vivoDevice)
        
        // Then  
        assertEquals("Should detect Vivo", OemType.VIVO, vivoResult.oemType)
        
        // Given - Realme device
        val realmeDevice = DeviceInfo(
            manufacturer = "realme",
            brand = "realme",
            model = "RMX3085"
        )
        
        // When
        val realmeResult = deviceDetectionFactory.detectOemType(realmeDevice)
        
        // Then
        assertEquals("Should detect Realme as Oppo ecosystem", OemType.OPPO, realmeResult.oemType)
    }

    @Test
    fun `should fall back to generic cho unknown manufacturers`() {
        // Given
        val unknownDevice = DeviceInfo(
            manufacturer = "Unknown Manufacturer",
            brand = "unknown",
            model = "Unknown Model"
        )
        
        // When  
        val result = deviceDetectionFactory.detectOemType(unknownDevice)
        
        // Then
        assertEquals("Should fall back to Generic", OemType.GENERIC, result.oemType)
        assertTrue("Should have lower confidence", result.confidence < 70)
        assertFalse("Should not be high confidence", result.isHighConfidence())
    }

    @Test  
    fun `should handle edge cases và custom ROMs`() {
        // Given - Device với custom ROM
        val customRomDevice = DeviceInfo(
            manufacturer = "samsung", // Hardware manufacturer
            brand = "lineageos",       // Custom ROM brand
            model = "SM-G975F",
            buildFingerprint = "lineage_beyond1lte-userdebug/lineage_beyond1lte:11/RQ3A.211001.001/eng.user:userdebug/test-keys"
        )
        
        // When
        val result = deviceDetectionFactory.detectOemType(customRomDevice)
        
        // Then - Should still detect hardware manufacturer
        assertEquals("Should detect Samsung hardware", OemType.SAMSUNG, result.oemType)
        // But confidence might be lower due to custom ROM
    }

    @Test
    fun `should validate detection confidence scoring`() {
        // Test confidence scoring algorithm
        val highConfidenceDevice = DeviceInfo(
            manufacturer = "Google",
            brand = "google", 
            model = "Pixel 7"
        )
        
        val result = deviceDetectionFactory.detectOemType(highConfidenceDevice)
        
        assertTrue("High confidence device should score >90", result.confidence > 90)
        assertTrue("Should be reliable detection", result.isReliable())
        assertTrue("Should be high confidence", result.isHighConfidence())
    }
}
