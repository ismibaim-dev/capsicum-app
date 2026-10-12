# capsicum-app

**Capsibus** (ChiliVision) is an offline Android app that photographs a chili leaf and runs an
InceptionV3 TensorFlow Lite model on the device. It reports one of six classes (Bacterial, Fungal,
Healthy, NonChili, Pests, Virus), or says **Uncertain** when the top probability is below the
confidence threshold (0.70). The app has no INTERNET permission.

Product and design notes live in [`docs/capsibus-prd.md`](docs/capsibus-prd.md) and
[`docs/capsibus-ui-spec.md`](docs/capsibus-ui-spec.md).

## Project facts

| | |
|---|---|
| Module | `:app` (single module) |
| applicationId | `com.chilivision.app` (debug builds add `.debug`) |
| minSdk / targetSdk / compileSdk | 24 (Android 7.0) / 34 / 34 |
| Toolchain | Gradle 8.5 (wrapper), AGP 8.2.2, Kotlin 1.9.22, JDK 17 |

## The model file (Git LFS)

`app/src/main/assets/chilivision_inceptionv3.tflite` is about 86 MiB, so it is stored with
[Git LFS](https://git-lfs.com) (see `.gitattributes`). Install Git LFS once with `git lfs install`
before cloning or the file comes down as a small text pointer.

The app builds without the model, but it shows "Model initialization failed" at runtime. The
release workflow refuses to build if the model is missing or is only an LFS pointer.

## Build locally

Requires JDK 17 and the Android SDK (Android Studio provides both).

```bat
gradlew.bat assembleDebug          :: Windows
```

```sh
./gradlew assembleDebug            # macOS / Linux
```

The debug APK lands in `app/build/outputs/apk/debug/`. Unit tests: `gradlew testDebugUnitTest`.

## Releases

Pushing a tag that starts with `v` (for example `v1.0.0`) runs
`.github/workflows/android-release.yml`, which builds a signed release APK and attaches it to a
GitHub Release. Signing reads these repository secrets:

| Secret | Contents |
|---|---|
| `KEYSTORE_BASE64` | The release keystore file, base64-encoded |
| `KEYSTORE_PASSWORD` | Keystore password |
| `KEY_ALIAS` | Key alias inside the keystore |
| `KEY_PASSWORD` | Key password |

The keystore itself is never committed (`*.jks`, `*.keystore` and `*.b64` are ignored). Keep a
backup of it: Android only installs updates signed with the same key.
