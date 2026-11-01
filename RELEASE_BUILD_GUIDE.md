# HireGrid — Production Release & AAB Signing Guide

This guide details how to build and sign a production-ready **Android App Bundle (`.aab`)** for Google Play Store publication.

---

## 1. Prerequisites
- **JDK 17+** (installed on your system).
- **Android Studio Iguana / Jellyfish / Koala** or newer.

---

## 2. Step 1: Generate a Production Keystore

Run the following command in PowerShell or Command Prompt to generate your private release keystore:

```powershell
keytool -genkey -v -keystore hiregrid-release-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias hiregrid -storetype JKS
```

### You will be prompted to enter:
1. Keystore password (choose a secure password and store it safely).
2. Your name / organization name.
3. City, State, Country code.

> [!CAUTION]
> **Keep your keystore and password secure!**  
> If you lose your upload keystore, you will not be able to push updates to the Google Play Store without contacting Google Developer Support for a key reset.

---

## 3. Step 2: Configure Signing in `app/build.gradle.kts`

Place `hiregrid-release-key.jks` inside `android/app/` (or specify an absolute path).

Update `android/app/build.gradle.kts`:

```kotlin
android {
    ...
    signingConfigs {
        create("release") {
            storeFile = file("hiregrid-release-key.jks")
            storePassword = System.getenv("KEYSTORE_PASSWORD") ?: "YourKeystorePassword"
            keyAlias = "hiregrid"
            keyPassword = System.getenv("KEY_PASSWORD") ?: "YourKeyPassword"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}
```

---

## 4. Step 3: Build the Signed Android App Bundle (AAB)

Open a terminal in the `android/` directory and execute:

```powershell
# On Windows
.\gradlew bundleRelease
```

### Build Artifact Location:
Upon completion, your signed production bundle will be located at:
```
android/app/build/outputs/bundle/release/app-release.aab
```

This `.aab` file is the exact artifact to upload directly to the **Google Play Console** under **Internal Testing**, **Closed Testing**, or **Production**.

---

## 5. Step 4: Connecting the Android App to Your Backend in Production

During local development, the app connects to `http://10.0.2.2:8000/api/v1/` on the Android emulator.

When deploying for production:
1. Deploy your FastAPI backend to any cloud host (e.g. Render, Railway, Fly.io, or AWS EC2).
2. In `RetrofitClient.kt`, update `baseUrl`:
   ```kotlin
   var baseUrl: String = "https://api.yourdomain.com/api/v1/"
   ```
3. Ensure HTTPS is enabled with a valid SSL/TLS certificate.
