# 🎬 Cincut Video Editor Pro — Professional Mobile Video Editor
> Professional Android video editing suite with CapCut multi-track timeline, ZapUPI payment gateway, real-time export progress, and Founder Admin Command Station.

[![Android](https://img.shields.io/badge/Platform-Android_8.0+-3DDC84.svg?logo=android&logoColor=white)](https://android.com)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin_2.0-7F52FF.svg?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/UI-Jetpack_Compose-4285F4.svg?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Build APK](https://github.com/robinisking3-byte/CincutVideoEdittorPro/actions/workflows/build-apk.yml/badge.svg)](https://github.com/robinisking3-byte/CincutVideoEdittorPro/actions/workflows/build-apk.yml)

---

## 📦 Production Releases & Direct APK Download

| Package | Version | Application ID | Direct Releases |
| :--- | :--- | :--- | :--- |
| **Cincut Video Editor Pro** | v1.0.0 Production | `com.cincut.editor.studio` | [🚀 GitHub Releases & Direct APK](https://github.com/robinisking3-byte/CincutVideoEdittorPro/releases) |

---

## ⚡ Architecture & Subsystems

### 1. Independent Application Separation
- **Cincut Video Editor Pro** (`com.cincut.editor.studio`): Standalone, clean installation for creators and filmmakers. Never conflicts with or loads old package installations.
- Both applications can be installed simultaneously on the same Android device without conflict.

### 2. Video Editor Subsystem
- **Android Photo/Video Picker**: Modern zero-permission media selection via `ActivityResultContracts.PickVisualMedia`.
- **Interactive Trimming Engine**: Range trimming with real-time second calculation and cut duration readouts.
- **Playback Speed Controller**: 0.5x, 1.0x, 1.5x, and 2.0x playback speed chips.
- **Audio Mixing**: Real-time volume scaling (0% to 200%).
- **Color Grading**: Cinematic Gold, Noir B&W, Warm Sunset, Cool Teal, and Vivid Contrast presets.
- **Aspect Ratio Formatting**: 16:9 widescreen, 9:16 vertical reels/shorts, 1:1 square, and 4:5 social portrait.

### 3. Real Social & Friend Network
- **Live Firestore User Directory**: Search creators by handle, email, or display name.
- **Persistent Friend Requests**: Request dispatching, real-time Firestore listeners, accept/decline state handling.
- **Zero Mock Data**: No hardcoded follower numbers or preloaded chats. All stats reflect real database state.

### 4. Professional AI Studio
- **Script Generation**: Generates structured scene-by-scene scripts with tone selection.
- **Social Metadata**: Generates caption templates, director workflow tips, and hashtags.
- **Beat Cut Cues**: Produces millisecond-accurate timeline cutting cues and transition instructions.

### 5. Architectural Theme Engine
- **Cinematic Charcoal & Gold**: Minimalist, dark, creator-focused aesthetic (default).
- **Diwali Festival Theme**: Deep navy background with warm amber and marigold highlights.
- **Holi Festival Theme**: Deep slate background with vibrant festival color accents.

---

## 🛡️ Administrative Console Modules (`CutMedia Admin`)
- **Real-Time Telemetry**: Live metric counts for registered creators, audit events, and broadcast notices.
- **User Directory**: View registered accounts and promote roles (Creator, VIP, Admin).
- **Immutable Audit Trail**: Chronological event logging with administrator timestamps.
- **Emergency App Lock**: Global maintenance kill-switch that can be activated from the cloud.
- **Push Broadcaster**: Broadcast announcements to connected devices.
- **Voucher Engine**: Issue and manage promotional coupon codes.

---

## 📱 How to Install on Android
1. Download **`CutMedia-Release-v2.4.0.apk`** to your phone.
2. Tap the file in your notification bar or Downloads folder.
3. If prompted, toggle **"Allow from this source"** in Android Settings.
4. Tap **Install** and open CutMedia.
