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

# ========== REGLAS ESPECÍFICAS PARA CRECIENDO JUNTOS ==========

# Navigation Component
-keep class androidx.navigation.** { *; }

# ViewModel y LiveData
-keep class androidx.lifecycle.** { *; }

# Ads (AdMob)
-keep class com.google.android.gms.ads.** { *; }
-keep public class com.google.ads.** { *; }

# Material Design
-keep class com.google.android.material.** { *; }

# Kotlin
-keep class kotlin.** { *; }
-keep class kotlinx.** { *; }

# App específica - mantener tus clases principales
-keep class com.tuusuario.CreciendoJuntos.** { *; }
-keep class * extends android.app.Application

# Activity y Fragment
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Fragment
-keep public class * extends androidx.fragment.app.Fragment

# ViewModel
-keep class * extends androidx.lifecycle.ViewModel { *; }

# Serializable/Parcelable
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# GSON/Serialization si lo usas
-keepattributes Signature
-keepattributes *Annotation*

# Resources
-keep class **.R
-keep class **.R$* { *; }