# SOSBlu para iOS (iPhone)

Versión oficial de **SOSBlu** desarrollada en Swift + SwiftUI para la plataforma iOS de Apple.

## Especificaciones de Arquitectura

- **Lenguaje / Framework**: Swift 5.9 + SwiftUI (iOS 16.0+)
- **Radio Bluetooth Malla**: `CoreBluetooth` (`CBCentralManager` y `CBPeripheralManager` con permisos `UIBackgroundModes`)
- **Criptografía**: Apple `CryptoKit` (Firma asimétrica Ed25519)
- **Geolocalización**: `CoreLocation`
- **Notificaciones Criticas**: `UserNotifications` (`UNNotificationSound.defaultCritical`)

---

## Estructura del Proyecto iOS

```text
ios/
└── SOSBlu/
    ├── Info.plist               [Permisos de Bluetooth y Ubicación en segundo plano]
    ├── SOSBluApp.swift          [Punto de entrada de la app iOS]
    ├── Models/
    │   ├── SOSBeaconPayload.swift   [Parser binario del protocolo SOS_BEACON 0x30u]
    │   └── ReceivedSOSBeacon.swift  [Modelo de alertas recibidas de auxilio]
    ├── Services/
    │   ├── CryptoManager.swift      [Gestor de claves y firmas Ed25519 con CryptoKit]
    │   ├── BluetoothMeshService.swift [Manejo de CoreBluetooth para publicidad y escaneo]
    │   ├── LocationManager.swift    [Geolocalización GPS con CoreLocation]
    │   └── BridgeRelayService.swift [Deduplicador y gestor de notificaciones]
    └── Views/
        └── EmergencyScreen.swift    [Interfaz gráfica nativa SwiftUI]
```

---

## Interoperabilidad Cruzada iPhone <-> Android

El parser `SOSBeaconPayload.swift` implementa la misma serialización binaria en `Data` utilizada en Android.  
Un iPhone emitiendo auxilio desde **SOSBlu iOS** será detectado inmediatamente por cualquier dispositivo **Android** cercano con **SOSBlu** y viceversa.

---

## Instrucciones para Compilar en Xcode

1. Abre el proyecto en una Mac con Xcode 15+.
2. Selecciona el equipo de desarrollo (*Development Team*) en **Signing & Capabilities**.
3. Selecciona el dispositivo objetivo (iPhone físico o Simulador iOS).
4. Presiona `Cmd + R` para compilar y ejecutar.
