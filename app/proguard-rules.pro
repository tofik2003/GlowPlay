# GlowPlay release rules — keep media/effects pipeline intact.
-keepattributes *Annotation*, InnerClasses, Signature, EnclosingMethod, SourceFile, LineNumberTable
-keep class com.glowplay.player.** { *; }

-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

-keep class androidx.datastore.** { *; }
-keep class coil.** { *; }
-dontwarn coil.**

-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

-dontwarn javax.annotation.**
-dontwarn org.codehaus.mojo.animal_sniffer.**
-dontwarn org.checkerframework.**
-dontwarn org.jetbrains.annotations.**
-dontwarn kotlin.Unit
