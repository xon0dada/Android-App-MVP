$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$androidStudioJbr = "C:\Program Files\Android\Android Studio\jbr"
$gradleBat = "C:\Users\os\.gradle\wrapper\dists\gradle-8.10.2-bin\a04bxjujx95o3nb99gddekhwo\gradle-8.10.2\bin\gradle.bat"
$apkSource = Join-Path $root "app\build\outputs\apk\debug\app-debug.apk"
$apkOutputDir = Join-Path $root "dist"
$apkOutput = Join-Path $apkOutputDir "anti-scam-safety-mvp-debug.apk"

if (!(Test-Path $androidStudioJbr)) {
    throw "找不到 Android Studio JBR：$androidStudioJbr"
}

if (!(Test-Path $gradleBat)) {
    throw "找不到 Gradle：$gradleBat"
}

if (!(Test-Path $apkOutputDir)) {
    New-Item -ItemType Directory -Path $apkOutputDir | Out-Null
}

$env:JAVA_HOME = $androidStudioJbr
Push-Location $root
try {
    & $gradleBat assembleDebug --offline
    Copy-Item -Path $apkSource -Destination $apkOutput -Force
    Write-Host ""
    Write-Host "APK 已產生：" -ForegroundColor Green
    Write-Host $apkOutput
} finally {
    Pop-Location
}
