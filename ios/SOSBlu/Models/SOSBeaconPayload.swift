import Foundation
import CoreLocation

/// Location source enum matching Android BinaryProtocol specifications
public enum LocationSource: UInt8 {
    case liveGps = 0x01
    case lastKnownGps = 0x02
    case noGpsRssiOnly = 0x03
}

/// Binary payload structure for SOS_BEACON (MessageType 0x30u)
public struct SOSBeaconPayload {
    public let deviceId: Data           // 8 bytes
    public let timestamp: Int64         // 8 bytes Unix epoch ms
    public let locationSource: LocationSource // 1 byte
    public let latitude: Double?        // 8 bytes (Double)
    public let longitude: Double?       // 8 bytes (Double)
    public let gpsAccuracy: Float?      // 4 bytes (Float)
    public let locationTimestamp: Int64?// 8 bytes (Int64)
    public let batteryLevel: UInt8      // 1 byte (0-100)
    public let freeText: String?        // Variable UTF-8 string

    public init(
        deviceId: Data,
        timestamp: Int64 = Int64(Date().timeIntervalSince1970 * 1000),
        locationSource: LocationSource,
        latitude: Double? = nil,
        longitude: Double? = nil,
        gpsAccuracy: Float? = nil,
        locationTimestamp: Int64? = nil,
        batteryLevel: UInt8,
        freeText: String? = nil
    ) {
        self.deviceId = deviceId
        self.timestamp = timestamp
        self.locationSource = locationSource
        self.latitude = latitude
        self.longitude = longitude
        self.gpsAccuracy = gpsAccuracy
        self.locationTimestamp = locationTimestamp
        self.batteryLevel = batteryLevel
        self.freeText = freeText
    }

    /// Serializes payload into compact binary Data matching Android ByteBuffer format
    public func toBinary() -> Data {
        var data = Data()

        // 1. deviceId (8 bytes)
        var devId = deviceId
        if devId.count < 8 {
            devId.append(Data(repeating: 0, count: 8 - devId.count))
        } else if devId.count > 8 {
            devId = devId.prefix(8)
        }
        data.append(devId)

        // 2. timestamp (8 bytes Int64 BigEndian)
        var ts = timestamp.bigEndian
        data.append(Data(bytes: &ts, count: 8))

        // 3. locationSource (1 byte)
        data.append(locationSource.rawValue)

        // 4. Location coordinates
        if locationSource == .liveGps || locationSource == .lastKnownGps,
           let lat = latitude, let lon = longitude {
            var latBits = lat.bitPattern.bigEndian
            var lonBits = lon.bitPattern.bigEndian
            data.append(Data(bytes: &latBits, count: 8))
            data.append(Data(bytes: &lonBits, count: 8))

            let acc = gpsAccuracy ?? 0.0
            var accBits = acc.bitPattern.bigEndian
            data.append(Data(bytes: &accBits, count: 4))

            if locationSource == .lastKnownGps, let locTs = locationTimestamp {
                var lts = locTs.bigEndian
                data.append(Data(bytes: &lts, count: 8))
            }
        }

        // 5. batteryLevel (1 byte)
        data.append(batteryLevel)

        // 6. freeText (UTF-8)
        if let text = freeText, !text.isEmpty, let textData = text.data(using: .utf8) {
            var length = UInt16(min(textData.count, 256)).bigEndian
            data.append(Data(bytes: &length, count: 2))
            data.append(textData.prefix(256))
        } else {
            var length: UInt16 = 0
            data.append(Data(bytes: &length, count: 2))
        }

        return data
    }

    /// Deserializes binary Data payload into SOSBeaconPayload struct
    public static func fromBinary(_ data: Data) -> SOSBeaconPayload? {
        guard data.count >= 18 else { return nil }

        var offset = 0

        // 1. deviceId (8 bytes)
        let deviceId = data.subdata(in: offset..<offset+8)
        offset += 8

        // 2. timestamp (8 bytes Int64)
        let tsRaw = data.subdata(in: offset..<offset+8).withUnsafeBytes { $0.load(as: Int64.self) }
        let timestamp = Int64(bigEndian: tsRaw)
        offset += 8

        // 3. locationSource (1 byte)
        let sourceByte = data[offset]
        guard let locationSource = LocationSource(rawValue: sourceByte) else { return nil }
        offset += 1

        var latitude: Double? = nil
        var longitude: Double? = nil
        var gpsAccuracy: Float? = nil
        var locationTimestamp: Int64? = nil

        if locationSource == .liveGps || locationSource == .lastKnownGps {
            guard data.count >= offset + 20 else { return nil }

            let latRaw = data.subdata(in: offset..<offset+8).withUnsafeBytes { $0.load(as: UInt64.self) }
            latitude = Double(bitPattern: UInt64(bigEndian: latRaw))
            offset += 8

            let lonRaw = data.subdata(in: offset..<offset+8).withUnsafeBytes { $0.load(as: UInt64.self) }
            longitude = Double(bitPattern: UInt64(bigEndian: lonRaw))
            offset += 8

            let accRaw = data.subdata(in: offset..<offset+4).withUnsafeBytes { $0.load(as: UInt32.self) }
            gpsAccuracy = Float(bitPattern: UInt32(bigEndian: accRaw))
            offset += 4

            if locationSource == .lastKnownGps {
                guard data.count >= offset + 8 else { return nil }
                let ltsRaw = data.subdata(in: offset..<offset+8).withUnsafeBytes { $0.load(as: Int64.self) }
                locationTimestamp = Int64(bigEndian: ltsRaw)
                offset += 8
            }
        }

        // 5. batteryLevel
        guard data.count > offset else { return nil }
        let batteryLevel = data[offset]
        offset += 1

        // 6. freeText
        var freeText: String? = nil
        if data.count >= offset + 2 {
            let lenRaw = data.subdata(in: offset..<offset+2).withUnsafeBytes { $0.load(as: UInt16.self) }
            let textLen = Int(UInt16(bigEndian: lenRaw))
            offset += 2

            if textLen > 0, data.count >= offset + textLen {
                let textData = data.subdata(in: offset..<offset+textLen)
                freeText = String(data: textData, encoding: .utf8)
            }
        }

        return SOSBeaconPayload(
            deviceId: deviceId,
            timestamp: timestamp,
            locationSource: locationSource,
            latitude: latitude,
            longitude: longitude,
            gpsAccuracy: gpsAccuracy,
            locationTimestamp: locationTimestamp,
            batteryLevel: batteryLevel,
            freeText: freeText
        )
    }
}
