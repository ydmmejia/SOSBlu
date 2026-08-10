```text
================================================================================
   ____   ____   ____ ____  _           
  / ___| / ___| / ___| __ )| |_   _     
  \___ \| |  _  \___ \  _ \| | | | |    
   ___) | |_| |  ___) | |_) | | |_| |    
  |____/ \____| |____/|____/|_|\__,_|    

  SOSBlu — Red de Auxilio BLE para Terremotos y Desastres (Offline Mesh)
================================================================================
```

## Resumen Ejecutivo

**SOSBlu** es una plataforma móvil de telecomunicaciones de emergencia diseñada para el rescate de víctimas atrapadas tras desastres naturales (terremotos, colapsos estructurales, deslaves) en escenarios con colapso total de infraestructura celular e internet.

La aplicación transforma cada dispositivo Android en un transceptor de la red malla Bluetooth Low Energy (BLE). Permite a las víctimas emitir un faro de auxilio continuo (`SOS_BEACON`) con un solo toque. La señal se propaga de forma salto-a-salto (hasta 20 saltos) a través de otros teléfonos cercanos hasta alcanzar a brigadistas de rescate o un nodo puente (*Gateway*) con enlace a internet.

---

## Arquitectura y Especificaciones Técnicas

### 1. Protocolo Binario de Emergencia (`SOS_BEACON` - `0x30u`)
- **Estructura Binaria**: Serialización compacta en `ByteBuffer` optimizada para transmisiones BLE de bajo ancho de banda:
  - `deviceId` (8 bytes)
  - `timestamp` (8 bytes Unix epoch ms)
  - `locationSource` (1 byte: `GPS_LIVE`, `GPS_LAST_KNOWN`, `NO_GPS_RSSI_ONLY`)
  - `latitude` / `longitude` / `gpsAccuracy` / `locationTimestamp`
  - `batteryLevel` (1 byte, 0-100%)
  - `freeText` (UTF-8 con longitud variable)
- **Firma Criptográfica**: Cada paquete se transmite sin cifrado de canal (para que cualquier nodo de la red pueda leerlo y retransmitirlo) pero firmado con criptografía asimétrica **Ed25519** usando la clave persistente del dispositivo para prevenir falsificación y spam.
- **TTL Ampliado**: Alcance de propagación configurado a 20 saltos.

### 2. Emisión Persistente en Segundo Plano (`EmergencyBeaconService`)
- Servicio *Foreground* persistente registrado con permisos `FOREGROUND_SERVICE_LOCATION` y `FOREGROUND_SERVICE_CONNECTED_DEVICE`.
- Retiene un `PARTIAL_WAKE_LOCK` del sistema (`SOSBlu:SOSBeaconWakeLock`) que garantiza la transmisión periódica cada 30 segundos aun con el dispositivo en modo doze, pantalla bloqueada o aplicación minimizada.

### 3. Modo de Energía "SOS Ultra" (`PowerManager`)
- Quinto perfil de energía diseñado para escenarios de supervivencia.
- Maximiza la potencia y prioridad de los anuncios BLE mientras suspende animaciones, sincronizaciones en segundo plano y procesamiento innecesario para extender la transmisión continua hasta por 48-72 horas según el estado de la batería.

### 4. Triangulación Táctica por Señal RSSI
- En colapsos de estructuras donde los receptores GPS no obtienen fijación satelital, la señal conmuta automáticamente al modo `NO_GPS_RSSI_ONLY`.
- La pantalla de rescate calcula la atenuación de potencia de la señal recibida (RSSI en dBm) para guiara los equipos de búsqueda por proximidad física (Menos de 5m, 5-15m, Distante).

### 5. Nodo Puente / Gateway Automático (`BridgeRelayService`)
- Monitorea la conectividad de red del dispositivo mediante `ConnectivityManager`.
- Al detectar enlace a internet (Wi-Fi, satelital o celular reactivado), serializa automáticamente los beacons recibidos en la malla local a un payload JSON estandarizado y los retransmite por HTTPS a la central de gestión de emergencias.
- Implementa filtros de deduplicación basados en firmas de tiempo e identificadores para evitar sobrecargas de red.

---

## Interfaz de Usuario y Usabilidad

- **Operación de Un Solo Toque**: Al abrir **SOSBlu**, la pantalla principal muestra directamente el botón gigante de activación de auxilio de 230dp en color rojo de emergencia.
- **Cero Emojis**: La interfaz sigue un estándar sobrio, técnico y clínico en Material Design 3, adecuado para agencias de defensa civil y equipos de rescate.
- **Separación de Funciones**: El canal de texto auxiliar en malla se mantiene desacoplado en un botón secundario para evitar distracciones en situaciones de pánico.

---

## Estructura del Proyecto

```text
app/src/main/java/com/bitchat/android/
├── protocol/
│   ├── BinaryProtocol.kt        [Definición de MessageType.SOS_BEACON (0x30u)]
│   └── SOSBeaconPayload.kt      [Serializador binario de faro de emergencia]
├── services/
│   ├── EmergencyBeaconService.kt[Servicio foreground con WakeLock para emisión SOS]
│   └── BridgeRelayService.kt   [Recolector, deduplicador y transmisor a Gateway HTTPS]
├── mesh/
│   ├── BluetoothMeshService.kt [Coordinador core de radio BLE]
│   ├── PacketRelayManager.kt   [Enrutador QoS con prioridad incondicional para SOS]
│   └── PowerManager.kt         [Perfil de energía SOS_ULTRA con telemetría]
└── ui/
    ├── EmergencyScreen.kt      [Interfaz táctica principal de auxilio y monitor]
    └── ChatHeader.kt           [Integración de acceso rápido SOSBlu]
```

---

## Compilación e Instalación

### Requisitos de Entorno
- JDK 17 / JDK 21
- Android SDK (API 26+)
- Gradle 8.x / 9.x

### Compilación del APK

```sh
git clone https://github.com/permissionlesstech/bitchat-android.git
cd bitchat-android
./gradlew :app:assembleDebug --dependency-verification=off --no-daemon
```

### Ubicación del APK Generado

```text
app/build/outputs/apk/debug/app-universal-debug.apk
```

### Instalación vía ADB

```sh
adb install -r app/build/outputs/apk/debug/app-universal-debug.apk
```

---

## Licencia y Privacidad

- Proyecto liberado bajo licencias de código abierto compatibles.
- Cumple estrictamente con las políticas de privacidad y protección de datos locales: no registra datos personales ni rastrea coordenadas históricas fuera de las emisiones explícitas de emergencia solicitadas por el usuario.
