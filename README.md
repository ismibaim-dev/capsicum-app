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

The app builds without the model, but it shows "Model initialization failed" at runtime.

## Build locally

Requires JDK 17 and the Android SDK (Android Studio provides both).

```bat
gradlew.bat assembleDebug          :: Windows
```

```sh
./gradlew assembleDebug            # macOS / Linux
```

The debug APK lands in `app/build/outputs/apk/debug/`. Unit tests: `gradlew testDebugUnitTest`.
