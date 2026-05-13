@echo off
REM ============================================
REM  Cerca de Ti - Aplicar Fix Completo
REM ============================================
REM
REM  Este script debe ejecutarse desde la carpeta raiz del proyecto
REM  (la que contiene la carpeta "app" y el archivo "settings.gradle.kts")
REM
REM  Copia los 6 archivos del fix sobre los existentes y luego
REM  hace git add, commit y push automaticamente.
REM ============================================

echo.
echo === Cerca de Ti - Fix Completo ===
echo.

REM Verificar que estamos en la carpeta correcta
if not exist "settings.gradle.kts" (
    echo ERROR: Este script debe ejecutarse desde la carpeta raiz del proyecto
    echo Esa carpeta tiene un archivo llamado "settings.gradle.kts"
    echo.
    pause
    exit /b 1
)

echo Copiando archivos corregidos...

REM Determinar donde estan los archivos del fix
set "FIX_DIR=%~dp0fix-completo"

if not exist "%FIX_DIR%" (
    echo ERROR: No se encuentra la carpeta "fix-completo"
    echo Asegurate de que este script esta en la misma carpeta que la carpeta "fix-completo"
    echo.
    pause
    exit /b 1
)

REM Copiar cada archivo (xcopy con /Y sobreescribe sin preguntar)
xcopy /Y /Q "%FIX_DIR%\app\src\main\AndroidManifest.xml" "app\src\main\AndroidManifest.xml"
xcopy /Y /Q "%FIX_DIR%\app\src\main\java\uk\legaxia\cercadeti\ui\MainActivity.kt" "app\src\main\java\uk\legaxia\cercadeti\ui\MainActivity.kt"
xcopy /Y /Q "%FIX_DIR%\app\src\main\java\uk\legaxia\cercadeti\ui\PermisosActivity.kt" "app\src\main\java\uk\legaxia\cercadeti\ui\PermisosActivity.kt"
xcopy /Y /Q "%FIX_DIR%\app\src\main\res\layout\activity_main.xml" "app\src\main\res\layout\activity_main.xml"
xcopy /Y /Q "%FIX_DIR%\app\src\main\res\layout\activity_onboarding.xml" "app\src\main\res\layout\activity_onboarding.xml"
xcopy /Y /Q "%FIX_DIR%\app\src\main\res\values\strings.xml" "app\src\main\res\values\strings.xml"

echo.
echo Archivos copiados correctamente.
echo.

echo Subiendo a GitHub...
echo.

git add app/src/main/AndroidManifest.xml
git add app/src/main/java/uk/legaxia/cercadeti/ui/MainActivity.kt
git add app/src/main/java/uk/legaxia/cercadeti/ui/PermisosActivity.kt
git add app/src/main/res/layout/activity_main.xml
git add app/src/main/res/layout/activity_onboarding.xml
git add app/src/main/res/values/strings.xml

git commit -m "Fix completo: archivos UTF-8 limpio + diagnostico de permisos"
git push

echo.
echo === LISTO ===
echo.
echo Ahora ve a:
echo https://github.com/help970080/cerca-de-ti/actions
echo.
echo Espera 5-8 minutos y descarga la APK del artifact CercaDeTi-debug-apk
echo.
pause
