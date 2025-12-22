# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep Markwon classes
-keep class io.noties.markwon.** { *; }
-dontwarn io.noties.markwon.**

# Keep Prism4j classes
-keep class io.noties.prism4j.** { *; }
-dontwarn io.noties.prism4j.**

# Keep data classes
-keep class com.kelexine.azubimark.data.model.** { *; }

# Keep Kotlin metadata
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# Keep Compose
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Keep DataStore
-keep class androidx.datastore.** { *; }
-dontwarn androidx.datastore.**

# Keep Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# Keep Kotlin Serialization (if used)
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault

# Remove logging in release
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# Keep line numbers for crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
