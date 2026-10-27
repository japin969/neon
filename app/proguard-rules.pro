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
