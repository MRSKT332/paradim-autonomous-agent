# Paradim Autonomous AI Agent for Android 🤖⚡

[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-purple.svg)](https://kotlinlang.org/)
[![Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-blue.svg)](https://developer.android.com/jetpack/compose)
[![Release](https://img.shields.io/badge/Release-v1.0.0-orange.svg)](https://github.com/MRSKT332/paradim-autonomous-agent/releases/tag/v1.0.0)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

**Paradim Autonomous AI Agent** is a full-featured, local-first Android AI companion and task execution platform. Built natively in **Kotlin & Jetpack Compose**, Paradim connects advanced Inference APIs (including NVIDIA NIM, Groq, OpenAI, Gemini, and Ollama) with real-time Android Accessibility Services and system intents.

---

## 📱 Releases & APK Download

You can download the pre-compiled, ready-to-install Android APK directly from the official release page:

📥 **[Download Latest APK (v1.0.0)](https://github.com/MRSKT332/paradim-autonomous-agent/releases/download/v1.0.0/paradim-agent-v1.0.0.apk)**

---

## 🌟 Key Features

### 🎙️ 1. Real-Time Voice Command Overlay
* **Spoken Voice Input:** Tap the voice icon on any screen to launch the animated voice command overlay.
* **Visual Wave Feedback:** Live listening indicator with immediate transcription.
* **Fast-Path Intent Automation:** Executes voice commands directly via native system intents or AI agent tasks.

### ⚙️ 2. Autonomous Task Execution & Interruption
* **Live Working Progress Bar:** Bottom progress bar displays the exact step and status during multi-step AI tasks.
* **Instant Task Cancellation:** Prominent **STOP** button allows immediate interruption and termination of long-running operations.

### 🤖 3. Multi-Model Inference API & NVIDIA NIM Engine
* **NVIDIA NIM Support:** Access 120B+ parameter open-source models including Nemotron-4 340B, Llama 3.1 405B, Llama 3.1 70B, and DeepSeek-R1.
* **Flexible Provider Selection:**
  * NVIDIA NIM Cloud (`https://integrate.api.nvidia.com/v1/`)
  * Groq Inference (Ultra-fast Llama 3.3 70B & DeepSeek R1)
  * OpenAI Direct API (GPT-4o, GPT-4o-mini, o3-mini)
  * Google Gemini API (Gemini 1.5 Flash / Pro)
  * DeepSeek Direct API
  * Ollama / Termux Local LLM (`http://localhost:11434/v1/`)
  * HuggingFace Inference API
* **API Key Testing:** In-app connection tester verifies model connectivity before applying configuration.

### 📱 4. Smart System Intents & App Automation
* **Instant YouTube Integration:** Commands like `"open YouTube and search Indies got latent"` launch directly in the YouTube app.
* **Spotify, WhatsApp & Phone Dialer:** Deep linkage with popular Android applications.
* **Accessibility Service Automation:** Automated UI clicking and input field insertion via standard Android Accessibility node tree inspection.

### 📲 5. Telegram Remote Bot Integration
* Remote command execution and status broadcasting directly through a Telegram bot interface.
* Bidirectional task logging and history synchronization.

### 🔐 6. Local Knowledge Vault & Security Policy Manager
* Encrypted credential storage for API keys and tokens.
* Pattern lock authentication for sensitive configuration screens.
* Comprehensive sync audit logging and Room DB persistence.

---

## 🏗️ Architecture & Stack

* **UI Framework:** Jetpack Compose with Material 3 Design System
* **Architecture:** MVVM (Model-View-ViewModel) + Clean Architecture
* **State Management:** `StateFlow` & `collectAsStateWithLifecycle`
* **Local Persistence:** Room Database with KSP (Kotlin Symbol Processing)
* **Networking:** OkHttp & Retrofit + Standard REST API JSON Parsing
* **Background Services:** Android Accessibility Service (`ParadimAccessibilityService`)
* **Asynchronous Execution:** Kotlin Coroutines & Flow

---

## 🚀 Getting Started & Building

### Prerequisites
* **Android Studio:** Ladybug (2024.2.1) or newer
* **JDK:** Version 17
* **Android SDK:** Compile SDK 35, Minimum SDK 26 (Android 8.0+)

### Building from Source

1. **Clone the repository:**
   ```bash
   git clone https://github.com/MRSKT332/paradim-autonomous-agent.git
   cd paradim-autonomous-agent
   ```

2. **Build the Debug APK:**
   ```bash
   gradle :app:assembleDebug
   ```
   The generated APK will be located at `app/build/outputs/apk/debug/app-debug.apk`.

3. **Install on Connected Device / Emulator:**
   ```bash
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

---

## 📄 License

This project is open-source and released under the [MIT License](LICENSE).
