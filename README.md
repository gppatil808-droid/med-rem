# MedCue — Medicine Reminder & Voice Announcement App

**MedCue** is an Android medication reminder application built with Kotlin, Jetpack Compose, Material 3, Room, DataStore, `AlarmManager`, and Android `TextToSpeech`.

## Building the APK from Exported Source

After exporting this project as a ZIP or pushing it to GitHub, you can build the installable debug APK locally or in GitHub Actions.

### Option A: Build with Android Studio
1. Open the exported project folder in **Android Studio**.
2. Generate `debug.keystore` from `debug.keystore.base64` in the project root (needed by `signingConfigs.debugConfig` in `app/build.gradle.kts`):
   - **macOS / Linux:**
     ```bash
     base64 -d debug.keystore.base64 > debug.keystore
     ```
   - **Windows (PowerShell):**
     ```powershell
     [IO.File]::WriteAllBytes("debug.keystore", [Convert]::FromBase64String((Get-Content "debug.keystore.base64" -Raw)))
     ```
3. Wait for Gradle Sync to complete.
4. Click **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
5. The APK will be generated at:
   `app/build/outputs/apk/debug/app-debug.apk`

### Option B: Build from Command Line (Gradle)
1. Ensure **JDK 17+** and the **Android SDK** (`ANDROID_HOME` / `ANDROID_SDK_ROOT`) are installed, along with **Gradle 9.3.1** (or generate the wrapper once with `gradle wrapper`).
2. Decode `debug.keystore.base64` into `debug.keystore` (see commands above).
3. Run:
   ```bash
   gradle :app:assembleDebug
   ```
4. Install or copy the generated APK from:
   `app/build/outputs/apk/debug/app-debug.apk`

### Option C: Automatic APK Build via GitHub Actions
If you push this repository to GitHub, the included workflow at `.github/workflows/build-apk.yml` automatically builds `app-debug.apk` on push or manual dispatch (**Actions > Build Debug APK > Run workflow**) and attaches `MedCue-debug-apk` as a downloadable build artifact.
