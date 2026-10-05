package io.github.acedroidx.frp

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.util.Log
import android.widget.Toast
import kotlin.system.exitProcess

/**
 * 运行时包名校验，防止被改包名二次打包后继续运行。
 * 注意：不可在 Application.attachBaseContext 中调用，此时 applicationContext 尚未就绪。
 */
object PackageGuard {
    /**
     * 校验当前进程包名以及 BuildConfig 是否均为官方包名。
     * 校验失败时提示用户并结束进程；校验过程本身出错时不阻断启动。
     */
    fun verify(context: Context): Boolean {
        return try {
            val expected = AppIdentity.EXPECTED_PACKAGE
            val actualNames = mutableListOf<String>()
            context.packageName?.takeIf { it.isNotBlank() }?.let { actualNames.add(it) }
            context.applicationInfo?.packageName?.takeIf { it.isNotBlank() }?.let { actualNames.add(it) }
            // attachBaseContext 阶段 getApplicationContext() 仍为 null，必须空安全
            try {
                context.applicationContext?.packageName?.takeIf { it.isNotBlank() }?.let {
                    actualNames.add(it)
                }
            } catch (_: Exception) {
            }
            actualNames.add(BuildConfig.APPLICATION_ID)

            val mismatch = actualNames.filter { it != expected }
            if (mismatch.isNotEmpty()) {
                Log.e("PackageGuard", "包名校验失败: actual=$actualNames expected=$expected")
                showInvalidToast(context)
                Process.killProcess(Process.myPid())
                exitProcess(1)
            }
            true
        } catch (e: Exception) {
            // 校验逻辑自身异常时不能把正式包打崩
            Log.e("PackageGuard", "包名校验异常，跳过以免影响启动", e)
            true
        }
    }

    private fun showInvalidToast(context: Context) {
        try {
            val appContext = context.applicationContext ?: context
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(appContext, R.string.package_invalid, Toast.LENGTH_LONG).show()
            }
        } catch (_: Exception) {
        }
    }
}
