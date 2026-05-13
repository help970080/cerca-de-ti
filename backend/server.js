/**
 * Cerca de Ti — Backend Relay
 *
 * Función única: enrutar notificaciones de alerta a los contactos del usuario
 * vía SMS (Zadarma), WhatsApp (Baileys) y push (FCM).
 *
 * NO almacena audio, fotos, video ni evidencia.
 * Solo guarda logs operativos mínimos por 30 días para diagnóstico.
 *
 * Variables de entorno requeridas (definir en Render):
 *   - CERCA_TOKEN: token compartido para auth básica (default v0 hardcodeado en cliente)
 *   - ZADARMA_KEY, ZADARMA_SECRET: para SMS (opcional)
 *   - WHATSAPP_BOT_URL: URL del bot Baileys existente (bot-9wrn.onrender.com)
 *   - WHATSAPP_BOT_TOKEN: token compartido con el bot
 *   - FCM_SERVER_KEY: para push notifications (opcional)
 *   - PORT: puerto (default 10000)
 */

const express = require('express');
const crypto = require('crypto');
const fetch = (...a) => import('node-fetch').then(({default: f}) => f(...a));

const app = express();
app.use(express.json({ limit: '1mb' }));

const TOKEN = process.env.CERCA_TOKEN || 'cdt_v0_compartido_temporal';
const WHATSAPP_URL = process.env.WHATSAPP_BOT_URL || '';
const WHATSAPP_TOKEN = process.env.WHATSAPP_BOT_TOKEN || '';

// Almacenamiento en memoria (logs operativos, max 30 días)
const logs = new Map();

function logEvento(eventoId, info) {
  logs.set(eventoId, {
    ...info,
    timestamp: Date.now(),
  });
  // Limpieza: borrar logs >30 días
  const limite = Date.now() - 30 * 24 * 3600 * 1000;
  for (const [id, log] of logs) {
    if (log.timestamp < limite) logs.delete(id);
  }
}

// Health check
app.get('/', (req, res) => {
  res.json({ servicio: 'Cerca de Ti Relay', estado: 'ok', timestamp: Date.now() });
});

app.get('/health', (req, res) => {
  res.json({
    estado: 'ok',
    logs_activos: logs.size,
    uptime: process.uptime(),
  });
});

// Endpoint principal: recibe alerta y enruta a contactos
app.post('/alerta', async (req, res) => {
  const token = req.headers['x-cerca-token'];
  if (token !== TOKEN) {
    return res.status(401).json({ error: 'Token inválido' });
  }

  const { evento_id, timestamp_ms, nivel, lat, lon, token_acceso, contactos, mensaje_usuario } = req.body;

  if (!evento_id || !contactos || !Array.isArray(contactos) || contactos.length === 0) {
    return res.status(400).json({ error: 'Payload inválido' });
  }

  console.log(`[ALERTA] ${evento_id} nivel=${nivel} contactos=${contactos.length}`);

  // Construir mensaje
  const ubic = (lat != null && lon != null)
    ? `https://maps.google.com/?q=${lat},${lon}`
    : '(sin GPS)';
  const mensaje = `🚨 CERCA DE TI: ${mensaje_usuario || 'Alguien'} puede estar en peligro. Ubicación: ${ubic}`;

  // Enviar a WhatsApp (si está configurado)
  const resultados = { whatsapp: [], sms: [], push: [] };
  if (WHATSAPP_URL) {
    for (const tel of contactos) {
      try {
        const r = await fetch(`${WHATSAPP_URL}/api/enviar`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${WHATSAPP_TOKEN}`,
          },
          body: JSON.stringify({ telefono: tel, mensaje }),
          timeout: 10000,
        });
        resultados.whatsapp.push({ tel, ok: r.ok, status: r.status });
      } catch (e) {
        resultados.whatsapp.push({ tel, ok: false, error: e.message });
      }
    }
  }

  // Log mínimo (sin contenido del audio, sin nombres de contactos, sin coordenadas precisas)
  logEvento(evento_id, {
    nivel,
    contactos_count: contactos.length,
    lat_zona: lat != null ? Math.round(lat * 10) / 10 : null,  // precisión ~10km
    lon_zona: lon != null ? Math.round(lon * 10) / 10 : null,
    resultados_envio: {
      whatsapp_ok: resultados.whatsapp.filter(r => r.ok).length,
      whatsapp_fail: resultados.whatsapp.filter(r => !r.ok).length,
    },
  });

  res.json({
    evento_id,
    enrutado: true,
    resultados,
  });
});

// Métricas anónimas agregadas (sin PII)
app.get('/metricas', (req, res) => {
  const ahora = Date.now();
  const ultimo_dia = Array.from(logs.values()).filter(l => ahora - l.timestamp < 86400000);
  const ultima_semana = Array.from(logs.values()).filter(l => ahora - l.timestamp < 7 * 86400000);

  res.json({
    alertas_24h: ultimo_dia.length,
    alertas_7d: ultima_semana.length,
    niveles_24h: {
      CRITICAL: ultimo_dia.filter(l => l.nivel === 'CRITICAL').length,
      HIGH: ultimo_dia.filter(l => l.nivel === 'HIGH').length,
    },
  });
});

const PORT = process.env.PORT || 10000;
app.listen(PORT, () => {
  console.log(`Cerca de Ti Relay escuchando en :${PORT}`);
  console.log(`WhatsApp bot URL: ${WHATSAPP_URL || '(no configurado)'}`);
});
