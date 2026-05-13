# Hoja de Ruta — Cerca de Ti

## Fase 0 — MVP (semanas 1-4)

**Objetivo:** APK funcional, instalable, que detecta situaciones obvias de peligro y avisa a contactos.

### Semana 1: Cimientos
- [x] Estructura del proyecto Kotlin
- [x] AndroidManifest con permisos declarados
- [ ] Pantalla de onboarding con consentimiento explícito
- [ ] Configuración de contactos de confianza (mínimo 1, máximo 5)
- [ ] Configuración de palabras clave personalizadas

### Semana 2: Detección
- [ ] `GuardianService` (ForegroundService) operando 24/7
- [ ] `AudioMonitor` con ring buffer de 60s
- [ ] `MotionMonitor` con detección de aceleración, caída, forcejeo
- [ ] `LocationMonitor` con FusedLocationProviderClient
- [ ] `RiskDetector` con reglas heurísticas (sin ML)
- [ ] Detección básica de palabras clave (string matching simple)

### Semana 3: Alerta
- [ ] `AlertManager` con escalado por niveles
- [ ] `CountdownActivity` con cuenta atrás de 30s cancelable
- [ ] `EvidencePacker` que empaqueta audio + GPS + foto
- [ ] `RelayClient` para enviar al backend
- [ ] Backend Node.js mínimo en Render con endpoints `/alerta` y `/evidencia/:token`
- [ ] Integración SMS (Zadarma o Twilio)
- [ ] Integración WhatsApp (Baileys, aprovechando bot existente)

### Semana 4: Validación
- [ ] Pruebas en dispositivos reales (mínimo 3 modelos Android distintos)
- [ ] Optimización de batería
- [ ] Encriptación local con AndroidX Security
- [ ] CI/CD con GitHub Actions para build automatizado de APK
- [ ] Política de privacidad publicada en `cerca.legaxia.uk`
- [ ] APK firmado listo para Play Store

---

## Fase 1 — Inteligencia (semanas 5-12)

**Objetivo:** reducir falsos positivos, mejorar precisión.

- [ ] `BaselineLearner`: aprende los patrones normales del usuario (voz, rutinas, zonas) durante 1-2 semanas tras instalar
- [ ] Modelo TensorFlow Lite cuantizado para detección de estrés vocal (basado en datasets públicos como CREMA-D, RAVDESS adaptados a español)
- [ ] Modelo de reconocimiento de palabras clave on-device (Vosk Kaldi en español)
- [ ] Vinculación parental con dashboard de alertas (no de contenido)
- [ ] Modo escuela/oficina con activación silenciosa de grabación de evidencia por palabra clave
- [ ] Foto frontal silenciosa al disparar alerta
- [ ] Detección de robo del celular (patrón específico: forcejeo + intento de apagado + cambio rápido de zona)

---

## Fase 2 — Escala y comunidad (mes 4-12)

- [ ] Versión iOS (Swift, código independiente)
- [ ] Versión Wear OS para smartwatch con HR real
- [ ] Mapa anónimo de zonas de alerta (agregado, sin identificadores personales)
- [ ] Integración opcional con C5/C4 estatal (si autoridades cooperan, modelo de "consentimiento de envío", no de monitoreo)
- [ ] Multi-idioma: español MX, inglés, portugués
- [ ] Modo offline-first robusto: envío de alerta vía SMS directo cuando no hay datos

---

## Métricas de éxito

**Para Fase 0:**
- 100 usuarios reales instalados (familia + equipo LeGaXi/CelExpress + voluntarios)
- < 1 falso positivo crítico por usuario por mes
- > 90% tasa de entrega de alertas a contactos en < 30 segundos
- 0 incidentes de fuga de datos
- 0 quejas formales por privacidad

**Para Fase 1:**
- 10,000 usuarios activos
- Validación independiente del modelo de detección (mínimo 1 universidad o ONG)
- Publicación en Play Store sin restricciones

**Para Fase 2:**
- 100,000 usuarios activos
- Mínimo 5 casos documentados (con consentimiento de los involucrados) donde la app contribuyó a evitar o documentar violencia

---

## Lo que medimos para mejorar (anonimizado y agregado)

- Conteo de disparos por tipo de detección (audio/movimiento/keyword)
- Tasa de cancelación por el usuario (indicador de falsos positivos)
- Latencia de entrega de alertas
- Cobertura geográfica (a nivel municipio, sin precisión mayor)
- Versión Android, modelo del dispositivo

**No medimos:** quién usa la app, identidades, contenido de alertas, rutas, conversaciones, comportamiento individual.
