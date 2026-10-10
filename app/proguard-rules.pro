-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class **$$serializer { *; }
-keepclasseswithmembers class * { @kotlinx.serialization.SerialName <fields>; }

-keep class com.hoho.android.usbserial.driver.** { *; }
