# Project specific ProGuard / R8 rules for Roll

# Preserve line numbers for debugging
-keepattributes SourceFile,LineNumberTable

# Room Database
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keep class * implements androidx.room.RoomDatabase
-keep class * extends androidx.room.RoomDatabase$Callback
-dontwarn androidx.room.paging.**

# Data Models
-keep class com.avikalabs.roll.data.model.** { *; }
-keep class com.avikalabs.roll.data.local.** { *; }

# WorkManager
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep class * extends androidx.work.Worker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep class * extends androidx.work.CoroutineWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep class androidx.work.** { *; }

# Services, Receivers, and App Components
-keep class com.avikalabs.roll.RollApplication { *; }
-keep class com.avikalabs.roll.MainActivity { *; }
-keep class com.avikalabs.roll.service.** { *; }
-keep class com.avikalabs.roll.receiver.** { *; }
-keep class com.avikalabs.roll.wallpaper.** { *; }
-keep class com.avikalabs.roll.widget.** { *; }
-keep class com.avikalabs.roll.billing.** { *; }

# Google Play Billing Library
-keep class com.android.billingclient.api.** { *; }
-keep class com.android.vending.billing.** { *; }
