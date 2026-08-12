import Foundation
import UserNotifications
import Combine

public class BridgeRelayService: ObservableObject {
    public static let shared = BridgeRelayService()

    @Published public var receivedBeacons: [ReceivedSOSBeacon] = []

    private init() {
        requestNotificationPermissions()
    }

    public func requestNotificationPermissions() {
        UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .sound, .badge]) { granted, error in
            if let error = error {
                print("BridgeRelayService Notification Auth Error: \(error)")
            }
        }
    }

    public func processReceivedSOSBeacon(payload: SOSBeaconPayload, rssi: Int? = nil) {
        let senderHex = payload.deviceId.map { String(format: "%02hhx", $0) }.joined()
        let beacon = ReceivedSOSBeacon(payload: payload, senderDeviceIdHex: senderHex, rssi: rssi)

        DispatchQueue.main.async {
            if let index = self.receivedBeacons.firstIndex(where: { $0.id == beacon.id }) {
                self.receivedBeacons[index] = beacon
            } else {
                self.receivedBeacons.insert(beacon, at: 0)
                self.triggerNotification(for: beacon)
            }
        }
    }

    private func triggerNotification(for beacon: ReceivedSOSBeacon) {
        let content = UNMutableNotificationContent()
        content.title = "ALERTA DE EMERGENCIA SOS RECIBIDA"
        let noteStr = beacon.payload.freeText.map { " Nota: \($0)" } ?? ""
        content.body = "Víctima ID: \(beacon.senderDeviceIdHex.prefix(8).uppercased()) (Batería: \(beacon.payload.batteryLevel)%).\(noteStr)"
        content.sound = UNNotificationSound.defaultCritical

        let request = UNNotificationRequest(
            identifier: beacon.id,
            content: content,
            trigger: nil
        )
        UNUserNotificationCenter.current().add(request)
    }
}
