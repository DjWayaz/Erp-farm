# Add project specific ProGuard rules here.
-keep class com.farmapp.data.** { *; }
-keep class com.farmapp.worker.** { *; }

# Keep kotlinx serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.farmapp.**$$serializer { *; }
-keepclassmembers class com.farmapp.** { *** Companion; }
-keepclasseswithmembers class com.farmapp.** { kotlinx.serialization.KSerializer serializer(...); }
