package io.github.acedroidx.frp

/**
 * 应用对外身份。广播、ContentProvider 等接口均基于该包名。
 * 修改此处时必须同步修改 Gradle applicationId，否则包名校验会拒绝启动。
 */
object AppIdentity {
    const val EXPECTED_PACKAGE = "com.zifufu.frp"
    const val CONFIG_AUTHORITY = "$EXPECTED_PACKAGE.config"
}
