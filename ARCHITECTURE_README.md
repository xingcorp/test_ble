## Executive Overview

- Mục tiêu: Ứng dụng Android quét Nordic Beacon ổn định, chạy nền, tối ưu hiệu năng và sẵn sàng production theo 4 phase (Foundation → Background → Advanced Analytics → Production Hardening).
- Kiến trúc: Clean Architecture + MVVM, Domain/Data/Presentation/Infrastructure, DI bằng Hilt, async bằng Coroutines/Flow, KSP thay KAPT.
- Chức năng cốt lõi: Foreground Service quét AltBeacon (iBeacon layout), lọc UUID Nordic, xin quyền tuần tự (version-aware), backup bằng WorkManager, monitoring + analytics real-time, dashboard.
- Tương thích OEM: Flow tối ưu pin theo hãng (Samsung/Xiaomi/Huawei/…), mở đúng màn hình Settings và hướng dẫn người dùng.
- Bảo mật & Prod: OWASP, mã hóa (AES-256 nơi cần), circuit breaker, ProGuard/R8, CI/CD, tài liệu vận hành/triển khai.
- Trạng thái hiện tại: Hoàn thiện kiến trúc cốt lõi, luồng xin quyền tuần tự, service scanning, analytics cơ bản, tài liệu và cấu hình build; Firebase ở dạng template (chưa bật).
- Cách chạy nhanh: Android Studio → Sync Gradle → Run; chấp thuận quyền theo luồng tuần tự (foreground trước, background sau).

## Tổng quan dự án: Nordic Beacon Scanner (Android)

### 1) Mục tiêu ban đầu
- **Xây dựng ứng dụng Android quét Nordic Beacon** với kiến trúc sạch, ổn định, có khả năng chạy nền, tối ưu hiệu năng và sẵn sàng production.
- Thực hiện theo 4 phase:
  - **Phase 1 – Foundation & Core Scanning**: Clean Architecture, lọc UUID Nordic `FDA50693-0000-0000-0000-290995101092`, Foreground Service, tương thích nhiều phiên bản Android, DI bằng Hilt.
  - **Phase 2 – Background Execution Mastery**: Bỏ tối ưu pin OEM theo hãng, WorkManager backup, System receivers (boot, quickboot), user education.
  - **Phase 3 – Advanced Features & Analytics**: Kalman filter, khoảng cách nâng cao, real-time analytics engine, dashboard, logging/debug.
  - **Phase 4 – Production Hardening**: Bảo mật (OWASP), mã hóa dữ liệu (AES-256), Firebase (Crashlytics/Analytics/Performance – đang để template), benchmark, resiliency (circuit breaker), ProGuard/R8, CI/CD, tài liệu triển khai.

### 2) Kiến trúc & Nguyên tắc
- **Clean Architecture + MVVM**: Tách Domain / Data / Presentation / Infrastructure.
  - Domain: entity, use-case thuần Kotlin.
  - Data: data source (BLE), repository (ẩn implementation), Room.
  - Presentation: `Activity`/`ViewModel` (MVVM), xử lý UI và luồng permissions.
  - Infrastructure: service, receivers, OEM strategies, permissions, resilience, security.
- **SOLID**: Áp dụng nguyên tắc single responsibility, interface segregation, dependency inversion.
- **DI – Hilt**: Cấu hình modules để cung cấp phụ thuộc (AltBeacon, filters, repo, PermissionManager…).
- **Kotlin Coroutines/Flow**: Xử lý bất đồng bộ, stream dữ liệu beacon/analytics.
- **KSP** (thay KAPT): Compiler mới cho Hilt/Room, tránh lỗi truy cập JDK nội bộ.

### 3) Công nghệ chính
- BLE scanning: thư viện AltBeacon.
- Foreground Service + Notification: đảm bảo chạy nền liên tục.
- WorkManager: backup scanning khi service bị kill.
- Room: lưu trữ local lịch sử beacon (nếu cần).
- Hilt (DI), Coroutines/Flow, Material 3 UI.
- Logging nâng cao qua `AdvancedLogger` (structured logs).

### 4) Phân lớp mã nguồn (một số tệp tiêu biểu)
- Presentation
  - `presentation/MainActivity.kt`: UI chính, điều phối quyền, start/stop service, quan sát ViewModel, kết nối Analytics.
  - `analytics/dashboard/AnalyticsDashboardActivity.kt`, `analytics/dashboard/viewmodels/AnalyticsDashboardViewModel.kt`.
- Domain
  - `domain/entities/NordicBeacon.kt`: entity core (sử dụng `@Parcelize` và `@RawValue`).
- Data
  - `data/datasources/BleDataSource.kt`: quét beacon, quản lý `BeaconConsumer` an toàn.
  - `data/repositories/BeaconRepositoryImpl.kt`.
  - `data/database/BeaconDatabase.kt`.
- Infrastructure
  - Services: `infrastructure/services/BeaconScanningService.kt` (FGS).
  - Permissions: `infrastructure/permissions/PermissionManager.kt` (hệ xin quyền tuần tự, version-aware).
  - OEM: `infrastructure/oem/**` (handlers theo hãng, strategy registry, education content).
  - Receivers: `infrastructure/receivers/SystemEventReceiver.kt`.
  - Resilience: `infrastructure/resilience/CircuitBreakerManager.kt`.
  - Security: `security/SecurityAuditManager.kt`, `security/DataProtectionManager.kt`.
- Analytics/Monitoring
  - Filters: `analytics/signal/filters/RssiKalmanFilter.kt`, `MovingAverageFilter.kt` (DI-ready, @Inject constructor, @Singleton).
  - Engine & Insights: `analytics/AnalyticsIntegrationManager.kt`, `analytics/insights/BeaconInsightsGenerator.kt`.
  - Monitoring: `analytics/monitoring/**` (ServicePerformanceMonitor, PerformanceOptimizer, BenchmarkSuite, ProductionHealthMonitor).
  - Debug/Logs: `debug/logging/AdvancedLogger.kt`.
- DI Modules
  - `di/BeaconModule.kt`, `di/AppModule.kt` (cung cấp AltBeacon, PermissionManager…).

### 5) Luồng chức năng chính
- Mở app → `MainActivity` hiển thị UI cơ bản.
- Bấm “Start Scanning” → Kiểm tra quyền bằng `PermissionManager` (chi tiết ở mục 6).
- Đủ quyền tối thiểu → khởi động `BeaconScanningService` (FGS) để quét beacon.
- Dữ liệu quét đẩy về ViewModel/Analytics → lọc/nắn tín hiệu (Kalman/MA), tính khoảng cách nâng cao.
- Analytics Engine phát ra insights, Performance Monitor thu thập chỉ số, Dashboard hiển thị.
- WorkManager đảm bảo tính liên tục khi hệ thống thu hồi service.

### 6) Chiến lược xin quyền (Sequential, Version-aware, OEM-friendly)
- Mục tiêu: tăng tỉ lệ chấp thuận, UX thân thiện, tuân thủ mô hình quyền từng phiên bản Android.
- Bước tuần tự (khuyến nghị):
  1. **Location (ACCESS_FINE_LOCATION)** – nền tảng cho BLE.
  2. **Bluetooth** – Android 12+: `BLUETOOTH_SCAN`, `BLUETOOTH_CONNECT`; Legacy: `BLUETOOTH`, `BLUETOOTH_ADMIN`.
  3. **Background Location** – cho trải nghiệm quét nền nâng cao.
- Hành vi:
  - Hiển thị educational dialogs giải thích lợi ích từng bước.
  - Khi bị block, hướng dẫn người dùng tới Settings.
  - Cho phép “progressive enhancement”: có thể quét foreground trước, nâng cấp lên background sau.
- Tệp: `infrastructure/permissions/PermissionManager.kt`; tích hợp trong `presentation/MainActivity.kt`.

### 7) BLE Scanning & Foreground Service
- Sử dụng AltBeacon với iBeacon layout: `"m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"`.
- `BeaconScanningService` chạy foreground, đảm bảo độ ổn định khi ở nền (Android 8+).
- Notification channel để tuân thủ FGS policy.

### 8) Xử lý OEM Battery Optimization
- Handlers theo hãng: Samsung, Xiaomi, Huawei, OnePlus, Oppo/Vivo/Realme, Pixel, Nothing/Sony, Generic.
- Registry/Coordinator: chọn flow theo `Build.MANUFACTURER`.
- Mở đúng màn hình Settings theo hãng; hiển thị education content.
- Tệp tiêu biểu: `infrastructure/oem/handlers/*.kt`, `infrastructure/oem/coordination/BatteryOptimizationCoordinator.kt`.

### 9) Analytics, Filters và Khoảng cách nâng cao
- **Kalman Filter**: làm mượt RSSI, chống nhiễu ngắn hạn.
- **Moving Average (có decay/outlier)**: ổn định chuỗi đo theo thời gian.
- **Path-loss model** + tối ưu tham số: tính khoảng cách đáng tin cậy hơn.
- **Engine**: gom dữ liệu, phát metrics/insights real-time.
- **Monitoring**: benchmark, performance metrics, health monitoring.

### 10) Bảo mật & Production Hardening
- Tuân thủ OWASP Mobile: kiểm tra input, hạn chế debug build, bảo vệ logs.
- **Mã hóa dữ liệu**: AES-256 khi lưu trữ nhạy cảm (nếu áp dụng).
- **ProGuard/R8**: `app/proguard-rules.pro` tối ưu & che giấu symbol, giữ Hilt/Firebase/Timber.
- **Resilience**: Circuit Breaker patterns giảm thiểu lỗi dây chuyền.
- **Firebase**: file `app/google-services.json.template` (tắt dependency trong Gradle cho môi trường chưa bật Firebase).

### 11) Build System, KSP & Gradle
- Đã chuyển từ **KAPT → KSP** để tránh `IllegalAccessError` với JDK 17+.
- `build.gradle.kts` (root) và `app/build.gradle.kts` (module) cấu hình KSP, Hilt, Room.
- `minSdk = 26` (để dùng adaptive icon, modern APIs).
- Cập nhật `.gitignore` loại trừ toàn bộ artifacts: `**/.gradle/`, `**/build/`, `**/*.apk`, `**/*.aab`, `**/intermediates/`, `**/generated/`, v.v.

### 12) CI/CD & Tài liệu vận hành
- CI/CD pipeline mẫu: `gradle/ci-cd-pipeline.yml`.
- Tài liệu: `PRODUCTION_DEPLOYMENT_GUIDE.md`, `OPERATIONAL_RUNBOOK.md` mô tả phát hành/vận hành.

### 13) Testing & Benchmark
- Unit test mẫu: `app/src/test/.../AnalyticsEngineTest.kt`.
- Benchmark: `analytics/monitoring/benchmarks/PerformanceBenchmarkSuite.kt` – chuẩn hóa đo thời gian filters/khoảng cách.

### 14) Cách chạy & kiểm thử nhanh
- Dùng Android Studio (khuyến nghị): sync Gradle, chọn thiết bị, Run.
- Lưu ý Gradle Wrapper: cần đủ `gradle-wrapper.jar` (Android Studio sẽ tự tải nếu thiếu).
- Lần chạy đầu tiên: app sẽ xin quyền tuần tự. Hãy chấp thuận để kích hoạt đầy đủ tính năng (foreground trước, background sau).

### 15) Tình trạng tích hợp Firebase
- Dependencies Firebase đang comment trong Gradle để tránh build-time error khi thiếu `google-services.json`.
- Khi sẵn sàng: thay `app/google-services.json.template` bằng file thật và bật dependencies.

### 16) Những thay đổi đáng chú ý (fix compile/build)
- Khôi phục file Gradle và wrapper, tăng `minSdk = 26`.
- KSP thay KAPT (thay `kapt` → `ksp`, cấu hình `ksp { arg("correctErrorTypes", "true") }`).
- Sửa `NordicBeacon.kt` dùng `@RawValue` đúng vị trí type.
- Thêm `@Inject constructor()` + `@Singleton` cho `RssiKalmanFilter`, `MovingAverageFilter`.
- Thay `BeaconParser.IBEACON_LAYOUT` bằng literal layout string.
- Sửa `BleDataSource` quản lý `BeaconConsumer` null-safe.
- Điều chỉnh permission flow theo tuần tự, version-aware trong `PermissionManager` + `MainActivity`.
- Cập nhật `.gitignore` và untrack build files.

### 17) Hạn chế hiện tại & hướng phát triển
- UI còn cơ bản (Material 3) – sẽ nâng cấp giao diện/dashboards khi xác nhận tính năng cốt lõi.
- Firebase tạm thời tắt – cần cấu hình môi trường thật để bật Crashlytics/Analytics/Performance.
- Cần thêm instrumentation tests cho service/permissions.
- Tối ưu thêm đối với OEM đặc thù (nhiều phiên bản ROM).

### 18) Tài nguyên/Tham khảo nội bộ
- `ANDROID_STUDIO_ALL_ERRORS.md`: tổng hợp lỗi build và hướng xử lý.
- Specs theo phase: `PHASE_3_TECHNICAL_SPECIFICATION.md` (và tài liệu khác trong repo).

---

Tài liệu này giúp onboarding nhanh cho dev mới và làm nền tảng review/duy trì chất lượng kiến trúc, đảm bảo hệ thống quét Nordic Beacon chạy ổn định, hiệu quả và sẵn sàng production.


