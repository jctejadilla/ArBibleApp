# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Keep standard line number information for crashlytics/debugging
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Firebase Firestore rules (Important: Firestore uses reflection for POJOs)
-keepclassmembers class * {
    @com.google.firebase.firestore.PropertyName <fields>;
}
-keep class com.google.firebase.** { *; }

# Firebase Auth
-keep class com.google.firebase.auth.** { *; }

# Preserve custom views for XML inflation
-keep public class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
    public void set*(...);
}

# ZXing / Barcode scanner rules
-keep class com.journeyapps.barcodescanner.** { *; }
-keep class com.google.zxing.** { *; }

# Shimmer
-keep class com.facebook.shimmer.** { *; }

# MPAndroidChart
-keep class com.github.mikephil.charting.** { *; }

# Unity Library Interop
-keep class com.unity3d.player.** { *; }

# Prevent R8 warnings for optional Java/Javax dependencies
-dontwarn java.sql.JDBCType
-dontwarn javax.lang.model.**
-dontwarn javax.annotation.**