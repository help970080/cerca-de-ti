# Política de Privacidad — Cerca de Ti

**Última actualización:** 12 de mayo de 2026
**Responsable del tratamiento:** Leonardo Luna Mendoza, operando bajo el proyecto sin fines de lucro "Cerca de Ti".
**Contacto:** privacidad@cerca.legaxia.uk

---

## Resumen en un párrafo

Cerca de Ti es una aplicación gratuita, sin fines de lucro y de código abierto que monitorea señales del propio dispositivo donde está instalada (audio, movimiento, ubicación) para detectar situaciones de peligro y avisar a contactos que **tú** eliges. **Todo el procesamiento ocurre en tu celular.** Ningún audio, foto o ubicación sale de tu dispositivo salvo cuando se dispara una alerta, y cuando sale, va únicamente a los contactos que tú configuraste — nunca a un servidor central. No vendemos datos, no mostramos anuncios, no rastreamos comportamiento.

---

## 1. Qué datos procesa la app

### 1.1 En tu dispositivo (procesamiento local)

La app accede continuamente, en segundo plano, a:

- **Audio del micrófono**, mantenido únicamente en memoria RAM en un buffer rotativo de 60 segundos. Este audio NO se guarda en disco, NO se transmite a internet, y se descarta automáticamente conforme entra audio nuevo.
- **Datos del acelerómetro y giroscopio** para detectar movimientos bruscos. Se procesan en memoria.
- **Ubicación GPS aproximada** para detectar cambios de zona y construir un patrón personal de zonas habituales. Se almacena cifrada localmente.
- **Estado del dispositivo**: intentos de desbloqueo, presiones del botón de encendido. Solo se evalúan en memoria.

### 1.2 Cuando se dispara una alerta

Cuando el detector identifica una situación de peligro y tú no la cancelas dentro de los 30 segundos de cuenta atrás, la app:

- Guarda en almacenamiento cifrado del dispositivo: audio de 60 segundos alrededor del evento, foto frontal silenciosa (si activaste esa opción), trayectoria GPS de los últimos 10 minutos, y timestamp.
- Envía a los contactos que configuraste: un SMS y/o mensaje de WhatsApp con tu ubicación y un enlace único de un solo uso para acceder a la evidencia.
- Transmite metadatos mínimos al servidor relay para enrutar las notificaciones a tus contactos (ver sección 2).

### 1.3 Lo que la app NUNCA hace

- ❌ NO graba audio continuo en disco.
- ❌ NO transmite audio crudo a servidores.
- ❌ NO comparte datos con anunciantes ni terceros comerciales.
- ❌ NO usa los datos para entrenar modelos de IA sin tu consentimiento explícito separado.
- ❌ NO accede a tu lista de contactos sin tu autorización explícita.
- ❌ NO te localiza para fines distintos de detectar situaciones de peligro.
- ❌ NO permite que un tercero (padre, pareja, empleador) vea tu actividad en tiempo real. La integración familiar opcional (Fase 1) solo envía alertas críticas, no contenido continuo.

---

## 2. Qué sucede en el servidor relay

El servidor `api.cerca.legaxia.uk` cumple una sola función: **enrutar notificaciones de alerta a los contactos que tú elegiste.** Específicamente:

- **Recibe:** un identificador anónimo del dispositivo, los números de teléfono de tus contactos (cifrados en tránsito), el texto de la alerta, y un enlace único firmado al servidor de evidencia local de tu dispositivo.
- **Envía:** SMS, mensaje de WhatsApp y notificación push a esos contactos.
- **NO guarda:** audio, fotos, video, contenido de evidencia.
- **Guarda únicamente, por máximo 30 días:** un registro técnico mínimo (timestamp, código de país aproximado, éxito/fallo de entrega) usado solo para diagnóstico operativo. Este registro no contiene tu identidad, tu nombre, tus contactos ni el contenido de la alerta.

Si en el futuro este servidor se cae o se cierra, la app puede seguir funcionando enviando SMS directos desde tu propio celular a tus contactos, sin pasar por servidor alguno.

---

## 3. Base legal del tratamiento

Conforme a la **Ley Federal de Protección de Datos Personales en Posesión de los Particulares** (LFPDPPP) de México y su Reglamento:

- **Consentimiento expreso** del usuario, obtenido en la pantalla de bienvenida antes de cualquier captura de datos sensibles.
- **Finalidad específica:** detección y respuesta a situaciones de peligro de la persona titular del dispositivo.
- **Principio de minimización:** solo se procesan los datos estrictamente necesarios para la finalidad declarada.
- **Principio de información:** este aviso de privacidad está disponible permanentemente dentro de la app y en `cerca.legaxia.uk/privacidad`.

Las grabaciones de audio que se capturan al disparar una alerta corresponden al supuesto del **Artículo 16 constitucional** y al **Artículo 210 del Código Nacional de Procedimientos Penales**: el usuario es interviniente de su propio entorno sonoro y tiene derecho a documentarlo. La evidencia es propiedad del usuario y solo el usuario decide compartirla.

---

## 4. Tus derechos (ARCO)

Tienes derecho a:

- **Acceso:** ver todos los datos que la app tiene de ti, desde el menú "Mis datos" dentro de la app.
- **Rectificación:** corregir datos incorrectos.
- **Cancelación:** borrar toda la información generada por la app. Es un botón único en "Configuración → Borrar todos mis datos".
- **Oposición:** detener el procesamiento sin desinstalar la app (pausar el servicio).

Para ejercer cualquier derecho ARCO, no necesitas contactarnos: todo es operable desde la app. Si requieres asistencia, escribe a `privacidad@cerca.legaxia.uk`.

---

## 5. Menores de edad

Cerca de Ti puede ser instalado en dispositivos de personas mayores de 13 años con consentimiento parental, o personas mayores de 16 años con consentimiento propio.

**Modo de vinculación parental (opcional, Fase 1):**
- Solo está disponible si el menor lo activa expresamente.
- El padre/madre recibe únicamente **alertas críticas** disparadas, no contenido continuo.
- El menor puede revocar la vinculación en cualquier momento.
- En la app del menor se muestra claramente qué se comparte con la cuenta vinculada.

**Lo que NO permite Cerca de Ti:**
- No permite que un adulto monitoree silenciosamente a un menor sin que el menor lo sepa.
- No permite escuchar conversaciones del menor.
- No permite ver ubicación en tiempo real (solo durante alertas activas).

---

## 6. Seguridad

- **Cifrado en reposo:** AES-256 mediante AndroidX Security `EncryptedFile`. Las llaves se almacenan en el Android Keystore, protegidas por hardware en dispositivos compatibles.
- **Cifrado en tránsito:** TLS 1.3 en todas las comunicaciones con el servidor relay.
- **Enlaces de evidencia:** los enlaces enviados a tus contactos son tokens de un solo uso, firmados, con expiración de 72 horas.
- **Sin acceso del desarrollador:** ni Leonardo Luna Mendoza ni ningún colaborador del proyecto tiene acceso técnico a tus datos. El código es abierto y auditable en GitHub.

---

## 7. Retención

- **En tu dispositivo:** las evidencias de alertas se conservan hasta que tú decidas borrarlas. Por defecto, se rotan después de 90 días salvo que las marques como "guardar permanentemente".
- **En el servidor relay:** los logs operativos se borran automáticamente después de 30 días.
- **Si desinstalas la app:** toda la información local se elimina con la desinstalación. Los logs del servidor también se anonimizan adicionalmente al detectar que un dispositivo dejó de comunicar por más de 60 días.

---

## 8. Transferencias internacionales

El servidor relay opera en **Render**, con regiones en Estados Unidos y Frankfurt. Los datos transmitidos son únicamente los metadatos mínimos descritos en la sección 2. Render es un proveedor con cumplimiento GDPR y SOC 2 Type II.

No se realizan otras transferencias internacionales.

---

## 9. Código abierto y auditabilidad

Todo el código fuente de Cerca de Ti, tanto la aplicación Android como el servidor relay, está disponible públicamente en:

**https://github.com/help970080/cerca-de-ti**

Cualquier persona puede auditar que el código hace exactamente lo que dice este aviso. Las contribuciones técnicas para mejorar la privacidad o la seguridad son bienvenidas y se revisan públicamente.

---

## 10. Modelo económico

Cerca de Ti es un proyecto sin fines de lucro. La app es gratuita, no muestra anuncios, no contiene compras dentro de la app, y no vende ni cede datos a terceros. Los costos operativos del servidor relay son cubiertos por el promotor del proyecto.

Si en el futuro el proyecto requiere recursos adicionales para sostenerse, se buscarán donaciones voluntarias transparentes, nunca a costa de la privacidad de los usuarios.

---

## 11. Cambios a este aviso

Cualquier cambio sustantivo a este aviso será notificado dentro de la app al menos 30 días antes de entrar en vigor, y requerirá tu consentimiento renovado si afecta cómo se procesan tus datos.

El historial completo de versiones de este aviso está disponible en el repositorio público en `docs/PRIVACIDAD.md`.

---

## 12. Contacto y autoridad

- Para preguntas sobre privacidad: `privacidad@cerca.legaxia.uk`
- Para reportes de seguridad: `seguridad@cerca.legaxia.uk`
- Autoridad mexicana competente: **Instituto Nacional de Transparencia, Acceso a la Información y Protección de Datos Personales (INAI)** — www.inai.org.mx

Tienes derecho a presentar una denuncia ante el INAI si consideras que tus derechos han sido vulnerados.
