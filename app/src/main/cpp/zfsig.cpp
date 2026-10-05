#include <jni.h>

#include <cstdint>
#include <cstring>

// 由 CMake 依据 Gradle 注入的签名证书 SHA-256（64 个 hex 字符）。
// 为空表示未启用签名固定（例如开发构建），此时不阻断启动。
#ifndef EXPECTED_SIGNER_SHA256
#define EXPECTED_SIGNER_SHA256 ""
#endif

namespace {

int hexNibble(char c) {
    if (c >= '0' && c <= '9') return c - '0';
    if (c >= 'a' && c <= 'f') return c - 'a' + 10;
    if (c >= 'A' && c <= 'F') return c - 'A' + 10;
    return -1;
}

// 常量时间比较，避免通过执行耗时侧信道推断正确的签名指纹
bool constantTimeEquals(const uint8_t* a, const uint8_t* b, size_t n) {
    uint8_t diff = 0;
    for (size_t i = 0; i < n; ++i) {
        diff = static_cast<uint8_t>(diff | static_cast<uint8_t>(a[i] ^ b[i]));
    }
    return diff == 0;
}

}  // namespace

extern "C" JNIEXPORT jboolean JNICALL
Java_io_github_acedroidx_frp_SignatureGuard_nativeVerifyDigest(
        JNIEnv* env, jobject /*thiz*/, jbyteArray digest) {
    const char* expectedHex = EXPECTED_SIGNER_SHA256;
    if (expectedHex == nullptr || expectedHex[0] == '\0') {
        return JNI_TRUE;  // 未固定签名，不阻断（开发构建）
    }
    if (std::strlen(expectedHex) != 64) {
        return JNI_TRUE;  // 配置异常时不做砖化处理
    }
    if (digest == nullptr || env->GetArrayLength(digest) != 32) {
        return JNI_FALSE;
    }

    uint8_t expected[32];
    for (int i = 0; i < 32; ++i) {
        const int hi = hexNibble(expectedHex[i * 2]);
        const int lo = hexNibble(expectedHex[i * 2 + 1]);
        if (hi < 0 || lo < 0) {
            return JNI_TRUE;  // 配置异常时不做砖化处理
        }
        expected[i] = static_cast<uint8_t>((hi << 4) | lo);
    }

    jbyte* raw = env->GetByteArrayElements(digest, nullptr);
    if (raw == nullptr) {
        return JNI_FALSE;
    }
    const bool ok = constantTimeEquals(
            reinterpret_cast<const uint8_t*>(raw), expected, sizeof(expected));
    env->ReleaseByteArrayElements(digest, raw, JNI_ABORT);
    return ok ? JNI_TRUE : JNI_FALSE;
}
