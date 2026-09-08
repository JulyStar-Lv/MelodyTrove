# Keep the desktop launcher and runtime boundaries that are accessed by name or
# from native/plugin code. The Compose Desktop plugin keeps MainKt separately.
-keep class uniffi.** { *; }
-keep class com.sun.jna.** { *; }
-keep class org.freedesktop.dbus.** { *; }
-keep class io.github.julystar.musicapp.di.** { *; }
-keep class io.github.julystar.musicapp.database.** { *; }
-keep class io.github.julystar.musicapp.plugin.runtime.** { *; }
-keep class io.github.julystar.musicapp.plugin.management.** { *; }

# DataStore Preferences uses generated protobuf-lite message metadata that
# resolves fields such as preferences_ by their generated JVM names. Do not
# obfuscate or strip those generated classes/members when ProGuard is re-enabled.
-keep class androidx.datastore.preferences.PreferencesProto** { *; }
-keep class androidx.datastore.preferences.protobuf.** { *; }

# sqlite-bundled registers JNI methods against fixed JVM classes such as
# BundledSQLiteDriverKt. Keep the boundary intact when shrinking is re-enabled.
-keep class androidx.sqlite.driver.bundled.** { *; }
-keepclasseswithmembers class androidx.sqlite.driver.bundled.** {
    native <methods>;
}

-keepattributes RuntimeVisibleAnnotations,RuntimeInvisibleAnnotations,
    RuntimeVisibleParameterAnnotations,RuntimeInvisibleParameterAnnotations,
    AnnotationDefault,Signature,InnerClasses,EnclosingMethod,SourceFile,LineNumberTable

# Keep generated Kotlin serialization serializers and their metadata.
-keep class **$$serializer { *; }
-keepclassmembers class ** {
    kotlinx.serialization.KSerializer serializer(...);
}

# The Release mapping is uploaded as a separate GitHub Release asset when
# Desktop ProGuard is enabled.
-printmapping 'build/compose/proguard/mapping.txt'
