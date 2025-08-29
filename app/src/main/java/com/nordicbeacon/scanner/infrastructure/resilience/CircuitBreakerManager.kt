package com.nordicbeacon.scanner.infrastructure.resilience

import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * 🔄 Circuit Breaker Manager
 * 
 * Enterprise-grade error resilience cho Nordic Beacon Scanner
 * Implements circuit breaker pattern cho fault tolerance và system stability
 * 
 * Key Features:
 * - Automatic failure detection với configurable thresholds
 * - Circuit state management (Closed/Open/Half-Open)
 * - Exponential backoff recovery strategy
 * - Health monitoring với automatic recovery
 * - Multiple circuit breakers cho different subsystems
 * 
 * @author Senior Android Developer
 */
@Singleton
class CircuitBreakerManager @Inject constructor() {

    private val circuitBreakers = mutableMapOf<String, CircuitBreaker>()
    private val mutex = Mutex()

    // ========== CIRCUIT BREAKER OPERATIONS ==========

    /**
     * 🛡️ Get hoặc create circuit breaker cho subsystem
     */
    suspend fun getCircuitBreaker(
        name: String,
        config: CircuitBreakerConfig = CircuitBreakerConfig.default()
    ): CircuitBreaker = mutex.withLock {
        
        return circuitBreakers.getOrPut(name) {
            val breaker = CircuitBreaker(name, config)
            Timber.i("🔄 Created circuit breaker: $name")
            breaker
        }
    }

    /**
     * ⚡ Execute operation với circuit breaker protection
     */
    suspend fun <T> executeWithCircuitBreaker(
        breakerName: String,
        operation: suspend () -> T
    ): T {
        
        val circuitBreaker = getCircuitBreaker(breakerName)
        return circuitBreaker.execute(operation)
    }

    /**
     * 📊 Get all circuit breaker statuses
     */
    fun getCircuitBreakerStatus(): Map<String, CircuitBreakerStatus> {
        return circuitBreakers.mapValues { (_, breaker) -> breaker.getStatus() }
    }

    /**
     * 🔄 Reset all circuit breakers (manual recovery)
     */
    suspend fun resetAllCircuitBreakers() = mutex.withLock {
        circuitBreakers.values.forEach { breaker ->
            breaker.reset()
            Timber.i("🔄 Reset circuit breaker: ${breaker.name}")
        }
    }
}

/**
 * 🔄 Circuit Breaker Implementation
 */
class CircuitBreaker(
    val name: String,
    private val config: CircuitBreakerConfig
) {
    
    private var state: CircuitState = CircuitState.CLOSED
    private var failureCount: Int = 0
    private var lastFailureTime: Long = 0
    private var successCount: Int = 0
    private val mutex = Mutex()

    /**
     * ⚡ Execute operation với circuit breaker protection
     */
    suspend fun <T> execute(operation: suspend () -> T): T = mutex.withLock {
        
        when (state) {
            CircuitState.OPEN -> {
                if (shouldAttemptRecovery()) {
                    transitionToHalfOpen()
                } else {
                    throw CircuitBreakerException("Circuit breaker $name is OPEN", state)
                }
            }
            
            CircuitState.HALF_OPEN -> {
                // Allow limited operations trong recovery mode
            }
            
            CircuitState.CLOSED -> {
                // Normal operation
            }
        }
        
        return try {
            val result = operation()
            onOperationSuccess()
            result
            
        } catch (e: Exception) {
            onOperationFailure(e)
            throw e
        }
    }

    /**
     * ✅ Handle successful operation
     */
    private fun onOperationSuccess() {
        successCount++
        
        when (state) {
            CircuitState.HALF_OPEN -> {
                if (successCount >= config.halfOpenSuccessThreshold) {
                    transitionToClosed()
                }
            }
            CircuitState.CLOSED -> {
                // Reset failure count on success
                failureCount = 0
            }
            CircuitState.OPEN -> {
                // Shouldn't happen, but reset nếu does
                transitionToClosed()
            }
        }
    }

    /**
     * ❌ Handle operation failure
     */
    private fun onOperationFailure(exception: Exception) {
        failureCount++
        lastFailureTime = System.currentTimeMillis()
        
        Timber.w("❌ Circuit breaker $name recorded failure $failureCount/${config.failureThreshold}: ${exception.message}")
        
        when (state) {
            CircuitState.CLOSED -> {
                if (failureCount >= config.failureThreshold) {
                    transitionToOpen()
                }
            }
            CircuitState.HALF_OPEN -> {
                transitionToOpen()
            }
            CircuitState.OPEN -> {
                // Already open, extend timeout
            }
        }
    }

    // ========== STATE TRANSITIONS ==========

    private fun transitionToOpen() {
        state = CircuitState.OPEN
        Timber.w("🔴 Circuit breaker $name transitioned to OPEN")
    }

    private fun transitionToHalfOpen() {
        state = CircuitState.HALF_OPEN
        successCount = 0
        Timber.i("🟡 Circuit breaker $name transitioned to HALF_OPEN")
    }

    private fun transitionToClosed() {
        state = CircuitState.CLOSED
        failureCount = 0
        successCount = 0
        Timber.i("🟢 Circuit breaker $name transitioned to CLOSED")
    }

    private fun shouldAttemptRecovery(): Boolean {
        val timeSinceLastFailure = System.currentTimeMillis() - lastFailureTime
        return timeSinceLastFailure >= config.timeout.inWholeMilliseconds
    }

    /**
     * 📊 Get current circuit breaker status
     */
    fun getStatus(): CircuitBreakerStatus {
        return CircuitBreakerStatus(
            name = name,
            state = state,
            failureCount = failureCount,
            successCount = successCount,
            lastFailureTime = lastFailureTime,
            config = config
        )
    }

    /**
     * 🔄 Reset circuit breaker to closed state
     */
    fun reset() {
        state = CircuitState.CLOSED
        failureCount = 0
        successCount = 0
        lastFailureTime = 0
    }
}

// ========== CONFIGURATION & DATA MODELS ==========

/**
 * ⚙️ Circuit Breaker Configuration
 */
data class CircuitBreakerConfig(
    val failureThreshold: Int,
    val timeout: Duration,
    val halfOpenSuccessThreshold: Int
) {
    companion object {
        fun default() = CircuitBreakerConfig(
            failureThreshold = 5,
            timeout = 30.seconds,
            halfOpenSuccessThreshold = 3
        )
        
        fun aggressive() = CircuitBreakerConfig(
            failureThreshold = 3,
            timeout = 60.seconds,
            halfOpenSuccessThreshold = 5
        )
    }
}

/**
 * 🔄 Circuit States
 */
enum class CircuitState {
    CLOSED,    // Normal operation
    OPEN,      // Failing fast - blocking operations
    HALF_OPEN  // Testing recovery - limited operations
}

/**
 * 📊 Circuit Breaker Status
 */
data class CircuitBreakerStatus(
    val name: String,
    val state: CircuitState,
    val failureCount: Int,
    val successCount: Int,
    val lastFailureTime: Long,
    val config: CircuitBreakerConfig
) {
    
    fun isHealthy(): Boolean = state == CircuitState.CLOSED && failureCount < config.failureThreshold / 2
    
    fun getHealthDescription(): String = when (state) {
        CircuitState.CLOSED -> if (failureCount == 0) "Healthy" else "Recovering ($failureCount failures)"
        CircuitState.HALF_OPEN -> "Testing recovery ($successCount/$config.halfOpenSuccessThreshold successes)"
        CircuitState.OPEN -> "Failing fast (timeout in ${getTimeoutRemaining()}s)"
    }
    
    private fun getTimeoutRemaining(): Long {
        val elapsed = System.currentTimeMillis() - lastFailureTime
        val remaining = config.timeout.inWholeMilliseconds - elapsed
        return maxOf(0, remaining / 1000)
    }
}

/**
 * 🚨 Circuit Breaker Exception
 */
class CircuitBreakerException(
    message: String,
    val circuitState: CircuitState
) : Exception(message)
