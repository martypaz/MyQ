# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.martypaz.myq.**$$serializer { *; }
-keepclassmembers class com.martypaz.myq.** { *** Companion; }
-keepclasseswithmembers class com.martypaz.myq.** { kotlinx.serialization.KSerializer serializer(...); }
