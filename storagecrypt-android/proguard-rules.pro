# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /home/pierre/dev/android-sdk/tools/proguard/proguard-android.txt
# You can edit the include destinationPath and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class destinationPath to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

-keep class * extends java.util.ListResourceBundle {
    protected Object[][] getContents();
}

-keep public class com.google.android.gms.common.internal.safeparcel.SafeParcelable {
    public static final *** NULL;
}

-keepnames @com.google.android.gms.common.annotation.KeepName class *
-keepclassmembernames class * {
    @com.google.android.gms.common.annotation.KeepName *;
}

-keepnames class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

#For EventBus :
-keepclassmembers class ** {
    public void onEvent*(**);
}

#For Retrofit 2.0.0:
# Platform calls Class.forName on types which do not exist on Android to determine platform.
-dontnote retrofit2.Platform
# Platform used when running on RoboVM on iOS. Will not be used at runtime.
-dontnote retrofit2.Platform$IOS$MainThreadExecutor
# Platform used when running on Java 8 VMs. Will not be used at runtime.
-dontwarn retrofit2.Platform$Java8
# Retain generic type information for use by reflection by converters and adapters.
-keepattributes Signature
# Retain declared checked exceptions for use by a Proxy instance.
-keepattributes Exceptions

#Test
-dontoptimize
-dontobfuscate

-dontwarn com.j256.ormlite.**
-dontwarn org.h2.**
-dontwarn okio.**
-dontwarn javax.annotation.Nullable
-dontwarn javax.annotation.ParametersAreNonnullByDefault
-dontwarn javax.annotation.**
-dontwarn org.greenrobot.eventbus.**
-dontwarn org.joda.time.**
-dontwarn org.spongycastle.**

-keep public class org.spongycastle.**
-keep public class fr.petrus.**
-keep public class * extends fr.petrus.tools.storagecrypt.android.tasks.ServiceTask {
    public <init>(...);
}