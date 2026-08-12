import SwiftUI
import CoreLocation

struct EmergencyScreen: View {
    @StateObject private var bleService = BluetoothMeshService.shared
    @StateObject private var relayService = BridgeRelayService.shared
    @StateObject private var locationManager = LocationManager.shared

    @State private var selectedTab = 0
    @State private var freeTextNote = ""
    @State private var isSOSActive = false
    @State private var isDarkMode = true

    var body: some View {
        ZStack {
            (isDarkMode ? Color(red: 0.035, green: 0.05, blue: 0.086) : Color(red: 0.97, green: 0.98, blue: 0.99))
                .ignoresSafeArea()

            VStack(spacing: 0) {
                // Top Header Bar
                HStack {
                    HStack(spacing: 10) {
                        ZStack {
                            RoundedRectangle(cornerRadius: 8)
                                .fill(Color(red: 0.898, green: 0.224, blue: 0.208))
                                .frame(width: 32, height: 32)
                            Image(systemName: "shield.fill")
                                .foregroundColor(.white)
                                .font(.system(size: 16))
                        }

                        VStack(alignment: .leading, spacing: 2) {
                            Text("SOSBlu")
                                .font(.system(size: 20, weight: .black))
                                .foregroundColor(isDarkMode ? .white : Color(red: 0.06, green: 0.09, blue: 0.16))
                            Text("RED DE AUXILIO BLE (OFFLINE)")
                                .font(.system(size: 9, weight: .bold))
                                .foregroundColor(Color(red: 0.898, green: 0.224, blue: 0.208))
                        }
                    }

                    Spacer()

                    Button(action: { isDarkMode.toggle() }) {
                        Image(systemName: isDarkMode ? "sun.max.fill" : "moon.fill")
                            .font(.system(size: 18))
                            .foregroundColor(isDarkMode ? .white : .black)
                    }
                }
                .padding(.horizontal, 20)
                .padding(.vertical, 14)
                .background(isDarkMode ? Color(red: 0.06, green: 0.09, blue: 0.16) : Color.white)

                // Segmented Tab Picker
                Picker("", selection: $selectedTab) {
                    Text("EMITIR SEÑAL SOS").tag(0)
                    Text("ALERTAS DE AUXILIO (\(relayService.receivedBeacons.count))").tag(1)
                }
                .pickerStyle(SegmentedPickerStyle())
                .padding(.horizontal, 16)
                .padding(.vertical, 10)

                if selectedTab == 0 {
                    SpaciousSOSView(
                        isSOSActive: $isSOSActive,
                        freeTextNote: $freeTextNote,
                        activePeerCount: bleService.activePeerCount,
                        isDarkMode: isDarkMode
                    )
                } else {
                    RescueMonitorView(beacons: relayService.receivedBeacons, isDarkMode: isDarkMode)
                }
            }
        }
        .onAppear {
            bleService.startServices()
            locationManager.requestPermissions()
        }
    }
}

struct SpaciousSOSView: View {
    @Binding var isSOSActive: Bool
    @Binding var freeTextNote: String
    let activePeerCount: Int
    let isDarkMode: Bool

    @State private var isPulsing = false

    var body: some View {
        VStack {
            // Status & Telemetry Bar
            HStack {
                HStack(spacing: 8) {
                    Circle()
                        .fill(isSOSActive ? Color.red : Color.green)
                        .frame(width: 10, height: 10)
                    Text(isSOSActive ? "EMITIENDO AUXILIO" : "DISPOSITIVO EN ESPERA")
                        .font(.system(size: 11, weight: .bold))
                        .foregroundColor(isDarkMode ? .white : .black)
                }

                Spacer()

                Text("\(activePeerCount) DISPOSITIVOS CERCANOS")
                    .font(.system(size: 11, weight: .bold))
                    .foregroundColor(Color.gray)
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 14)
            .background(isDarkMode ? Color(red: 0.086, green: 0.125, blue: 0.196) : Color.white)
            .cornerRadius(12)
            .overlay(
                RoundedRectangle(cornerRadius: 12)
                    .stroke(isDarkMode ? Color(red: 0.145, green: 0.2, blue: 0.3) : Color.gray.opacity(0.3), lineWidth: 1)
            )

            Spacer()

            // 220dp Center SOS Target Button
            Button(action: {
                isSOSActive.toggle()
                if isSOSActive {
                    let payload = SOSBeaconPayload(
                        deviceId: CryptoManager.shared.deviceIdData,
                        locationSource: .liveGps,
                        latitude: LocationManager.shared.currentLocation?.coordinate.latitude,
                        longitude: LocationManager.shared.currentLocation?.coordinate.longitude,
                        gpsAccuracy: Float(LocationManager.shared.currentLocation?.horizontalAccuracy ?? 0),
                        batteryLevel: 85,
                        freeText: freeTextNote.isEmpty ? nil : freeTextNote
                    )
                    BluetoothMeshService.shared.startSOSBroadcast(payloadData: payload.toBinary())
                } else {
                    BluetoothMeshService.shared.stopSOSBroadcast()
                }
            }) {
                ZStack {
                    Circle()
                        .fill(isSOSActive ? Color(red: 0.717, green: 0.11, blue: 0.11) : Color(red: 0.898, green: 0.224, blue: 0.208))
                        .frame(width: 220, height: 220)
                        .shadow(color: Color.red.opacity(0.4), radius: isSOSActive ? 20 : 10)
                        .scaleEffect(isPulsing && isSOSActive ? 1.05 : 1.0)
                        .animation(isSOSActive ? Animation.easeInOut(duration: 1.0).repeatForever(autoreverses: true) : .default, value: isPulsing)

                    VStack(spacing: 6) {
                        Image(systemName: "power")
                            .font(.system(size: 56, weight: .bold))
                            .foregroundColor(.white)
                        Text(isSOSActive ? "SOS EN VIVO" : "PRESIONAR SOS")
                            .font(.system(size: 20, weight: .black))
                            .foregroundColor(.white)
                        Text(isSOSActive ? "TOCAR PARA CANCELAR" : "EMITIR ALERTA")
                            .font(.system(size: 11, weight: .bold))
                            .foregroundColor(.white.opacity(0.85))
                    }
                }
            }
            .onAppear {
                isPulsing = true
            }

            Spacer()

            // Bottom Location Note Field
            VStack(spacing: 10) {
                TextField("Nota de Ubicación Opcional (ej: Piso 3)", text: $freeTextNote)
                    .textFieldStyle(RoundedBorderTextFieldStyle())
                    .disabled(isSOSActive)

                HStack(spacing: 6) {
                    Image(systemName: "antenna.radiowaves.left.and.right")
                        .font(.system(size: 12))
                        .foregroundColor(Color.green)
                    Text("TRANSMISIÓN CONTINUA EN SEGUNDO PLANO Y PANTALLA BLOQUEADA")
                        .font(.system(size: 9.5, weight: .bold))
                        .foregroundColor(Color.gray)
                }
            }
        }
        .padding(.horizontal, 24)
        .padding(.vertical, 16)
    }
}

struct RescueMonitorView: View {
    let beacons: [ReceivedSOSBeacon]
    let isDarkMode: Bool

    var body: some View {
        if beacons.isEmpty {
            VStack(spacing: 16) {
                Spacer()
                Image(systemName: "dot.radiowaves.left.and.right")
                    .font(.system(size: 48))
                    .foregroundColor(.gray)
                Text("NO HAY ALERTAS ACTIVAS EN RANGO")
                    .font(.system(size: 14, weight: .bold))
                    .foregroundColor(isDarkMode ? .white : .black)
                Text("El sistema monitorea emisiones de auxilio en la red malla de celular a celular.")
                    .font(.system(size: 12))
                    .foregroundColor(.gray)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 32)
                Spacer()
            }
        } else {
            List(beacons) { beacon in
                SOSVictimRow(beacon: beacon, isDarkMode: isDarkMode)
                    .listRowBackground(Color.clear)
            }
            .listStyle(PlainListStyle())
        }
    }
}

struct SOSVictimRow: View {
    let beacon: ReceivedSOSBeacon
    let isDarkMode: Bool

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                HStack(spacing: 4) {
                    Image(systemName: "location.fill")
                        .foregroundColor(.red)
                    Text("ALERTA ID: \(beacon.senderDeviceIdHex.prefix(8).uppercased())")
                        .font(.system(size: 14, weight: .bold, design: .monospaced))
                        .foregroundColor(isDarkMode ? .white : .black)
                }
                Spacer()
                Text("Recibido")
                    .font(.system(size: 11, weight: .bold))
                    .foregroundColor(.orange)
            }

            if let lat = beacon.payload.latitude, let lon = beacon.payload.longitude {
                Text("UBICACIÓN: \(lat), \(lon) (±\(Int(beacon.payload.gpsAccuracy ?? 0))m)")
                    .font(.system(size: 12, weight: .bold, design: .monospaced))
                    .foregroundColor(isDarkMode ? .white : .black)
            } else {
                Text("UBICACIÓN: TRANSMISIÓN MESH SIN GPS")
                    .font(.system(size: 11, weight: .bold))
                    .foregroundColor(.orange)
            }

            if let note = beacon.payload.freeText, !note.isEmpty {
                Text("NOTA: \(note)")
                    .font(.system(size: 12, weight: .medium))
                    .foregroundColor(isDarkMode ? .white : .black)
            }

            HStack {
                Text("BATERÍA: \(beacon.payload.batteryLevel)% | SALTOS: \(beacon.estimatedHopsPassed)")
                    .font(.system(size: 10))
                    .foregroundColor(.gray)
                Spacer()
                Text("RED MALLA LOCAL")
                    .font(.system(size: 9, weight: .bold))
                    .padding(.horizontal, 8)
                    .padding(.vertical, 2)
                    .background(Color.blue)
                    .foregroundColor(.white)
                    .cornerRadius(4)
            }
        }
        .padding(16)
        .background(isDarkMode ? Color(red: 0.086, green: 0.125, blue: 0.196) : Color.white)
        .cornerRadius(12)
    }
}
