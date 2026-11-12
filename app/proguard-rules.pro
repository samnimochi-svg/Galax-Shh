# Add project specific ProGuard rules here.

# Keep Android components referenced in AndroidManifest
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.service.quicksettings.TileService

# Explicitly keep exported components & shortcut targets
-keep class com.example.MainActivity { *; }
-keep class com.example.ShortcutExecutionActivity { *; }

# Keep Tasker plugin classes (required by Locale/Tasker intent protocol)
-keep class com.example.tasker.** { *; }

# Keep data models
-keep class com.example.data.** { *; }

# Jetpack Compose & Kotlin Coroutines rules
-dontwarn androidx.compose.**
-keepclassmembers class * {
    @androidx.compose.runtime.Composable *;
    @androidx.compose.runtime.ReadOnlyComposable *;
}

# Preserve line numbers and essential annotations for crash stack traces
-keepattributes SourceFile,LineNumberTable
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

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
