# --- NEONT RELEASE PROGUARD RULES ---
# Jangan acak model data (Gson/Firebase butuh nama field asli)
-keep class app.neon.com.data.model.** { *; }

# Firebase
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }

# Hilt (Dagger)
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# WebRTC
-keep class org.webrtc.** { *; }

# Retrofit & OkHttp
-keep class retrofit2.** { *; }
-keep class okhttp3.** { *; }
-keepattributes Signature
-keepattributes *Annotation*

# Coroutines
-keepclassmembers class kotlinx.coroutines.** { volatile <fields>; }

# ==========================================
# RETROFIT + GSON R8 FIXES
# ==========================================
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes Exceptions

# Gson
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken
-keep public class * implements java.lang.reflect.Type

# Retrofit
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
-dontwarn javax.annotation.**
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
-keep,allowobfuscation,allowshrinking class retrofit2.*
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# Model AI Siput
-keep class app.neon.com.data.network.SiputChatResponse { *; }
-keep class app.neon.com.data.network.SiputChatData { *; }
