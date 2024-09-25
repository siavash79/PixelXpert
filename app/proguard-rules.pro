# Kotlin
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
	public static void check*(...);
	public static void throw*(...);
}
-assumenosideeffects class java.util.Objects {
    public static ** requireNonNull(...);
}

# Strip debug log
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
}

# Xposed
-keep class de.robv.android.xposed.**
-keep class sh.siava.pixelxpert.XPEntry
-keepnames class sh.siava.pixelxpert.modpacks.**
-keep class sh.siava.pixelxpert.modpacks.** {
    <init>(android.content.Context);
}

# Keep the ConstraintLayout Motion class
-keep,allowoptimization,allowobfuscation class androidx.constraintlayout.motion.widget.** { *; }

# Keep Recycler View Stuff
-keep,allowoptimization,allowobfuscation class androidx.recyclerview.widget.** { *; }

# Keep Parcelable Creators
-keepnames class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# Obfuscation
-repackageclasses ''
-allowaccessmodification

# Root Service
-keep class sh.siava.pixelxpert.service.RootProviderProxy { *; }
-keep class sh.siava.pixelxpert.IRootProviderProxy { *; }

# Services
-keep interface **.I* { *; }
-keep class **.I*$Stub { *; }
-keep class **.I*$Stub$Proxy { *; }
-keep class sh.siava.pixelxpert.service.*


# Keep all inner classes and their names within the specified package
# but allow optimization of their internal code
-keep class sh.siava.pixelxpert.**$* {
    public protected private *;
}

# Allow obfuscation of non-inner class members (fields and methods), but keep class names
-keep class sh.siava.pixelxpert.** { *; }

# Allow optimization and shrinking for all classes
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*
-keepattributes Exceptions,InnerClasses,Signature,Deprecated,*Annotation*,EnclosingMethod,SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Keep all native method names
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep all annotation types
-keep @interface ** { *; }

# Markdown View
-dontwarn java.awt.image.RGBImageFilter
-keep class br.tiagohm.markdownview.**
-keep class com.vladsch.flexmark.**