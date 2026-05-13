# Cerca de Ti — Backend Relay

Servicio Node.js minimalista que enruta alertas a los contactos del usuario vía WhatsApp y (opcional) SMS y push.

## Principios

- **NO almacena audio, fotos ni evidencia.** El audio se queda cifrado en el celular del usuario.
- **Logs operativos mínimos** (sin PII, sin coordenadas precisas, sin contenido del mensaje) por 30 días.
- **Stateless**: si se reinicia, no pierde nada importante.

## Despliegue en Render

1. Crear nuevo servicio Web en Render apuntando al repo `help970080/cerca-de-ti`, branch `master`, root directory `backend/`.
2. Build command: `npm install`
3. Start command: `npm start`
4. Variables de entorno:
   - `CERCA_TOKEN` — token compartido con la app Android
   - `WHATSAPP_BOT_URL` — URL del bot Baileys (ej. `https://bot-9wrn.onrender.com`)
   - `WHATSAPP_BOT_TOKEN` — token compartido con el bot WhatsApp
   - `PORT` — Render lo provee automáticamente
5. Dominio personalizado: `api.cerca.legaxia.uk` (configurar en Cloudflare DNS apuntando a Render)

## Endpoints

### `POST /alerta`

Recibe una alerta y la enruta a los contactos especificados.

**Headers:**
```
Content-Type: application/json
X-Cerca-Token: <token>
```

**Body:**
```json
{
  "evento_id": "evt_1715487612345_4271",
  "timestamp_ms": 1715487612345,
  "nivel": "HIGH",
  "lat": 19.4326,
  "lon": -99.1332,
  "token_acceso": "evt_1715487612345_4271.abc...",
  "contactos": ["5215512345678", "5215587654321"],
  "mensaje_usuario": "María García"
}
```

**Response:**
```json
{
  "evento_id": "evt_1715487612345_4271",
  "enrutado": true,
  "resultados": { "whatsapp": [...], "sms": [...], "push": [...] }
}
```

### `GET /health`

Health check para UptimeRobot u otros monitores.

### `GET /metricas`

Métricas anónimas agregadas (conteos por nivel, por ventana de tiempo). Sin PII.

## Mantenimiento operativo

- Como Render plan Gratis duerme tras 15min de inactividad, recomendamos UptimeRobot pingeando `/health` cada 5min para mantenerlo despierto.
- Los logs se limpian solos a los 30 días en memoria.
- Sin base de datos: si el servicio cae, no hay datos que perder.

## Para correr local

```bash
cd backend
npm install
CERCA_TOKEN=dev WHATSAPP_BOT_URL=https://bot-9wrn.onrender.com npm run dev
```
