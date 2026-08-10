package com.bitchat.android.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.Location
import android.location.LocationManager
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager as SystemPowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.bitchat.android.MainActivity
import com.bitchat.android.mesh.BluetoothMeshService
import com.bitchat.android.mesh.MeshPacketUtils
import com.bitchat.android.mesh.PowerManager
import com.bitchat.android.model.RoutedPacket
import com.bitchat.android.noise.NoiseEncryptionService
import com.bitchat.android.protocol.BitchatPacket
import com.bitchat.android.protocol.LocationSource
import com.bitchat.android.protocol.MessageType
import com.bitchat.android.protocol.SOSBeaconPayload
import com.bitchat.android.protocol.SpecialRecipients
import com.bitchat.android.service.TransportBridgeService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Continuous foreground service for EarthquakeMesh SOS beacons.
 * Broadcaster runs in background with screen off, sending unencrypted signed 
 * emergency beacons every 30 seconds.
 */
class EmergencyBeaconService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var beaconLoopJob: Job? = null
    private var wakeLock: SystemPowerManager.WakeLock? = null

    private var customFreeText: String? = null

    companion object {
        private const val TAG = "EmergencyBeaconService"
        private const val NOTIFICATION_ID = 9991
        private const val CHANNEL_ID = "earthquakemesh_sos_channel"

        private const val ACTION_START_SOS = "com.bitchat.android.action.START_SOS"
        private const val ACTION_STOP_SOS = "com.bitchat.android.action.STOP_SOS"
        private const val EXTRA_FREE_TEXT = "extra_free_text"

        private val _isSOSActive = MutableStateFlow(false)
        val isSOSActive: StateFlow<Boolean> = _isSOSActive.asStateFlow()

        private val _currentPayload = MutableStateFlow<SOSBeaconPayload?>(null)
        val currentPayload: StateFlow<SOSBeaconPayload?> = _currentPayload.asStateFlow()

        fun startSOS(context: Context, freeText: String? = null) {
            val intent = Intent(context, EmergencyBeaconService::class.java).apply {
                action = ACTION_START_SOS
                putExtra(EXTRA_FREE_TEXT, freeText)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopSOS(context: Context) {
            val intent = Intent(context, EmergencyBeaconService::class.java).apply {
                action = ACTION_STOP_SOS
            }
            context.startService(intent)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_SOS -> {
                customFreeText = intent.getStringExtra(EXTRA_FREE_TEXT)
                activateSOS()
            }
            ACTION_STOP_SOS -> {
                deactivateSOS()
            }
        }
        return START_STICKY
    }

    private fun activateSOS() {
        if (_isSOSActive.value) return
        _isSOSActive.value = true

        val notification = buildForegroundNotification()
        startForeground(NOTIFICATION_ID, notification)

        acquireWakeLock()
        PowerManager.getInstance(this)

        beaconLoopJob = serviceScope.launch {
            Log.i(TAG, "Emergency SOS beacon active. Starting continuous broadcast loop.")
            while (isActive && _isSOSActive.value) {
                try {
                    broadcastSOSBeacon()
                } catch (e: Exception) {
                    Log.e(TAG, "Error in SOS beacon broadcast loop: ${e.message}")
                }
                delay(30_000L) // Broadcast every 30 seconds
            }
        }
    }

    private fun deactivateSOS() {
        Log.i(TAG, "Deactivating Emergency SOS beacon.")
        _isSOSActive.value = false
        _currentPayload.value = null

        beaconLoopJob?.cancel()
        beaconLoopJob = null

        releaseWakeLock()

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun broadcastSOSBeacon() {
        val batteryPct = getBatteryPercentage()
        val locationFix = getBestLocationFix()

        val locationSource = when {
            locationFix == null -> LocationSource.NO_GPS_RSSI_ONLY
            (System.currentTimeMillis() - locationFix.time) < 60_000L -> LocationSource.GPS_LIVE
            else -> LocationSource.GPS_LAST_KNOWN
        }

        val bleService = com.bitchat.android.service.MeshServiceHolder.getOrCreate(this)
        val myPeerIDHex = bleService.myPeerID
        val myPeerIDBytes = MeshPacketUtils.hexStringToByteArray(myPeerIDHex)

        val payload = SOSBeaconPayload(
            deviceId = myPeerIDBytes,
            timestamp = System.currentTimeMillis(),
            locationSource = locationSource,
            latitude = locationFix?.latitude,
            longitude = locationFix?.longitude,
            gpsAccuracy = locationFix?.accuracy,
            locationTimestamp = if (locationSource == LocationSource.GPS_LAST_KNOWN) locationFix?.time else null,
            batteryLevel = batteryPct,
            freeText = customFreeText
        )
        _currentPayload.value = payload

        val packet = BitchatPacket(
            version = 1u,
            type = MessageType.SOS_BEACON.value,
            senderID = myPeerIDBytes,
            recipientID = SpecialRecipients.BROADCAST,
            timestamp = payload.timestamp.toULong(),
            payload = payload.toBinary(),
            signature = null,
            ttl = 20u // Amplified hop count for disaster topology
        )

        // Sign packet with Ed25519 using persistent device key
        val signedPacket = NoiseEncryptionService(this).signPacket(packet) ?: packet

        // Transmit out over all active mesh layers
        TransportBridgeService.broadcastFromLocal(RoutedPacket(signedPacket))
        Log.i(TAG, "Transmitted SOS_BEACON packet (ttl=20, locSource=$locationSource, battery=$batteryPct%)")
    }

    private fun getBatteryPercentage(): Int {
        return try {
            val intent = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            if (level >= 0 && scale > 0) (level * 100) / scale else 50
        } catch (e: Exception) {
            50
        }
    }

    private fun getBestLocationFix(): Location? {
        return try {
            val lm = getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
            var bestLocation: Location? = null

            val providers = lm.getProviders(true)
            for (provider in providers) {
                val l = lm.getLastKnownLocation(provider) ?: continue
                if (bestLocation == null || l.accuracy < bestLocation.accuracy || l.time > bestLocation.time) {
                    bestLocation = l
                }
            }
            bestLocation
        } catch (e: SecurityException) {
            Log.w(TAG, "Location permission not granted: ${e.message}")
            null
        } catch (e: Exception) {
            null
        }
    }

    private fun acquireWakeLock() {
        if (wakeLock == null) {
            val pm = getSystemService(Context.POWER_SERVICE) as? SystemPowerManager
            wakeLock = pm?.newWakeLock(SystemPowerManager.PARTIAL_WAKE_LOCK, "EarthquakeMesh:SOSBeaconWakeLock")?.apply {
                acquire(24 * 60 * 60 * 1000L) // Max 24h
            }
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
        wakeLock = null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "EarthquakeMesh Emergency SOS",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Active Emergency SOS Beacon notification"
            }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm?.createNotificationChannel(channel)
        }
    }

    private fun buildForegroundNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("EarthquakeMesh - EMERGENCY SOS ACTIVE")
            .setContentText("Broadcasting location and distress beacon over Bluetooth LE mesh.")
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .build()
    }

    override fun onDestroy() {
        deactivateSOS()
        super.onDestroy()
    }
}
