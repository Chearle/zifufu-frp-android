package io.github.acedroidx.frp

import android.content.Context
import android.os.Process
import android.util.Log
import android.widget.Toast
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import kotlin.system.exitProcess

/**
 * 校验内置内核文件，防止把 libfrpc.so / 官方 frp 直接丢进安装包或数据目录后运行。
 */
object NativeGuard {
    /**
     * 启动时做一次存在性与哈希校验；失败则提示并退出。
     */
    fun verifyInstalled(context: Context) {
        FrpType.entries.forEach { type ->
            requirePath(context, type)
        }
    }

    /**
     * 返回通过完整性校验的内核绝对路径，供 ProcessBuilder 执行。
     */
    fun requirePath(context: Context, type: FrpType): String {
        val file = resolve(context, type)
        if (!file.isFile) {
            fail(context, "missing ${type.typeName}")
        }
        val actual = sha256Hex(file)
        val allowed = expectedHashes(type)
        if (actual !in allowed) {
            fail(context, "hash mismatch ${type.typeName}")
        }
        return file.absolutePath
    }

    private fun resolve(context: Context, type: FrpType): File {
        val name = type.getLibName()
        val nativeDir = context.applicationInfo.nativeLibraryDir
        val direct = File(nativeDir, name)
        if (direct.isFile) return direct
        // 部分机型 nativeLibraryDir 会指到具体 ABI 的兄弟目录
        File(nativeDir).parentFile?.listFiles()?.forEach { abiDir ->
            val candidate = File(abiDir, name)
            if (candidate.isFile) return candidate
        }
        return direct
    }

    private fun expectedHashes(type: FrpType): Set<String> {
        val raw = when (type) {
            FrpType.FRPC -> BuildConfig.FrpcSha256List
            FrpType.FRPS -> BuildConfig.FrpsSha256List
        }
        return raw.split(',').map { it.trim().lowercase() }.filter { it.isNotEmpty() }.toSet()
    }

    private fun sha256Hex(file: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { input ->
            val buf = ByteArray(64 * 1024)
            while (true) {
                val n = input.read(buf)
                if (n <= 0) break
                md.update(buf, 0, n)
            }
        }
        return md.digest().joinToString("") { b -> "%02x".format(b) }
    }

    private fun fail(context: Context, reason: String): Nothing {
        Log.e("NativeGuard", "内核完整性校验失败: $reason")
        try {
            Toast.makeText(
                context.applicationContext,
                R.string.native_lib_tampered,
                Toast.LENGTH_LONG
            ).show()
        } catch (_: Exception) {
        }
        Process.killProcess(Process.myPid())
        exitProcess(1)
    }
}
