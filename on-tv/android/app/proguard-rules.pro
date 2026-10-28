# R8 for the native UI's sake (unminified Compose crawls on this TV). The libraries below use
# reflection or JNI naming, so they keep their shapes.
-keep class org.bouncycastle.** { *; }
-keep class org.conscrypt.** { *; }
-keep class io.github.muntashirakon.adb.** { *; }
-keep class fi.iki.elonen.** { *; }
# Adb.kt reaches the manager's private connection field when a connect hangs.
-keepclassmembers class * extends io.github.muntashirakon.adb.AbsAdbConnectionManager { <fields>; }
# The webview looks call these by name.
-keepclassmembers class * { @android.webkit.JavascriptInterface <methods>; }
-dontwarn org.bouncycastle.**
-dontwarn org.conscrypt.**
-dontwarn javax.naming.**
