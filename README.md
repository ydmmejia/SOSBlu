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

<p align="center">
  <a href="https://github.com/ydmmejia/SOSBlu/releases">
    <img src="https://img.shields.io/github/v/release/ydmmejia/SOSBlu?color=B71C1C&style=for-the-badge&logo=github&label=Version" alt="Ultima Version" />
  </a>
  <a href="https://github.com/ydmmejia/SOSBlu/releases">
    <img src="https://img.shields.io/github/downloads/ydmmejia/SOSBlu/total?color=00C851&style=for-the-badge&logo=github&label=Descargas" alt="Descargas Totales" />
  </a>
  <img src="https://img.shields.io/badge/Android-API%2026%2B-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Android API 26+" />
  <img src="https://img.shields.io/badge/Red-BLE%20Mesh%20Offline-007ACC?style=for-the-badge&logo=bluetooth&logoColor=white" alt="BLE Mesh" />
  <img src="https://img.shields.io/badge/Criptografia-Ed25519%20%2B%20Noise-6C757D?style=for-the-badge&logo=shield&logoColor=white" alt="Ed25519 Noise" />
</p>

## Resumen Ejecutivo

**SOSBlu** es una plataforma móvil de telecomunicaciones de emergencia diseñada para el rescate de víctimas atrapadas tras desastres naturales (terremotos, colapsos estructurales, deslaves) en escenarios con colapso total de infraestructura celular e internet.

La aplicación transforma cada dispositivo Android en un transceptor de la red malla Bluetooth Low Energy (BLE). Permite a las víctimas emitir un faro de auxilio continuo (`SOS_BEACON`) con un solo toque. La señal se propaga de forma salto-a-salto a través de otros teléfonos cercanos hasta alcanzar a personas o brigadistas de rescate.

---

## Guía de Instalación en Android y Advertencia de Seguridad

> [!IMPORTANT]
> **¿Por qué Android muestra una advertencia al instalar?**  
> Al descargar e instalar **SOSBlu** directamente desde GitHub (fuera de Google Play Store), el sistema Android mostrará un aviso de seguridad estándar (*"Aplicación de fuente desconocida"* o *"Play Protect: Aplicación no reconocida"*).  
> **Esto es 100% normal** en aplicaciones independientes de código abierto distribuidas en archivo `.apk`.

### Pasos para Instalar en 3 Segundos:
1. Al descargar el archivo `SOSBlu-v1.0.0.apk`, abre la notificación de descarga o la carpeta **Descargas**.
2. Si Android bloquea la instalación inicial, toca en **"Configuración"** / **"Ajustes"** y activa la casilla **"Permitir desde esta fuente"**.
3. Si Play Protect muestra una ventana de aviso, toca en **"Más detalles"** y selecciona **"Instalar de todas formas"**.
4. Abre **SOSBlu** y concede los permisos de Bluetooth y Ubicación para dejar la aplicación lista.

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

### 4. Medición por Intensidad de Señal BLE (dBm)
- En colapsos de estructuras donde los receptores GPS no obtienen fijación satelital, la señal conmuta automáticamente al modo `NO_GPS_RSSI_ONLY`.
- La pantalla de alertas mide la atenuación de potencia de la señal recibida (RSSI en dBm) para guiar a los equipos de búsqueda por proximidad física (Señal Muy Fuerte, Media, Débil).

---

## Interfaz de Usuario y Usabilidad

- **Operación de Un Solo Toque**: Al abrir **SOSBlu**, la pantalla principal muestra directamente el botón gigante de activación de auxilio en color rojo de emergencia.
- **Cero Emojis**: La interfaz sigue un estándar sobrio, técnico y clínico en Material Design 3 en español neutro.
- **Conmutador de Tema Claro / Oscuro**: Opción directa en la barra superior para cambiar entre tema oscuro táctico y tema claro clínico.

---

## Estructura del Proyecto

```text
app/src/main/java/com/bitchat/android/
├── protocol/
│   ├── BinaryProtocol.kt        [Definición de MessageType.SOS_BEACON (0x30u)]
│   └── SOSBeaconPayload.kt      [Serializador binario de faro de emergencia]
├── services/
│   ├── EmergencyBeaconService.kt[Servicio foreground con WakeLock para emisión SOS]
│   └── BridgeRelayService.kt   [Recolector y deduplicador de alertas de red malla]
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
git clone https://github.com/ydmmejia/SOSBlu.git
cd SOSBlu
./gradlew :app:assembleDebug --dependency-verification=off --no-daemon
```

### Ubicación del APK Generado

```text
app/build/outputs/apk/debug/app-universal-debug.apk
```

---

## Licencia y Privacidad

- Proyecto liberado bajo licencias de código abierto compatibles.
- Cumple estrictamente con las políticas de privacidad y protección de datos locales: no registra datos personales ni rastrea coordenadas históricas fuera de las emisiones explícitas de emergencia solicitadas por el usuario.
