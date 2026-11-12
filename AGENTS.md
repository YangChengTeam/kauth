# KAuth Agent Guide

Shared instructions for any coding agent working on this repo (Claude Code, Codex, ...). `CLAUDE.md` only imports this file; put changes here.

## Project

KAuth is an offline TOTP/HOTP authenticator for Android: Kotlin, Jetpack Compose, Hilt, Room, CameraX + ZXing. minSdk 26. There is no server and there never will be one.

## Repository map

| Path | Contents |
| --- | --- |
| `core/model` | `Account` and the OTP enums. Plain JVM |
| `core/otp` | Code generation, Base32, `otpauth://` parsing (`OtpAuthUri`), Google Authenticator `otpauth-migration://` parsing (`GoogleAuthMigration`). Plain JVM, unit tested |
| `core/backup` | `BackupFile`: the password protected export format (PBKDF2 + AES-GCM over `otpauth://` lines). Plain JVM, unit tested |
| `core/data` | Room database, `AccountRepository`, `SecretCipher` (Android Keystore), `AccountBackup` (file import/export) |
| `core/designsystem` | Ink-wash theme, colors, type, icons and shared components (`InkDialog`, `CountdownRing`, ...) |
| `feature/home` | Account list, search, edit/delete, backup dialogs |
| `feature/setup` | Entering a setup key by hand |
| `feature/scan` | Camera QR scanning: single accounts and Google Authenticator exports |
| `app` | `MainActivity`, navigation, release signing |
| `build-logic` | Gradle convention plugins (`kauth.android.feature`, `kauth.jvm.library`, ...) |
| `.github/workflows` | `ci.yml` (tests + debug build on every push/PR), `release.yml` (signed APK on `v*` tags) |

## Commands

```bash
./gradlew test             # All unit tests; run before every commit
./gradlew assembleDebug    # Debug APK, package com.yangfeisg.kauth.debug
```

Needs JDK 17+. Locally, Android Studio's JBR works: `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"`.

## Rules that must not break

- **Nothing leaves the device.** No `INTERNET` permission, no analytics, no crash reporting, no network libraries. Accounts move between phones only through a backup file or a QR code.
- **Secrets are never stored in plain text.** The database holds only `SecretCipher` output. Never log a secret, a code or an `otpauth://` URI.
- **`android:allowBackup` stays false.** The Keystore key can't leave the device, so a system backup could not be decrypted anyway.
- **Released formats only grow.** Users already have v1.0.0 data:
  - Room schema changes need a `Migration`; never bump the version with destructive fallback.
  - `BackupFile` must keep reading every version it ever wrote. Bump its version byte for a new layout.
- **Imports skip duplicates** through `AccountRepository.addAll`. New import paths use it too.
- **Parsers stay in the plain JVM modules** (`core/otp`, `core/backup`) with unit tests. Write test vectors by hand; don't depend on real accounts.
- **User-facing text goes in string resources, in both `values` (English) and `values-zh` (Chinese).**
- **No new dependencies without a clear need.** ZXing is the only third-party library outside AndroidX and Hilt.

## Code style

Match the surrounding code. Comments are short and say *why*. Composables take a `Modifier` and keep state in a `ViewModel`. New UI follows the ink-wash look in `core/designsystem`: dialogs use `InkDialog`, colors come from the theme.

## Release

1. Update README.md and README_CN.md together when a user-facing feature changes.
2. Tag and push: `git tag vX.Y.Z && git push origin vX.Y.Z`. `release.yml` tests, builds and signs `KAuth-vX.Y.Z.apk`, then publishes it with `SHA256SUMS`. The tag sets the version (`versionCode = X*10000 + Y*100 + Z`).
3. The signing keystore lives in repository secrets (`KAUTH_*`) and outside the repo with the maintainer. Never commit a keystore, `signing.env` or `local.properties`.
