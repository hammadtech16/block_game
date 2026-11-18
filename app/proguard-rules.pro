# Block Game ProGuard Rules

-keep class com.blockgame.** { *; }
-keepclassmembers class com.blockgame.** { *; }

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep enums
-keepclassmembers enum * {
    **[] $VALUES;
    public *;
}