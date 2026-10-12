# Capsibus — Product Requirements Document

**Version:** 1.0 (draft)
**Date:** 2026-09-06
**Owner:** Baim
**Parent project:** ChiliVision — *Chili Leaf Disease Detection App Deployment: Calibrated Rejection and Field-Robustness Evaluation of an InceptionV3 Classifier*
**Status:** Ready for build, gated on Phase 0 model verification

---

## 1. Problem

The ChiliVision InceptionV3 classifier reaches 99.74% accuracy on the clean held-out test split but degrades sharply under domain shift — Pests collapses to ~2% on field imagery, leaf-spot sits between 6% and 54%, and whole-canopy anthracnose photographs are misread as Virus in 143 of 162 cases. A model with that profile cannot be shipped as a plain classifier: an app that answers every question with a confident label would be actively misleading in exactly the conditions it is meant for. Capsibus is the deployment artefact for FYP2 — an offline Android app that runs the model on-device and, critically, **declines to answer** when the input is out of distribution or the prediction is not confident enough. The app is both a usable field tool and the empirical vehicle for demonstrating that the rejection mechanism behaves as claimed on real hardware.

---

## 2. Constraints

### Hard (non-negotiable)

| Constraint | Value |
|---|---|
| Platform | Android, Kotlin + Jetpack Compose |
| Inference runtime | `org.tensorflow:tensorflow-lite:2.14.0` |
| Capture | CameraX (photo), Android Photo Picker (gallery) |
| Connectivity | **100% offline.** No network permission in the manifest. Model and labels from local assets only. |
| Package | `com.chilivision.app` |
| minSdk / targetSdk | 24 / latest stable (36) |
| Model input | `[1, 299, 299, 3]` Float32, raw RGB in `[0.0, 255.0]` — **no division by 255** |
| Model output | `[1, 6]` Float32 softmax |
| Class order | `Bacterial, Fungal, Healthy, NonChili, Pests, Virus` (alphabetical, per `image_dataset_from_directory`) — **must be verified against the export, not assumed** |

### Soft

- No third-party analytics, crash reporting, or ad SDKs — offline claim must survive a manifest audit by an examiner.
- No account, no login, no server. Nothing to provision at demo time.
- Must run acceptably on a mid-range device; single inference on CPU, no GPU delegate required.

### Known model limitations the product must be designed around

- Hard-negative rejection at the shipped threshold is **25.55%** (measured, τ=0.70). Roughly three in four non-chili leaves will still receive a label.
- Virus is over-predicted across nearly every field class.
- The dominant failure signature is high green coverage + low edge detail — i.e. whole-plant photos taken at a distance.

---

## 3. Alternatives Considered

### 3.1 Preprocessing and inference path

**Option A — TFLite `Interpreter` + hand-built `ByteBuffer` (chosen).**
Full control over decode, EXIF rotation, resize interpolation, and buffer layout. More code to write and more to get wrong, but every step is inspectable and testable.

**Option B — TFLite Task Library `ImageClassifier` + `ImageProcessor`.**
Much less code; handles resize and normalization declaratively. The problem is that it hides the resize interpolation behind `ResizeOp` and expects normalization to be expressed as its own mean/std options. Training used a bilinear squash with rescaling *inside* the graph. Any silent mismatch between the library's defaults and the training pipeline produces no error — just quietly wrong probabilities, which is the single worst failure mode for this project.

**Decision: A.** The parity risk is the deciding factor. When the whole thesis rests on confidence values being meaningful, preprocessing must be explicit and testable rather than convenient.

### 3.2 Where the rejection logic lives

**Option A — Kotlin class operating on the output probability array (chosen).**
`ChiliClassifier` is pure Kotlin with no Android dependencies, unit-testable on the JVM in milliseconds. τ and T are named constants; retuning after FYP2 field data is a one-line change plus a test update.

**Option B — Bake the threshold and temperature into the TF graph before conversion.**
Fewer moving parts on-device and guarantees the app cannot drift from the evaluated configuration. But every retune requires a Python re-export and a new `.tflite`, and the rejection logic becomes untestable from the Android test suite.

**Decision: A.** τ is explicitly provisional. Optimising for cheap retuning beats optimising for fewer artefacts.

### 3.3 Operating point

**Option A — τ = 0.70 on raw softmax (chosen, per your call).**
Matches the legacy threshold used throughout earlier project documentation. Highest coverage, so the app answers most of the time — good for demos and for collecting per-image FYP2 evidence.

**Option B — T = 1.220418 with τ = 0.8788 or 0.9929.**
Raises hard-negative rejection from 25.55% to 45.43% or 86.44% respectively, and deploys the calibration mechanism the paper claims as its contribution. Cost: at τ=0.9929, field coverage drops to ~34% — the app refuses to answer two thirds of the time, which is hard to demo and frustrating in the field.

**Decision: A, with B one constant away.** The code will define both `CONFIDENCE_THRESHOLD = 0.70f` and `TEMPERATURE = 1.0f` as named constants, with the temperature scaling path implemented and unit-tested but set to identity. Switching to the calibrated operating point later means editing two numbers, not writing new code.

> **Recorded disagreement, once, for the file:** shipping τ=0.70 with T=1.0 means the deployed app does not implement the calibrated rejection mechanism that the AGRETA paper presents as the core contribution. If an examiner asks "does the app do what the paper says," the honest answer at this setting is "it uses the legacy threshold." That is a defensible engineering choice for a coverage-first prototype — but decide *before* the viva how you'll answer it, and consider whether the dev screen (§7.6) should be demonstrated rather than hidden.

---

## 4. Decision — Chosen Architecture

Single-module Compose app, single Activity, three screens, MVVM-lite.

```
com.chilivision.app
├── MainActivity.kt                  # single activity, NavHost
├── ui/
│   ├── theme/                       # Color.kt, Type.kt, Theme.kt
│   ├── home/HomeScreen.kt
│   ├── camera/CameraScreen.kt       # CameraX preview + capture
│   ├── result/ResultScreen.kt
│   ├── result/ResultViewModel.kt
│   └── dev/DevSettingsScreen.kt     # hidden, debug builds only
├── ml/
│   ├── ChiliClassifier.kt           # PURE KOTLIN — no android.* imports
│   ├── ClassificationResult.kt      # sealed interface
│   ├── ChiliClass.kt                # enum, ordinal == model output index
│   ├── TfLiteChiliModel.kt          # Interpreter lifecycle, Closeable
│   └── ImagePreprocessor.kt         # Uri -> Bitmap -> ByteBuffer
└── util/
    ├── ImageDecoder.kt              # sampled decode + EXIF rotation
    └── Result.kt                    # domain error type

assets/
├── chilivision_inceptionv3.tflite
├── labels.txt                       # one class per line, model index order
└── golden/                          # instrumented-test fixtures
    ├── manifest.json                # filename -> expected 6 probs (from predict_chili.py)
    └── *.jpg                        # 10 images
```

**Threading:** inference runs on `Dispatchers.Default` inside a ViewModel coroutine. The `Interpreter` is not thread-safe — one instance, one mutex, held by `TfLiteChiliModel`.

**Navigation payloads:** pass the image `Uri` as a string argument, never a `Bitmap`. Passing bitmaps through navigation or saved state is the classic `TransactionTooLargeException` crash.

**Model lifetime:** `TfLiteChiliModel` is created once and held for the process lifetime (Application-scoped or a simple object holder), not re-created per inference — interpreter construction costs hundreds of milliseconds.

---

## 5. Phase 0 — Model Verification Gate (blocking)

The `.tflite` exists but has not been verified. **No app screen work starts until every item below passes.** This is not busywork: a preprocessing mismatch produces plausible-looking but wrong probabilities, with no crash and no error message, and it will not be caught by any UI test.

### 5.1 Tensor introspection

Load the model on desktop (Python) or in an instrumented test and assert:

- input tensor shape `[1, 299, 299, 3]`, dtype `FLOAT32`
- output tensor shape `[1, 6]`, dtype `FLOAT32`
- exactly one input, one output
- no quantization parameters on either tensor (scale 0, zero_point 0)

If the input dtype is `UINT8`, the model was converted with default optimizations and the float contract is void — re-export with `converter.optimizations = []`.

### 5.2 Rescaling location

Feed a constant tensor of all `127.5` and a constant tensor of all `0.0`. If the rescaling layer survived conversion, these produce clearly different, non-degenerate outputs. If output probabilities are near-uniform `[0.167 × 6]` for realistic leaf images, suspect that the `Rescaling(1/255, offset=-1)` layer was stripped and that the model is receiving `[0,255]` where it expects `[-1,1]`.

### 5.3 Golden parity test (the real gate)

1. Pick 10 images: one per class from `models\run_images\!sanity check\`, plus 4 hard cases (an anthracnose whole-plant shot, a hard negative from `nonchili_otherleaves`, a Pests field image, one deliberately blurry photo).
2. Run each through `predict_chili.py --model models\final_inceptionv3_model.keras --image <path>` and record the full 6-vector to `assets/golden/manifest.json`.
3. Run the same files through the `.tflite` on-device via the app's own `ImagePreprocessor`.
4. **Assert:** `argmax` identical for all 10, and `max |Δp| < 1e-3` across all 60 values.

### 5.4 If parity fails — diagnostic order

Check in this sequence, it's roughly likelihood-ordered:

1. **Resize interpolation.** Training used `image_dataset_from_directory`, which squashes to 299×299 bilinearly with no aspect preservation. Use `Bitmap.createScaledBitmap(src, 299, 299, true)`. A center-crop or letterbox will not match.
2. **EXIF rotation.** Both camera captures and gallery images carry orientation metadata. Apply `ExifInterface` rotation *before* resize.
3. **Channel order.** Extract as R, G, B in that order from `Bitmap.getPixels` ARGB ints. A BGR buffer will not error, it will just be wrong.
4. **Rescaling stripped** — see §5.2.
5. **Bitmap config.** Decode to `ARGB_8888`; `RGB_565` loses precision and will break a 1e-3 tolerance.
6. **Colour management.** On API 26+, `BitmapFactory.Options.inPreferredColorSpace` should be sRGB so wide-gamut phone captures aren't silently converted.

---

## 6. Model Contract

```kotlin
// ml/ChiliClass.kt — ordinal MUST match model output index
enum class ChiliClass(val label: String) {
    BACTERIAL("Bacterial"),   // 0
    FUNGAL("Fungal"),         // 1
    HEALTHY("Healthy"),       // 2
    NON_CHILI("NonChili"),    // 3
    PESTS("Pests"),           // 4
    VIRUS("Virus")            // 5
}
```

`labels.txt` ships alongside the model and is parsed at load time; if its contents do not match the enum order, `TfLiteChiliModel` throws at init rather than serving wrong labels. Fail loud at startup, never quietly at inference.

**Input buffer construction:**

```
ByteBuffer.allocateDirect(1 * 299 * 299 * 3 * 4)
    .order(ByteOrder.nativeOrder())
// for each pixel, row-major:
//   putFloat((pixel shr 16 and 0xFF).toFloat())  // R, 0..255
//   putFloat((pixel shr 8  and 0xFF).toFloat())  // G
//   putFloat((pixel        and 0xFF).toFloat())  // B
// rewind() before every run()
```

---

## 7. Rejection Logic

### 7.1 Constants

```kotlin
const val CONFIDENCE_THRESHOLD = 0.70f   // provisional; project docs may retune
const val TEMPERATURE = 1.0f             // identity. Calibrated value: 1.220418
const val NUM_CLASSES = 6
private const val PROB_EPSILON = 1e-12f
private const val SUM_TOLERANCE = 1e-3f
```

### 7.2 Temperature scaling (implemented, currently identity)

Because the model emits softmax rather than logits, temperature scaling is recovered as `softmax(log(p) / T)`. This is exact — the additive constant that `log(softmax(z))` introduces cancels inside the outer softmax. Clamp `p` to `PROB_EPSILON` before the log so a zero probability cannot produce `-Inf`.

When `TEMPERATURE == 1.0f`, short-circuit and return the input array unchanged. Unit-test both branches, including a test asserting that `T = 1.220418` reproduces a known hand-computed vector.

### 7.3 Decision order

```
1. Validate:  size == 6, all values finite, all in [0,1], |sum - 1| <= 1e-3
              -> otherwise ClassificationResult.InvalidOutput
2. Apply temperature scaling
3. maxProb < CONFIDENCE_THRESHOLD    -> Uncertain
4. argmax == NON_CHILI               -> NotChili
5. otherwise                          -> Diagnosed(class, confidence, probabilities)
```

The confidence check precedes the NonChili check deliberately: a low-confidence NonChili prediction is reported as **Uncertain**, not as "not a chili leaf," because at 0.55 confidence the model is not entitled to make either claim. This ordering is an explicit product decision and has its own test case.

Threshold comparison is `>=` — a confidence of exactly 0.70 is **Diagnosed**, not Uncertain. State it in the test names so it cannot drift.

### 7.4 Result type

```kotlin
sealed interface ClassificationResult {
    data class Diagnosed(
        val chiliClass: ChiliClass,
        val confidence: Float,
        val probabilities: FloatArray   // all 6, post-temperature
    ) : ClassificationResult

    data class Uncertain(val topConfidence: Float, val probabilities: FloatArray) : ClassificationResult
    data class NotChili(val confidence: Float, val probabilities: FloatArray) : ClassificationResult
    data class InvalidOutput(val reason: String) : ClassificationResult
}
```

Every branch carries the full probability vector so the expandable details section works identically on all result types. (`data class` with a `FloatArray` field: override `equals`/`hashCode` or the tests will bite.)

### 7.5 User-facing copy

**UNCERTAIN**
> The image is unclear or cannot be confidently identified. Please take a closer, well-lit photo of the affected chili leaf.

**NON-CHILI**
> This does not appear to be a chili plant leaf. Please capture a valid chili leaf.

**DIAGNOSED** — class name, confidence percentage to one decimal, recommended action text, and an expandable "Detailed probabilities" section listing all six classes with their values.

**Recommended action text** (advisory, never prescriptive — no product names, no dosages):

| Class | Action text |
|---|---|
| Bacterial | Signs consistent with bacterial infection. Remove and destroy affected leaves, avoid overhead watering, and sanitise tools between plants. Confirm with your local agriculture officer before applying any treatment. |
| Fungal | Signs consistent with fungal infection. Improve spacing and airflow, keep foliage dry, and clear fallen debris around the plant. Confirm with your local agriculture officer before applying any treatment. |
| Healthy | No disease indicators detected in this leaf. Continue routine monitoring, especially on new growth and leaf undersides. |
| Pests | Signs consistent with pest damage. Inspect leaf undersides and stems for insects or eggs. **Note: this app performs poorly on pest damage under field conditions — treat this result as a prompt to inspect, not as a diagnosis.** |
| Virus | Signs consistent with viral infection. Viral disease cannot be cured; isolate or remove affected plants, control insect vectors, and sanitise tools. Confirm with your local agriculture officer. |

**Persistent footer on every result screen:**
> Research prototype. Accuracy degrades on distant or whole-plant photographs. Not a substitute for professional agronomic advice.

That footer is not legal boilerplate — it is the honest summary of your own field-validation numbers, and having it visible during the viva is a strength rather than an admission.

### 7.6 Dev settings screen (debug builds only)

Gated behind a long-press on the app title on Home; excluded from release via `BuildConfig.DEBUG`. Exposes:

- τ slider, 0.50 → 0.999 (defaults 0.70, 0.8467, 0.8788, 0.9929 as snap points)
- temperature toggle, 1.0 ↔ 1.220418
- last raw output vector, unrounded
- inference latency in ms

This exists so FYP2 field sessions can sweep the operating point without a rebuild, and so per-image evidence can be collected for the results section your supervisor asked to see.

---

## 8. User Flow

```
Home
 ├── [Take Photo]  ──> permission check
 │                      ├── granted ──> Camera ──> capture ──> Result
 │                      ├── denied  ──> inline rationale + Retry
 │                      └── denied permanently ──> "Open Settings" deep link
 │
 └── [Choose from Gallery] ──> Photo Picker ──> Result
                                    └── cancelled ──> back to Home, no error

Result
 ├── image thumbnail (as submitted to the model — post-rotation, post-resize preview)
 ├── verdict block  (Diagnosed | Uncertain | NotChili | error)
 ├── confidence     (Diagnosed / Uncertain / NotChili only)
 ├── recommended action text
 ├── [Detailed probabilities ▾]  collapsed by default
 ├── disclaimer footer
 └── [Analyse Another]  ──> Home
```

**Capture screen guidance overlay** — a framing guide plus a one-line hint: *"Fill the frame with a single leaf. Avoid whole-plant shots."* This is directly motivated by the measured failure signature (anthracnose green fraction 0.272 vs 0.111 in training, edge density 0.192 vs 0.322). The overlay is a UX countermeasure to a documented model weakness and should be described as such in the report.

---

## 9. Error Handling

No path may crash. Every failure resolves to a screen the user can act on.

| Failure | Handling |
|---|---|
| Camera permission denied | Inline rationale card + Retry button; gallery remains available |
| Permission permanently denied | Same card, button becomes "Open Settings" (`ACTION_APPLICATION_DETAILS_SETTINGS`) |
| No camera hardware | Hide "Take Photo", show gallery-only home |
| CameraX bind failure | Error state with Retry; do not leave a black preview |
| Corrupt / undecodable image | "This image could not be opened. Please choose another." |
| Unsupported format (HEIC on API < 28) | Same message — do not attempt format detection heuristics |
| Very large image | Two-pass decode with `inJustDecodeBounds` + `inSampleSize` to cap the intermediate bitmap near 1024px before the final resize |
| OOM during decode | Catch `OutOfMemoryError`, recycle, show the decode-failure message |
| Interpreter init failure | Full-screen error with Retry; log the exception message in debug builds |
| Model output malformed | `InvalidOutput` → "Analysis failed. Please try again." Never render a label from a vector that failed validation |
| Process death mid-flow | Image `Uri` survives in `SavedStateHandle`; re-run inference on restore rather than persisting the result |

---

## 10. Test Plan

### 10.1 Unit — `ChiliClassifierTest` (JVM, no Android)

| Case | Input | Expected |
|---|---|---|
| Clear diagnosis | `[0.02,0.01,0.93,0.01,0.02,0.01]` | Diagnosed(HEALTHY, 0.93) |
| Exactly at threshold | max = 0.70 on Bacterial | **Diagnosed** (`>=`) |
| Just below threshold | max = 0.699 on Bacterial | Uncertain |
| Confident non-chili | `[0.05,0.02,0.03,0.85,0.03,0.02]` | NotChili(0.85) |
| Low-confidence non-chili | max = 0.55 on NonChili | **Uncertain**, not NotChili |
| Near-uniform | all ≈ 0.1667 | Uncertain |
| Every class diagnosable | 6 parameterised cases at 0.95 | correct label each time |
| Wrong length | 5 or 7 elements | InvalidOutput |
| NaN present | contains `Float.NaN` | InvalidOutput |
| Does not sum to 1 | sums to 0.4 | InvalidOutput |
| Temperature identity | T = 1.0 | output vector unchanged |
| Temperature applied | T = 1.220418 | matches hand-computed expected vector |
| Temperature flips outcome | vector at 0.72 raw that drops below τ after T = 1.220418 | Uncertain |

### 10.2 Unit — preprocessing (JVM with Robolectric, or instrumented)

- Buffer capacity is exactly `1*299*299*3*4` bytes
- Buffer values fall in `[0, 255]`, not `[0, 1]` and not `[-1, 1]`
- A known 2×2 synthetic bitmap produces the expected R,G,B ordering
- EXIF orientation 6 (90° CW) produces a rotated buffer distinguishable from orientation 1

### 10.3 Instrumented — `ModelIntegrationTest` (real device/emulator)

- `.tflite` loads from assets as a `MappedByteBuffer`
- Input/output tensor shapes and dtypes match §5.1
- `labels.txt` order matches `ChiliClass` ordinals
- **Golden parity:** all 10 fixtures — `argmax` matches `manifest.json` and `max |Δp| < 1e-3`
- Single-inference latency logged (informational, no hard assertion)
- Repeated inference (50×) does not leak — heap stable, no interpreter re-creation

### 10.4 Manual / field checklist

- Airplane mode on for the entire demo — proves the offline claim visibly
- One anthracnose whole-plant photo, to confirm the framing hint appears and to see what the app actually says
- One non-leaf object (hand, floor, printed text) — expect a label roughly 3 times out of 4 at τ=0.70; know this before an examiner tries it

### 10.5 Build hygiene

- `androidResources { noCompress += "tflite" }` — without it, `loadMappedFile` fails at runtime with a confusing error
- No `android.permission.INTERNET` in the merged manifest; add a CI grep or a lint check asserting its absence
- R8 keep rules for TFLite: `-keep class org.tensorflow.lite.** { *; }`

---

## 11. Acceptance Criteria

Capsibus is done when:

1. Phase 0 golden parity passes at `max |Δp| < 1e-3` on all 10 fixtures.
2. All unit and instrumented tests pass.
3. The merged release manifest contains no INTERNET permission.
4. Full flow works end to end in airplane mode on a physical device.
5. Camera-permission denial, permanent denial, gallery cancellation, and a corrupt file are each exercised without a crash.
6. `CONFIDENCE_THRESHOLD` and `TEMPERATURE` exist as documented named constants with the calibrated values recorded in comments.
7. Result screen shows class, confidence to one decimal, action text, expandable six-class breakdown, and the disclaimer footer.

---

## 12. Open Questions

1. **Package vs product name.** Spec fixes `com.chilivision.app` while the product is Capsibus. Fine for a research build, and `applicationId` cannot change after a Play release — confirm you're not planning a store listing under the Capsibus name.
2. **Class-order verification.** Alphabetical ordering is inferred from `image_dataset_from_directory` convention, not read from a saved artefact. Confirm against the training script's `class_names` output before trusting `labels.txt`.
3. **Result persistence.** Currently none — no history, no saved diagnoses. FYP2 per-image evidence collection may want a local export (CSV of URI, top class, six probabilities, timestamp). Cheap to add now, awkward to retrofit. Decide before build.
4. **Grad-CAM.** Listed as an FYP2 priority for the background-shortcut investigation. Out of scope for v1; note that adding it later needs a second model export with intermediate activations exposed, which affects the `.tflite` contract.
5. **τ retune trigger.** What field evidence would move you off 0.70? Worth writing down now while the reasoning is fresh.

---

## Appendix A — Reference values

| Quantity | Value | Source |
|---|---|---|
| Fitted temperature T | 1.220418 | val set, N = 1,165 |
| τ candidates (val-derived) | 0.70 (legacy), 0.8467, 0.8788, 0.992937 | val only, field-independent |
| Hard-negative rejection by τ | 25.55% / 41.01% / 45.43% / 86.44% | N = 317 |
| Clean test accuracy | 99.74% (1158/1161) | de-leaked test split |
| Training input | 299×299 RGB, `Rescaling(1/255, offset=-1)` | verified training config |
| Class totals (train+val+test) | Bacterial 1,913 · Fungal 997 · Healthy 1,988 · NonChili 400 · Pests 1,031 · Virus 1,428 | on-disk enumeration |
