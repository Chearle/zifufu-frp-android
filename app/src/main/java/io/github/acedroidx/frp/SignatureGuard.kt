package io.github.acedroidx.frp

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.util.Log
import android.widget.Toast
import java.security.MessageDigest
import kotlin.system.exitProcess

/**
 * 运行时签名校验，用于防止二次打包。
 *
 * 原理：把"官方签名证书的 SHA-256"在编译期固定进应用（同时注入 native 层
 * libzfsig.so 与 BuildConfig）。二次打包者无法拿到原始签名私钥，必须用别的密钥
 * 重新签名，安装后实际签名指纹与固定值不一致，于是拒绝启动。
 *
 * 校验在 native 层完成比对，纯 smali 补丁较难直接绕过；native 不可用时回退到
 * Kotlin 比对，保证可用性。
 */
object SignatureGuard {

    private const val TAG = "SignatureGuard"

    /** 由 libzfsig.so 实现：比对运行期签名证书摘要与编译期固定值。 */
    private external fun nativeVerifyDigest(digest: ByteArray): Boolean

    private val nativeLoaded: Boolean by lazy {
        try {
            System.loadLibrary("zfsig")
            true
        } catch (t: Throwable) {
            // 某些 ABI（如 x86）没有编出该库，回退到 Kotlin 比对
            Log.e(TAG, "native 签名校验库加载失败，回退到 Kotlin 校验", t)
            false
        }
    }

    /**
     * 校验失败时提示并结束进程；校验过程自身异常时不阻断启动，避免误伤正式包。
     */
    fun verify(context: Context): Boolean {
        return try {
            val expected = BuildConfig.SignerCertSha256.trim().lowercase()
            if (expected.isEmpty()) {
                // 没有配置签名信息（开发构建）时不阻断
                Log.w(TAG, "未配置签名固定，跳过签名校验")
                return true
            }

            val actual = currentSignerDigest(context) ?: return fail(context, "无法读取签名信息")
            val actualHex = actual.joinToString("") { "%02x".format(it) }

            val ok = if (nativeLoaded) {
                nativeVerifyDigest(actual)
            } else {
                MessageDigest.isEqual(actualHex.toByteArray(), expected.toByteArray())
            }

            if (!ok) {
                fail(context, "签名不匹配 actual=$actualHex")
            }
            true
        } catch (t: Throwable) {
            Log.e(TAG, "签名校验异常，跳过以免影响启动", t)
            true
        }
    }

    /** 取安装包实际签名证书（DER）的 SHA-256 摘要。 */
    private fun currentSignerDigest(context: Context): ByteArray? {
        val pm = context.packageManager
        val pkg = context.packageName
        @Suppress("DEPRECATION")
        val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            pm.getPackageInfo(pkg, PackageManager.GET_SIGNING_CERTIFICATES)
        } else {
            pm.getPackageInfo(pkg, PackageManager.GET_SIGNATURES)
        }
        val signer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info.signingInfo?.apkContentsSigners?.firstOrNull()
        } else {
            @Suppress("DEPRECATION")
            info.signatures?.firstOrNull()
        }
        val der = signer?.toByteArray() ?: return null
        return MessageDigest.getInstance("SHA-256").digest(der)
    }

    private fun fail(context: Context, reason: String): Nothing {
        Log.e(TAG, "签名校验失败: $reason")
        try {
            val appContext = context.applicationContext ?: context
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(appContext, R.string.signature_invalid, Toast.LENGTH_LONG).show()
            }
        } catch (_: Exception) {
        }
        Process.killProcess(Process.myPid())
        exitProcess(1)
    }
}
