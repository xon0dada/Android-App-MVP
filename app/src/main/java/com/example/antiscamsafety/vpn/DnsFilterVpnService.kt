package com.example.antiscamsafety.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import com.example.antiscamsafety.ui.MainActivity
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.concurrent.atomic.AtomicBoolean

class DnsFilterVpnService : VpnService() {
    private val running = AtomicBoolean(false)
    private var vpnInterface: ParcelFileDescriptor? = null
    private var worker: Thread? = null
    private val packetHandler = DnsPacketHandler { socket -> protect(socket) }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> stopVpn()
            else -> startVpn()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        stopVpn()
        super.onDestroy()
    }

    private fun startVpn() {
        if (running.get()) return
        startForeground(NOTIFICATION_ID, buildNotification())
        vpnInterface = Builder()
            .setSession("防詐 DNS 防護")
            .addAddress("10.88.0.2", 32)
            .addDnsServer(UPSTREAM_DNS)
            .addRoute(UPSTREAM_DNS, 32)
            .setBlocking(true)
            .establish()

        val descriptor = vpnInterface ?: return
        running.set(true)
        worker = Thread {
            FileInputStream(descriptor.fileDescriptor).use { input ->
                FileOutputStream(descriptor.fileDescriptor).use { output ->
                    val buffer = ByteArray(32767)
                    while (running.get()) {
                        val length = input.read(buffer)
                        if (length > 0) {
                            packetHandler.handle(buffer, length)?.let { output.write(it) }
                        }
                    }
                }
            }
        }.apply {
            name = "anti-scam-dns-vpn"
            start()
        }
    }

    private fun stopVpn() {
        running.set(false)
        vpnInterface?.close()
        vpnInterface = null
        worker = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun buildNotification(): Notification {
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "防詐 DNS 防護", NotificationManager.IMPORTANCE_LOW),
            )
        }
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }
        return builder
            .setContentTitle("防詐 DNS 防護已開啟")
            .setContentText("正在用本地規則阻擋高風險網域")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(contentIntent)
            .build()
    }

    companion object {
        const val ACTION_START = "com.example.antiscamsafety.START"
        const val ACTION_STOP = "com.example.antiscamsafety.STOP"
        private const val CHANNEL_ID = "anti_scam_dns"
        private const val NOTIFICATION_ID = 701
        private const val UPSTREAM_DNS = "1.1.1.1"
    }
}

private class DnsPacketHandler(
    private val protectSocket: (DatagramSocket) -> Boolean,
) {
    private val analyzer = com.example.antiscamsafety.rules.UrlRiskAnalyzer()
    private val upstream = InetAddress.getByName("1.1.1.1")

    fun handle(packet: ByteArray, length: Int): ByteArray? {
        val parsed = Ipv4UdpDnsPacket.parse(packet, length) ?: return null
        val query = DnsMessage.parseQuery(parsed.dnsPayload) ?: return null
        val risk = analyzer.analyze(query.host)
        val responseDns = if (risk.score >= 61) {
            DnsMessage.nxDomain(parsed.dnsPayload, query.questionEnd)
        } else {
            forwardDns(parsed.dnsPayload) ?: DnsMessage.serverFailure(parsed.dnsPayload, query.questionEnd)
        }
        return parsed.buildUdpResponse(responseDns)
    }

    private fun forwardDns(payload: ByteArray): ByteArray? {
        return runCatching {
            DatagramSocket().use { socket ->
                protectSocket(socket)
                socket.soTimeout = 2000
                socket.send(DatagramPacket(payload, payload.size, upstream, 53))
                val response = ByteArray(4096)
                val packet = DatagramPacket(response, response.size)
                socket.receive(packet)
                response.copyOf(packet.length)
            }
        }.getOrNull()
    }
}

private data class DnsQuery(val host: String, val questionEnd: Int)

private object DnsMessage {
    fun parseQuery(payload: ByteArray): DnsQuery? {
        if (payload.size < 12) return null
        val labels = mutableListOf<String>()
        var index = 12
        while (index < payload.size) {
            val size = payload[index].toInt() and 0xff
            if (size == 0) {
                index += 1
                break
            }
            if (size > 63 || index + size >= payload.size) return null
            labels += payload.copyOfRange(index + 1, index + 1 + size).toString(Charsets.UTF_8)
            index += size + 1
        }
        val questionEnd = index + 4
        if (labels.isEmpty() || questionEnd > payload.size) return null
        return DnsQuery(labels.joinToString("."), questionEnd)
    }

    fun nxDomain(queryPayload: ByteArray, questionEnd: Int): ByteArray = response(queryPayload, questionEnd, rcode = 3)

    fun serverFailure(queryPayload: ByteArray, questionEnd: Int): ByteArray = response(queryPayload, questionEnd, rcode = 2)

    private fun response(queryPayload: ByteArray, questionEnd: Int, rcode: Int): ByteArray {
        val end = questionEnd.coerceIn(12, queryPayload.size)
        val out = queryPayload.copyOf(end)
        out[2] = 0x81.toByte()
        out[3] = (0x80 or rcode).toByte()
        out[6] = 0
        out[7] = 0
        out[8] = 0
        out[9] = 0
        out[10] = 0
        out[11] = 0
        return out
    }
}

private data class Ipv4UdpDnsPacket(
    val sourceIp: ByteArray,
    val destinationIp: ByteArray,
    val sourcePort: Int,
    val destinationPort: Int,
    val dnsPayload: ByteArray,
) {
    fun buildUdpResponse(responseDns: ByteArray): ByteArray {
        val totalLength = 20 + 8 + responseDns.size
        val out = ByteArray(totalLength)
        out[0] = 0x45
        out[1] = 0
        writeShort(out, 2, totalLength)
        writeShort(out, 4, 0)
        writeShort(out, 6, 0)
        out[8] = 64
        out[9] = 17
        destinationIp.copyInto(out, 12)
        sourceIp.copyInto(out, 16)
        writeShort(out, 20, destinationPort)
        writeShort(out, 22, sourcePort)
        writeShort(out, 24, 8 + responseDns.size)
        responseDns.copyInto(out, 28)
        writeShort(out, 10, checksum(out, 0, 20))
        writeShort(out, 26, udpChecksum(out, responseDns.size))
        return out
    }

    private fun udpChecksum(packet: ByteArray, dnsLength: Int): Int {
        val pseudo = ByteArray(12 + 8 + dnsLength)
        packet.copyOfRange(12, 20).copyInto(pseudo, 0)
        pseudo[8] = 0
        pseudo[9] = 17
        writeShort(pseudo, 10, 8 + dnsLength)
        packet.copyOfRange(20, packet.size).copyInto(pseudo, 12)
        return checksum(pseudo, 0, pseudo.size).let { if (it == 0) 0xffff else it }
    }

    companion object {
        fun parse(packet: ByteArray, length: Int): Ipv4UdpDnsPacket? {
            if (length < 28) return null
            val version = (packet[0].toInt() ushr 4) and 0x0f
            val headerLength = (packet[0].toInt() and 0x0f) * 4
            if (version != 4 || headerLength < 20 || length < headerLength + 8) return null
            if ((packet[9].toInt() and 0xff) != 17) return null
            val sourcePort = readShort(packet, headerLength)
            val destinationPort = readShort(packet, headerLength + 2)
            if (destinationPort != 53) return null
            val udpLength = readShort(packet, headerLength + 4)
            val dnsOffset = headerLength + 8
            val dnsLength = udpLength - 8
            if (dnsLength <= 0 || dnsOffset + dnsLength > length) return null
            return Ipv4UdpDnsPacket(
                sourceIp = packet.copyOfRange(12, 16),
                destinationIp = packet.copyOfRange(16, 20),
                sourcePort = sourcePort,
                destinationPort = destinationPort,
                dnsPayload = packet.copyOfRange(dnsOffset, dnsOffset + dnsLength),
            )
        }
    }
}

private fun readShort(bytes: ByteArray, offset: Int): Int {
    return ((bytes[offset].toInt() and 0xff) shl 8) or (bytes[offset + 1].toInt() and 0xff)
}

private fun writeShort(bytes: ByteArray, offset: Int, value: Int) {
    bytes[offset] = ((value ushr 8) and 0xff).toByte()
    bytes[offset + 1] = (value and 0xff).toByte()
}

private fun checksum(bytes: ByteArray, offset: Int, length: Int): Int {
    var sum = 0
    var index = offset
    while (index < offset + length) {
        val high = bytes[index].toInt() and 0xff
        val low = if (index + 1 < offset + length) bytes[index + 1].toInt() and 0xff else 0
        sum += (high shl 8) + low
        while (sum > 0xffff) sum = (sum and 0xffff) + (sum ushr 16)
        index += 2
    }
    return sum.inv() and 0xffff
}
