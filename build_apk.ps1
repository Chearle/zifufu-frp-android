<#
  Build the frp-Android release APK.

  Default output is the universal APK (all ABIs). If the project lives under a
  non-ASCII path, the script builds through a temporary ASCII drive letter,
  because ninja encodes the source directory in the ANSI code page and fails
  with a FindFirstFileExA error on such paths.

  Usage:
    .\build_apk.ps1                       # universal (arm64 + armeabi-v7a + x86_64)
    .\build_apk.ps1 -Abis arm64-v8a       # arm64 only
    .\build_apk.ps1 -Abis arm64-v8a,armeabi-v7a
#>
param(
    [string]$Abis = ""
)
$ErrorActionPreference = "Stop"

$projectDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$parentDir  = Split-Path -Parent $projectDir
$leaf       = Split-Path -Leaf $projectDir

# Only the common prefix of every absolute path matters for ninja.
$needAscii = ($parentDir.ToCharArray() | Where-Object { [int]$_ -gt 127 }).Count -gt 0

$buildDir = $projectDir
if ($needAscii) {
    # Always use R: so the absolute build paths stay stable between runs; a
    # varying drive letter would invalidate the CMake cache every time.
    $drive = "R"
    if (Test-Path "${drive}:\") {
        if (-not (Test-Path "${drive}:\$leaf\build_apk.ps1")) {
            throw "Drive ${drive}: is used by another mapping. Free it (subst ${drive}: /D) and retry."
        }
    } else {
        subst "${drive}:" "$parentDir"
        Write-Host "Non-ASCII path detected, mapped ${drive}: to $parentDir" -ForegroundColor Yellow
    }
    $buildDir = "${drive}:\$leaf"
    Write-Host "Building from $buildDir`n" -ForegroundColor Yellow
}

if ($Abis -ne "") {
    $gradleArgs = @(":app:assembleRelease", "-PfrpAbis=$Abis", "-PfrpUniversal=false", "--console=plain")
} else {
    $gradleArgs = @(":app:assembleRelease", "-PfrpUniversal=true", "--console=plain")
}

Push-Location $buildDir
try {
    & ".\gradlew.bat" @gradleArgs
    if ($LASTEXITCODE -ne 0) { throw "Gradle build failed with exit code $LASTEXITCODE" }
    Write-Host "`nOutput: $buildDir\app\build\outputs\renamed_apks\release" -ForegroundColor Green
} finally {
    Pop-Location
}
