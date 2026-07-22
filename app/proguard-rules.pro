# ========== REGLAS ESPECÍFICAS PARA AulaHelpIA ==========

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

# ========== REGLAS ESPECÍFICAS PARA DATA CLASSES Y ROOM ==========

# Keep Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep class * implements androidx.room.Database

# Keep data classes (PlanItem, etc.)
-keep class com.tuusuario.aulahelpia.home.data.** { *; }
-keepclassmembers class com.tuusuario.aulahelpia.home.data.** {
    <init>(...);
    *** get*();
    void set*(***);
}

# Keep enums
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep TypeConverters
-keep class * implements androidx.room.TypeConverter
-keepclassmembers class * {
    @androidx.room.TypeConverter <methods>;
}

# App específica
-keep class com.tuusuario.aulahelpia.** { *; }
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

# GSON
-keepattributes Signature
-keepattributes *Annotation*

# Resources
-keep class **.R
-keep class **.R$* { *; }