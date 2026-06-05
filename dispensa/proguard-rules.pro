# Default ProGuard rules. R8 is disabled for debug; release is not minified.
-keepattributes *Annotation*, InnerClasses
-dontwarn org.bouncycastle.jsse.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**

# Keep kotlinx-serialization metadata for our @Serializable models.
-keepclasseswithmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}
-keep,includedescriptorclasses class com.dispensa.app.**$$serializer { *; }
-keepclassmembers class com.dispensa.app.** {
    *** Companion;
}
-keepclasseswithmembers class com.dispensa.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}
