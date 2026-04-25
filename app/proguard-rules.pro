# EduLife release hardening rules for R8/ProGuard.
# These rules keep release builds obfuscated while preserving runtime APIs that use reflection.
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

# Retrofit reads method annotations and generic response types at runtime to build API calls.
-keepattributes Signature, InnerClasses, EnclosingMethod, RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations, AnnotationDefault
-keep class retrofit2.** { *; }
-dontwarn retrofit2.**

# OkHttp/Okio may reference optional platform integrations that are absent on Android devices.
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# Gson needs generic signatures and annotations so Retrofit's converter can deserialize safely.
-keepattributes *Annotation*
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken

# Fields marked with @SerializedName are safe to obfuscate because Gson uses the annotation value.
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# EduLife model and DTO packages may be serialized by Gson without annotations during the MVP.
# Preserve their members so release obfuscation does not rename JSON field names unexpectedly.
-keep class com.baghdad.edulife.features.**.model.** { *; }
-keep class com.baghdad.edulife.**.dto.** { *; }

# ViewModel subclasses can be created by AndroidX factories; keeping constructors avoids release-only crashes.
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    public <init>(...);
}
