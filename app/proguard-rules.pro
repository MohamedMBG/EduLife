# EduLife release hardening rules for R8/ProGuard.
# These rules keep release builds obfuscated while preserving runtime APIs that use reflection.
#
# Scope (P2 hardening 2026-06-18): broad "-keep class retrofit2.**" was replaced with the
# Retrofit-recommended targeted rules so retrofit2 internals are still obfuscated/shrunk.
# Model/DTO classes remain broadly kept because most fields are deserialized by reflection
# without @SerializedName — narrowing those rules without first migrating every DTO to
# @SerializedName would break Gson on the live API. That migration is tracked as P3.
#
# For more details, see http://developer.android.com/guide/developing/tools/proguard.html

# Uncomment this to preserve the line number information for debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to hide the original source file name.
#-renamesourcefileattribute SourceFile

# Reflection-driven runtime metadata Retrofit/Gson need to resolve generic types and annotations.
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations, AnnotationDefault

# Retrofit's recommended R8 rules (apply across all interface types that declare @retrofit2.http.*
# methods so the call factory can still build the request). Replaces the older catch-all
# `-keep class retrofit2.** { *; }` which kept Retrofit's entire internals from being shrunk.
-keepclasseswithmembers,includedescriptorclasses class * {
    @retrofit2.http.* <methods>;
}
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response

# Suspend-conversion / Kotlin reflection types referenced by Retrofit on platforms that don't ship
# them (we never use them, but R8 still resolves the descriptors).
-dontwarn retrofit2.KotlinExtensions
-dontwarn retrofit2.KotlinExtensions$*
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement

# Keep the EduLife ApiService interface itself reachable so Retrofit can create its dynamic proxy.
# Members are allowed to be obfuscated because Retrofit dispatches by reflection on annotations.
-keep,allowobfuscation,allowshrinking interface com.baghdad.edulife.core.network.ApiService

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

# EduLife DTO/model packages are still broadly kept because most fields are bare field-name
# matches against the live backend JSON. Migrating every DTO to @SerializedName is tracked
# as P3 hardening; once that lands, this rule can be removed.
-keep class com.baghdad.edulife.features.**.model.** { *; }
-keep class com.baghdad.edulife.**.dto.** { *; }

# ViewModel subclasses can be created by AndroidX factories; keeping constructors avoids release-only crashes.
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    public <init>(...);
}

# Strip verbose/debug logging from release builds (audit 2026-06-19 P3-1) so non-essential
# diagnostics never reach logcat on a shipped APK. Log.w/Log.e are deliberately retained for
# crash triage — call sites must never pass sensitive values (tokens, emails, ids, content URIs)
# to them, since those lines do ship.
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
}
