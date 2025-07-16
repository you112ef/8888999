# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Keep PyTorch Mobile classes
-keep class org.pytorch.** { *; }
-keep class com.facebook.jni.** { *; }

# Keep analysis classes
-keep class com.example.spermanalyzer.AnalysisEngine { *; }
-keep class com.example.spermanalyzer.CASACalculator { *; }
-keep class com.example.spermanalyzer.SpermTracker { *; }

# Keep data classes
-keep class com.example.spermanalyzer.TrackedObject { *; }
-keep class com.example.spermanalyzer.CASAMetrics { *; }
-keep class com.example.spermanalyzer.Detection { *; }
-keep class com.example.spermanalyzer.Position { *; }

# Keep camera classes
-keep class androidx.camera.** { *; }

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep enum classes
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Remove logging in release builds
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
    public static int e(...);
}