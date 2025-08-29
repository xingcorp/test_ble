# 📋 OPERATIONAL RUNBOOK
## Nordic Beacon Scanner - Production Operations

*Senior Android Developer - Day-to-Day Operations Manual*

---

## 🎯 **DAILY OPERATIONS**

### **Morning Health Check (5 minutes)**
```
📊 Daily Health Validation:
├── Check Firebase Crashlytics dashboard
├── Review performance metrics trends  
├── Validate service uptime statistics
├── Check critical alert notifications
├── Review user feedback ratings
└── Verify Nordic beacon detection success rates

🎯 Health Targets:
├── Crash-free session rate: >99.5%
├── Service uptime: >99%
├── Battery usage: <2% daily average
├── Nordic detection success: >95%
└── User rating: >4.5 stars average
```

### **Weekly Performance Review (30 minutes)**  
```
📈 Weekly Performance Analysis:
├── Run PerformanceBenchmarkSuite validation
├── Analyze memory usage trends
├── Review signal processing effectiveness  
├── Check OEM optimization success rates
├── Validate analytics accuracy
├── Review circuit breaker health
└── Plan optimization improvements

📊 Performance Trends:
├── Compare with previous week metrics
├── Identify performance regressions  
├── Plan optimization initiatives
├── Update performance baselines
└── Document performance improvements
```

---

## 🚨 **INCIDENT RESPONSE PROCEDURES**

### **🔴 Critical Incident: Service Not Detecting Nordic Beacons**

**Immediate Response (< 10 minutes):**
```bash
# 1. Check service status
adb shell dumpsys activity services | grep BeaconScanningService

# 2. Validate Bluetooth status  
adb shell dumpsys bluetooth_manager | grep -E "enabled|state"

# 3. Check permissions
adb shell dumpsys package com.nordicbeacon.scanner | grep -A 20 "declared permissions"

# 4. Review logs cho errors
adb logcat -s NordicBeacon:* | tail -50

# 5. Check OEM battery optimization
# Device-specific commands based on OEM detection
```

**Detailed Investigation (< 30 minutes):**
```kotlin
Investigation Checklist:
├── SecurityAuditManager.performSecurityAudit() results
├── ServicePerformanceMonitor.getServiceHealthStatus() analysis
├── CircuitBreakerManager.getCircuitBreakerStatus() review  
├── BatteryOptimizationCoordinator.getCurrentOptimizationStatus() check
├── Firebase Crashlytics error correlation
├── Device-specific OEM optimization validation
└── AltBeacon library status verification

Root Cause Categories:
├── Permission Issues: Missing runtime permissions
├── OEM Restrictions: Battery optimization interference
├── Service Issues: Service killed by system
├── Hardware Issues: Bluetooth stack failures
├── Environmental Issues: Signal interference
└── Software Issues: Logic bugs hoặc memory leaks
```

**Resolution Actions:**
```kotlin
Resolution Strategies:
├── Permission Issues: Guide user through permission flow
├── OEM Issues: Execute battery optimization flow
├── Service Issues: Restart service với circuit breaker reset
├── Hardware Issues: Reinitialize BeaconManager
├── Environmental Issues: Adjust signal processing parameters
└── Software Issues: Apply emergency patch với expedited testing
```

### **🟡 Performance Degradation Response**

**Analysis Procedure:**
```kotlin
Performance Investigation:
├── Check PerformanceOptimizer.optimizePerformance() recommendations
├── Review ServicePerformanceMonitor real-time metrics
├── Analyze AdvancedLogger structured logs cho bottlenecks
├── Validate Kalman filter effectiveness scores
├── Check memory allocation patterns
└── Review battery consumption analytics

Performance Tuning:
├── Adjust scan frequencies based on battery state
├── Optimize analytics processing intervals  
├── Clear cache nếu memory pressure detected
├── Apply circuit breaker optimizations
└── Update scan parameters for current environment
```

---

## 📊 **MONITORING DASHBOARD OPERATIONS**

### **Firebase Monitoring Setup:**
```kotlin
Daily Firebase Review:
├── Crashlytics: Review crash trends với stack trace analysis
├── Analytics: Nordic beacon detection patterns analysis
├── Performance: Service performance metrics validation
├── Remote Config: Dynamic configuration updates if needed
└── User Engagement: App usage patterns và retention analysis

Firebase Alert Configuration:
├── Critical crashes: >5 crashes/hour → Immediate alert
├── Performance issues: >10ms average latency → Warning alert  
├── Battery usage spike: >5% hourly usage → Performance alert
├── User retention drop: >15% weekly → Product alert
└── Nordic detection failure: >10% failure rate → Critical alert
```

### **Analytics Dashboard Operations:**
```kotlin
Real-time Monitoring:
├── Open AnalyticsDashboardActivity cho live metrics
├── Review signal processing performance indicators
├── Check service health composite scoring
├── Validate Nordic beacon detection analytics
├── Monitor memory/CPU/battery utilization trends
└── Review circuit breaker status cho system resilience

Weekly Analytics Review:
├── Generate BeaconInsightsGenerator comprehensive report
├── Analyze detection frequency patterns  
├── Review signal quality trend analysis
├── Check environmental interference levels
├── Validate predictive insights accuracy
└── Update performance optimization parameters
```

---

## 🔧 **MAINTENANCE OPERATIONS**

### **Database Maintenance (Weekly):**
```sql
-- Run database maintenance operations
LocalBeaconDataSource.performMaintenance()

-- Clean old beacon data (GDPR compliance)
LocalBeaconDataSource.clearBeaconHistory(retentionDays = 30)

-- Optimize database performance
BeaconDao.removeDuplicateDetections(timeWindowMs = 2000L)

-- Validate database integrity
BeaconDatabase.checkIntegrity()
```

### **Analytics Cleanup (Monthly):**
```kotlin
Analytics Maintenance:
├── Clear old signal history (keep last 7 days)
├── Optimize analytics cache memory usage
├── Archive historical performance reports  
├── Clean up debug logs (keep last 30 days)
├── Update signal processing calibration parameters
├── Review và update Nordic beacon detection thresholds
└── Optimize database indexes cho query performance
```

### **Security Audit (Monthly):**
```kotlin
Security Maintenance:
├── Execute SecurityAuditManager.performSecurityAudit()
├── Review security audit results cho new vulnerabilities
├── Update dependency versions với security patches
├── Validate encryption key rotation needs
├── Check privacy compliance với latest regulations
├── Review OEM security compatibility
└── Update threat model based on new attack vectors
```

---

## 📈 **PERFORMANCE OPTIMIZATION**

### **Signal Processing Tuning:**
```kotlin
Signal Processing Optimization:
├── RssiKalmanFilter parameter tuning based on environment
├── MovingAverageFilter window size optimization
├── AdvancedDistanceCalculator calibration updates
├── SignalQualityAssessment threshold adjustments  
├── Environmental compensation factor updates
└── Nordic-specific parameter optimization

Tuning Parameters:
├── Process Noise (Q): 0.05-0.2 range optimization
├── Measurement Noise (R): 2.0-6.0 range tuning
├── Distance calculation model selection
├── Outlier detection sensitivity adjustment
└── Quality scoring algorithm refinement
```

### **Battery Optimization:**
```kotlin
Battery Performance Tuning:
├── Adaptive scan frequency based on battery level
├── Power save mode integration optimization
├── Doze mode compatibility enhancement  
├── Foreground service notification optimization
├── WorkManager backup frequency adjustment
└── OEM-specific power management integration

Battery Monitoring:
├── Track battery consumption patterns
├── Analyze charging/non-charging behavior differences
├── Monitor temperature impact on battery usage
├── Optimize scan parameters cho different power states
└── Validate battery optimization effectiveness
```

---

## 🔍 **TROUBLESHOOTING GUIDE**

### **Common Issues & Solutions:**

#### **Issue: Nordic Beacon Not Detected**
```
Diagnosis Steps:
1. Check BeaconScanningService.isServiceRunning status
2. Validate Nordic UUID filtering: FDA50693-0000-0000-0000-290995101092
3. Check signal strength requirements (RSSI > -95dBm)
4. Verify distance limitations (<50m range)
5. Check environmental interference levels

Solutions:
├── Restart BeaconScanningService
├── Reset Kalman filter state
├── Adjust signal quality thresholds
├── Check Nordic beacon transmission power
└── Validate beacon hardware functionality
```

#### **Issue: High Battery Usage**  
```
Diagnosis Steps:
1. Check PerformanceOptimizer.optimizeBatteryUsage() recommendations
2. Review scan frequency configuration
3. Analyze analytics processing overhead
4. Check for memory leaks
5. Validate OEM battery optimization status

Solutions:
├── Reduce scan frequency temporarily  
├── Disable analytics processing if needed
├── Clear memory cache và trigger GC
├── Re-run OEM battery optimization flow
└── Apply conservative power management settings
```

#### **Issue: Service Killed by System**
```
Diagnosis Steps:
1. Check OEM battery optimization status
2. Review CircuitBreakerManager status
3. Validate WorkManager backup execution
4. Check SystemEventReceiver functionality
5. Review recent system updates

Solutions:
├── Re-execute OEM battery optimization
├── Reset circuit breakers
├── Restart WorkManager backup jobs
├── Re-register system event receivers  
└── Update service restart policy if needed
```

---

## 📞 **SUPPORT ESCALATION MATRIX**

### **L1 Support (User Issues):**
```
Common User Issues:
├── Permission setup guidance
├── OEM battery optimization help
├── Basic troubleshooting steps
├── App configuration assistance
└── User education content delivery

Resolution Time: <2 hours
Tools: User guides, FAQ, email support
```

### **L2 Support (Technical Issues):**
```
Technical Issue Categories:
├── Device-specific compatibility problems
├── Performance degradation issues
├── Analytics reporting problems  
├── Bluetooth connectivity issues
└── Advanced configuration requirements

Resolution Time: <24 hours
Tools: Remote debugging, device logs, performance analysis
```

### **L3 Support (Engineering Issues):**
```
Engineering Escalation:
├── Core functionality failures
├── Security incidents
├── Performance regression analysis
├── Architecture-level issues
└── Critical bug fixes

Resolution Time: <48 hours
Tools: Source code access, production debugging, hotfix deployment
```

---

**🏥 OPERATIONS READY!** Comprehensive operational procedures established cho enterprise Nordic Beacon Scanner deployment!
