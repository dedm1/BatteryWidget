# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep line number information for debugging stack traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Kotlin
-keep class kotlin.Metadata { *; }
-keepclassmembers class **$WhenMappings {
    <fields>;
}
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}
-dontwarn kotlin.**
# Оптимизация: удаление проверок на null в release (уже включены в proguard-android-optimize.txt)
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    static void checkParameterIsNotNull(java.lang.Object, java.lang.String);
}

# AndroidX - минимальные keep правила (R8 автоматически определит используемые классы)
-dontwarn androidx.**

# Compose - R8 автоматически определит используемые классы через анализ кода
-dontwarn androidx.compose.**

# Keep application classes
-keep class com.dedm.batterywidget.** { *; }

# Keep MainActivity (launcher activity)
-keep class com.dedm.batterywidget.MainActivity { *; }

# Keep widget providers (обязательно для работы виджетов)
-keep class * extends android.appwidget.AppWidgetProvider {
    *;
}
-keep class com.dedm.batterywidget.widget.BatteryWidgetProvider { *; }
-keep class com.dedm.batterywidget.widget.DateWidgetProvider { *; }

# Keep services (включая AccessibilityService)
-keep class * extends android.app.Service {
    *;
}
-keep class * extends android.accessibilityservice.AccessibilityService {
    *;
}
-keep class com.dedm.batterywidget.timezone.TimeZoneAutomationService { *; }

# Keep BroadcastReceivers
-keep class * extends android.content.BroadcastReceiver {
    *;
}

# Keep Parcelable implementations
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator CREATOR;
}

# Keep Serializable classes
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# Keep classes referenced in AndroidManifest.xml
-keep class com.dedm.batterywidget.MainActivity { *; }
-keep class com.dedm.batterywidget.widget.BatteryWidgetProvider { *; }
-keep class com.dedm.batterywidget.widget.DateWidgetProvider { *; }
-keep class com.dedm.batterywidget.timezone.TimeZoneAutomationService { *; }

# Keep data classes and models (если используются для сериализации)
-keep class com.dedm.batterywidget.metrics.** { *; }
-keep class com.dedm.batterywidget.timezone.** { *; }
-keep class com.dedm.batterywidget.widget.** { *; }