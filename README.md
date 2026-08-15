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
    <img src="https://img.shields.io/badge/Versión-v1.1.0-B71C1C?style=for-the-badge&logo=github&labelColor=1E293B" alt="Versión v1.1.0" />
  </a>
  <img src="https://img.shields.io/badge/Android-API%2026%2B-3DDC84?style=for-the-badge&logo=android&logoColor=white&labelColor=1E293B" alt="Android API 26+" />
  <img src="https://img.shields.io/badge/Red-BLE%20Mesh%20Offline-0284C7?style=for-the-badge&logo=bluetooth&logoColor=white&labelColor=1E293B" alt="BLE Mesh" />
  <img src="https://img.shields.io/badge/Criptografía-Ed25519%20%2B%20Noise-64748B?style=for-the-badge&logo=shield&logoColor=white&labelColor=1E293B" alt="Ed25519 Noise" />
</p>

---

## Descargo de Responsabilidad (Disclaimer)

> [!WARNING]
> **AVISO IMPORTANTE Y DE RESPONSABILIDAD**:  
> **SOSBlu NO es una aplicación oficial gubernamental, militar, de defensa civil ni de organismos estatales de atención de emergencias.**  
> Esta aplicación es un proyecto independiente de código abierto creado por una iniciativa ciudadana para servir como una opción complementaria y comunitaria de comunicación salto-a-salto fuera de línea durante catástrofes y colapsos de infraestructura.  
> En cualquier situación de emergencia donde exista red telefónica o celular disponible, **comuníquese siempre de forma prioritaria con las líneas y autoridades oficiales de rescate de su país** (ej. 911 / 123 / 112).

---

## Contacto del Desarrollador

Para consultas técnicas, sugerencias o reporte de incidentes:
- **Correo Electrónico:** [yemejiam@unal.edu.co](mailto:yemejiam@unal.edu.co)
- **Repositorio:** [github.com/ydmmejia/SOSBlu](https://github.com/ydmmejia/SOSBlu)

---

## Resumen Ejecutivo

**SOSBlu** es una plataforma móvil de telecomunicaciones de emergencia diseñada para la localización y rescate de personas atrapadas tras desastres naturales (terremotos, colapsos estructurales, deslaves) en escenarios con caída total de internet y redes móviles.

La aplicación convierte cada teléfono en un nodo de una red malla Bluetooth Low Energy (BLE). Permite a una víctima emitir una señal de auxilio continua (`SOS_BEACON`) con un solo toque. La señal se retransmite automáticamente de teléfono a teléfono (hasta 20 saltos) hasta alcanzar a personas o brigadas de búsqueda en la superficie.

---

## Características Principales (v1.1.0)

1. **Operación 100% Fuera de Línea (Offline)**:
   - Funciona sin internet, sin saldo, sin tarjeta SIM y **es compatible con Modo Avión** (manteniendo el Bluetooth encendido).
2. **Emisión Continua con Pantalla Bloqueada (`EmergencyBeaconService`)**:
   - Transmisión persistente en segundo plano mediante un servicio Foreground y `WakeLock` del sistema.
3. **Alerta y Vibración Háptica Inmediata**:
   - Los celulares cercanos que captan la señal de una víctima vibran de inmediato con un patrón táctico de auxilio (`... --- ...`).
4. **Deduplicación Estricta de Alertas por Persona**:
   - Si una víctima transmite constantemente, el sistema actualiza su tarjeta en tiempo real (coordenadas, tiempo transcurrido y batería) manteniendo el contador en **1 alerta** por persona afectada.
5. **Apertura Directa en Mapas**:
   - Visualización de coordenadas GPS precisas con acceso rápido a Google Maps / OpenStreetMap o copia directa al portapapeles.
6. **Diseño Táctico y Responsivo**:
   - Interfaz en español neutro, sobria y adaptada a cualquier tamaño de pantalla (desde teléfonos compactos hasta dispositivos grandes).

---

## Guía de Instalación en Android

> [!IMPORTANT]
> **¿Por qué Android muestra una advertencia al instalar?**  
> Al descargar e instalar **SOSBlu** directamente desde GitHub (en formato `.apk`), Android mostrará un aviso estándar (*"Aplicación de fuente desconocida"* o *"Play Protect: Aplicación no reconocida"*).  
> **Esto es 100% normal** en aplicaciones independientes de código abierto distribuidas fuera de Google Play Store.

### Pasos para Instalar:
1. Descarga el archivo `SOSBlu-v1.1.0.apk` desde la sección de **Releases** de este repositorio.
2. Abre el archivo descargado. Si Android bloquea la instalación, presiona **"Ajustes" / "Configuración"** y activa la opción **"Permitir desde esta fuente"**.
3. Si Google Play Protect muestra una ventana de aviso, toca en **"Más detalles"** y luego en **"Instalar de todas formas"**.
4. Abre **SOSBlu** y concede los permisos de Bluetooth y Ubicación para dejar el nodo activo.

---

## Arquitectura y Protocolo

### 1. Protocolo Binario de Emergencia (`SOS_BEACON` - `0x30u`)
- **Estructura Binaria**: Serialización compacta en `ByteBuffer` optimizada para transmisiones BLE de bajo consumo:
  - `deviceId` (8 bytes)
  - `timestamp` (8 bytes Unix epoch ms)
  - `locationSource` (1 byte: `GPS_LIVE`, `GPS_LAST_KNOWN`, `NO_GPS_RSSI_ONLY`)
  - `latitude` / `longitude` / `gpsAccuracy` / `locationTimestamp`
  - `batteryLevel` (1 byte, 0-100%)
  - `freeText` (UTF-8 opcional para detalles de ubicación: ej. "Piso 2, bajo losa")
- **Firma Criptográfica**: Cada paquete se firma con criptografía asimétrica **Ed25519** usando la clave del dispositivo para evitar falsificación y spam en la red.
- **TTL Ampliado**: Alcance de propagación configurado hasta 20 saltos de retransmisión.

---

## Estructura del Proyecto

```text
app/src/main/java/com/bitchat/android/
├── protocol/
│   ├── BinaryProtocol.kt        [Definición de MessageType.SOS_BEACON (0x30u)]
│   └── SOSBeaconPayload.kt      [Serializador binario de faro de emergencia]
├── services/
│   ├── EmergencyBeaconService.kt[Servicio foreground con WakeLock para emisión SOS]
│   └── BridgeRelayService.kt   [Recolector, vibración y deduplicador de alertas]
├── mesh/
│   ├── BluetoothMeshService.kt [Coordinador de radio BLE con soporte para Modo Avión]
│   ├── PacketRelayManager.kt   [Enrutador QoS con prioridad incondicional para SOS]
│   └── PowerManager.kt         [Perfil de energía y telemetría de batería]
└── ui/
    ├── EmergencyScreen.kt      [Pantalla principal táctica y monitor de alertas]
    └── ChatHeader.kt           [Acceso directo SOSBlu]
```

---

## Compilación Local

```sh
git clone https://github.com/ydmmejia/SOSBlu.git
cd SOSBlu
./gradlew :app:assembleDebug --dependency-verification=off --no-daemon
```

El archivo APK resultante se genera en:
`app/build/outputs/apk/debug/app-debug.apk`

---

## Licencia

Este proyecto está bajo licencia de código abierto. Desarrollado con fines humanitarios y de preservación de la vida en situaciones de catástrofe.
