package com.bitchat.android.services

import com.bitchat.android.protocol.LocationSource
import com.bitchat.android.protocol.SOSBeaconPayload
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class BridgeRelayServiceTest {

    @Test
    fun testJsonPayloadConstruction() {
        val deviceId = byteArrayOf(0x12, 0x34, 0x56, 0x78, 0x9A.toByte(), 0xBC.toByte(), 0xDE.toByte(), 0xF0.toByte())
        val timestamp = 1723284000000L
        val payload = SOSBeaconPayload(
            deviceId = deviceId,
            timestamp = timestamp,
            locationSource = LocationSource.GPS_LIVE,
            latitude = 19.4326,
            longitude = -99.1332,
            gpsAccuracy = 5.0f,
            batteryLevel = 90,
            freeText = "North stairwell"
        )

        val beacon = ReceivedSOSBeacon(
            payload = payload,
            senderDeviceIdHex = "123456789abcdef0",
            receivedTimestamp = timestamp + 2000L,
            rssi = -68,
            ttlHopsRemaining = 18,
            initialTTL = 20,
            relayStatus = GatewayRelayStatus.LOCAL_MESH_ONLY
        )

        val service = BridgeRelayService.getInstance(
            org.mockito.Mockito.mock(android.content.Context::class.java, org.mockito.Mockito.RETURNS_DEEP_STUBS)
        )

        val json: JSONObject = service.buildGatewayJsonPayload(beacon)

        assertEquals("123456789abcdef0", json.getString("deviceId"))
        assertEquals(timestamp, json.getLong("timestamp"))
        assertEquals("GPS_LIVE", json.getString("locationSource"))
        assertEquals(19.4326, json.getDouble("latitude"), 0.0001)
        assertEquals(-99.1332, json.getDouble("longitude"), 0.0001)
        assertEquals(5.0, json.getDouble("gpsAccuracy"), 0.1)
        assertEquals(90, json.getInt("batteryLevel"))
        assertEquals("North stairwell", json.getString("freeText"))
        assertEquals(2, json.getInt("estimatedHopsPassed"))
        assertEquals(18, json.getInt("ttlHopsRemaining"))
    }
}
