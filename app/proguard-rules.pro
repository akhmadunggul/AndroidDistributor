# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keepclassmembers class * {
    @androidx.room.* <methods>;
}
-keep class * extends androidx.room.migration.Migration

# ViewModel — keep constructors so ViewModelProvider can instantiate them
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}
-keepclassmembers class * extends androidx.lifecycle.AndroidViewModel {
    <init>(android.app.Application);
}

# Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# WorkManager workers — keep constructor for WorkerFactory reflection
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.CoroutineWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# ZXing (QR code)
-keep class com.google.zxing.** { *; }

# Compose — keep lambda and function types used by reflection
-keepclassmembers class androidx.compose.** { *; }
-dontwarn androidx.compose.**
