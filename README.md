# TURBO DRIVE

Android telemetry, GPS speed, RPM & turbo boost gauges, and engine sound synthesis app built with Kotlin and Jetpack Compose.

## Repository Structure
- `.github/workflows/build_apk.yml` — GitHub Actions workflow that builds `app-debug.apk` and uploads it as an artifact.
- `build.gradle.kts`, `settings.gradle.kts`, `gradle.properties`, `gradlew` — Root Gradle build files.
- `app/build.gradle.kts`, `app/proguard-rules.pro` — App module configuration.
- `app/src/main/AndroidManifest.xml` — App manifest, permissions, activity, and `EngineForegroundService`.
- `app/src/main/java/com/turbodrive/app/...` — Kotlin source files (UI, gauges, audio routing/synthesis, GPS manager, foreground service).
- `app/src/main/res/...` — Resources (`strings.xml`, `raw/README_SOUNDS.txt`).

## Building the Debug APK via GitHub Actions
1. Push to `main` or open **Actions** -> **Build Debug APK** -> **Run workflow**.
2. Once the job finishes, download the **`TurboDrive-Debug-APK`** artifact to get `app-debug.apk`.
