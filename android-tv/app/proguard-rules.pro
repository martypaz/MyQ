# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.martypaz.freeviewguide.**$$serializer { *; }
-keepclassmembers class com.martypaz.freeviewguide.** { *** Companion; }
-keepclasseswithmembers class com.martypaz.freeviewguide.** { kotlinx.serialization.KSerializer serializer(...); }
