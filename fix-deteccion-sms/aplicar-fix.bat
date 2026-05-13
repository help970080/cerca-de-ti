@echo off
REM ============================================
REM  Cerca de Ti - Fix Deteccion + SMS
REM ============================================
REM
REM  Este script:
REM  1. Sobreescribe 3 archivos con las correcciones
REM  2. Hace git add, commit y push
REM
REM  IMPORTANTE: ejecutar desde la raiz del proyecto cerca-de-ti
REM ============================================

echo.
echo === Fix Deteccion + SMS ===
echo.

if not exist "settings.gradle.kts" (
    echo ERROR: Este script debe ejecutarse desde la carpeta raiz del proyecto
    echo La carpeta correcta tiene un archivo "settings.gradle.kts"
    pause
    exit /b 1
)

set "FIX_DIR=%~dp0fix-deteccion-sms"

if not exist "%FIX_DIR%" (
    echo ERROR: No se encuentra la carpeta "fix-deteccion-sms"
    echo Debe estar junto a este script
    pause
    exit /b 1
)

echo Copiando archivos corregidos...

xcopy /Y /Q "%FIX_DIR%\app\src\main\java\uk\legaxia\cercadeti\alert\RelayClient.kt" "app\src\main\java\uk\legaxia\cercadeti\alert\RelayClient.kt"
xcopy /Y /Q "%FIX_DIR%\app\src\main\java\uk\legaxia\cercadeti\alert\AlertManager.kt" "app\src\main\java\uk\legaxia\cercadeti\alert\AlertManager.kt"
xcopy /Y /Q "%FIX_DIR%\app\src\main\java\uk\legaxia\cercadeti\detector\RiskDetector.kt" "app\src\main\java\uk\legaxia\cercadeti\detector\RiskDetector.kt"

echo.
echo Archivos copiados. Subiendo a GitHub...
echo.

git add app/src/main/java/uk/legaxia/cercadeti/alert/RelayClient.kt
git add app/src/main/java/uk/legaxia/cercadeti/alert/AlertManager.kt
git add app/src/main/java/uk/legaxia/cercadeti/detector/RiskDetector.kt

git commit -m "Fix: SMS robusto + deteccion mejorada + notificaciones autocierre"
git push

echo.
echo === LISTO ===
echo.
echo Ve a https://github.com/help970080/cerca-de-ti/actions
echo Espera 5-8 minutos y descarga la nueva APK
echo.
pause
