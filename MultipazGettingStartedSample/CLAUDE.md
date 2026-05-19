# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Multipaz Getting Started Sample — a Kotlin Multiplatform (KMP) mobile wallet app for digital identity credentials (mDL, mDoc). Targets Android and iOS using Compose Multiplatform for shared UI. Built on the **Multipaz SDK v0.98.0** from the OpenWallet Foundation.

## Build Commands

```bash
# Build the project
./gradlew build

# Android: assemble debug APK
./gradlew :composeApp:assembleDebug

# Run tests
./gradlew test

# Clean
./gradlew clean
```

iOS builds use CocoaPods. Run `pod install` in `iosApp/` before opening `iosApp.xcworkspace` in Xcode.

## Architecture

### Module Structure

- **`composeApp/`** — Main application entry point. Composes all feature modules. Contains `MainActivity` (Android) and platform-specific setup.
- **`core/`** — Shared dependency injection (`AppContainer` / `AppContainerImpl`) and app configuration. All feature modules depend on this.
- **`feature/biometrics/`** — Face detection and biometric matching (TensorFlow Lite on Android, GoogleMLKit on iOS).
- **`feature/presentment/`** — Credential presentation, QR code display, NFC/BLE engagement.
- **`feature/provisioning/`** — OpenID4VCI credential provisioning.
- **`feature/verification/`** — W3C Digital Credentials verification.
- **`iosApp/`** — Swift entry point wrapping Compose framework via SwiftUI.

### Key Patterns

- Singleton `App` class manages initialization and top-level navigation.
- `AppContainer` interface provides manual dependency injection (no DI framework).
- KMP source sets: `commonMain` for shared code, `androidMain`/`iosMain` for platform-specific implementations.
- Platform-specific HTTP engines: OkHttp (Android), Darwin (iOS) via Ktor.

### Key Dependencies

- **Multipaz SDK** (`multipaz`, `multipaz-compose`, `multipaz-doctypes`, `multipaz-dcapi`, `multipaz-vision`) — core identity credential framework
- **Compose Multiplatform 1.10.1** — shared UI
- **Ktor 3.4.0** — HTTP client
- **Kotlinx Serialization** — JSON handling
- **TensorFlow Lite** (Android) / **GoogleMLKit** (iOS) — face detection

## Platform Requirements

- **Android**: minSdk 29, compileSdk 36, targetSdk 36
- **iOS**: 16.0+
- **Kotlin**: 2.3.10
- **Gradle**: 8.13 (use wrapper)
- **JVM**: Gradle daemon uses 3GB heap, Gradle process uses 4GB

## Version Catalog

All dependency versions are managed in `gradle/libs.versions.toml`. Update versions there, not in individual `build.gradle.kts` files.
