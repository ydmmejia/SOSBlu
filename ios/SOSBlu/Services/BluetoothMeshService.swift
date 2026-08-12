import Foundation
import CoreBluetooth
import Combine

/// Service CBUUID for SOSBlu BLE Mesh (Matching Android CBUUID: 0000FA10-0000-1000-8000-00805F9B34FB)
public let SOSBLU_SERVICE_UUID = CBUUID(string: "0000FA10-0000-1000-8000-00805F9B34FB")
public let SOSBLU_CHARACTERISTIC_UUID = CBUUID(string: "0000FA11-0000-1000-8000-00805F9B34FB")

public class BluetoothMeshService: NSObject, ObservableObject, CBCentralManagerDelegate, CBPeripheralManagerDelegate {
    public static let shared = BluetoothMeshService()

    private var centralManager: CBCentralManager!
    private var peripheralManager: CBPeripheralManager!

    private var sosCharacteristic: CBMutableCharacteristic?

    @Published public var isBluetoothOn: Bool = false
    @Published public var activePeerCount: Int = 0
    @Published public var isBroadcastingSOS: Bool = false

    private var discoveredPeers: [String: Date] = [:]
    private var timer: Timer?

    override private init() {
        super.init()
        centralManager = CBCentralManager(delegate: self, queue: nil, options: [CBCentralManagerOptionRestoreIdentifierKey: "com.sosblu.ios.central"])
        peripheralManager = CBPeripheralManager(delegate: self, queue: nil, options: [CBPeripheralManagerOptionRestoreIdentifierKey: "com.sosblu.ios.peripheral"])

        timer = Timer.scheduledTimer(withTimeInterval: 5.0, repeats: true) { [weak self] _ in
            self?.cleanStalePeers()
        }
    }

    public func startServices() {
        if centralManager.state == .poweredOn {
            centralManager.scanForPeripherals(withServices: [SOSBLU_SERVICE_UUID], options: [CBCentralManagerScanOptionAllowDuplicatesKey: true])
        }
        setupPeripheralGATT()
    }

    public func startSOSBroadcast(payloadData: Data) {
        isBroadcastingSOS = true
        guard peripheralManager.state == .poweredOn else { return }

        let advertisementData: [String: Any] = [
            CBAdvertisementDataServiceUUIDsKey: [SOSBLU_SERVICE_UUID],
            CBAdvertisementDataLocalNameKey: "SOSBlu-Beacon"
        ]
        peripheralManager.startAdvertising(advertisementData)
    }

    public func stopSOSBroadcast() {
        isBroadcastingSOS = false
        if peripheralManager.isAdvertising {
            peripheralManager.stopAdvertising()
        }
    }

    private func setupPeripheralGATT() {
        guard peripheralManager.state == .poweredOn else { return }

        let characteristic = CBMutableCharacteristic(
            type: SOSBLU_CHARACTERISTIC_UUID,
            properties: [.read, .notify, .writeWithoutResponse],
            value: nil,
            permissions: [.readable, .writeable]
        )
        let service = CBMutableService(type: SOSBLU_SERVICE_UUID, primary: true)
        service.characteristics = [characteristic]

        peripheralManager.removeAllServices()
        peripheralManager.add(service)
        self.sosCharacteristic = characteristic
    }

    private func cleanStalePeers() {
        let now = Date()
        discoveredPeers = discoveredPeers.filter { now.timeIntervalSince($0.value) < 60.0 }
        DispatchQueue.main.async {
            self.activePeerCount = self.discoveredPeers.count
        }
    }

    // MARK: - CBCentralManagerDelegate
    public func centralManagerDidUpdateState(_ central: CBCentralManager) {
        DispatchQueue.main.async {
            self.isBluetoothOn = (central.state == .poweredOn)
        }
        if central.state == .poweredOn {
            central.scanForPeripherals(withServices: [SOSBLU_SERVICE_UUID], options: [CBCentralManagerScanOptionAllowDuplicatesKey: true])
        }
    }

    public func centralManager(_ central: CBCentralManager, didDiscover peripheral: CBPeripheral, advertisementData: [String: Any], rssi RSSI: NSNumber) {
        let peerId = peripheral.identifier.uuidString
        discoveredPeers[peerId] = Date()
        DispatchQueue.main.async {
            self.activePeerCount = self.discoveredPeers.count
        }

        // If service data contains SOSBeaconPayload
        if let serviceDataDict = advertisementData[CBAdvertisementDataServiceDataKey] as? [CBUUID: Data],
           let payloadBytes = serviceDataDict[SOSBLU_SERVICE_UUID],
           let beaconPayload = SOSBeaconPayload.fromBinary(payloadBytes) {
            BridgeRelayService.shared.processReceivedSOSBeacon(payload: beaconPayload, rssi: RSSI.intValue)
        }
    }

    // MARK: - CBPeripheralManagerDelegate
    public func peripheralManagerDidUpdateState(_ peripheral: CBPeripheral) {
        if peripheral.state == .poweredOn {
            setupPeripheralGATT()
        }
    }
}
