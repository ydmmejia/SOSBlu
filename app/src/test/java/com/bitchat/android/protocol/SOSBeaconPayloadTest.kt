package com.bitchat.android.protocol

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SOSBeaconPayloadTest {

    @Test
    fun testSOSBeaconPayloadWithLiveGPS() {
        val deviceId = byteArrayOf(0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08)
        val timestamp = System.currentTimeMillis()
        val payload = SOSBeaconPayload(
            deviceId = deviceId,
            timestamp = timestamp,
            locationSource = LocationSource.GPS_LIVE,
            latitude = 19.432608,
            longitude = -99.133209,
            gpsAccuracy = 4.5f,
            batteryLevel = 85,
            freeText = "Trapped on 3rd floor"
        )

        val binary = payload.toBinary()
        val decoded = SOSBeaconPayload.fromBinary(binary)

        assertNotNull(decoded)
        assertArrayEquals(deviceId, decoded!!.deviceId)
        assertEquals(timestamp, decoded.timestamp)
        assertEquals(LocationSource.GPS_LIVE, decoded.locationSource)
        assertEquals(19.432608, decoded.latitude!!, 0.000001)
        assertEquals(-99.133209, decoded.longitude!!, 0.000001)
        assertEquals(4.5f, decoded.gpsAccuracy!!, 0.01f)
        assertNull(decoded.locationTimestamp)
        assertEquals(85, decoded.batteryLevel)
        assertEquals("Trapped on 3rd floor", decoded.freeText)
    }

    @Test
    fun testSOSBeaconPayloadNoGPSRSSIOnly() {
        val deviceId = byteArrayOf(0xAA.toByte(), 0xBB.toByte(), 0xCC.toByte(), 0xDD.toByte(), 0x11, 0x22, 0x33, 0x44)
        val timestamp = System.currentTimeMillis()
        val payload = SOSBeaconPayload(
            deviceId = deviceId,
            timestamp = timestamp,
            locationSource = LocationSource.NO_GPS_RSSI_ONLY,
            batteryLevel = 12,
            freeText = null
        )

        val binary = payload.toBinary()
        val decoded = SOSBeaconPayload.fromBinary(binary)

        assertNotNull(decoded)
        assertArrayEquals(deviceId, decoded!!.deviceId)
        assertEquals(timestamp, decoded.timestamp)
        assertEquals(LocationSource.NO_GPS_RSSI_ONLY, decoded.locationSource)
        assertNull(decoded.latitude)
        assertNull(decoded.longitude)
        assertNull(decoded.gpsAccuracy)
        assertEquals(12, decoded.batteryLevel)
        assertNull(decoded.freeText)
    }

    @Test
    fun testSOSBeaconPayloadLastKnownGPS() {
        val deviceId = byteArrayOf(1, 1, 1, 1, 2, 2, 2, 2)
        val timestamp = System.currentTimeMillis()
        val locTimestamp = timestamp - 3600000L
        val payload = SOSBeaconPayload(
            deviceId = deviceId,
            timestamp = timestamp,
            locationSource = LocationSource.GPS_LAST_KNOWN,
            latitude = 40.7128,
            longitude = -74.0060,
            gpsAccuracy = 15.0f,
            locationTimestamp = locTimestamp,
            batteryLevel = 45,
            freeText = "Near stairwell"
        )

        val binary = payload.toBinary()
        val decoded = SOSBeaconPayload.fromBinary(binary)

        assertNotNull(decoded)
        assertEquals(LocationSource.GPS_LAST_KNOWN, decoded!!.locationSource)
        assertEquals(40.7128, decoded.latitude!!, 0.0001)
        assertEquals(-74.0060, decoded.longitude!!, 0.0001)
        assertEquals(locTimestamp, decoded.locationTimestamp)
    }
}
