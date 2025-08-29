# ========== NORDIC BEACON SCANNER - PROGUARD RULES ==========
# Production-optimized ProGuard configuration cho Nordic Beacon Scanner
# @author Senior Android Developer

# ========== KEEP APPLICATION CLASSES ==========
-keep class com.nordicbeacon.scanner.** { *; }

# ========== ALTBEACON LIBRARY ==========  
# Keep AltBeacon library classes - required cho runtime beacon detection
-keep class org.altbeacon.beacon.** { *; }
-keepattributes *Annotation*
-dontwarn org.altbeacon.beacon.**

# ========== HILT DEPENDENCY INJECTION ==========
# Keep Hilt generated classes
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ApplicationComponentManager { *; }
-keep class **_HiltComponents$SingletonC { *; }

# Keep injected fields và methods
-keepclasseswithmembernames class * {
    @javax.inject.* <fields>;
}
-keepclasseswithmembernames class * {
    @javax.inject.* <methods>;
}

# ========== ROOM DATABASE ==========
# Keep Room generated classes
-keep class androidx.room.** { *; }
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }

# ========== KOTLIN COROUTINES ==========
# Keep coroutines classes cho async operations
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# ========== ANDROID COMPONENTS ==========
# Keep Service classes
-keep public class * extends android.app.Service { *; }
-keep public class * extends android.content.BroadcastReceiver { *; }

# Keep Parcelable implementations
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# ========== SERIALIZATION ==========
# Keep serialization classes cho data persistence
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# ========== TIMBER LOGGING ==========
# Keep Timber logging classes cho production logging
-keep class timber.log.** { *; }

# ========== PRODUCTION SECURITY HARDENING ==========

# Enable aggressive optimization cho production  
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*

# Remove debug information în production builds
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
}

# Remove Timber logging în release builds
-assumenosideeffects class timber.log.Timber {
    public static *** v(...);
    public static *** d(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
}

# ========== SECURITY OBFUSCATION ==========

# Obfuscate Nordic beacon related classes
-obfuscate
-renamesourcefileattribute SourceFile

# Keep line numbers cho stack traces în production
-keepattributes SourceFile,LineNumberTable

# ========== ANTI-REVERSE ENGINEERING ==========

# Remove debugging information
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    static void checkParameterIsNotNull(java.lang.Object, java.lang.String);
    static void checkNotNullParameter(java.lang.Object, java.lang.String);
    static void checkExpressionValueIsNotNull(java.lang.Object, java.lang.String);
    static void checkNotNullExpressionValue(java.lang.Object, java.lang.String);
}

# Obfuscate sensitive constants (keep Nordic UUID protected)
-keepclassmembers class com.nordicbeacon.scanner.domain.entities.NordicBeacon {
    public static final java.lang.String NORDIC_UUID;
}

# ========== FIREBASE SECURITY ==========

# Keep Firebase classes cho production monitoring
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# ========== ADVANCED SECURITY MEASURES ==========

# String encryption (obfuscate string literals)
-adaptclassstrings
-adaptresourcefilenames    
-adaptresourcefilecontents

# Remove unused code aggressively  
-dontshrink
-dontoptimize
-keepattributes Exceptions,InnerClasses,Signature,Deprecated,SourceFile,LineNumberTable,*Annotation*,EnclosingMethod

# ========== BLUETOOTH CLASSES ==========
# Keep Bluetooth related classes cho BLE operations  
-keep class android.bluetooth.** { *; }
-dontwarn android.bluetooth.**

# ========== REFLECTION ==========
# Keep classes used via reflection
-keepclassmembers class * {
    @androidx.annotation.Keep *;
}

# ========== NATIVE METHODS ==========
# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# ========== MATERIAL DESIGN ==========
# Keep Material Design components
-keep class com.google.android.material.** { *; }
-dontwarn com.google.android.material.**
