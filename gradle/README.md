# Gradle Wrapper

Esta carpeta y los archivos `gradlew` / `gradlew.bat` del directorio raíz son parte del Gradle Wrapper, que permite construir el proyecto sin tener Gradle instalado globalmente.

## Si no tienes los binarios del wrapper (primera vez)

Después de clonar el repo, regenera el wrapper ejecutando una vez (en una máquina con Gradle instalado o usando Android Studio):

```bash
gradle wrapper --gradle-version 8.5
```

O simplemente abre el proyecto en Android Studio, que regenera el wrapper automáticamente al primer build.

## Para CI/CD (GitHub Actions)

El workflow `.github/workflows/build-apk.yml` espera que `gradlew` exista en el repo. Una vez que ejecutes `gradle wrapper` localmente y hagas commit de los archivos generados (`gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar`), GitHub Actions podrá compilar el APK.

## Archivos esperados después del primer setup

- `gradlew` (script Unix, debe ser ejecutable)
- `gradlew.bat` (script Windows)
- `gradle/wrapper/gradle-wrapper.jar` (binario, ~60 KB)
- `gradle/wrapper/gradle-wrapper.properties` (ya está en el repo)
