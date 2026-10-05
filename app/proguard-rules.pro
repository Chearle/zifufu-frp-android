# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.

-keep class io.github.acedroidx.frp.BuildConfig { *; }

# 签名校验走 JNI，类名/方法名被混淆会导致 native 符号对不上
-keep class io.github.acedroidx.frp.SignatureGuard { *; }

# Parcelize / 枚举在跨进程 Intent 中使用
-keep class io.github.acedroidx.frp.FrpConfig { *; }
-keep class io.github.acedroidx.frp.FrpType { *; }
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# Tasker 通过反射创建插件类
-keep class io.github.acedroidx.frp.tasker.** { *; }
-keep class com.joaomgcd.** { *; }
-dontwarn com.joaomgcd.**

-keepattributes RuntimeVisibleAnnotations,AnnotationDefault,*Annotation*

# 发布包去掉调试日志，避免泄露内核路径
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
}
