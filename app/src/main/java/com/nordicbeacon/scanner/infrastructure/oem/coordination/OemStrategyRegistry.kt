package com.nordicbeacon.scanner.infrastructure.oem.coordination

import com.nordicbeacon.scanner.infrastructure.oem.detection.OemType
import com.nordicbeacon.scanner.infrastructure.oem.handlers.*
import com.nordicbeacon.scanner.infrastructure.oem.strategies.OemBatteryOptimizationStrategy
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 📋 OEM Strategy Registry
 * 
 * Central registry cho all OEM battery optimization strategies
 * Implements Registry pattern cho strategy management và lookup
 * 
 * Key Features:
 * - Automatic strategy registration
 * - Strategy lookup by OEM type
 * - Fallback strategy cho unsupported OEMs
 * - Strategy validation và health checking
 * 
 * @author Senior Android Developer
 */
@Singleton
class OemStrategyRegistry @Inject constructor(
    // Inject all OEM strategy implementations
    private val samsungHandler: SamsungOptimizationHandler,
    private val xiaomiHandler: XiaomiOptimizationHandler,
    private val huaweiHandler: HuaweiOptimizationHandler,
    private val onePlusHandler: OnePlusOptimizationHandler,
    private val oppoVivoHandler: OppoVivoOptimizationHandler,
    private val googlePixelHandler: GooglePixelOptimizationHandler,
    private val nothingSonyHandler: NothingSonyOptimizationHandler,
    private val genericHandler: GenericOptimizationHandler
) {

    private val strategies = mutableMapOf<OemType, OemBatteryOptimizationStrategy>()

    init {
        initializeStrategies()
    }

    // ========== REGISTRY OPERATIONS ==========

    /**
     * 🔍 Get optimization strategy cho specific OEM type
     * 
     * @param oemType Target OEM type
     * @return Strategy implementation hoặc null if not supported
     */
    fun getStrategy(oemType: OemType): OemBatteryOptimizationStrategy? {
        return strategies[oemType] ?: run {
            Timber.w("⚠️ No strategy available cho OEM: $oemType, using generic fallback")
            genericHandler
        }
    }

    /**
     * 📋 Get all available strategies
     */
    fun getAllStrategies(): Map<OemType, OemBatteryOptimizationStrategy> {
        return strategies.toMap()
    }

    /**
     * 📊 Get supported OEM types
     */
    fun getSupportedOemTypes(): Set<OemType> {
        return strategies.keys
    }

    /**
     * 🔍 Check if OEM type is supported
     */
    fun isOemSupported(oemType: OemType): Boolean {
        return strategies.containsKey(oemType)
    }

    /**
     * 📊 Get registry statistics cho debugging
     */
    fun getRegistryStats(): StrategyRegistryStats {
        return StrategyRegistryStats(
            totalStrategies = strategies.size,
            supportedOems = strategies.keys.map { it.displayName },
            majorOemCoverage = calculateMajorOemCoverage(),
            totalMarketCoverage = calculateTotalMarketCoverage()
        )
    }

    // ========== PRIVATE IMPLEMENTATION ==========

    /**
     * 🏭 Initialize all strategy mappings
     */
    private fun initializeStrategies() {
        
        Timber.i("🏭 Initializing OEM strategy registry...")
        
        try {
            // Register major OEM strategies
            registerStrategy(OemType.SAMSUNG, samsungHandler)
            registerStrategy(OemType.SAMSUNG_GENERIC, samsungHandler)
            
            registerStrategy(OemType.XIAOMI, xiaomiHandler)
            registerStrategy(OemType.XIAOMI_GENERIC, xiaomiHandler)
            
            registerStrategy(OemType.HUAWEI, huaweiHandler)
            registerStrategy(OemType.HUAWEI_GENERIC, huaweiHandler)
            
            registerStrategy(OemType.ONEPLUS, onePlusHandler)
            
            registerStrategy(OemType.OPPO, oppoVivoHandler)
            registerStrategy(OemType.VIVO, oppoVivoHandler)
            
            registerStrategy(OemType.GOOGLE, googlePixelHandler)
            
            registerStrategy(OemType.NOTHING, nothingSonyHandler)
            registerStrategy(OemType.SONY, nothingSonyHandler)
            
            registerStrategy(OemType.LG, genericHandler)
            registerStrategy(OemType.MOTOROLA, genericHandler)
            registerStrategy(OemType.GENERIC, genericHandler)
            
            Timber.i("✅ Registry initialized với ${strategies.size} strategies")
            Timber.i("📊 Supported OEMs: ${getSupportedOemTypes().joinToString { it.displayName }}")
            
        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to initialize strategy registry")
            throw e
        }
    }

    /**
     * 📝 Register single strategy với validation
     */
    private fun registerStrategy(oemType: OemType, strategy: OemBatteryOptimizationStrategy) {
        
        try {
            // Validate strategy before registration
            if (strategy.oemName.isBlank()) {
                Timber.w("⚠️ Strategy has empty OEM name, skipping registration")
                return
            }
            
            strategies[oemType] = strategy
            
            Timber.d("📝 Registered strategy: ${oemType.displayName} → ${strategy.oemName}")
            
        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to register strategy cho $oemType")
        }
    }

    /**
     * 📊 Calculate market coverage của major OEMs
     */
    private fun calculateMajorOemCoverage(): Double {
        val majorOemTypes = strategies.keys.filter { it.isMajorOem() }
        val majorMarketShare = majorOemTypes.sumOf { it.marketShare }
        
        return majorMarketShare
    }

    /**
     * 📊 Calculate total market coverage
     */
    private fun calculateTotalMarketCoverage(): Double {
        return strategies.keys.sumOf { it.marketShare }
    }
}

/**
 * 🔧 Generic Optimization Handler cho unsupported OEMs
 */
class GenericOptimizationHandler @Inject constructor() : com.nordicbeacon.scanner.infrastructure.oem.strategies.BaseOemStrategy() {

    override val oemName: String = "Generic"
    
    override val supportedCriteria = com.nordicbeacon.scanner.infrastructure.oem.models.DeviceMatchCriteria(
        manufacturers = emptyList() // Supports any manufacturer
    )

    override fun isSupported(deviceInfo: com.nordicbeacon.scanner.infrastructure.oem.models.DeviceInfo): Boolean = true // Universal fallback

    override suspend fun checkOptimizationStatus(context: Context): OptimizationStatus {
        return try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                val powerManager = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
                val isIgnored = powerManager.isIgnoringBatteryOptimizations(context.packageName)
                
                if (isIgnored) OptimizationStatus.OPTIMIZED else OptimizationStatus.NOT_OPTIMIZED
            } else {
                OptimizationStatus.OPTIMIZED
            }
        } catch (e: Exception) {
            OptimizationStatus.CANNOT_DETERMINE
        }
    }

    override fun getOptimizationStrategy(): com.nordicbeacon.scanner.infrastructure.oem.models.OptimizationStrategy {
        return com.nordicbeacon.scanner.infrastructure.oem.models.OptimizationStrategy(
            oemName = "Generic Android",
            strategyName = "Standard Android Battery Optimization",
            description = "Configure standard Android battery optimization settings",
            requiredSettings = getGenericSettingsPaths(),
            userInstructions = getGenericUserInstructions(),
            successIndicators = getGenericSuccessIndicators(),
            estimatedComplexity = com.nordicbeacon.scanner.infrastructure.oem.models.ComplexityLevel.SIMPLE
        )
    }

    override suspend fun requestOptimization(context: Context): BatteryOptimizationResult {
        return try {
            val intent = android.content.Intent().apply {
                action = android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            if (isIntentSafe(context, intent)) {
                context.startActivity(intent)
                
                BatteryOptimizationResult.SettingsOpened(
                    settingsType = "Battery Optimization",
                    instructionsProvided = getGenericUserInstructions()
                )
            } else {
                BatteryOptimizationResult.Failed(
                    reason = "Cannot open battery settings",
                    fallbackInstructions = getGenericFallbackInstructions()
                )
            }
            
        } catch (e: Exception) {
            BatteryOptimizationResult.Failed(
                reason = "Generic optimization failed",
                fallbackInstructions = getGenericFallbackInstructions(),
                cause = e
            )
        }
    }

    override fun getUserEducationContent(): com.nordicbeacon.scanner.infrastructure.oem.strategies.OemEducationContent {
        return com.nordicbeacon.scanner.infrastructure.oem.strategies.OemEducationContent(
            title = "Android Battery Optimization",
            explanation = "Standard Android battery optimization configuration",
            whyNeeded = "Disable battery optimization to ensure continuous beacon scanning",
            steps = getGenericUserInstructions()
        )
    }

    override fun getDetectionConfidence(deviceInfo: com.nordicbeacon.scanner.infrastructure.oem.models.DeviceInfo): Int = 50 // Lower confidence cho generic

    // ========== HELPER METHODS ==========

    private fun getGenericSettingsPaths(): List<com.nordicbeacon.scanner.infrastructure.oem.models.SettingPath> = emptyList()
    private fun getGenericUserInstructions(): List<String> = emptyList()
    private fun getGenericSuccessIndicators(): List<String> = emptyList()
}

/**
 * 📊 Strategy Registry Statistics
 */
data class StrategyRegistryStats(
    val totalStrategies: Int,
    val supportedOems: List<String>,
    val majorOemCoverage: Double,
    val totalMarketCoverage: Double
) {
    
    fun getFormattedStats(): String {
        return """
            📊 Strategy Registry Stats:
            🔧 Total Strategies: $totalStrategies
            🏭 Supported OEMs: ${supportedOems.joinToString(", ")}
            📈 Major OEM Coverage: ${"%.1f".format(majorOemCoverage)}%
            🌍 Total Market Coverage: ${"%.1f".format(totalMarketCoverage)}%
        """.trimIndent()
    }
}
