# Android 防詐安全 App MVP

這是一個 Android Kotlin MVP，目標是保護長輩避開社群廣告後面的攻擊鏈：跳轉釣魚網站、APK / XAPK / APKS 下載、未知來源安裝與高風險權限授權。

## MVP 範圍

- 不修改 FB、IG、TikTok、LINE 或瀏覽器內部廣告內容。
- 使用本地規則，不串接雲端 API。
- 透過 `VpnService` 建立 DNS 過濾型防護，阻擋高風險網域解析。
- 透過分享網址、瀏覽器開啟網址、主畫面貼上網址，顯示全螢幕風險警告。
- 高風險與阻擋級操作需要家人管理 PIN 才能繼續。
- UI 採大字、紅黃綠提示，優先照顧長輩可讀性。

## 專案架構

```text
.
├── settings.gradle.kts
├── build.gradle.kts
├── app
│   ├── build.gradle.kts
│   └── src/main
│       ├── AndroidManifest.xml
│       ├── java/com/example/antiscamsafety
│       │   ├── rules
│       │   │   ├── PermissionRiskCatalog.kt
│       │   │   ├── RiskLevel.kt
│       │   │   ├── RiskResult.kt
│       │   │   ├── RuleSet.kt
│       │   │   └── UrlRiskAnalyzer.kt
│       │   ├── security
│       │   │   └── PinManager.kt
│       │   ├── ui
│       │   │   ├── MainActivity.kt
│       │   │   ├── UiKit.kt
│       │   │   ├── UrlCheckActivity.kt
│       │   │   └── WarningActivity.kt
│       │   └── vpn
│       │       └── DnsFilterVpnService.kt
│       └── res
│           ├── drawable
│           ├── mipmap-anydpi-v26
│           └── values
```

## 主要檔案

- `UrlRiskAnalyzer.kt`：本地風險分數引擎。
- `RuleSet.kt`：白名單、短網址、黑名單高風險字詞、APK 副檔名規則。
- `DnsFilterVpnService.kt`：DNS 過濾型 `VpnService`，高風險網域回 NXDOMAIN，一般網域轉送到 `1.1.1.1`。
- `WarningActivity.kt`：全螢幕風險提示與 PIN 驗證。
- `MainActivity.kt`：大字主畫面、VPN 啟停、網址檢查、PIN 設定、未知來源安裝設定入口。
- `PinManager.kt`：本地 PIN 雜湊儲存與驗證。

## 風險分數

- `0-30`：安全，綠色。
- `31-60`：可疑，黃色。
- `61-80`：高風險，紅色，需要 PIN。
- `81-100`：阻擋，紅色，建議不要繼續。

加分規則包含：

- 短網址：`bit.ly`、`tinyurl.com`、`reurl.cc`、`lihi.cc` 等。
- APK 下載：`.apk`、`.xapk`、`.apks`。
- 高風險字：`airdrop`、`claim`、`wallet`、`verify`、`bonus`、`investment`、`teacher`、`line group`、`seed phrase`、`private key`、`投資`、`老師`、`交易所` 等。
- 非 HTTPS、疑似仿冒交易所/客服頁、國際化仿冒網域、大量連字號網域。

白名單包含 Google、YouTube、LINE、台灣政府網站、常用銀行、momo、PChome 等常見安全目的地。

## AndroidManifest 與權限

目前宣告：

- `INTERNET`：DNS 轉送到上游解析器。
- `FOREGROUND_SERVICE`：VPN 服務需以前景服務執行。
- `FOREGROUND_SERVICE_SPECIAL_USE`：Android 14+ 前景服務類型說明。
- `POST_NOTIFICATIONS`：Android 13+ 顯示 VPN 執行通知。
- `BIND_VPN_SERVICE`：綁定 `VpnService`，只能由系統授權。

`UrlCheckActivity` 註冊：

- `ACTION_SEND text/plain`：可從 LINE、瀏覽器、社群 App 分享網址到本 App 檢查。
- `ACTION_VIEW http/https`：可在 Android 開啟網址選單中選擇本 App 檢查。

## 測試步驟

1. 用 Android Studio 開啟專案。
2. Sync Gradle，安裝到 Android 8.0+ 測試機或模擬器。
3. 開啟 App，先設定家人管理 PIN，例如 `2468`。
4. 點選「開啟 DNS 防護 VPN」，接受 Android VPN 授權。
5. 在主畫面貼上安全網址：
   - `https://www.google.com`
   - 預期：綠色安全。
6. 貼上可疑短網址：
   - `https://bit.ly/claim-wallet-bonus`
   - 預期：可疑或高風險。
7. 貼上 APK 下載網址：
   - `http://example.com/app-release.apk`
   - 預期：高風險或阻擋，需要 PIN。
8. 貼上假投資/假交易所網址：
   - `https://vip-crypto-exchange-verify.example/bonus`
   - 預期：高風險或阻擋。
9. 從 LINE 或瀏覽器分享一段含網址的文字到「防詐安全 MVP」。
   - 預期：直接進入全螢幕風險頁。
10. 測試未知來源安裝設定入口。
   - 預期：顯示提醒並開啟 Android 未知來源設定頁。

## 打包與安裝指令

改完程式後，在專案根目錄執行：

```powershell
.\build-apk.ps1
```

輸出 APK 會放在：

```text
dist\anti-scam-safety-mvp-debug.apk
```

如果手機已開啟 USB 偵錯並連上電腦，可直接安裝：

```powershell
.\install-apk.ps1
```

也可以手動把 APK 傳到手機下載或聊天軟體，再從手機點開安裝。

## 已知限制

- DNS VPN 只能根據網域與 DNS 查詢阻擋，無法讀取 HTTPS 頁面內容。
- 若 App 使用 DoH、內建代理、硬編碼 IP，DNS 層可能看不到完整目標。
- Android 一般 App 無法直接攔截所有「未知來源安裝」與「高風險權限授權」系統流程；MVP 先提供入口提醒與 APK 連結偵測。商業版可考慮 Accessibility、Device Owner / MDM 或 Family 管理模式。
- `ACTION_VIEW` 需要使用者選擇本 App 作為檢查器，不會強制攔截所有瀏覽器跳轉。

## 未來擴充方向

- Google Safe Browsing API。
- AI 網頁內容與截圖分析。
- 家人遠端管理與白名單同步。
- 政府 165 或可信威脅情資匯入。
- Accessibility 輔助偵測未知來源安裝、權限頁與高風險關鍵字畫面。
- 更完整的 VPN 封包代理或 DoH/DoT 策略。
