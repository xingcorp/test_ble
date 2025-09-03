package com.nordicbeacon.scanner.infrastructure.oem.education

import android.content.Context
import com.nordicbeacon.scanner.infrastructure.oem.coordination.BatteryOptimizationCoordinator
import com.nordicbeacon.scanner.infrastructure.oem.coordination.DeviceOptimizationAnalysis
import com.nordicbeacon.scanner.infrastructure.oem.coordination.UrgencyLevel
import com.nordicbeacon.scanner.infrastructure.oem.models.BatteryOptimizationResult
import com.nordicbeacon.scanner.infrastructure.oem.models.OptimizationStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 📚 User Education Helper
 * 
 * Provides comprehensive user education cho battery optimization
 * Delivers OEM-specific guidance với progressive disclosure
 * 
 * Key Features:
 * - OEM-specific educational content
 * - Progressive disclosure based on user needs
 * - Success validation và feedback
 * - Troubleshooting assistance
 * 
 * @author Senior Android Developer
 */
@Singleton
class UserEducationHelper @Inject constructor(
    @ApplicationContext private val context: Context,
    private val batteryOptimizationCoordinator: BatteryOptimizationCoordinator
) {

    // ========== EDUCATIONAL CONTENT DELIVERY ==========

    /**
     * 📖 Get comprehensive education package cho current device
     */
    suspend fun getEducationPackage(): UserEducationPackage {
        
        Timber.i("📖 Generating user education package...")
        
        return try {
            val analysis = batteryOptimizationCoordinator.analyzeDeviceOptimizationNeeds()
            val educationContent = batteryOptimizationCoordinator.getEducationalContent()
            
            UserEducationPackage(
                deviceSpecificContent = educationContent,
                analysisResult = analysis,
                urgencyMessage = generateUrgencyMessage(analysis),
                stepByStepGuide = generateStepByStepGuide(analysis),
                troubleshootingTips = generateTroubleshootingTips(analysis),
                expectedOutcome = generateExpectedOutcome(analysis)
            )
            
        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to generate education package")
            createFallbackEducationPackage()
        }
    }

    /**
     * 🎯 Generate quick action guidance
     */
    suspend fun getQuickActionGuidance(): QuickActionGuidance {
        
        return try {
            val analysis = batteryOptimizationCoordinator.analyzeDeviceOptimizationNeeds()
            
            QuickActionGuidance(
                primaryAction = generatePrimaryAction(analysis),
                secondaryActions = generateSecondaryActions(analysis),
                timeEstimate = estimateConfigurationTime(analysis),
                difficultyLevel = analysis.strategy?.getOptimizationStrategy()?.estimatedComplexity?.description ?: "Unknown",
                criticalWarnings = generateCriticalWarnings(analysis)
            )
            
        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to generate quick action guidance")
            createFallbackQuickGuidance()
        }
    }

    /**
     * ✅ Validate optimization success với user feedback
     */
    suspend fun validateOptimizationSuccess(): OptimizationValidationResult {
        
        Timber.i("✅ Validating optimization success...")
        
        return try {
            // Re-check optimization status after user action
            val currentStatus = batteryOptimizationCoordinator.getCurrentOptimizationStatus()
            val analysis = batteryOptimizationCoordinator.analyzeDeviceOptimizationNeeds()
            
            OptimizationValidationResult(
                currentStatus = currentStatus,
                isOptimized = currentStatus == OptimizationStatus.OPTIMIZED,
                improvements = calculateImprovements(analysis),
                remainingIssues = identifyRemainingIssues(analysis),
                nextSteps = generateNextSteps(currentStatus, analysis)
            )
            
        } catch (e: Exception) {
            Timber.e(e, "❌ Optimization validation failed")
            
            OptimizationValidationResult(
                currentStatus = OptimizationStatus.CANNOT_DETERMINE,
                isOptimized = false,
                improvements = emptyList(),
                remainingIssues = listOf("Unable to validate optimization status"),
                nextSteps = listOf("Manual verification required")
            )
        }
    }

    // ========== CONTENT GENERATION ==========

    /**
     * 🚨 Generate urgency-based messaging
     */
    private fun generateUrgencyMessage(analysis: DeviceOptimizationAnalysis): String {
        
        return when (analysis.urgencyLevel) {
            UrgencyLevel.CRITICAL -> """
                🚨 CRITICAL: Your ${analysis.detectionResult.oemType.displayName} device will stop Nordic beacon scanning when screen turns off.
                
                Battery optimization must be configured immediately for the app to work properly.
            """.trimIndent()
            
            UrgencyLevel.HIGH -> """
                ⚠️ IMPORTANT: ${analysis.detectionResult.oemType.displayName} devices may interrupt beacon scanning.
                
                Configuring battery optimization is highly recommended for reliable operation.
            """.trimIndent()
            
            UrgencyLevel.MODERATE -> """
                💡 RECOMMENDED: Configure ${analysis.detectionResult.oemType.displayName} battery settings for optimal performance.
                
                This will improve background scanning reliability.
            """.trimIndent()
            
            UrgencyLevel.LOW -> """
                ℹ️ OPTIONAL: Your device should work well với default settings.
                
                Battery optimization can provide minor improvements.
            """.trimIndent()
            
            UrgencyLevel.NONE -> """
                ✅ EXCELLENT: Your device is already optimized for Nordic beacon scanning!
                
                No further action required.
            """.trimIndent()
        }
    }

    /**
     * 📋 Generate step-by-step guide
     */
    private fun generateStepByStepGuide(analysis: DeviceOptimizationAnalysis): List<EducationalStep> {
        
        val strategy = analysis.strategy
        if (strategy == null) {
            return createGenericSteps()
        }
        
        val instructions = strategy.getOptimizationStrategy().userInstructions
        
        return instructions.mapIndexed { index, instruction ->
            EducationalStep(
                stepNumber = index + 1,
                title = "Step ${index + 1}",
                instruction = instruction,
                expectedResult = generateExpectedResult(instruction),
                troubleshootingTip = generateTroubleshootingTip(instruction),
                isOptional = false
            )
        }
    }

    /**
     * 🔧 Generate troubleshooting tips
     */
    private fun generateTroubleshootingTips(analysis: DeviceOptimizationAnalysis): Map<String, String> {
        
        val strategy = analysis.strategy
        val educationContent = strategy?.getUserEducationContent()
        
        return if (educationContent?.troubleshooting?.isNotEmpty() == true) {
            mapOf("troubleshooting" to educationContent.troubleshooting)
        } else {
            mapOf(
                "Settings won't open" to "Try opening Settings app manually và navigate to Battery section",
                "App option not found" to "Look cho the app in Apps hoặc Application management section", 
                "Changes don't save" to "Make sure to tap Save/Done after making changes"
            )
        }
    }

    /**
     * 🎯 Generate expected optimization outcome
     */
    private fun generateExpectedOutcome(analysis: DeviceOptimizationAnalysis): String {
        
        return when (analysis.estimatedImpact) {
            com.nordicbeacon.scanner.infrastructure.oem.coordination.OptimizationImpact.CRITICAL -> """
                After optimization, Nordic beacon scanning will:
                ✅ Continue running when screen is off
                ✅ Survive phone lock/unlock cycles  
                ✅ Restart automatically if stopped by system
                ✅ Maintain consistent detection performance
            """.trimIndent()
            
            com.nordicbeacon.scanner.infrastructure.oem.coordination.OptimizationImpact.HIGH -> """
                You should notice:
                ✅ More reliable background scanning
                ✅ Better service persistence  
                ✅ Reduced scanning interruptions
            """.trimIndent()
            
            else -> """
                Expected improvements:
                ✅ Enhanced background operation
                ✅ Better app reliability
            """.trimIndent()
        }
    }

    // ========== HELPER METHODS ==========

    private fun createFallbackEducationPackage(): UserEducationPackage {
        return UserEducationPackage(
            deviceSpecificContent = null,
            analysisResult = null,
            urgencyMessage = "Configure battery optimization cho continuous Nordic beacon scanning",
            stepByStepGuide = createGenericSteps(),
            troubleshootingTips = mapOf(
                "General issue" to "Contact support cho device-specific assistance"
            ),
            expectedOutcome = "Improved background scanning reliability"
        )
    }

    private fun createFallbackQuickGuidance(): QuickActionGuidance {
        return QuickActionGuidance(
            primaryAction = "Open Settings > Battery > Battery optimization",
            secondaryActions = listOf("Find Nordic Beacon Scanner", "Select Don't optimize"),
            timeEstimate = "2-3 minutes",
            difficultyLevel = "Easy",
            criticalWarnings = emptyList()
        )
    }

    private fun createGenericSteps(): List<EducationalStep> = emptyList()
    private fun generatePrimaryAction(analysis: DeviceOptimizationAnalysis): String = "Configure battery settings"
    private fun generateSecondaryActions(analysis: DeviceOptimizationAnalysis): List<String> = emptyList()
    private fun estimateConfigurationTime(analysis: DeviceOptimizationAnalysis): String = "2-5 minutes"
    private fun generateCriticalWarnings(analysis: DeviceOptimizationAnalysis): List<String> = emptyList()
    private fun generateExpectedResult(instruction: String): String = ""
    private fun generateTroubleshootingTip(instruction: String): String = ""
    private fun calculateImprovements(analysis: DeviceOptimizationAnalysis): List<String> = emptyList()
    private fun identifyRemainingIssues(analysis: DeviceOptimizationAnalysis): List<String> = emptyList()
    private fun generateNextSteps(status: OptimizationStatus, analysis: DeviceOptimizationAnalysis): List<String> = emptyList()
}

// ========== DATA MODELS ==========

data class UserEducationPackage(
    val deviceSpecificContent: com.nordicbeacon.scanner.infrastructure.oem.strategies.OemEducationContent?,
    val analysisResult: DeviceOptimizationAnalysis?,
    val urgencyMessage: String,
    val stepByStepGuide: List<EducationalStep>,
    val troubleshootingTips: Map<String, String>,
    val expectedOutcome: String
)

data class QuickActionGuidance(
    val primaryAction: String,
    val secondaryActions: List<String>,
    val timeEstimate: String,
    val difficultyLevel: String,
    val criticalWarnings: List<String>
)

data class EducationalStep(
    val stepNumber: Int,
    val title: String,
    val instruction: String,
    val expectedResult: String,
    val troubleshootingTip: String,
    val isOptional: Boolean
)

data class OptimizationValidationResult(
    val currentStatus: OptimizationStatus,
    val isOptimized: Boolean,
    val improvements: List<String>,
    val remainingIssues: List<String>,
    val nextSteps: List<String>
)
