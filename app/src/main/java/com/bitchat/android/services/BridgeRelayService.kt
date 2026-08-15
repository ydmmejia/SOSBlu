package com.bitchat.android.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.bitchat.android.MainActivity
import com.bitchat.android.R
import com.bitchat.android.net.OkHttpProvider
import com.bitchat.android.protocol.BitchatPacket
import com.bitchat.android.protocol.MessageType
import com.bitchat.android.protocol.SOSBeaconPayload
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

enum class GatewayRelayStatus {
    LOCAL_MESH_ONLY,
    RELAYED_TO_GATEWAY
}

data class ReceivedSOSBeacon(
    val payload: SOSBeaconPayload,
    val senderDeviceIdHex: String,
    val receivedTimestamp: Long,
    val rssi: Int?,
    val ttlHopsRemaining: Int,
    val initialTTL: Int = 20,
    val packetCount: Int = 1,
    var relayStatus: GatewayRelayStatus = GatewayRelayStatus.LOCAL_MESH_ONLY
) {
    val estimatedHopsPassed: Int get() = (initialTTL - ttlHopsRemaining).coerceAtLeast(0)
}

/**
 * Service that collects incoming SOS_BEACON mesh packets and relays them via HTTPS 
 * to emergency gateway services when internet/cellular connectivity is available.
 * Deduplicates alerts by victim device ID so multiple beacons from the same device
 * update the existing alert rather than creating duplicate entries.
 */
class BridgeRelayService private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _receivedBeacons = MutableStateFlow<List<ReceivedSOSBeacon>>(emptyList())
    val receivedBeacons: StateFlow<List<ReceivedSOSBeacon>> = _receivedBeacons.asStateFlow()

    private val _isGatewayConnected = MutableStateFlow(false)
    val isGatewayConnected: StateFlow<Boolean> = _isGatewayConnected.asStateFlow()

    private val forwardedBeaconKeys = ConcurrentHashMap.newKeySet<String>()

    private var targetGatewayEndpoint: String = "https://earthquakemesh-gateway.example.org/api/v1/beacons"

    companion object {
        private const val TAG = "BridgeRelayService"

        @Volatile
        private var INSTANCE: BridgeRelayService? = null

        fun getInstance(context: Context): BridgeRelayService =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: BridgeRelayService(context.applicationContext).also { INSTANCE = it }
            }

        fun hexString(bytes: ByteArray): String {
            return bytes.joinToString("") { "%02x".format(it) }
        }
    }

    init {
        setupConnectivityMonitoring()
    }

    fun setGatewayEndpoint(url: String) {
        targetGatewayEndpoint = url
    }

    /**
     * Process an incoming SOS_BEACON packet received from the BLE mesh network.
     * Deduplicates strictly by sender device ID so repeated broadcasts from the same
     * victim update the existing alert in real-time instead of inflating the alert counter.
     */
    fun onSOSBeaconPacketReceived(packet: BitchatPacket, rssi: Int? = null) {
        if (packet.type != MessageType.SOS_BEACON.value) return

        val payload = SOSBeaconPayload.fromBinary(packet.payload) ?: return
        val deviceIdHex = hexString(payload.deviceId)
        val deduplicationKey = "${deviceIdHex}_${payload.timestamp}"

        val currentList = _receivedBeacons.value.toMutableList()
        val existingIndex = currentList.indexOfFirst { it.senderDeviceIdHex == deviceIdHex }

        // Trigger hardware emergency vibration on receiving SOS packet
        triggerEmergencyVibration(appContext)

        if (existingIndex >= 0) {
            val existing = currentList[existingIndex]
            // Update existing victim alert with latest payload, increment packet count, latest coordinates & battery
            val updatedBeacon = existing.copy(
                payload = payload,
                receivedTimestamp = System.currentTimeMillis(),
                rssi = rssi ?: existing.rssi,
                ttlHopsRemaining = packet.ttl.toInt(),
                packetCount = existing.packetCount + 1,
                relayStatus = if (forwardedBeaconKeys.contains(deduplicationKey)) GatewayRelayStatus.RELAYED_TO_GATEWAY else existing.relayStatus
            )
            currentList[existingIndex] = updatedBeacon
            _receivedBeacons.value = currentList
            Log.d(TAG, "Updated alert for device: $deviceIdHex (Retransmissions: ${updatedBeacon.packetCount})")
        } else {
            // New distinct victim detected
            val newBeacon = ReceivedSOSBeacon(
                payload = payload,
                senderDeviceIdHex = deviceIdHex,
                receivedTimestamp = System.currentTimeMillis(),
                rssi = rssi,
                ttlHopsRemaining = packet.ttl.toInt(),
                packetCount = 1,
                relayStatus = if (forwardedBeaconKeys.contains(deduplicationKey)) GatewayRelayStatus.RELAYED_TO_GATEWAY else GatewayRelayStatus.LOCAL_MESH_ONLY
            )
            currentList.add(0, newBeacon)
            _receivedBeacons.value = currentList
            triggerEmergencyNotification(newBeacon)
            Log.i(TAG, "New emergency alert registered from device: $deviceIdHex")
        }

        // Attempt gateway relay if internet connection is available
        if (_isGatewayConnected.value && !forwardedBeaconKeys.contains(deduplicationKey)) {
            val beaconToRelay = currentList.firstOrNull { it.senderDeviceIdHex == deviceIdHex }
            if (beaconToRelay != null) {
                scope.launch {
                    relayToGateway(beaconToRelay, deduplicationKey)
                }
            }
        }
    }

    /**
     * Direct hardware haptic vibration in high-priority emergency pattern
     */
    private fun triggerEmergencyVibration(context: Context) {
        try {
            val timings = longArrayOf(0, 300, 150, 300, 150, 300, 250, 600, 150, 600, 150, 600, 250, 300, 150, 300, 150, 300)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                val vibrator = vibratorManager?.defaultVibrator
                val amplitudes = intArrayOf(0, 255, 0, 255, 0, 255, 0, 255, 0, 255, 0, 255, 0, 255, 0, 255, 0, 255)
                val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
                vibrator?.vibrate(effect)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                val effect = VibrationEffect.createWaveform(timings, -1)
                vibrator?.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                @Suppress("DEPRECATION")
                vibrator?.vibrate(timings, -1)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Hardware vibration trigger failed: ${e.message}")
        }
    }

    private fun triggerEmergencyNotification(beacon: ReceivedSOSBeacon) {
        try {
            val notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return

            val channelId = "sosblu_emergency_alerts"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    channelId,
                    "Alertas SOS de Emergencia",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Alertas sonoras y de vibración cuando se recibe un faro de auxilio SOS cercano."
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 500, 200, 500, 200, 500)
                }
                notificationManager.createNotificationChannel(channel)
            }

            val intent = Intent(appContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                appContext,
                beacon.senderDeviceIdHex.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val locationText = if (beacon.payload.latitude != null && beacon.payload.longitude != null) {
                "Ubicación: %.5f, %.5f".format(beacon.payload.latitude, beacon.payload.longitude)
            } else {
                "Ubicación: Transmisión Malla"
            }

            val noteText = if (!beacon.payload.freeText.isNullOrBlank()) " | ${beacon.payload.freeText}" else ""

            val notification = NotificationCompat.Builder(appContext, channelId)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("ALERTA DE AUXILIO RECIBIDA")
                .setContentText("Dispositivo #${beacon.senderDeviceIdHex.take(8).uppercase()} ($locationText$noteText)")
                .setStyle(NotificationCompat.BigTextStyle().bigText("Se ha recibido una señal de emergencia SOS de un dispositivo cercano.\n$locationText$noteText\nBatería: ${beacon.payload.batteryLevel}%"))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

            notificationManager.notify(beacon.senderDeviceIdHex.hashCode(), notification)
        } catch (e: Exception) {
            Log.e(TAG, "Error triggering emergency notification: ${e.message}")
        }
    }

    private fun setupConnectivityMonitoring() {
        try {
            val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()

            cm.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    _isGatewayConnected.value = true
                    flushPendingBeaconsToGateway()
                }

                override fun onLost(network: Network) {
                    _isGatewayConnected.value = false
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register network callback: ${e.message}")
        }
    }

    private fun flushPendingBeaconsToGateway() {
        scope.launch {
            val beacons = _receivedBeacons.value
            for (beacon in beacons) {
                val key = "${beacon.senderDeviceIdHex}_${beacon.payload.timestamp}"
                if (!forwardedBeaconKeys.contains(key)) {
                    relayToGateway(beacon, key)
                }
            }
        }
    }

    private suspend fun relayToGateway(beacon: ReceivedSOSBeacon, deduplicationKey: String) {
        try {
            val json = JSONObject().apply {
                put("deviceId", beacon.senderDeviceIdHex)
                put("timestamp", beacon.payload.timestamp)
                put("locationSource", beacon.payload.locationSource.name)
                put("latitude", beacon.payload.latitude ?: JSONObject.NULL)
                put("longitude", beacon.payload.longitude ?: JSONObject.NULL)
                put("gpsAccuracy", beacon.payload.gpsAccuracy ?: JSONObject.NULL)
                put("batteryLevel", beacon.payload.batteryLevel)
                put("freeText", beacon.payload.freeText ?: JSONObject.NULL)
                put("hopsPassed", beacon.estimatedHopsPassed)
                put("rssi", beacon.rssi ?: JSONObject.NULL)
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = json.toString().toRequestBody(mediaType)
            val request = Request.Builder()
                .url(targetGatewayEndpoint)
                .post(requestBody)
                .build()

            val client = OkHttpProvider.httpClient()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                forwardedBeaconKeys.add(deduplicationKey)
                val currentList = _receivedBeacons.value.toMutableList()
                val index = currentList.indexOfFirst { it.senderDeviceIdHex == beacon.senderDeviceIdHex }
                if (index >= 0) {
                    currentList[index] = currentList[index].copy(relayStatus = GatewayRelayStatus.RELAYED_TO_GATEWAY)
                    _receivedBeacons.value = currentList
                }
                Log.i(TAG, "Successfully relayed beacon to gateway: $deduplicationKey")
            } else {
                Log.w(TAG, "Gateway returned error ${response.code} for beacon: $deduplicationKey")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to relay beacon to gateway: ${e.message}")
        }
    }
}
