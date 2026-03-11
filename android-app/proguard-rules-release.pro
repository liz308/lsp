# LSP Android Release ProGuard Rules
# Requirement 6.1: Build and Release Configuration

# Keep native methods and JNI interfaces
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep JNI bridge classes and LSP core functionality
-keep class com.example.lspandroid.** { *; }
-keep class com.example.lspandroid.jni.** { *; }
-keep class com.example.lspandroid.audio.** { *; }
-keep class com.example.lspandroid.ui.** { *; }

# Keep Oboe audio library classes
-keep class com.google.oboe.** { *; }
-keepclassmembers class com.google.oboe.** {
    public <methods>;
    public <fields>;
}

# Keep Jetpack Compose classes and runtime
-keep class androidx.compose.** { *; }
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.ui.** { *; }
-keep class androidx.compose.foundation.** { *; }
-keep class androidx.compose.material3.** { *; }
-keep class kotlin.Metadata { *; }
-keep class kotlin.coroutines.** { *; }

# Keep Kotlin serialization classes
-keepattributes *Annotation*, InnerClasses, Signature, EnclosingMethod
-dontnote kotlinx.serialization.AnnotationsKt
-dontnote kotlinx.serialization.SerializationKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep data classes for serialization
-keep @kotlinx.serialization.Serializable class ** {
    *;
}
-keepclassmembers @kotlinx.serialization.Serializable class ** {
    *** Companion;
    *** INSTANCE;
    kotlinx.serialization.KSerializer $$serializer();
}

# Keep AAP (Android Audio Plugin) service classes
-keep class org.androidaudioplugin.** { *; }
-keep interface org.androidaudioplugin.** { *; }
-keepclassmembers class org.androidaudioplugin.** {
    public <methods>;
    public <fields>;
}

# Keep Android Audio framework classes
-keep class android.media.** { *; }
-keep class android.media.audiofx.** { *; }

# Keep reflection-based classes
-keepclassmembers class * {
    @androidx.annotation.Keep *;
}

# Keep enum classes
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep Parcelable implementations
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator CREATOR;
}

# Optimization settings
-optimizationpasses 5
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-dontpreverify
-verbose
-allowaccessmodification
-mergeinterfacesaggressively

# Optimization filters
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
# Remove logging in release builds
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
    public static int e(...);
}

# Remove debug and test code
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    static void checkParameterIsNotNull(java.lang.Object, java.lang.String);
    static void checkNotNullParameter(java.lang.Object, java.lang.String);
    static void checkExpressionValueIsNotNull(java.lang.Object, java.lang.String);
    static void checkNotNullExpressionValue(java.lang.Object, java.lang.String);
}

# Keep crash reporting attributes
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
# Keep custom exceptions for crash reporting
-keep public class * extends java.lang.Exception
-keep public class * extends java.lang.Error
-keep public class * extends java.lang.RuntimeException

# Keep R class and resources
-keepclassmembers class **.R$* {
    public static <fields>;
}
-keep class **.R$*

# Keep BuildConfig
-keep class **.BuildConfig { *; }

# Keep manifest classes
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
