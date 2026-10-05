cmake_minimum_required(VERSION 3.22.1)
project(zfsig CXX)

add_library(zfsig SHARED zfsig.cpp)

target_compile_options(zfsig PRIVATE -O2 -fvisibility=hidden)
target_link_options(zfsig PRIVATE -Wl,--build-id=none)

# 期望的签名证书 SHA-256（十六进制）。构建时由 Gradle 依据实际使用的签名密钥库注入，
# 这里不再落盘到源码，避免把固定值暴露在仓库里。
if(DEFINED EXPECTED_SIGNER_SHA256 AND NOT "${EXPECTED_SIGNER_SHA256}" STREQUAL "")
    target_compile_definitions(zfsig PRIVATE EXPECTED_SIGNER_SHA256="${EXPECTED_SIGNER_SHA256}")
endif()
