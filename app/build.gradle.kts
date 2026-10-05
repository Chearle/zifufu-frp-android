import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.dsl.ApkSigningConfig
import com.android.build.api.variant.BuiltArtifactsLoader
import com.android.build.api.variant.FilterConfiguration.FilterType.ABI
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.register
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import java.security.KeyStore
import java.util.Properties

abstract class CopyRenamedApksTask : DefaultTask() {

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val input: DirectoryProperty

    @get:OutputDirectory
    abstract val output: DirectoryProperty

    @get:Input
    abstract val versionName: Property<String>

    @get:Internal
    abstract val builtArtifactsLoader: Property<BuiltArtifactsLoader>

    @TaskAction
    fun copyRenamedApks() {
        val outputDirectory = output.get().asFile
        outputDirectory.deleteRecursively()
        outputDirectory.mkdirs()

        val builtArtifacts = builtArtifactsLoader.get().load(input.get())
            ?: throw GradleException("Cannot load APK metadata from ${input.get().asFile}")

        builtArtifacts.elements.forEach { artifact ->
            val abi = artifact.filters.find { it.filterType == ABI }?.identifier ?: "universal"
            val resolvedVersionName = artifact.versionName?.takeIf { it.isNotBlank() } ?: versionName.get()

            // AGP 9 不再支持通过旧 Variant API 直接修改 APK 文件名，这里在产物生成后复制一份约定命名的 APK。
            File(artifact.outputFile).copyTo(
                target = outputDirectory.resolve("frp_${abi}_${resolvedVersionName}.apk"),
                overwrite = true,
            )
        }
    }
}

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.parcelize")
}

fun sha256Hex(file: File): String {
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

fun jniSha256List(libName: String): String {
    val abis = listOf("arm64-v8a", "armeabi-v7a", "x86_64")
    return abis.joinToString(",") { abi ->
        val f = file("src/main/jniLibs/$abi/$libName")
        check(f.isFile) { "缺少内核文件: ${f.path}" }
        sha256Hex(f)
    }
}

/**
 * 读取签名密钥库中的证书并计算 SHA-256（hex）。
 * 该值会被固定进应用，用于运行时校验安装包签名，防止二次打包。
 */
fun signerCertSha256(storeFile: File?, storePassword: String?, alias: String?): String? {
    if (storeFile == null || !storeFile.isFile || storePassword.isNullOrEmpty() || alias.isNullOrEmpty()) {
        return null
    }
    for (type in listOf("PKCS12", "JKS")) {
        try {
            val keyStore = KeyStore.getInstance(type)
            FileInputStream(storeFile).use { keyStore.load(it, storePassword.toCharArray()) }
            val cert = keyStore.getCertificate(alias) ?: continue
            return MessageDigest.getInstance("SHA-256")
                .digest(cert.encoded)
                .joinToString("") { b -> "%02x".format(b) }
        } catch (_: Exception) {
            // 换下一种 key store 类型重试
        }
    }
    return null
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}

val fileSigningAvailable = keystorePropertiesFile.exists() && listOf(
    "keyAlias", "keyPassword", "storeFile", "storePassword"
).all { !keystoreProperties.getProperty(it).isNullOrBlank() }

val envSigningAvailable = listOf("KEY_ALIAS", "KEY_PASSWORD", "STORE_FILE", "STORE_PASSWORD").all {
    !System.getenv(it).isNullOrBlank()
}

// 编译期固定"官方签名"：从实际使用的签名密钥库推导证书 SHA-256。
// 未配置正式签名时回退到本机 debug 证书，保证本地构建也能通过校验。
val signerCertSha256Value: String = run {
    val fromSigningConfig: String? = when {
        fileSigningAvailable -> signerCertSha256(
            file(keystoreProperties.getProperty("storeFile")),
            keystoreProperties.getProperty("storePassword"),
            keystoreProperties.getProperty("keyAlias"),
        )

        envSigningAvailable -> signerCertSha256(
            if (System.getenv("STORE_FILE")?.isNotBlank() == true) {
                file("../keystore.jks")
            } else {
                file(System.getenv("STORE_FILE"))
            },
            System.getenv("STORE_PASSWORD"),
            System.getenv("KEY_ALIAS"),
        )

        else -> null
    }
    fromSigningConfig
        ?: signerCertSha256(
            File(System.getProperty("user.home"), ".android/debug.keystore"),
            "android",
            "androiddebugkey",
        )
        ?: ""
}

// 体积控制：
//   -PfrpAbis=arm64-v8a            只打包指定 ABI（逗号分隔），默认全部三种
//   -PfrpUniversal=false           不额外产出打包所有 ABI 的 universal 包
val frpAbis: List<String> = (findProperty("frpAbis") as String?)
    ?.split(',')?.map { it.trim() }?.filter { it.isNotEmpty() }?.takeIf { it.isNotEmpty() }
    ?: listOf("arm64-v8a", "x86_64", "armeabi-v7a")
val frpUniversal: Boolean = (findProperty("frpUniversal") as String?)?.toBoolean() ?: true

android {
    // 需要 NDK 编译签名校验库 libzfsig.so
    ndkVersion = "27.0.12077973"

    androidResources {
        generateLocaleConfig = true
        localeFilters += listOf("en", "zh")
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    lateinit var releaseSigning: ApkSigningConfig

    signingConfigs {
        val aceSigning = when {
            fileSigningAvailable -> {
                create("AceKeystore") {
                    keyAlias = keystoreProperties.getProperty("keyAlias")
                    keyPassword = keystoreProperties.getProperty("keyPassword")
                    storeFile = file(keystoreProperties.getProperty("storeFile"))
                    storePassword = keystoreProperties.getProperty("storePassword")
                }
            }

            envSigningAvailable -> {
                create("AceKeystore") {
                    keyAlias = System.getenv("KEY_ALIAS")
                    keyPassword = System.getenv("KEY_PASSWORD")
                    storeFile = if (System.getenv("STORE_FILE")?.isNotBlank() == true) {
                        // CI 跑脚本会生成 keystore.jks
                        file("../keystore.jks")
                    } else {
                        file(System.getenv("STORE_FILE"))
                    }
                    storePassword = System.getenv("STORE_PASSWORD")
                }
            }

            else -> null
        }

        // 没有提供签名信息时，回退到 Android 默认的 debug 签名，保证本地/CI 可编译
        releaseSigning = aceSigning ?: getByName("debug")
    }

    defaultConfig {
        applicationId = "com.zifufu.frp"
        minSdk = 23
        targetSdk = 37
        compileSdk = 37
        versionCode = 22
        versionName = "1.3.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        signingConfig = releaseSigning

        buildConfigField("String", "FrpcFileName", "\"libzfcore.so\"")
        buildConfigField("String", "FrpsFileName", "\"libzfsvc.so\"")
        buildConfigField("String", "FrpcConfigFileName", "\"frpc.toml\"")
        buildConfigField("String", "FrpsConfigFileName", "\"frps.toml\"")
        // 编译期写入各 ABI 内核哈希；jniLibs 变更后必须重新配置，避免沿用旧哈希误报篡改
        buildConfigField("String", "FrpcSha256List", "\"${jniSha256List("libzfcore.so")}\"")
        buildConfigField("String", "FrpsSha256List", "\"${jniSha256List("libzfsvc.so")}\"")
        // 固定官方签名证书指纹，运行时校验，防二次打包
        buildConfigField("String", "SignerCertSha256", "\"$signerCertSha256Value\"")

        externalNativeBuild {
            cmake {
                // 传给 libzfsig.so 的编译期常量，避免把指纹明文写进 DEX
                arguments += "-DEXPECTED_SIGNER_SHA256=$signerCertSha256Value"
                // 关闭 CMake 自带的 ninja 重配置规则：该规则会在每次构建时 stat 源码目录，
                // 而本机 ninja 使用 ANSI 接口，路径含中文时会直接报错导致构建失败。
                // 配置时机仍由 AGP 的 configureCMake 任务负责，改动 CMakeLists 也能正常重配。
                arguments += "-DCMAKE_SUPPRESS_REGENERATION=ON"
            }
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                // Includes the default ProGuard rules files that are packaged with
                // the Android Gradle plugin. To learn more, go to the section about
                // R8 configuration files.
                getDefaultProguardFile("proguard-android-optimize.txt"),
                // Includes a local, custom Proguard rules file
                "proguard-rules.pro"
            )
            signingConfig = releaseSigning
            ndk {
                // 与 keepDebugSymbols 一起避免 stripReleaseDebugSymbols 改写内核
                debugSymbolLevel = "none"
            }
        }
        getByName("debug") {
            signingConfig = releaseSigning
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    packaging {
        jniLibs {
            useLegacyPackaging = true
            // 禁止 AGP 再 strip 内核，否则哈希与安装后的 .so 不一致，会误报篡改
            keepDebugSymbols.add("**/*.so")
        }
    }
    splits {
        abi {
            isEnable = true
            reset()
            include(*frpAbis.toTypedArray())
            isUniversalApk = frpUniversal
        }
    }
    namespace = "io.github.acedroidx.frp"

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
        }
    }

}

androidComponents {
    onVariants { variant ->
        val taskName = "copyRenamedApksFor${variant.name.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }}"
        val copyRenamedApks = tasks.register<CopyRenamedApksTask>(taskName) {
            output.set(layout.buildDirectory.dir("outputs/renamed_apks/${variant.name}"))
            versionName.set(variant.outputs.first().versionName)
            builtArtifactsLoader.set(variant.artifacts.getBuiltArtifactsLoader())
        }

        variant.artifacts.use(copyRenamedApks).wiredWith {
            it.input
        }.toListenTo(SingleArtifact.APK)
    }
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib:2.3.21")
    implementation("androidx.core:core-ktx:1.18.0")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("com.google.android.material:material:1.13.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")

    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.10.0")
    implementation("androidx.lifecycle:lifecycle-service:2.10.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")

    val composeBom = platform("androidx.compose:compose-bom:2026.04.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation("androidx.compose.material3:material3")
    // Android Studio Preview support
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    // UI Tests
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    // Optional - Integration with activities
    implementation("androidx.activity:activity-compose")

    // Tasker Plugin Library
    implementation("com.joaomgcd:taskerpluginlibrary:0.4.10")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
}
