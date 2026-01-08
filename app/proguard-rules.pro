# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# ========================================
# WorkManager & Hilt Workers
# ========================================
# Keep Worker constructors - required for WorkManager reflection
-keepclassmembers class * extends androidx.work.Worker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# Keep CoroutineWorker constructors
-keepclassmembers class * extends androidx.work.CoroutineWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# Keep Hilt Worker generated classes
-keep class dagger.hilt.android.internal.work.* { *; }
-keep class javax.inject.Origin { *; }

# Keep our Workers explicitly
-keep class com.dailydrug.data.alarm.BootRescheduleWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep class com.dailydrug.data.worker.DailyScheduleWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep class com.dailydrug.data.worker.MedicationReminderWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}