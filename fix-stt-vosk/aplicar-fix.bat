@echo off
REM ============================================
REM  Fix STT - Integracion Vosk Speech-to-Text
REM ============================================

echo.
echo === Fix STT con Vosk ===
echo.

if not exist "settings.gradle.kts" (
    echo ERROR: Ejecutar desde la raiz del proyecto cerca-de-ti
    pause
    exit /b 1
)

set "FIX_DIR=%~dp0fix-stt-vosk"

if not exist "%FIX_DIR%" (
    echo ERROR: No se encuentra la carpeta "fix-stt-vosk"
    pause
    exit /b 1
)

echo Copiando archivos modificados...

REM build files
xcopy /Y /Q "%FIX_DIR%\settings.gradle.kts" "settings.gradle.kts"
xcopy /Y /Q "%FIX_DIR%\app\build.gradle.kts" "app\build.gradle.kts"

REM manifest
xcopy /Y /Q "%FIX_DIR%\app\src\main\AndroidManifest.xml" "app\src\main\AndroidManifest.xml"

REM recursos
xcopy /Y /Q "%FIX_DIR%\app\src\main\res\layout\activity_main.xml" "app\src\main\res\layout\activity_main.xml"
xcopy /Y /Q "%FIX_DIR%\app\src\main\res\values\strings.xml" "app\src\main\res\values\strings.xml"

REM service (modificados)
xcopy /Y /Q "%FIX_DIR%\app\src\main\java\uk\legaxia\cercadeti\service\AudioMonitor.kt" "app\src\main\java\uk\legaxia\cercadeti\service\AudioMonitor.kt"
xcopy /Y /Q "%FIX_DIR%\app\src\main\java\uk\legaxia\cercadeti\service\GuardianService.kt" "app\src\main\java\uk\legaxia\cercadeti\service\GuardianService.kt"

REM stt (NUEVOS)
mkdir "app\src\main\java\uk\legaxia\cercadeti\stt" 2>nul
xcopy /Y /Q "%FIX_DIR%\app\src\main\java\uk\legaxia\cercadeti\stt\VoskManager.kt" "app\src\main\java\uk\legaxia\cercadeti\stt\VoskManager.kt"
xcopy /Y /Q "%FIX_DIR%\app\src\main\java\uk\legaxia\cercadeti\stt\KeywordSpotter.kt" "app\src\main\java\uk\legaxia\cercadeti\stt\KeywordSpotter.kt"

REM ui (modificada + nueva)
xcopy /Y /Q "%FIX_DIR%\app\src\main\java\uk\legaxia\cercadeti\ui\MainActivity.kt" "app\src\main\java\uk\legaxia\cercadeti\ui\MainActivity.kt"
xcopy /Y /Q "%FIX_DIR%\app\src\main\java\uk\legaxia\cercadeti\ui\DescargaModeloActivity.kt" "app\src\main\java\uk\legaxia\cercadeti\ui\DescargaModeloActivity.kt"

REM doc
xcopy /Y /Q "%FIX_DIR%\SUBIR_MODELO.txt" "SUBIR_MODELO.txt"

echo.
echo Archivos copiados. Verificando...
echo.

REM Verificar que los archivos clave existen
if not exist "app\src\main\java\uk\legaxia\cercadeti\stt\VoskManager.kt" (
    echo ERROR: VoskManager.kt no se copio. Abortando.
    pause
    exit /b 1
)
if not exist "app\src\main\java\uk\legaxia\cercadeti\stt\KeywordSpotter.kt" (
    echo ERROR: KeywordSpotter.kt no se copio. Abortando.
    pause
    exit /b 1
)

echo OK, archivos verificados.
echo.
echo Subiendo a GitHub...
echo.

git add settings.gradle.kts
git add app/build.gradle.kts
git add app/src/main/AndroidManifest.xml
git add app/src/main/res/layout/activity_main.xml
git add app/src/main/res/values/strings.xml
git add app/src/main/java/uk/legaxia/cercadeti/service/AudioMonitor.kt
git add app/src/main/java/uk/legaxia/cercadeti/service/GuardianService.kt
git add app/src/main/java/uk/legaxia/cercadeti/stt/VoskManager.kt
git add app/src/main/java/uk/legaxia/cercadeti/stt/KeywordSpotter.kt
git add app/src/main/java/uk/legaxia/cercadeti/ui/MainActivity.kt
git add app/src/main/java/uk/legaxia/cercadeti/ui/DescargaModeloActivity.kt
git add SUBIR_MODELO.txt

git commit -m "Integracion STT con Vosk: modelo descargable desde GitHub Releases"
git push

echo.
echo === LISTO ===
echo.
echo SIGUIENTE PASO IMPORTANTE:
echo   Abre el archivo SUBIR_MODELO.txt y sube el modelo a GitHub Releases.
echo   La app NO funcionara hasta que el modelo este en el release.
echo.
echo Despues espera 5-8 minutos a que GitHub Actions compile la APK nueva
echo y descargala desde https://github.com/help970080/cerca-de-ti/actions
echo.
pause
