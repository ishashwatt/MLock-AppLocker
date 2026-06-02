<p align="center">
  <img src="https://img.icons8.com/color/150/000000/lock--v1.png" alt="MLock Logo" width="100"/>
</p>

# 🔐 MLock - Advanced App Locker & Privacy Guardian

**Made for personal use.**

[![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-B125EA?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack_Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Local Only](https://img.shields.io/badge/Privacy-100%25_Local-blue?style=for-the-badge)](#)

**MLock** is an ultra-fast, offline-only Android application designed to secure your sensitive apps with biometric authentication. Built with modern Android architecture and Jetpack Compose, MLock operates silently in the background using an Accessibility Service, ensuring your privacy is never compromised.

---

## ✨ Key Features

- 🔒 **Instant App Locking**: Detects and locks selected applications immediately upon launch.
- 🖐️ **Biometric & Pattern Security**: Seamless unlocking using your device's native biometric sensors (Fingerprint/Face Unlock), along with standard PIN, Password, or Pattern lock fallbacks.
- ⏱️ **Zero-Glitch Transitions**: Optimized memory and state management to prevent lock screen flickering or delays.
- 🛡️ **Notification Protection**: Automatically filters and hides sensitive notification content from locked apps, ensuring your messages and alerts remain private.
- ⚙️ **Smart Grace Period**: Configurable auto-lock timers (Immediately, 1s, 5s, 10s, 30s) so you aren't repeatedly prompted for authentication when briefly switching between apps.
- 🔋 **Battery Optimization Bypass**: Easily disable battery optimizations to ensure the accessibility service is never killed by the OS.
- 👁️ **Data at Rest & UI Security**: Leverages built-in Android `FLAG_SECURE` techniques to prevent the lock screen from appearing in recent apps or leaked via screenshots.
- 📵 **Offline & Secure**: No tracking, no analytics, no external servers. Your data and security preferences never leave your device.

---

## 🚀 Installation & Setup

### 📥 Try the App (APK)

Want to skip the build process? Download the latest stable APK directly and install it on your device:

🔗 **[Download MLock APK Placeholder Link](https://github.com/ishashwat/MLock-AppLocker/releases/download/v1.0.0/MLock.apk)**

*(Note: Ensure you have "Install from Unknown Sources" enabled on your Android device to install the APK directly.)*

### 🛠️ Build from Source

If you prefer to compile the application yourself, follow these detailed steps:

#### 1. Prerequisites
- **Android Studio**: Download and install the latest version of [Android Studio](https://developer.android.com/studio).
- **Android SDK**: Ensure API level 34+ is installed.
- **Java**: JDK 17 or higher.

#### 2. Clone the Repository
Open your terminal and run:
```bash
git clone https://github.com/ishashwatt/MLock-AppLocker.git
cd MLock
```

#### 3. Open in Android Studio
1. Launch Android Studio.
2. Select **File > Open**, navigate to the cloned `MLock` folder, and click **OK**.
3. Allow Gradle to sync the project dependencies completely (this may take a minute).

#### 4. Compile and Run
1. Connect your Android device via USB (make sure **USB Debugging** is enabled in Developer Options) or start an Android Emulator.
2. Select your device from the target drop-down menu in the toolbar.
3. Click the green **Run ▶️** button (or press `Shift + F10`).
4. The app will compile, install, and launch on your device automatically.

---

## 📱 How to Use

1. **Initial Setup**: Launch MLock for the first time.
2. **Grant Permissions**: The app will guide you to grant specific permissions required for its operation:
   - **Accessibility Service**: Critical for detecting when you open a locked app.
   - **Usage Access**: Helps monitor app transitions.
   - **Notification Access (Optional)**: Required if you want to hide notification content from locked apps.
3. **Select Apps**: Browse the list of installed applications and toggle the switch for any app you wish to secure (e.g., WhatsApp, Banking Apps, Photos).
4. **Configure Grace Period**: Open the **Settings** tab within MLock and set your preferred "Auto-lock time" to balance security and convenience.

---

## 🤝 Contributing

Contributions, issues, and feature requests are welcome! If you'd like to improve MLock, follow these steps:

1. **Fork the Repository**: Click the `Fork` button at the top right of the GitHub page.
2. **Clone your Fork**: 
   ```bash
   git clone https://github.com/ishashwatt/MLock-AppLocker.git
   ```
3. **Create a Feature Branch**: 
   ```bash
   git checkout -b feature/AmazingFeature
   ```
4. **Commit your Changes**: 
   ```bash
   git commit -m 'Add some AmazingFeature'
   ```
5. **Push to your Branch**: 
   ```bash
   git push origin feature/AmazingFeature
   ```
6. **Open a Pull Request**: Go to the original repository and click **New Pull Request**.

---

<p align="center">
  <i>Built with ❤️ for ultimate privacy.</i>
</p>
