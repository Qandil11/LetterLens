# Letter Lens UK (Android + AI)
![Android CI](https://github.com/Qandil11/LetterLens/actions/workflows/android-ci.yml/badge.svg)
![Server CI](https://github.com/Qandil11/LetterLens/actions/workflows/server-ci.yml/badge.svg)

Scan UK official letters and get a plain-English explanation with **GOV.UK citations** and a **next-steps checklist**.  
- **Android app:** Compose + CameraX + ML Kit (on-device OCR)  
- **Backend:** Ktor (Kotlin). Simple heuristics now; RAG/LLM planned.  

> Not legal advice. No images are uploaded; only OCR text is sent to the API.

## Demo
<!-- Replace with your link -->
- YouTube: https://youtu.be/XXXXXXXX
- APK: see GitHub Releases

## Quick start

### Android app
- Open the `app/` module in Android Studio and **Run**.
- For local server testing on emulator, set `BuildConfig.LETTER_LENS_API = "http://10.0.2.2:8080"` (already wired in `build.gradle`).
- On device, use your laptop IP, e.g. `"http://192.168.X.Y:8080"` (and allow cleartext in debug).

### Server (Ktor)
```bash
# Dev run
./gradlew :server:run

# Fat JAR (for deployment)
./gradlew :server:shadowJar
java -jar server/build/libs/server-all.jar -port 8080
