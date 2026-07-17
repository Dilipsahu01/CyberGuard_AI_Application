# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

-assumenosideeffects class android.util.Log { *; }

# Preserve ONNX and Sherpa JNI bindings (Crucial for V1.1_Updates Section 2)
-keep class ai.onnxruntime.** { *; }
-keep class com.k2fsa.sherpa.onnx.** { *; }

# Preserve Native Secrets JNI mapping
-keep class com.example.security.NativeSecrets { *; }

# Preserve Moshi Data Classes for API parsing
-keep class com.example.models.** { *; }
-keep class com.squareup.moshi.** { *; }
-keep interface com.squareup.moshi.** { *; }

# Prevent R8 from modifying Compose Snapshot locks to stop Lock Verification errors
-keepclassmembers class androidx.compose.runtime.snapshots.Snapshot { *; }
-dontwarn androidx.compose.runtime.snapshots.**
