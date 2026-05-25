package com.example.antiscamsafety.ui

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ScrollView
import android.widget.Toast
import com.example.antiscamsafety.rules.PermissionRiskCatalog
import com.example.antiscamsafety.rules.UrlRiskAnalyzer
import com.example.antiscamsafety.security.PinManager
import com.example.antiscamsafety.vpn.DnsFilterVpnService

class MainActivity : Activity() {
    private val analyzer = UrlRiskAnalyzer()
    private lateinit var pinManager: PinManager
    private lateinit var urlInput: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pinManager = PinManager(this)
        render()
    }

    private fun render() {
        val root = UiKit.verticalRoot(this)
        root.addView(UiKit.title(this, "防詐安全 MVP"))
        root.addView(UiKit.body(this, "保護長輩避開釣魚網站、假投資、假交易所與 APK 下載。"))

        root.addView(UiKit.button(this, "開啟 DNS 防護 VPN", UiKit.GREEN).apply {
            setOnClickListener { requestVpn() }
        })

        root.addView(UiKit.button(this, "停止 DNS 防護", UiKit.DARK).apply {
            setOnClickListener {
                startService(Intent(this@MainActivity, DnsFilterVpnService::class.java).setAction(DnsFilterVpnService.ACTION_STOP))
            }
        })

        urlInput = EditText(this).apply {
            hint = "貼上可疑網址"
            textSize = 22f
            minHeight = 88
            inputType = android.text.InputType.TYPE_TEXT_VARIATION_URI
            imeOptions = EditorInfo.IME_ACTION_DONE
        }
        root.addView(urlInput)

        root.addView(UiKit.button(this, "檢查網址", UiKit.YELLOW).apply {
            setOnClickListener { openResult(urlInput.text?.toString().orEmpty()) }
        })

        root.addView(UiKit.button(this, "設定家人管理 PIN", UiKit.DARK).apply {
            setOnClickListener { showSetPinDialog() }
        })

        root.addView(UiKit.button(this, "檢查未知來源安裝設定", UiKit.RED).apply {
            setOnClickListener {
                Toast.makeText(this@MainActivity, "請確認瀏覽器與通訊軟體不要允許安裝未知 App", Toast.LENGTH_LONG).show()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES))
                }
            }
        })

        val risks = PermissionRiskCatalog.highRiskPermissions.joinToString("\n") { "• $it" }
        root.addView(UiKit.body(this, "高風險操作：\n$risks"))

        setContentView(ScrollView(this).apply { addView(root) })
    }

    private fun requestVpn() {
        val prepareIntent = VpnService.prepare(this)
        if (prepareIntent != null) {
            startActivityForResult(prepareIntent, REQ_VPN)
        } else {
            startVpn()
        }
    }

    private fun startVpn() {
        val intent = Intent(this, DnsFilterVpnService::class.java).setAction(DnsFilterVpnService.ACTION_START)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent) else startService(intent)
    }

    @Deprecated("Kept for a small framework-only MVP.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_VPN && resultCode == RESULT_OK) startVpn()
    }

    private fun openResult(target: String) {
        val result = analyzer.analyze(target)
        val intent = Intent(this, WarningActivity::class.java)
            .putExtra(WarningActivity.EXTRA_TARGET, result.normalizedTarget)
            .putExtra(WarningActivity.EXTRA_SCORE, result.score)
            .putExtra(WarningActivity.EXTRA_LEVEL, result.level.name)
            .putStringArrayListExtra(WarningActivity.EXTRA_REASONS, ArrayList(result.reasons))
        startActivity(intent)
    }

    private fun showSetPinDialog() {
        val input = EditText(this).apply {
            hint = "輸入 4 到 8 位數 PIN"
            textSize = 22f
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
        }
        android.app.AlertDialog.Builder(this)
            .setTitle("設定家人管理 PIN")
            .setView(input)
            .setPositiveButton("儲存") { _, _ ->
                val ok = pinManager.setPin(input.text?.toString().orEmpty())
                Toast.makeText(this, if (ok) "PIN 已設定" else "PIN 需為 4 到 8 位數字", Toast.LENGTH_LONG).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private companion object {
        const val REQ_VPN = 12
    }
}
