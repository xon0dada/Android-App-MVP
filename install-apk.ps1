$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$adb = Join-Path $root ".tools\android-sdk\platform-tools\adb.exe"
$apk = Join-Path $root "dist\anti-scam-safety-mvp-debug.apk"

if (!(Test-Path $adb)) {
    throw "找不到 adb：$adb"
}

if (!(Test-Path $apk)) {
    throw "找不到 APK，請先執行 .\build-apk.ps1"
}

& $adb devices
& $adb install -r $apk
