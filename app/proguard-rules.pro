# Cerca de Ti — reglas ProGuard
# Mantenemos los nombres de las clases del modelo de datos para que el JSON
# de evidencia sea legible en disco aún con ofuscación.

-keep class uk.legaxia.cercadeti.service.SensorWindow { *; }
-keep class uk.legaxia.cercadeti.service.AudioSnapshot { *; }
-keep class uk.legaxia.cercadeti.service.MotionSnapshot { *; }
-keep class uk.legaxia.cercadeti.service.LocationSnapshot { *; }
-keep class uk.legaxia.cercadeti.service.TrayectoriaPunto { *; }
-keep class uk.legaxia.cercadeti.service.DeviceState { *; }
-keep class uk.legaxia.cercadeti.alert.PaqueteEvidencia { *; }
-keep class uk.legaxia.cercadeti.alert.RelayClient$Contacto { *; }
-keep class uk.legaxia.cercadeti.detector.RiskScore { *; }
-keep class uk.legaxia.cercadeti.detector.Contribucion { *; }

# AndroidX Security
-keep class androidx.security.crypto.** { *; }

# Play Services Location
-keep class com.google.android.gms.location.** { *; }
