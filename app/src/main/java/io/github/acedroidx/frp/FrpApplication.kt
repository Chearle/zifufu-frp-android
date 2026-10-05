package io.github.acedroidx.frp

import android.app.Application

/**
 * 自定义 Application，在 onCreate 中执行签名与包名校验。
 * 不可放在 attachBaseContext：该阶段 applicationContext 为 null，会导致启动闪退。
 */
class FrpApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        SignatureGuard.verify(this)
        PackageGuard.verify(this)
        NativeGuard.verifyInstalled(this)
    }
}
