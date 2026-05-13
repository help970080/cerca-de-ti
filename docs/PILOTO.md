# Plan de Pruebas Piloto — Cerca de Ti

## Objetivo del piloto

Validar en condiciones reales que Cerca de Ti:
1. **Detecta correctamente** situaciones de peligro genuinas (sensibilidad).
2. **No dispara** ante actividades cotidianas normales (especificidad / pocos falsos positivos).
3. **Entrega alertas** a contactos en menos de 30 segundos de forma confiable.
4. **No agota la batería** ni interfiere con el uso normal del dispositivo.
5. **Es legalmente sólido** en su operación y consentimiento.

---

## Fase Alpha — Familia y círculo cercano (semanas 4-6)

**Usuarios:** 5-10 personas del círculo familiar y profesional de Leo.

**Sugerencia de perfiles:**
- Leo y miembros adultos de su familia
- 2-3 gestores de campo de LeGaXi (perfil de cobrador en colonia brava)
- 1-2 promotoras de CelExpress (perfil de mujer trabajadora en transporte público)
- 1 adulto mayor del entorno (si aplica)

**Duración:** 2 semanas con monitoreo activo.

**Mecánica:**
1. Instalación asistida con onboarding presencial.
2. Configuración de contactos de prueba (entre sí, para validar entregas).
3. Uso normal 24/7.
4. Bitácora simple en hoja de cálculo compartida donde cada usuario registra:
   - Cada disparo (real o falso positivo) con timestamp y contexto
   - Cualquier comportamiento anómalo de la app
   - Impresión subjetiva de batería y rendimiento
5. Pruebas controladas semanales:
   - **Prueba A:** simular voz alterada (gritar a propósito durante 5 segundos)
   - **Prueba B:** simular forcejeo (sacudir el celular bruscamente 10 segundos)
   - **Prueba C:** decir palabra clave en voz normal
   - **Prueba D:** combinación de las tres
   - Registrar si dispara, qué nivel, latencia de entrega de alerta a contactos

**Criterios de éxito Alpha:**
- ✅ Detección: cuando se simula peligro deliberadamente, dispara al menos 80% de las veces.
- ✅ Falsos positivos: menos de 2 por usuario por semana de uso normal.
- ✅ Entrega: 95% de alertas llegan a contactos en menos de 30 segundos.
- ✅ Batería: incremento de consumo menor a 5% diario respecto a uso normal.
- ✅ Sin crashes en al menos 7 días continuos por dispositivo.

---

## Fase Beta — Comunidad ampliada (semanas 7-12)

**Usuarios:** 50-100 personas voluntarias.

**Cómo reclutar:**
- Boca a boca desde Alpha
- Grupos comunitarios en municipios del Eje Volcánico
- Colaboración con alguna ONG local de género o protección a colectivos vulnerables
- Difusión en redes sociales de LeGaXi/CelExpress con disclaimer claro de que es piloto

**Diversidad necesaria del piloto:**
- Geográfica: zonas urbanas y semi-rurales del centro de México
- Demográfica: mujeres y hombres, edades de 16 a 70
- Económica: distintos modelos de celulares (gama baja a alta, Android 8 a 14)
- Operadora: Telcel, Movistar, AT&T (para validar entrega de SMS)

**Mecánica:**
1. Distribución de APK firmado vía link directo (`cerca.legaxia.uk/descargar`) — todavía sin Play Store
2. Formulario inicial de consentimiento + perfil demográfico anónimo
3. Reporte de incidentes vía formulario en la propia app
4. Sesiones de retroalimentación cada 2 semanas vía WhatsApp/Zoom
5. Telemetría anónima opt-in para entender patrones agregados

**Criterios de éxito Beta:**
- ✅ Tasa de retención a 30 días superior al 60%
- ✅ Falsos positivos por usuario inferiores a 1 semanal
- ✅ Al menos 1 caso documentado donde la app activó alerta legítima en situación real (con consentimiento del involucrado para documentarlo)
- ✅ Tasa de desinstalación inferior al 25%
- ✅ Cero incidentes de seguridad o fuga de datos

---

## Fase Gamma — Lanzamiento público en Play Store

**Pre-requisitos:**
- ✅ Política de privacidad revisada por abogado
- ✅ Cuenta de desarrollador Google Play creada (USD 25)
- ✅ Listado de Play Store con descripción clara del propósito, capturas, video corto
- ✅ Documentación de permisos sensibles preparada para revisión manual
- ✅ Mecanismo de soporte (email + canal de issues en GitHub)
- ✅ Plan de respuesta a incidentes de seguridad

**Lanzamiento escalonado:**
1. Release como Beta abierta en Play Store (límite 10,000 usuarios)
2. Tras 2-4 semanas sin incidentes graves, promoción a producción
3. Disponibilidad inicial limitada a México; expansión a Latam tras 3 meses

---

## Métricas de telemetría (anonimizadas, opt-in)

Lo que **sí** medimos:
- Tipo de detección que disparó (audio/movimiento/keyword/combinada)
- Si el usuario canceló durante la cuenta atrás
- Latencia de entrega de alerta (sin contenido)
- Versión de la app, modelo del dispositivo, versión de Android
- Municipio (no más preciso que eso)
- Crashes y errores técnicos

Lo que **no** medimos:
- Identidad del usuario
- Identidad de contactos
- Contenido de la alerta
- Ubicación precisa
- Audio o foto
- Patrón de uso individual

Toda esta telemetría es **opt-in** durante onboarding y desactivable en cualquier momento.

---

## Pruebas técnicas obligatorias antes de cada release

### Pruebas funcionales
- [ ] Servicio sobrevive a reinicio del dispositivo
- [ ] Servicio sobrevive a modos de ahorro de batería (Doze, App Standby)
- [ ] Alerta dispara con dispositivo bloqueado
- [ ] Alerta dispara sin conexión a internet (envía SMS directo)
- [ ] Alerta dispara con datos pero sin WiFi
- [ ] Cuenta atrás cancela correctamente con un tap
- [ ] Evidencia se cifra correctamente en almacenamiento local
- [ ] Backup automático NO incluye evidencia (allowBackup configurado correctamente)
- [ ] Desinstalar borra todos los datos

### Pruebas de privacidad
- [ ] Audio NUNCA se persiste en disco fuera de evento disparado
- [ ] Inspeccionar tráfico de red: nada sale en operación normal
- [ ] Inspeccionar logs: no contienen información personal
- [ ] Permisos solicitados coinciden exactamente con los declarados
- [ ] Política de privacidad accesible desde dentro de la app

### Pruebas de seguridad
- [ ] APK firmado con llave segura (no debug)
- [ ] ProGuard/R8 ofuscación activada
- [ ] No hay llaves API hardcodeadas en código
- [ ] Comunicación con backend solo vía HTTPS (no fallback a HTTP)
- [ ] Pinning de certificado del backend

### Pruebas de rendimiento
- [ ] Consumo de batería medido con Android Battery Historian
- [ ] CPU promedio inferior al 5% durante operación normal
- [ ] RAM utilizada inferior a 80 MB en estado idle
- [ ] Sin memory leaks tras 24h de operación continua

---

## Plan de respuesta a incidentes

### Si la app dispara un falso positivo masivo (>10% de usuarios en 24h)
1. Pausa global vía remote kill switch (feature flag en backend)
2. Análisis de logs anónimos para identificar causa
3. Patch + release en menos de 72h
4. Comunicación pública en GitHub y a usuarios afectados

### Si se detecta vulnerabilidad de seguridad
1. Reporte privado al equipo (`seguridad@cerca.legaxia.uk`)
2. Patch antes de divulgación pública
3. CVE asignado si aplica
4. Aviso público con detalles tras 30 días del patch

### Si una autoridad solicita datos
1. Como el desarrollador no tiene acceso técnico a datos del usuario, no hay nada que entregar.
2. Cualquier orden judicial se responde con copia del código abierto y explicación técnica.
3. Si una orden judicial específica solicita logs del servidor relay, solo se entregan los logs operativos anonimizados que existan dentro de la ventana de retención de 30 días.

---

## Documentación post-piloto

Al cierre de cada fase, se publica en el repositorio:
- Resumen agregado anonimizado de resultados
- Lecciones aprendidas
- Cambios planeados para la siguiente fase
- Testimonios voluntarios de usuarios (con consentimiento)
