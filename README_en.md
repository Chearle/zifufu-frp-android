<div align="center">

<img src="icon.png" width="130" alt="Zifufu FRP" />

# Zifufu FRP

**A polished, ready-to-use frp client / server for Android**

[fatedier/frp](https://github.com/fatedier/frp) cross-compiled into Android native libraries.
No root, no Termux — install and go.

[![License](https://img.shields.io/badge/License-Apache--2.0-blue.svg)](LICENSE)
[![Platform](https://img.shields.io/badge/Platform-Android-3DDC84.svg?logo=android&logoColor=white)](https://www.android.com)
[![Min SDK](https://img.shields.io/badge/minSdk-23-orange.svg)](#-requirements)
[![Target SDK](https://img.shields.io/badge/targetSdk-37-orange.svg)](#-requirements)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.3-7F52FF.svg?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4.svg)](https://developer.android.com/jetpack/compose)
[![frp](https://img.shields.io/badge/powered%20by-fatedier%2Ffrp-00ADD8.svg)](https://github.com/fatedier/frp)
[![Stars](https://img.shields.io/github/stars/Chearle/zifufu-frp-android?style=social)](https://github.com/Chearle/zifufu-frp-android/stargazers)

[简体中文](README.md) | English

</div>

---

## ✨ Features

| | |
|---|---|
| 🧩 **frpc + frps** | Both client and server cores are bundled; manage any number of instances |
| 📝 **Multi-config** | Configs are plain TOML, editable / addable / removable inside the app |
| ⚡ **Quick Settings tile** | Start and stop a tunnel straight from the notification shade |
| 🤖 **Tasker plugin** | Standard Tasker actions for automation workflows |
| 📡 **Broadcast control** | Start / stop configs from `adb` or automation scripts |
| 🔌 **ContentProvider** | External apps can read and write configs directly |
| 🚀 **Auto-start & keep-alive** | Foreground service, with boot / app-launch / broadcast triggers |
| 🛡 **Anti-repackaging** | Signature fingerprint pinning plus core-file hash verification |
| 📦 **Per-ABI and universal** | arm64-v8a / armeabi-v7a / x86_64, or one universal build |
| 🌗 **Modern UI** | Jetpack Compose + Material 3, dark mode, Chinese and English |

## 📥 Download

Grab the latest build from [**Releases**](https://github.com/Chearle/zifufu-frp-android/releases).

Every `v*` tag triggers CI, which attaches the following APKs:

| APK | Best for | Size |
|---|---|---|
| `frp_arm64-v8a_*.apk` | Almost all modern phones (recommended) | ≈ 14.2 MB |
| `frp_armeabi-v7a_*.apk` | Older 32-bit devices | ≈ 14.8 MB |
| `frp_x86_64_*.apk` | Emulators and x86 tablets | ≈ 15.5 MB |
| `frp_universal_*.apk` | Not sure? This one runs everywhere | ≈ 41.4 MB |

> Size is dominated by the bundled cores; `classes.dex` is only about 1 MB. The choice only affects size, not features.

## 🚀 Quick start

1. Install and open the app, then follow the onboarding to grant notification permission
   (and **local network** permission on Android 17+).
2. Tap **Add Config** on the home screen, choose `frpc` or `frps`, and paste your TOML config.
3. Start it from the list; connection state and logs are shown live.
4. Enable **auto-start** for background persistence, or add the **Quick Settings tile** for one-tap control.

## 🔧 Requirements

| Item | Version |
|---|---|
| Android | 6.0 (API 23) or newer |
| Build JDK | 17 |
| compileSdk / targetSdk | 37 |
| Android Gradle Plugin | 9.2.1 |
| NDK | 27.0.12077973 |

## 🛠 Building from source

### Option 1: GitHub Actions (recommended)

1. Fork this repository.
2. *(Optional)* Add release signing secrets `KEY_ALIAS`, `KEY_PASSWORD`, `STORE_FILE` (base64
   keystore), `STORE_PASSWORD` under `Settings → Secrets and variables → Actions → Repository secrets`.
   On Linux:

   ```bash
   base64 -w 0 keystore.jks > keystore.jks.base64
   ```

3. Run the **Android CI** workflow manually — you may specify an frp core tag (e.g. `v0.65.0`);
   leaving it blank (or pushing a commit) uses the latest core. Artifacts appear on the run page.
4. Pushing a `v*` tag additionally creates a Release with every APK attached.

### Option 2: Local command line

```powershell
# Windows: build the universal APK (handles non-ASCII paths automatically)
.\build_apk.ps1

# arm64 only
.\build_apk.ps1 -Abis arm64-v8a
```

```bash
# Or use Gradle directly
./gradlew :app:assembleRelease -PfrpAbis=arm64-v8a -PfrpUniversal=false
```

Output lands in `app/build/outputs/renamed_apks/release/`.

> ⚠️ **Avoid non-ASCII paths.** The bundled ninja uses ANSI APIs and fails with
> `FindFirstFileExA ... syntax is incorrect` when the path contains non-ASCII characters.
> `build_apk.ps1` maps the project onto an ASCII drive letter for you.

### Signing (important)

Without a keystore the build falls back to the **Android debug certificate**, which is public —
anyone can re-sign your APK, making the anti-tamper check useless. For production, either create
`keystore.properties` (see [keystore.example.properties](keystore.example.properties)) or set the
environment variables `KEY_ALIAS` / `KEY_PASSWORD` / `STORE_FILE` / `STORE_PASSWORD`.

The certificate fingerprint is derived at build time and pinned into a native library. **Rebuild
after changing the keystore**, otherwise previously installed builds will refuse to start.

### Updating the frp core

The core is baked in at build time. Re-fetch it and rebuild:

```bash
./scripts/update_frp_binaries.sh                 # latest release
./scripts/update_frp_binaries.sh --tag v0.65.0   # specific version
```

```powershell
pwsh ./scripts/update_frp_binaries.ps1
pwsh ./scripts/update_frp_binaries.ps1 -Tag v0.65.0
```

You can also cross-compile from source; see [scripts/README.md](scripts/README.md).

## ❓ FAQ

<details>
<summary><b>Where do the bundled frp cores come from?</b></summary>

They are cross-compiled from the [fatedier/frp](https://github.com/fatedier/frp) sources with
`GOOS=android` using the NDK Clang toolchain, for arm64-v8a, armeabi-v7a and x86_64.

The libraries are not called through JNI. Each `.so` is treated as an **executable** and launched
from a shell.
</details>

<details>
<summary><b>Can I swap or hot-update the frp core inside the app?</b></summary>

**No.** [Android 10 removed execute permission for the app home directory](https://developer.android.com/about/versions/10/behavior-changes-10#execute-permission),
so a downloaded core cannot be executed at runtime. The core must be bundled at build time.

To change versions, rebuild the APK as described above. App updates carry the new core with them.
</details>

<details>
<summary><b>The first login fails and the tunnel exits immediately.</b></summary>

Add `loginFailExit = false` to your frpc config. This keeps retrying instead of exiting when the
first login fails — handy for boot-time auto-start before the network is ready.
</details>

<details>
<summary><b>DNS resolution fails.</b></summary>

All architectures now use cores built with `GOOS=android`, which resolves the common DNS issues.
If problems persist, set `dnsServer` explicitly in your config.
</details>

<details>
<summary><b>Android 17 (API 37) asks for local network permission.</b></summary>

Android 17+ requires `ACCESS_LOCAL_NETWORK` for LAN access. The onboarding screen prompts for it,
and it can also be granted from system settings. Without it, frp cannot reach LAN devices.
</details>

<details>
<summary><b>Antivirus flags the app.</b></summary>

**False positive.** The bundled server core (`libzfsvc.so`) is a renamed `frps` executable, and some
engines classify bundled frp binaries as `HackTool/Linux.Frpc.a`. It is not malware, and there is
nothing to "fix" — obfuscation only makes the false positive worse.
</details>

## 📡 Advanced: broadcasts and ContentProvider

Enable the corresponding switches in Settings first.

```shell
# Broadcast control
adb shell am broadcast -a com.zifufu.frp.START com.zifufu.frp
adb shell am broadcast -a com.zifufu.frp.STOP  com.zifufu.frp
adb shell am broadcast -a com.zifufu.frp.START -e TYPE frpc -e NAME example.toml com.zifufu.frp

# Config access
adb shell content query  --uri content://com.zifufu.frp.config
adb shell content read   --uri content://com.zifufu.frp.config/frpc/example.toml
adb shell content write  --uri content://com.zifufu.frp.config/frpc/example.toml < example.toml
adb shell content delete --uri content://com.zifufu.frp.config/frpc/example.toml
```

## 🛡 Security

- **Signature pinning** — the signing certificate SHA-256 is derived at build time, embedded in a
  native library, and verified at runtime; a mismatch terminates the app.
- **Core integrity** — per-ABI SHA-256 hashes of the bundled cores are verified at runtime.
- **R8 obfuscation and resource shrinking** are enabled for release builds.

These measures raise the bar; they are not absolute. Always download from this repository's Releases.

## 🙏 Credits

- [fatedier/frp](https://github.com/fatedier/frp) — the tunneling tool this project is built on.
- [acedroidx/frp-Android](https://github.com/acedroidx/frp-Android) — the upstream project this
  app is derived from.

## 📄 License

Released under the [Apache License 2.0](LICENSE). As a derivative of `acedroidx/frp-Android`,
please also comply with its license and retain the original copyright notices.

<div align="center"><sub>If this project helps you, consider leaving a ⭐ Star</sub></div>
