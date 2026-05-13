# Marco Legal Mexicano — Cerca de Ti

> Este documento es de referencia interna para el equipo del proyecto. **No constituye asesoría legal formal.** Antes del lanzamiento público se recomienda revisión por un abogado con experiencia en protección de datos personales y derecho digital en México.

## Marco constitucional y federal aplicable

### Artículo 16 Constitucional
- **Inviolabilidad de comunicaciones privadas.** Las comunicaciones privadas son inviolables, salvo orden judicial.
- **Excepción del interviniente:** quien participa en una conversación puede aportarla como prueba. **Esta es la base legal que sustenta que el usuario grabe su propio entorno con Cerca de Ti.**

### Artículo 210 del Código Nacional de Procedimientos Penales (CNPP)
- Las grabaciones de comunicaciones privadas son admisibles como prueba cuando son aportadas por uno de los participantes.
- **Implicación para Cerca de Ti:** las evidencias capturadas (audio del entorno donde está el usuario) son legalmente aportables por el propio usuario como prueba en procedimientos penales, civiles o laborales.

### Artículo 167 del Código Penal Federal
- Sanciona la **intervención de comunicaciones ajenas sin autorización judicial**.
- **Implicación para Cerca de Ti:** la app NO debe permitir que un tercero (padre, pareja, empleador) instale la app en celular ajeno sin consentimiento del titular. Esto se previene en diseño: el onboarding requiere consentimiento expreso del usuario que carga el celular, y no existe instalación silenciosa ni modo oculto.

---

## Ley Federal de Protección de Datos Personales en Posesión de los Particulares (LFPDPPP)

### Obligaciones principales

| Obligación | Cumplimiento en Cerca de Ti |
|---|---|
| Aviso de privacidad | Publicado en `docs/PRIVACIDAD.md`, integrado en onboarding y en `cerca.legaxia.uk/privacidad` |
| Consentimiento expreso para datos sensibles | Onboarding obliga al checkbox explícito antes de activar micrófono/ubicación |
| Principio de finalidad | Datos solo se procesan para detectar peligro al titular |
| Principio de minimización | Audio solo en RAM, ubicación solo agregada localmente, nada superfluo |
| Derechos ARCO | Operables desde la app sin contactar al responsable |
| Designación de Encargado | Leonardo Luna Mendoza, contacto `privacidad@cerca.legaxia.uk` |
| Medidas de seguridad | AES-256 local, TLS 1.3 en tránsito, código abierto auditable |
| Notificación de vulneraciones | Procedimiento documentado en repositorio público |

### Datos sensibles según LFPDPPP (Art. 3, fracción VI)
- Audio del entorno → puede contener datos sensibles (estado de salud, vida sexual, opiniones políticas, etc.).
- Ubicación geográfica → puede revelar hábitos, religión, opinión política.
- **Por eso el procesamiento es 100% local y nada se transmite salvo cuando dispara una alerta.**

---

## Ley General de los Derechos de Niñas, Niños y Adolescentes (LGDNNA)

### Artículo 76
- Derecho a la intimidad personal y familiar de niñas, niños y adolescentes.

### Artículo 80
- Las autoridades garantizarán que los menores no sean objeto de injerencias arbitrarias o ilegales en su vida privada.

### Criterio SCJN sobre interés superior del niño
- El derecho a la intimidad del menor es **progresivo**: aumenta con la edad y la madurez.
- **Implicación para Cerca de Ti:**
  - Menores de 13 años: requiere consentimiento parental obligatorio.
  - 13-15 años: consentimiento parental + asentimiento expreso del menor.
  - 16-17 años: consentimiento del menor; vinculación parental opcional y revocable.
  - 18+: consentimiento únicamente del usuario.
- **La app nunca permite monitoreo oculto de un menor.** El menor siempre sabe qué se monitorea y qué se comparte con sus padres.

---

## Lineamientos para grabaciones de audio

### Cuando es legal grabar (con Cerca de Ti)
✅ El propio usuario graba su entorno donde él/ella participa o está presente.
✅ Grabación se activa por detección de peligro al propio usuario.
✅ Evidencia queda bajo control del usuario.
✅ Audio en buffer RAM se descarta automáticamente si no hay alerta.

### Cuando NO es legal
❌ Instalar la app en celular de un tercero sin su conocimiento.
❌ Grabar conversaciones donde el usuario NO está presente (ej. dejar el celular en otra habitación para grabar).
❌ Compartir grabaciones de terceros sin justificación legal (la evidencia es solo para defensa propia o denuncia).

---

## Lineamientos para Google Play Store

### Política de Datos del Usuario de Play Store
- **Permisos sensibles** (RECORD_AUDIO, ACCESS_BACKGROUND_LOCATION, CAMERA) requieren:
  - Declaración detallada del propósito en Play Console
  - Política de privacidad pública en URL accesible
  - Función claramente comunicada al usuario
- **Limitación de uso:** los datos personales no pueden venderse, transferirse para publicidad, ni usarse para fines distintos al declarado.

### Política de Permisos en Background
- Acceso a ubicación en background requiere justificación explícita en revisión manual.
- **Justificación para Cerca de Ti:** "La aplicación detecta situaciones de peligro físico al usuario monitoreando patrones de movimiento, audio y ubicación; el background es necesario porque las emergencias ocurren cuando el usuario no está activamente interactuando con el dispositivo. Sin ubicación en background, no podemos identificar trayectorias anómalas indicativas de secuestro o asalto."

### Política de Familia (Google Play Families)
- Si la app puede ser usada por menores: requiere etiqueta "Diseñada para Familias" o "Adecuada para todas las edades", política específica de privacidad para menores, y restricciones publicitarias (que ya cumplimos por no tener anuncios).

---

## Lineamientos para evidencia procesal

### Cadena de custodia
La evidencia capturada por Cerca de Ti tiene mayor probabilidad de ser admisible si:
1. Se firma criptográficamente con timestamp en el momento de captura.
2. Se conserva el hash original sin modificación.
3. Se documenta el modelo del dispositivo, versión de la app, y configuración al momento del evento.
4. Se entrega a autoridades el archivo original sin re-codificación.

**Implementación técnica en Cerca de Ti:**
- Cada paquete de evidencia se firma con la llave del Android Keystore del dispositivo.
- Se incluye archivo `metadata.json` con: timestamp UTC, hash SHA-256 del audio, modelo del dispositivo, versión de la app, coordenadas GPS, ID anónimo del dispositivo.
- El usuario puede exportar el paquete completo en `.zip` para entregar a autoridades.

---

## Casos de uso y su cobertura legal

| Caso | Cobertura |
|---|---|
| Mujer instala la app en su celular | ✅ Sin restricciones |
| Padre instala en celular de hijo de 10 años | ✅ Con consentimiento parental + información clara al menor |
| Padre instala en celular de hijo de 17 años | ✅ Solo si el menor consiente; vinculación parental revocable |
| Hombre instala en celular de su pareja sin que ella sepa | ❌ Ilegal (Art. 167 CPF); la app no lo permite por diseño |
| Empleador instala en celulares de empleados | ⚠️ Solo con consentimiento escrito y para protección laboral del empleado en campo; recomendar contratos específicos |
| ONG instala en celulares de víctimas de violencia | ✅ Con consentimiento de cada víctima |

---

## Riesgos legales identificados y mitigación

| Riesgo | Mitigación |
|---|---|
| Demanda por uso indebido (alguien instala en tercero) | App requiere consentimiento explícito en onboarding; términos de uso eximen al desarrollador del uso indebido por terceros |
| Grabación accidental de conversación ajena | Audio solo se guarda al disparar alerta; el usuario controla qué hace con la evidencia |
| Fuga de datos del servidor relay | Servidor no almacena audio/foto; solo metadata mínima 30 días; cifrado en tránsito y reposo |
| Uso por parte de agresores para localizar a víctimas | La app no permite seguimiento en tiempo real por terceros; solo alertas críticas en vinculación familiar opt-in |
| Reclamo por reconocimiento de voz en menores | Procesamiento on-device, sin datasets externos, sin entrenamiento sobre datos del usuario |
| Demanda por falsos positivos que generaron acción policial | La app no contacta autoridades directamente, solo a contactos del usuario; el usuario decide si escala |

---

## Próximos pasos legales recomendados

1. **Antes de Play Store:** revisión de política de privacidad por abogado especialista en LFPDPPP.
2. **Antes de Fase 1:** evaluar registro de marca "Cerca de Ti" en IMPI.
3. **Antes de Fase 2:** estructurar el proyecto como Asociación Civil sin fines de lucro si crece la base de usuarios, para protección patrimonial del promotor.
4. **Si se integra con C5/autoridades:** convenios específicos por estado, con cláusulas de no-divulgación y cláusulas de uso restringido.
