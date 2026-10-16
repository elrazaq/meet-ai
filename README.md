# MeetAI: Enterprise Android Meeting Recorder & AI Documentation Engine

[![Android CI](https://github.com/elrazaq/meet-ai/actions/workflows/build-android.yml/badge.svg)](https://github.com/elrazaq/meet-ai/actions/workflows/build-android.yml)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.0-blue.svg)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-2024.09.03-green.svg)](https://developer.android.com/jetpack/compose)
[![FastAPI](https://img.shields.io/badge/FastAPI-0.115+-009688.svg)](https://fastapi.tiangolo.com)
[![Google Gemini](https://img.shields.io/badge/Google%20Gemini-Multimodal%20Audio-orange.svg)](https://ai.google.dev)
[![Target SDK](https://img.shields.io/badge/Target%20SDK-34%20(Android%2014)-brightgreen.svg)](https://developer.android.com)

**MeetAI** is an ultra-lightweight, battery-efficient enterprise Android application and backend service designed to record long corporate discussions (1–3 hours) and automatically extract **4 separate, highly-structured meeting documents** without information gaps or plot holes using **Google Gemini Multimodal Audio**.

---

## 🚀 Key Highlights & Architecture

```text
┌───────────────────────────────────────┐
│     MeetAI Android Client (Kotlin)     │
│  - Hardware-Accelerated AAC Encoder   │
│  - 16 kHz Mono @ 32 kbps (~14 MB/hr)  │
│  - Android 14 Foreground Service      │
│  - Jetpack Compose Realtime Waveform  │
└───────────────────┬───────────────────┘
                    │
           WorkManager Upload
                    │
                    ▼
┌───────────────────────────────────────┐
│       MeetAI FastAPI Backend          │
│  - Multipart Receiver Endpoint        │
│  - Direct Gemini Multimodal Pipeline  │
│  - Resilient Model Fallback System    │
└───────────────────┬───────────────────┘
                    │
           Google Gemini Flash
                    │
                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│               4 Dedicated, Anti-Plot-Hole Documents                     │
│  1. Formal Meeting Minutes (Attendees, Decisions, Action Items & PIC)  │
│  2. Executive Summary (1-2 pages for C-Level leadership)               │
│  3. Key Takeaways & Quantifiable Metrics (Financials, Percentages)     │
│  4. Comprehensive Q&A & Anti-Plot-Hole Unresolved Issues Catalog      │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 📑 The 4 AI-Generated Output Documents

Unlike typical generic summary tools, MeetAI strictly partitions meeting intelligence into 4 standalone documents:

| # | Document | File Name | Key Focus |
|---|---|---|---|
| **1** | **Formal Minutes** | `1_notulen_rapat.md` | Formal header, identified attendees, discussion chronology, unanimous decisions, and an **Action Items table** with Assignees (PIC), Deadlines, and Status. |
| **2** | **Executive Summary** | `2_ringkasan_eksekutif.md` | High-level synthesis (1–2 pages) covering context, strategic results, and cross-departmental business implications. |
| **3** | **Key Takeaways & Metrics** | `3_poin_penting_dan_data.md` | Quantifiable facts, financial numbers, percentage growths, performance metrics, and critical milestone dates. |
| **4** | **Q&A & Anti-Plot-Hole** | `4_rekap_tanya_jawab_anti_plot_hole.md` | **Anti-Plot-Hole Engine**: Catalogs all asked & answered questions, plus an explicit section identifying **unanswered questions, deferred topics, conflicting arguments, or pending items**. |

---

## 🔋 Power & Thermal Efficiency Specs

- **Hardware DSP Acceleration**: Employs native Android `MediaRecorder` direct-to-chip encoding with zero heavy CPU cycles.
- **Audio Profile**: AAC (`.m4a`), Mono (1 channel), 16.000 Hz, 32 kbps.
- **File Footprint**: Generates merely **~14.4 MB per hour** (a 3-hour meeting is only ~43 MB).
- **Background Persistence**: Guaranteed background recording through `ServiceCompat.FOREGROUND_SERVICE_TYPE_MICROPHONE` and `PowerManager.PARTIAL_WAKE_LOCK` (immune to Android Doze mode).

---

## 🛡️ Google Play Store Compliance

- **Prominent In-App Disclosure**: Includes a dedicated disclosure dialog compliant with Google Play's Background Microphone and Foreground Service policies before requesting `RECORD_AUDIO`.
- **Private Storage**: Audio files are kept inside app-private internal storage (`context.filesDir`) and never exposed to public external folders.

---

## 🛠️ Getting Started

### 1. Backend Server Setup

```bash
cd backend

# Create virtual environment
python -m venv venv

# Activate virtual environment
# Windows:
.\venv\Scripts\activate
# Linux/macOS:
source venv/bin/activate

# Install dependencies
pip install -r requirements.txt

# Configure environment variables
copy .env.example .env
```

Edit `.env` with your Google AI Studio API Key:
```env
GEMINI_API_KEY=your_gemini_api_key_here
GEMINI_MODEL=gemini-3.5-flash
PORT=8000
```

Start the server:
```bash
python main.py
```
Access the interactive Swagger UI at **`http://localhost:8000/docs`**.

---

### 2. Android CI/CD Cloud Build (No Local Android Studio Required)

This repository includes a preconfigured **GitHub Actions** workflow (`.github/workflows/build-android.yml`):

1. Push this repository to your GitHub account:
   ```bash
   git push origin main
   ```
2. Navigate to your repository's **Actions** tab on GitHub.
3. The build job will automatically compile:
   - **`.aab`** (Android App Bundle for Google Play Store upload).
   - **`.apk`** (Ready for direct sideloading and testing on your Android phone).
4. Download the compiled artifacts directly from the **Artifacts** section of the finished build.

---

### 3. Local Wi-Fi Testing on Physical Android Phone

1. Ensure your phone and development computer are connected to the same Wi-Fi network.
2. In the MeetAI app on your phone, tap the **Settings (Gear)** icon in the top-right corner.
3. Enter your computer's local IP address (e.g., `http://192.168.1.5:8000`).
4. Start recording a meeting and tap **Selesai** — the audio will automatically upload via WorkManager and generate your 4 documents!

---

## 📦 Project Structure

```text
meet-ai/
├── .github/
│   └── workflows/
│       └── build-android.yml          # Cloud CI/CD for APK and AAB
├── app/                               # Android native module (Jetpack Compose)
│   ├── src/main/java/com/enterprise/meetai/
│   │   ├── recorder/                  # Hardware MediaRecorder & Foreground Service
│   │   ├── worker/                    # WorkManager background upload worker
│   │   ├── ui/                        # Dark purple gradient UI & Waveform visualizer
│   │   └── MainActivity.kt
│   └── AndroidManifest.xml
├── backend/                           # Python FastAPI receiver & Gemini integration
│   ├── services/
│   │   └── gemini_service.py          # Multimodal audio analysis & document generator
│   ├── main.py                        # FastAPI routes & endpoints
│   ├── prompts.py                     # Specialized 4-document prompt engineering
│   └── requirements.txt
└── settings.gradle.kts
```

---

## 📄 License

This project is licensed under the Apache 2.0 License - see the LICENSE file for details.
