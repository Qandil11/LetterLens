{\rtf1\ansi\ansicpg1252\cocoartf2822
\cocoatextscaling0\cocoaplatform0{\fonttbl\f0\fswiss\fcharset0 Helvetica;}
{\colortbl;\red255\green255\blue255;}
{\*\expandedcolortbl;;}
\paperw11900\paperh16840\margl1440\margr1440\vieww11520\viewh8400\viewkind0
\pard\tx720\tx1440\tx2160\tx2880\tx3600\tx4320\tx5040\tx5760\tx6480\tx7200\tx7920\tx8640\pardirnatural\partightenfactor0

\f0\fs24 \cf0 # Letter Lens UK (Android + AI)\
\
Scan UK official letters and get a plain-English explanation with **GOV.UK citations** and a **next-steps checklist**.  \
- **Android app:** Compose + CameraX + ML Kit (on-device OCR)  \
- **Backend:** Ktor (Kotlin). Simple heuristics now; RAG/LLM planned.  \
\
> Not legal advice. No images are uploaded; only OCR text is sent to the API.\
\
## Demo\
<!-- Replace with your link -->\
- YouTube: https://youtu.be/XXXXXXXX\
- APK: see GitHub Releases\
\
## Quick start\
\
### Android app\
- Open the `app/` module in Android Studio and **Run**.\
- For local server testing on emulator, set `BuildConfig.LETTER_LENS_API = "http://10.0.2.2:8080"` (already wired in `build.gradle`).\
- On device, use your laptop IP, e.g. `"http://192.168.X.Y:8080"` (and allow cleartext in debug).\
\
### Server (Ktor)\
```bash\
# Dev run\
./gradlew :server:run\
\
# Fat JAR (for deployment)\
./gradlew :server:shadowJar\
java -jar server/build/libs/server-all.jar -port 8080\
}