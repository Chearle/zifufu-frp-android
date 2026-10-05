<div align="center">

<img src="icon.png" width="130" alt="紫芙FRP" />

# 紫芙 FRP

**Android 上开箱即用的 frp 客户端 / 服务端**

把 [fatedier/frp](https://github.com/fatedier/frp) 直接用 Go 交叉编译成 Android 原生库,
无需 Root、无需 Termux,装上就能用。

[![License](https://img.shields.io/badge/License-Apache--2.0-blue.svg)](LICENSE)
[![Platform](https://img.shields.io/badge/Platform-Android-3DDC84.svg?logo=android&logoColor=white)](https://www.android.com)
[![Min SDK](https://img.shields.io/badge/minSdk-23-orange.svg)](#-环境要求)
[![Target SDK](https://img.shields.io/badge/targetSdk-37-orange.svg)](#-环境要求)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.3-7F52FF.svg?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4.svg)](https://developer.android.com/jetpack/compose)
[![frp](https://img.shields.io/badge/powered%20by-fatedier%2Ffrp-00ADD8.svg)](https://github.com/fatedier/frp)
[![Stars](https://img.shields.io/github/stars/Chearle/zifufu-frp-android?style=social)](https://github.com/Chearle/zifufu-frp-android/stargazers)

简体中文 | [English](README_en.md)

</div>

---

## ✨ 特性

| | |
|---|---|
| 🧩 **frpc / frps 双内核** | 内置客户端与服务端两种内核,可同时管理多个穿透与服务端实例 |
| 📝 **多配置管理** | 配置以 TOML 保存,应用内可视化编辑 / 新增 / 删除 / 一键导入导出 |
| ⚡ **快捷设置磁贴** | 下拉通知栏即可一键启停,长按磁贴直达应用 |
| 🤖 **Tasker 插件** | 提供标准 Tasker Action,可接入自动化流程 |
| 📡 **广播接口** | 用 `adb` 或自动化脚本通过广播启停指定配置 |
| 🔌 **ContentProvider** | 外部应用可直接读写配置,便于批量下发 |
| 🚀 **开机自启 + 保活** | 前台服务常驻,支持开机、应用启动、广播三种自启方式 |
| 🛡 **防二次打包** | 编译期固定签名指纹 + 内核文件哈希校验,被重签或替换即拒绝启动 |
| 📦 **分架构 + 通用包** | arm64-v8a / armeabi-v7a / x86_64,也可以选一个通用包走天下 |
| 🌗 **现代界面** | Jetpack Compose + Material 3,支持深色模式与中英文 |

## 📥 下载

前往 [**Releases**](https://github.com/Chearle/zifufu-frp-android/releases) 页面下载最新版本。

每次推送 `v*` 标签,CI 会自动编译并附加以下安装包:

| 安装包 | 适用设备 | 体积 |
|---|---|---|
| `frp_arm64-v8a_*.apk` | 绝大多数现代手机(推荐) | ≈ 14.2 MB |
| `frp_armeabi-v7a_*.apk` | 老旧的 32 位设备 | ≈ 14.8 MB |
| `frp_x86_64_*.apk` | 模拟器、x86 平板 | ≈ 15.5 MB |
| `frp_universal_*.apk` | 不确定机型时选它,全架构通吃 | ≈ 41.4 MB |

> 体积主要来自内置内核,`classes.dex` 仅约 1 MB。装哪个都不会影响功能,只影响体积。

## 🚀 快速开始

1. 安装并打开应用,按引导授予必要的权限(通知、 Android 17+ 还需授予**本地网络**权限)。
2. 在主页点击 **添加配置**,选择 `frpc` 或 `frps`,填写你的 TOML 配置。
3. 回到列表点击启动,连接状态与日志可在应用内实时查看。
4. 需要后台常驻,打开该配置的 **自启动开关**;需要一键启停,可添加 **快捷设置磁贴**。

## 🔧 环境要求

| 项目 | 版本 |
|---|---|
| Android | 6.0 (API 23) 及以上 |
| 编译 JDK | 17 |
| compileSdk / targetSdk | 37 |
| Android Gradle Plugin | 9.2.1 |
| NDK | 27.0.12077973 |

## 🛠 从源码编译

想更换内置的 frp 内核版本,或做二次开发,可以自行编译。

### 方式一:GitHub Actions(推荐)

1. Fork 本仓库。
2. (可选) 配置正式签名密钥:把 keystore 转成 base64 后,在
   `Settings → Secrets and variables → Actions → Repository secrets` 添加
   `KEY_ALIAS`、`KEY_PASSWORD`、`STORE_FILE`(base64 内容)、`STORE_PASSWORD`。
   Linux 下生成 base64:

   ```bash
   base64 -w 0 keystore.jks > keystore.jks.base64
   ```

3. 在 Actions 页面手动触发 **Android CI**,可输入指定的 frp 内核 tag(如 `v0.65.0`);
   留空或 push 提交则使用最新内核。产物在 workflow 的 Artifacts 中。
4. 推送 `v*` 标签时,CI 还会自动创建 Release 并上传全部 APK。

### 方式二:本地命令行

```powershell
# Windows:一键构建通用包(会自动处理非 ASCII 路径问题)
.\build_apk.ps1

# 只构建 arm64 单包
.\build_apk.ps1 -Abis arm64-v8a

# 构建 arm64 + armeabi-v7a
.\build_apk.ps1 -Abis arm64-v8a,armeabi-v7a
```

```bash
# 或直接使用 Gradle
./gradlew :app:assembleRelease -PfrpAbis=arm64-v8a -PfrpUniversal=false
```

产物位于 `app/build/outputs/renamed_apks/release/`。

> ⚠️ **路径不要含中文。** 本机 ninja 使用 ANSI 接口,遇到非 ASCII 路径会报
> `FindFirstFileExA ... syntax is incorrect` 而中断构建。`build_apk.ps1` 会自动把项目
> 映射到 ASCII 盘符再编译;也可以直接把项目放到纯英文路径下。

### 配置正式签名(重要)

未配置签名时,构建会回退到 Android 默认 **debug 证书**。debug 证书是公开的,
任何人都能用它重签你的包,防篡改形同虚设。正式发布请务必配置自己的密钥库:

1. 在项目根目录创建 `keystore.properties`,内容参考 [keystore.example.properties](keystore.example.properties);
2. 或设置环境变量 `KEY_ALIAS` / `KEY_PASSWORD` / `STORE_FILE` / `STORE_PASSWORD`。

应用会在编译期从实际使用的密钥库推导证书指纹并固定进 native 库,运行时校验不通过即退出。
**更换密钥库后必须重新编译**,否则已安装的旧版本会因指纹不匹配而无法启动。

### 更新 frp 内核

内核是编译期打进去的,需要用脚本重新拉取并再次编译:

```bash
# Linux / macOS:下载最新 frp release 并写入 jniLibs
./scripts/update_frp_binaries.sh

# 指定版本
./scripts/update_frp_binaries.sh --tag v0.65.0
```

```powershell
# Windows PowerShell
pwsh ./scripts/update_frp_binaries.ps1
pwsh ./scripts/update_frp_binaries.ps1 -Tag v0.65.0
```

也可以从源码交叉编译,详见 [scripts/README.md](scripts/README.md)。

## ❓ 常见问题

<details>
<summary><b>内置的 frp 内核是怎么来的?</b></summary>

通过 Go 交叉编译直接从 [fatedier/frp](https://github.com/fatedier/frp) 源码构建为 Android
原生库(`GOOS=android`),借助 NDK 的 Clang 工具链完成跨平台编译,支持 arm64-v8a、
armeabi-v7a、x86_64 三种架构。

应用并不是通过 JNI 调用 so 里的函数,而是把这个 so 当作**可执行文件**,用 shell 去执行它。
</details>

<details>
<summary><b>可以在应用内更换 / 热更新 frp 内核吗?</b></summary>

**不能。** [Android 10 起移除了应用主目录的执行权限](https://developer.android.com/about/versions/10/behavior-changes-10#execute-permission),
无法在运行时下载并执行新的内核文件,内核只能在打包时内置。

想换版本请参考上面的「更新 frp 内核」重新编译 APK。应用更新时内核会随新包一起升级。
</details>

<details>
<summary><b>连接重试:第一次登录失败就退出了?</b></summary>

在 frpc 配置里加上 `loginFailExit = false`,第一次登录失败后不会退出,而是持续重试。
适合开机自启动时网络尚未就绪的场景。
</details>

<details>
<summary><b>DNS 解析失败?</b></summary>

所有架构现均使用 `GOOS=android` 编译的内核,已可有效解决 DNS 解析问题。
如仍有异常,可在配置文件中用 `dnsServer` 显式指定 DNS 服务器。
</details>

<details>
<summary><b>Android 17 (API 37) 提示需要本地网络权限?</b></summary>

Android 17+ 新增了 `ACCESS_LOCAL_NETWORK` 权限要求,访问局域网资源需用户明确授权。
首次启动引导页会提示授予,也可以在系统设置中手动开启。未授予时 frp 将无法与局域网设备通信。
</details>

<details>
<summary><b>退到后台就断线 / 被杀后台?</b></summary>

应用已按原生 Android 规范使用前台服务保活,但部分国产系统后台管控更严格,需手动放行。
例如 ColorOS 需在【应用设置 → 耗电管理 → 完全允许后台行为】中开启。
</details>

<details>
<summary><b>杀毒软件报毒?</b></summary>

**属于误报。** 内置的服务端内核(`libzfsvc.so`)本质上是重命名后的 frps 可执行文件,
部分杀软会把这类捆绑的 frp 二进制识别为 `HackTool/Linux.Frpc.a`。它不是病毒,
也不必"处理"——加壳或混淆反而会让误报更严重。
</details>

## 📡 进阶:广播与 ContentProvider

### 广播控制

需在设置中打开「在收到广播时启动 / 关闭」对应开关:

```shell
# 启动 / 停止全部开启了自启动的配置
adb shell am broadcast -a com.zifufu.frp.START com.zifufu.frp
adb shell am broadcast -a com.zifufu.frp.STOP  com.zifufu.frp

# 仅操作指定配置
adb shell am broadcast -a com.zifufu.frp.START -e TYPE frpc -e NAME example.toml com.zifufu.frp
adb shell am broadcast -a com.zifufu.frp.STOP  -e TYPE frpc -e NAME example.toml com.zifufu.frp
```

### 配置读写接口

使用前请在「设置 → frp 配置读写接口」开启读 / 写开关。注意配置中可能包含密码等敏感信息。
通过 ContentProvider 写入的新配置会自动开启自启动。

```shell
# 列出全部配置(需开启"允许读取")
adb shell content query --uri content://com.zifufu.frp.config

# 读取单个配置
adb shell content read --uri content://com.zifufu.frp.config/frpc/example.toml

# 写入 / 删除单个配置(需开启"允许写入")
adb shell content write  --uri content://com.zifufu.frp.config/frpc/example.toml < example.toml
adb shell content delete --uri content://com.zifufu.frp.config/frpc/example.toml
```

## 🛡 安全说明

- **签名固定**:编译期从签名密钥库推导证书 SHA-256 并写入 native 库,运行时比对当前安装包签名,
  不匹配直接退出,防止二次打包。
- **内核校验**:编译期记录各 ABI 内核文件的 SHA-256,运行时校验文件完整性,防止内核被替换。
- **混淆与资源压缩**:Release 构建默认开启 R8 混淆与资源压缩。
- 以上手段只能提高门槛,不能保证绝对安全;请始终从本仓库 Releases 下载安装包。

## 🙏 致谢

- [fatedier/frp](https://github.com/fatedier/frp) —— 强大的内网穿透工具,本项目的内核来源。
- [acedroidx/frp-Android](https://github.com/acedroidx/frp-Android) —— 本项目在其基础上二次开发。
- 以及所有为本项目提供建议与反馈的朋友。

## 📄 许可证

本项目基于 [Apache License 2.0](LICENSE) 开源。

由于源自 `acedroidx/frp-Android`,使用时请一并遵守其许可证条款并保留原始版权声明。

<div align="center"><sub>如果这个项目对你有帮助,欢迎点一个 ⭐ Star</sub></div>
