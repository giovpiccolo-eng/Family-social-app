# Keep Firebase Firestore model classes (they are deserialized via reflection).
-keepclassmembers class com.familynest.app.data.model.** {
  *;
}
-keepattributes Signature
-keepattributes *Annotation*
