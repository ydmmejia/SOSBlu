package com.bitchat.android.protocol

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Location source classification for emergency SOS beacon payloads.
 */
enum class LocationSource(val value: Byte) {
    GPS_LIVE(0),
    GPS_LAST_KNOWN(1),
    NO_GPS_RSSI_ONLY(2);

    companion object {
        fun fromValue(value: Byte): LocationSource =
            values().find { it.value == value } ?: NO_GPS_RSSI_ONLY
    }
}

/**
 * SOS Beacon Payload representation.
 * Unencrypted payload broadcasted across the mesh network for disaster recovery & rescue operations.
 */
@Parcelize
data class SOSBeaconPayload(
    val deviceId: ByteArray,
    val timestamp: Long,
    val locationSource: LocationSource,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val gpsAccuracy: Float? = null,
    val locationTimestamp: Long? = null,
    val batteryLevel: Int,
    val freeText: String? = null
) : Parcelable {

    fun toBinary(): ByteArray {
        val freeTextBytes = freeText?.toByteArray(Charsets.UTF_8) ?: ByteArray(0)
        val freeTextLen = freeTextBytes.size.coerceAtMost(0xFFFF)

        // Calculate needed capacity:
        // deviceId (8) + timestamp (8) + locationSource (1)
        // + (if GPS available: lat 8 + lon 8 + acc 4 = 20)
        // + (if LAST_KNOWN: locationTimestamp 8)
        // + batteryLevel (1)
        // + freeTextLen (2) + freeTextBytes
        var capacity = 8 + 8 + 1 + 1 + 2 + freeTextLen
        if (locationSource == LocationSource.GPS_LIVE || locationSource == LocationSource.GPS_LAST_KNOWN) {
            capacity += 20
        }
        if (locationSource == LocationSource.GPS_LAST_KNOWN) {
            capacity += 8
        }

        val buffer = ByteBuffer.allocate(capacity).apply { order(ByteOrder.BIG_ENDIAN) }

        // deviceId (8 bytes)
        val devIdPadded = ByteArray(8)
        val copyLen = deviceId.size.coerceAtMost(8)
        System.arraycopy(deviceId, 0, devIdPadded, 0, copyLen)
        buffer.put(devIdPadded)

        // timestamp (8 bytes)
        buffer.putLong(timestamp)

        // locationSource (1 byte)
        buffer.put(locationSource.value)

        // Coordinates if available
        if (locationSource == LocationSource.GPS_LIVE || locationSource == LocationSource.GPS_LAST_KNOWN) {
            buffer.putDouble(latitude ?: 0.0)
            buffer.putDouble(longitude ?: 0.0)
            buffer.putFloat(gpsAccuracy ?: 0.0f)
        }

        // Location timestamp if last known
        if (locationSource == LocationSource.GPS_LAST_KNOWN) {
            buffer.putLong(locationTimestamp ?: 0L)
        }

        // batteryLevel (1 byte)
        buffer.put((batteryLevel.coerceIn(0, 100)).toByte())

        // freeText (2 bytes len + bytes)
        buffer.putShort(freeTextLen.toShort())
        if (freeTextLen > 0) {
            buffer.put(freeTextBytes, 0, freeTextLen)
        }

        return buffer.array()
    }

    companion object {
        fun fromBinary(data: ByteArray): SOSBeaconPayload? {
            try {
                if (data.size < 8 + 8 + 1 + 1 + 2) return null

                val buffer = ByteBuffer.wrap(data).apply { order(ByteOrder.BIG_ENDIAN) }

                val deviceId = ByteArray(8)
                buffer.get(deviceId)

                val timestamp = buffer.getLong()
                val locSourceByte = buffer.get()
                val locationSource = LocationSource.fromValue(locSourceByte)

                var latitude: Double? = null
                var longitude: Double? = null
                var gpsAccuracy: Float? = null
                var locationTimestamp: Long? = null

                if (locationSource == LocationSource.GPS_LIVE || locationSource == LocationSource.GPS_LAST_KNOWN) {
                    if (buffer.remaining() < 20) return null
                    latitude = buffer.getDouble()
                    longitude = buffer.getDouble()
                    gpsAccuracy = buffer.getFloat()
                }

                if (locationSource == LocationSource.GPS_LAST_KNOWN) {
                    if (buffer.remaining() < 8) return null
                    locationTimestamp = buffer.getLong()
                }

                if (buffer.remaining() < 3) return null
                val batteryLevel = buffer.get().toInt() and 0xFF

                val freeTextLen = buffer.getShort().toInt() and 0xFFFF
                var freeText: String? = null
                if (freeTextLen > 0) {
                    if (buffer.remaining() < freeTextLen) return null
                    val textBytes = ByteArray(freeTextLen)
                    buffer.get(textBytes)
                    freeText = String(textBytes, Charsets.UTF_8)
                }

                return SOSBeaconPayload(
                    deviceId = deviceId,
                    timestamp = timestamp,
                    locationSource = locationSource,
                    latitude = latitude,
                    longitude = longitude,
                    gpsAccuracy = gpsAccuracy,
                    locationTimestamp = locationTimestamp,
                    batteryLevel = batteryLevel,
                    freeText = freeText
                )
            } catch (e: Exception) {
                return null
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SOSBeaconPayload

        if (!deviceId.contentEquals(other.deviceId)) return false
        if (timestamp != other.timestamp) return false
        if (locationSource != other.locationSource) return false
        if (latitude != other.latitude) return false
        if (longitude != other.longitude) return false
        if (gpsAccuracy != other.gpsAccuracy) return false
        if (locationTimestamp != other.locationTimestamp) return false
        if (batteryLevel != other.batteryLevel) return false
        if (freeText != other.freeText) return false

        return true
    }

    override fun hashCode(): Int {
        var result = deviceId.contentHashCode()
        result = 31 * result + timestamp.hashCode()
        result = 31 * result + locationSource.hashCode()
        result = 31 * result + (latitude?.hashCode() ?: 0)
        result = 31 * result + (longitude?.hashCode() ?: 0)
        result = 31 * result + (gpsAccuracy?.hashCode() ?: 0)
        result = 31 * result + (locationTimestamp?.hashCode() ?: 0)
        result = 31 * result + batteryLevel
        result = 31 * result + (freeText?.hashCode() ?: 0)
        return result
    }
}
