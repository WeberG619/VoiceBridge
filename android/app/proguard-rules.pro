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

# VoiceBridge specific rules

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep Whisper.cpp JNI bindings
-keep class com.voicebridge.whisper.** { *; }

# Keep Tesseract OCR classes
-keep class com.rmtheis.tess4j.** { *; }

# Keep accessibility service classes
-keep class com.voicebridge.accessibility.** { *; }

# Keep skill engine classes
-keep class com.voicebridge.skills.** { *; }

# Keep telemetry classes for crash reporting
-keep class com.voicebridge.telemetry.** { *; }

# Keep data classes for JSON serialization
-keep class com.voicebridge.data.** { *; }

# Keep enum classes
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep Parcelable implementations
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# Keep Serializable classes
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    !private <fields>;
    !private <methods>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# Keep Jetpack Compose classes
-keep class androidx.compose.** { *; }
-keep class kotlin.** { *; }
-keep class kotlinx.** { *; }

# Keep Material Design 3 classes
-keep class com.google.android.material.** { *; }

# Keep ViewModel classes
-keep class androidx.lifecycle.** { *; }

# Keep Room database classes
-keep class androidx.room.** { *; }

# Keep Hilt dependency injection
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# Keep R8 from removing classes with only static methods
-keep class com.voicebridge.utils.** { *; }

# Keep classes that are used via reflection
-keepclassmembers class * {
    @androidx.annotation.Keep *;
}

# Keep custom exceptions
-keep class com.voicebridge.exceptions.** { *; }

# AI/ML model specific rules
-keep class org.tensorflow.lite.** { *; }
-keep class org.pytorch.** { *; }

# Privacy and security
-keep class javax.crypto.** { *; }
-keep class java.security.** { *; }