# Acceso UTCJ — keep Room entities and Hilt
-keep class edu.utcj.acceso.data.local.** { *; }
-keep class * extends androidx.room.RoomDatabase
-dontwarn com.google.firebase.**
