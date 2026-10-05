# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to the flags specified
# in C:\Users\3D MAX\AppData\Local\Android\Sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.

-keep public class com.google.android.gms.ads.** { *; }
-keep public class com.google.ads.mediation.** { *; }
-keep class com.facebook.ads.** { *; }
-keep class com.unity3d.ads.** { *; }
-keep class com.unity3d.services.** { *; }
