# Cerca de Ti

**Tu celular detecta cuando estás en peligro y avisa a quien tú elijas. No te vigila — te cuida.**

Cerca de Ti es una aplicación Android de código abierto, gratuita y sin publicidad, diseñada para detectar situaciones de peligro de forma pasiva (sin que el usuario tenga que abrir la app o presionar botones) y alertar automáticamente a contactos de confianza con ubicación, audio y evidencia.

## El problema

En México suceden alrededor de 10 feminicidios al día, miles de asaltos diarios, y muchas situaciones de violencia donde la víctima **no tiene tiempo de sacar el celular y presionar un botón de pánico**. Las apps existentes (Red Violeta, Vive Segura CDMX, Life360, bSafe) todas requieren acción manual del usuario en el momento del peligro — justo cuando es más difícil hacerlo.

## La propuesta

Una app que monitorea de forma **completamente local** señales del propio usuario:

- Voz alterada (volumen, tono, velocidad anómalos respecto al baseline personal)
- Movimientos bruscos (forcejeo, caída, carrera súbita)
- Ubicación inusual y cambios rápidos de zona
- Palabras clave personalizadas elegidas por el usuario
- Patrones de uso del dispositivo (intentos repetidos de apagado, desbloqueo fallido múltiple)

Cuando se cumple el umbral de alerta, la app:

1. Da 30 segundos al usuario para cancelar (notificación silenciosa + vibración)
2. Si no hay cancelación: envía SMS + WhatsApp + push a los contactos de confianza con ubicación, audio de los 60 segundos alrededor del evento, y foto frontal silenciosa
3. Guarda evidencia cifrada localmente en el dispositivo del usuario
4. Activa grabación continua hasta cancelación manual

## Principios de diseño

- **Privacidad por diseño.** Todo el procesamiento corre on-device. Ningún audio crudo sale del celular salvo cuando dispara una alerta — y solo va a contactos elegidos por el propio usuario, nunca a un servidor central.
- **Cero lucro.** Sin anuncios, sin compras dentro de la app, sin venta de datos. Código abierto.
- **Cero falsos positivos catastróficos.** Modelo de baseline personal + cuenta atrás cancelable + escalado por niveles.
- **Diseñado para el usuario que carga el celular.** Auto-protección, no vigilancia de terceros.

## Estado del proyecto

**Fase 0 — MVP en desarrollo.** Detector basado en reglas (sin ML todavía), envío de alertas, persistencia local cifrada. Ver [docs/ROADMAP.md](docs/ROADMAP.md) para el plan completo.

## Casos de uso

| Caso | Cómo ayuda Cerca de Ti |
|---|---|
| Mujer en transporte público / colonia brava | Detección pasiva de voz alterada + forcejeo dispara alerta sin requerir sacar el celular |
| Cobrador o gestor de campo | Alerta automática a coordinador si suena situación de asalto |
| Menor en escuela ante acoso | Palabra clave personalizada activa grabación de evidencia legal |
| Adulto mayor | Detección de caída + ausencia de movimiento prolongada alerta a familia |
| Repartidor / chofer | Alerta de robo del celular con foto frontal del agresor |

## Documentación

- [Arquitectura técnica](docs/ARQUITECTURA.md)
- [Política de privacidad](docs/PRIVACIDAD.md)
- [Hoja de ruta](docs/ROADMAP.md)
- [Marco legal mexicano](docs/MARCO_LEGAL.md)
- [Plan de pruebas piloto](docs/PILOTO.md)

## Licencia

MIT. Hazlo tuyo, mejóralo, cópialo, redistribúyelo. El objetivo es que más gente esté segura.

## Autor

Proyecto iniciado por Leonardo Luna Mendoza (LeGaXi / CelExpress) como aportación a la sociedad. Sin fines de lucro.

---

> Si estás en una emergencia, llama al **911**. Esta app es complementaria, no reemplaza a los servicios de emergencia.
