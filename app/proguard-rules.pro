# R8 rules for kasir-mobile. kotlinx.serialization, OkHttp, Coil and Compose
# ship their own consumer rules; the rules below cover gaps for R8 full mode
# (default on AGP 8+).

# --- Retrofit 2.9 + suspend functions under R8 full mode -------------------
# Retrofit reads generic signatures of service methods reflectively. Full mode
# strips them from classes it thinks are unused, which breaks suspend calls
# ("Unable to create call adapter" / ParameterizedType ClassCastException).
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations, AnnotationDefault
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
-if interface * { @retrofit2.http.* <methods>; }
-keep,allowobfuscation interface <1>
-if interface * { @retrofit2.http.* <methods>; }
-keep,allowobfuscation interface * extends <1>
-dontwarn retrofit2.KotlinExtensions
-dontwarn retrofit2.KotlinExtensions$*

# --- kotlinx.serialization DTOs ------------------------------------------
# Library rules keep generated serializers; also keep the companion
# serializer() lookups for our own models explicitly to be safe.
-keepclassmembers @kotlinx.serialization.Serializable class com.kasir.mobile.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.kasir.mobile.**$$serializer { *; }

# Keep line numbers for readable crash stack traces; hide source file names.
-keepattributes SourceFile, LineNumberTable
-renamesourcefileattribute SourceFile
