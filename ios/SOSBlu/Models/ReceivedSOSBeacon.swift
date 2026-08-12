import Foundation

public enum GatewayRelayStatus: String {
    case localMeshOnly = "LOCAL_MESH_ONLY"
    case relayedToGateway = "RELAYED_TO_GATEWAY"
}

public struct ReceivedSOSBeacon: Identifiable, Equatable {
    public var id: String { "\(senderDeviceIdHex)_\(payload.timestamp)" }
    public let payload: SOSBeaconPayload
    public let senderDeviceIdHex: String
    public let receivedTimestamp: Double
    public var rssi: Int?
    public var ttlHopsRemaining: Int
    public var relayStatus: GatewayRelayStatus

    public var estimatedHopsPassed: Int {
        return max(0, 20 - ttlHopsRemaining)
    }

    public init(
        payload: SOSBeaconPayload,
        senderDeviceIdHex: String,
        receivedTimestamp: Double = Date().timeIntervalSince1970,
        rssi: Int? = nil,
        ttlHopsRemaining: Int = 20,
        relayStatus: GatewayRelayStatus = .localMeshOnly
    ) {
        self.payload = payload
        self.senderDeviceIdHex = senderDeviceIdHex
        self.receivedTimestamp = receivedTimestamp
        self.rssi = rssi
        self.ttlHopsRemaining = ttlHopsRemaining
        self.relayStatus = relayStatus
    }

    public static func == (lhs: ReceivedSOSBeacon, rhs: ReceivedSOSBeacon) -> Bool {
        return lhs.id == rhs.id
    }
}
