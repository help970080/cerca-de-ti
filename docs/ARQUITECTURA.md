# Arquitectura — Cerca de Ti

## Principio rector

**Todo el procesamiento de señales sensibles ocurre on-device. El servidor solo es un relay de notificaciones.** Esto no es solo una decisión de privacidad — es la única forma de que el sistema sea legalmente defendible, sostenible económicamente sin lucro, y resistente a fallas de red.

## Componentes

```
┌─────────────────────────────────────────────────────────┐
│                  CELULAR DEL USUARIO                    │
│                                                         │
│  ┌──────────────────────────────────────────────────┐  │
│  │           ForegroundService (24/7)               │  │
│  │  ┌────────────┐  ┌────────────┐  ┌────────────┐  │  │
│  │  │ AudioRing  │  │ MotionRing │  │ LocationRing│  │  │
│  │  │  Buffer    │  │  Buffer    │  │   Buffer    │  │  │
│  │  │ (últimos   │  │ (últimos   │  │ (últimos    │  │  │
│  │  │  60 seg)   │  │  60 seg)   │  │  10 min)    │  │  │
│  │  └─────┬──────┘  └─────┬──────┘  └─────┬──────┘  │  │
│  │        │                │                │        │  │
│  │        └────────────────┴────────────────┘        │  │
│  │                         │                         │  │
│  │                  ┌──────▼───────┐                 │  │
│  │                  │  Detector    │                 │  │
│  │                  │  (rule-based │                 │  │
│  │                  │   en MVP, ML │                 │  │
│  │                  │   en Fase 1) │                 │  │
│  │                  └──────┬───────┘                 │  │
│  │                         │                         │  │
│  │                  ┌──────▼───────┐                 │  │
│  │                  │ AlertManager │                 │  │
│  │                  │  (30s cancel │                 │  │
│  │                  │   countdown) │                 │  │
│  │                  └──────┬───────┘                 │  │
│  └─────────────────────────┼──────────────────────────┘  │
│                            │                          │
│  ┌─────────────────────────▼──────────────────────────┐ │
│  │  EvidenceStore (cifrado AES-256, local)            │ │
│  │  - Audio 60s alrededor del evento                  │ │
│  │  - Foto frontal silenciosa                         │ │
│  │  - Trayectoria GPS                                 │ │
│  │  - Timestamp + hash de integridad                  │ │
│  └─────────────────────────┬──────────────────────────┘ │
└────────────────────────────┼────────────────────────────┘
                             │
                             │ HTTPS (solo cuando dispara)
                             │
              ┌──────────────▼────────────────┐
              │     Backend Relay (Render)    │
              │  - POST /alerta → fan-out a:  │
              │      • SMS (Twilio/Zadarma)   │
              │      • WhatsApp (Baileys)     │
              │      • FCM push a contactos   │
              │  - GET /evidencia/:token →    │
              │      sirve a contactos auto-  │
              │      rizados con link único   │
              │  - NUNCA guarda audio/foto;   │
              │    solo metadata mínima       │
              └───────────────────────────────┘
```

## Stack técnico

### App Android (cliente)

| Capa | Tecnología | Justificación |
|---|---|---|
| Lenguaje | Kotlin | Estándar Android, ya manejado en repo `Celular` |
| Min SDK | 26 (Android 8.0) | Cubre ~95% del mercado mexicano |
| Target SDK | 34 (Android 14) | Requisito Play Store |
| Background | ForegroundService tipo `microphone` + `location` | Único modo legítimo de monitoreo continuo en Android 14 |
| Audio | AudioRecord (PCM 16kHz mono) | Permite acceso al stream sin guardar en disco |
| Movimiento | SensorManager (accelerometer + gyroscope) | API nativa, bajo consumo |
| Ubicación | FusedLocationProviderClient | Balance precisión/batería |
| Cifrado local | AndroidX Security (EncryptedFile, MasterKey) | Cifrado AES-256 transparente |
| Detector MVP | Reglas heurísticas en Kotlin puro | Auditable, explicable, sin caja negra |
| Detector Fase 1 | TensorFlow Lite + modelo cuantizado | On-device, sin envío de datos |
| Notificación local | NotificationCompat (canal HIGH_PRIORITY) | Vibración + sonido para cuenta atrás |
| UI | Jetpack Compose | Moderno, menos código |

### Backend relay (mínimo)

| Componente | Tecnología | Justificación |
|---|---|---|
| Runtime | Node.js 20 en Render | Reaprovecha infraestructura existente |
| Framework | Express | Ya usado en `Celular` backend |
| SMS | Zadarma o Twilio | Zadarma ya integrado en pipeline LeGaXi |
| WhatsApp | Baileys | Ya usado en bot `wha1` |
| Push | Firebase Cloud Messaging | Gratuito, estándar Android |
| Storage | Ninguno permanente | El backend NO guarda audio ni fotos |
| Logging | Solo agregados anónimos (count de alertas/día/región) | Sin PII |

### Dominio y hosting

- **App ID Android:** `uk.legaxia.cercadeti`
- **Subdominio:** `cerca.legaxia.uk` (landing + política de privacidad + viewer de evidencia)
- **Backend:** `api.cerca.legaxia.uk` apuntando a Render
- **Repositorio:** `help970080/cerca-de-ti` (público, MIT)

## Flujo de detección (MVP, basado en reglas)

```kotlin
// Pseudocódigo del detector MVP
fun evaluateRisk(window: SensorWindow): RiskScore {
    var score = 0

    // Señal 1: Audio
    if (window.audio.avgDbAbove(baseline.audio.avgDb + 12, durationSec = 5))
        score += 30
    if (window.audio.containsKeyword(user.codeWords))
        score += 100  // disparo directo
    if (window.audio.pitchVarianceAbove(baseline.audio.pitchVar * 2.5))
        score += 20

    // Señal 2: Movimiento
    if (window.motion.maxAccelMagnitude > 25.0)  // m/s², impacto fuerte
        score += 25
    if (window.motion.suddenStop(thresholdSec = 3))  // caída + inmovilidad
        score += 30
    if (window.motion.continuousHighIntensity(durationSec = 8))  // forcejeo
        score += 25

    // Señal 3: Ubicación
    if (window.location.speedMs > 8 && !user.isInTransit())  // ~30 km/h sin estar en transporte
        score += 15
    if (window.location.distanceFromBaseline > 500)  // zona no habitual
        score += 10

    // Señal 4: Dispositivo
    if (window.device.failedUnlockAttempts > 3)
        score += 20
    if (window.device.powerButtonPressed > 3)  // intento de apagado
        score += 25

    return RiskScore(
        total = score,
        level = when {
            score >= 100 -> RiskLevel.CRITICAL   // dispara inmediato
            score >= 70  -> RiskLevel.HIGH       // cuenta atrás 30s
            score >= 50  -> RiskLevel.MEDIUM     // notificación silenciosa
            else         -> RiskLevel.LOW
        }
    )
}
```

## Estructura del proyecto

```
cerca-de-ti/
├── app/
│   ├── src/main/
│   │   ├── AndroidManifest.xml
│   │   ├── java/uk/legaxia/cercadeti/
│   │   │   ├── CercaApp.kt              # Application class
│   │   │   ├── service/
│   │   │   │   ├── GuardianService.kt   # ForegroundService principal
│   │   │   │   ├── AudioMonitor.kt      # Ring buffer de audio
│   │   │   │   ├── MotionMonitor.kt     # Acelerómetro/giroscopio
│   │   │   │   └── LocationMonitor.kt   # GPS
│   │   │   ├── detector/
│   │   │   │   ├── RiskDetector.kt      # Lógica de scoring
│   │   │   │   ├── BaselineLearner.kt   # Aprende patrones normales del usuario
│   │   │   │   └── KeywordSpotter.kt    # Detección de palabras clave
│   │   │   ├── alert/
│   │   │   │   ├── AlertManager.kt      # Coordinador de alertas
│   │   │   │   ├── CountdownActivity.kt # Pantalla de 30s para cancelar
│   │   │   │   ├── EvidencePacker.kt    # Empaqueta audio/foto/GPS
│   │   │   │   └── RelayClient.kt       # Envía al backend
│   │   │   ├── storage/
│   │   │   │   ├── EvidenceStore.kt     # Cifrado AES-256 local
│   │   │   │   ├── ContactsRepo.kt      # Contactos de confianza
│   │   │   │   └── SettingsRepo.kt      # Preferencias
│   │   │   └── ui/
│   │   │       ├── MainActivity.kt
│   │   │       ├── OnboardingActivity.kt # Consentimiento + setup inicial
│   │   │       ├── ContactsActivity.kt
│   │   │       └── HistoryActivity.kt    # Historial de alertas
│   │   └── res/
│   │       ├── values/strings.xml        # Español MX
│   │       ├── layout/
│   │       └── xml/
│   ├── build.gradle.kts
│   └── proguard-rules.pro
├── backend/                              # Relay Node.js mínimo
│   ├── server.js
│   ├── package.json
│   └── README.md
├── docs/
│   ├── ARQUITECTURA.md                   # este archivo
│   ├── PRIVACIDAD.md
│   ├── MARCO_LEGAL.md
│   ├── ROADMAP.md
│   └── PILOTO.md
├── .github/workflows/
│   └── build-apk.yml                     # CI/CD vía GitHub Actions
├── LICENSE                                # MIT
├── README.md
└── settings.gradle.kts
```

## Permisos Android requeridos

| Permiso | Por qué | Cuándo se solicita |
|---|---|---|
| `RECORD_AUDIO` | Detectar voz alterada y palabras clave | Onboarding, después de explicación |
| `ACCESS_FINE_LOCATION` | Ubicación para alerta y baseline de zonas | Onboarding |
| `ACCESS_BACKGROUND_LOCATION` | Monitoreo en background | Solicitud separada Android 11+ |
| `FOREGROUND_SERVICE` | Servicio persistente | Auto-declarado |
| `FOREGROUND_SERVICE_MICROPHONE` | Tipo de servicio (Android 14) | Auto-declarado |
| `FOREGROUND_SERVICE_LOCATION` | Tipo de servicio (Android 14) | Auto-declarado |
| `POST_NOTIFICATIONS` | Notificación persistente del servicio | Android 13+ |
| `SEND_SMS` | Enviar SMS de respaldo si no hay datos | Solo si usuario lo activa |
| `CAMERA` | Foto frontal silenciosa al disparar | Solo si usuario lo activa |
| `VIBRATE` | Cuenta atrás de cancelación | Auto-declarado |
| `WAKE_LOCK` | Mantener procesamiento al disparar | Auto-declarado |
| `INTERNET` | Enviar alerta a contactos | Auto-declarado |
| `RECEIVE_BOOT_COMPLETED` | Re-iniciar servicio tras reinicio | Auto-declarado |

## Consumo de batería estimado

| Componente | Consumo / hora | Estrategia |
|---|---|---|
| Acelerómetro | 0.1% | Sensor SAMPLING_PERIOD_NORMAL (200ms) |
| Audio (VAD) | 1.5% | Solo procesa cuando detecta voz; idle = sleep |
| GPS | 0.8% | Actualizaciones cada 60s o 100m de cambio |
| Detector (CPU) | 0.5% | Evaluación cada 2 segundos |
| **TOTAL** | **~3% / hora** | Aceptable para uso continuo |

Con batería de 4000 mAh típica, el overhead es de ~30% en uso continuo de 24h, lo cual está alineado con apps de fitness o navegación. La mayoría de usuarios cargan diariamente, por lo que el impacto real es marginal.

## Decisiones explícitas de diseño

1. **No usamos ML en el MVP.** El detector basado en reglas es auditable y explicable. ML viene en Fase 1 cuando tengamos datos de uso real anonimizados.

2. **No grabamos audio crudo continuo en disco.** Solo el ring buffer en RAM (60s rotando). Si no hay alerta, el audio se descarta.

3. **No usamos servicios de Google para procesamiento.** Speech-to-text para palabras clave usa modelos on-device (Vosk Kaldi o modelos Whisper cuantizados a futuro).

4. **El backend NO guarda evidencia.** Solo enruta notificaciones. La evidencia se sube cifrada al storage personal del usuario (Google Drive del usuario, Dropbox del usuario) o queda solo local.

5. **No hay "modo administrador" ni vigilancia parental remota en MVP.** La integración familiar viene en Fase 1 con consentimiento explícito del menor.

6. **Open source desde el día uno.** Auditabilidad pública es la única forma de generar confianza en una app que pide micrófono permanente.

## Lo que NO es Cerca de Ti

- No es un botón de pánico tradicional (eso ya existe en cada celular)
- No es un rastreador familiar tipo Life360
- No es un grabador continuo (eso sería ilegal y abusivo)
- No es un dispositivo médico (no diagnostica, no monitorea salud clínica)
- No es un servicio de respuesta de emergencia (avisa a contactos, no manda patrullas)
- No es un producto comercial (no se cobra, no hay anuncios, no se venden datos)
