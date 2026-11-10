# SnakeYAML uses reflection for some internals; keep it intact.
-keep class org.yaml.snakeyaml.** { *; }
-dontwarn org.yaml.snakeyaml.**
# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
# Keep Room generated code
-keep class * extends androidx.room.RoomDatabase
