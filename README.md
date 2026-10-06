# AeroMaintenance AI

An AI-assisted predictive-maintenance prototype for aircraft health monitoring and component risk assessment, built as a native Android app in **Java** using only the Android SDK (no Kotlin, no third-party libraries).

> **Important.** This application is an academic research prototype using simulated/synthetic data. It is not certified for real-world aviation safety, maintenance, or flight-critical decision-making. The AI shown is **Prototype / Simulated AI Inference**: a transparent statistical pipeline standing in for a trained neural network.

The app walks through the full workflow from the project brief:

```
Aircraft/sensor data → preprocessing → AI/ML analysis → anomaly detection → component health
→ failure-risk prediction → remaining useful life (RUL) → maintenance recommendation → mobile alert
```

For the headline case (aircraft **AERO-101**, abnormal data) the engine computes: anomaly score **0.91**, failure risk **HIGH**, component **Engine Bearing**, *progressive degradation detected*, RUL **42 operating hours**, confidence **94.2 %**, recommendation *"Schedule engine-bearing inspection during the next available maintenance window."* These numbers are computed from the simulated telemetry every time, not typed into the UI, and a unit test locks them in.

---

## 1. Install and run

### On a phone (quickest)

1. Download `AeroMaintenanceAI.apk` (the release build) from this repository's **Releases** page (the newest "AeroMaintenance AI (Java)" pre-release), or from the `apk` artifact of the latest **Build APK** run under the **Actions** tab.
2. Open it on the phone. Android asks to allow installs from that source the first time; allow it.
3. Launch **AeroMaintenance AI**. Requires Android 8.0 or later; works fully offline.
4. When you tap **Start demo** the app asks for notification permission (Android 13+). Allow it so the AI alert also shows up as a phone notification.

The release APK is signed with a demo key that is committed in the repo (`app/aero-demo.jks`), so a newer build installs over an older one (including the earlier Kotlin build). That key is public, which is fine for a classroom prototype and not for anything published to a store.

### In Android Studio

1. Install Android Studio (Hedgehog or newer) with Android SDK 34 and JDK 17.
2. **File → Open** and choose this folder. Gradle sync downloads only the Android Gradle Plugin 8.5 (and JUnit for tests); the app itself has no library dependencies.
3. Pick an emulator or a USB-connected phone and press **Run**.

### From the command line

```bash
./gradlew testDebugUnitTest      # AI engine unit tests (plain JVM)
./gradlew assembleRelease        # app/build/outputs/apk/release/app-release.apk
adb install -r app/build/outputs/apk/release/app-release.apk
```

### Opening a screen directly (handy while presenting)

```bash
adb shell am start -n com.aeromaintenance.ai/.MainActivity --es screen analysis --es aircraft AERO-101 --es condition abnormal --ez run_analysis true
adb shell am start -n com.aeromaintenance.ai/.MainActivity --ez demo true
```

`screen` accepts `dashboard`, `aircraft`, `detail`, `monitoring`, `analysis`, `insights`, `predictive`, `planning`, `alerts`, `reports`, `faculty`, `about`.

---

## 2. What is in the app

| Screen | What it shows |
|---|---|
| Dashboard | Fleet health dial (87/100, GOOD), fleet overview (12 aircraft: 8 healthy, 3 warning, 1 critical), active alerts, quick actions, aircraft needing attention, the AI pipeline, **Start demo** |
| Aircraft | All 12 aircraft with model, flight hours, cycles, health score, risk level and last maintenance; filter by status |
| Aircraft detail | Aircraft information, component health cards (Engine 91 %, Hydraulic System 78 %, Engine Bearing 62 %, Fuel System 94 % for AERO-101), vibration trend, shortcuts |
| Sensor monitoring | Six live-updating instrument dials and six trend charts (temperature, vibration, hydraulic pressure, oil pressure, RPM, fuel flow) with caution/warning bands, removed spikes and detected onset; **Normal data / Abnormal data** toggle; **Run AI analysis** |
| AI prediction | Model name with the *Prototype / Simulated AI Inference* label, animated nine-stage pipeline, anomaly score dial, failure risk, component, predicted condition, RUL with projection chart, confidence, recommendation and work package, explanation and per-sensor evidence, all-component table, pipeline trace |
| Maintenance | **Predictive** tab: Component / Health / Risk / RUL / Action table, RUL risk chart and fleet watchlist. **Work orders** tab: upcoming maintenance with Scheduled / In progress / Completed statuses you can advance |
| Alerts | Critical / Warning / Monitor alerts with timestamp, aircraft, component, severity, AI-generated explanation and recommended action; acknowledge; new alerts are also posted as Android notifications |
| Reports | **Generate maintenance report** produces a formatted report (aircraft, finding, recommendation, sensor evidence, explanation, component table, engineer sign-off, disclaimer) that can be shared as text |
| AI insights | What the prototype actually computes at each stage, and the models a production version could use (LSTM, GRU, BiLSTM, 1D CNN, CNN-LSTM, Transformer, autoencoder) |
| Faculty demo mode | Problem, proposed solution, AI pipeline, benefits, future scope and the guided demo |
| About | Description and the disclaimer |

**Start demo** runs the ten steps from the brief automatically, with narration and Back / Pause / Next controls: select AERO-101 → show telemetry → switch to abnormal trend → run AI analysis → anomaly score → failure risk → RUL → recommendation → create alert → show maintenance action. It takes about 70 seconds without pauses.

---

## 3. Folder structure

```
.
├── app/
│   ├── build.gradle.kts                    Android module: SDK levels, signing, lint, JUnit
│   ├── aero-demo.jks                       demo signing key (public on purpose)
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/aeromaintenance/ai/
│       │   │   ├── MainActivity.java       the only activity: app shell, screen switching, deep links
│       │   │   ├── AppState.java           single source of truth: navigation, selection, analysis,
│       │   │   │                           alerts, work orders, report, guided demo (observer pattern)
│       │   │   ├── Screen.java, Destination.java   screens and back-stack entries
│       │   │   ├── DemoScript.java         the ten demo steps and their narration
│       │   │   ├── LaunchOptions.java      intent extras for deep links and CI screenshots
│       │   │   ├── data/                   plain Java, no Android imports
│       │   │   │   ├── SensorType, ComponentType, FaultMode, RiskLevel, …   domain enums
│       │   │   │   ├── Aircraft, Alert, MaintenanceTask, TelemetryWindow     immutable models
│       │   │   │   ├── FleetRepository     the 12 simulated aircraft and initial work orders
│       │   │   │   ├── TelemetrySimulator  deterministic normal / abnormal telemetry
│       │   │   │   └── XorWowRandom        seedable random generator (repeatable demo)
│       │   │   ├── engine/                 plain Java "AI" pipeline
│       │   │   │   ├── InferenceEngine     preprocessing → features → anomaly → health → risk → RUL
│       │   │   │   ├── Recommendations     rule base: condition, recommendation, work package
│       │   │   │   ├── AlertFactory        result → alert
│       │   │   │   └── ReportGenerator     result → maintenance report
│       │   │   ├── notify/AlertNotifier    Android notifications
│       │   │   └── ui/
│       │   │       ├── Palette, Type, Ui, Controls   colours, Barlow type scale, view toolkit
│       │   │       ├── widget/             Canvas views: DialView, TrendChartView, ProjectionChartView,
│       │   │       │                       RiskChartView, PipelineStepperView, PanelView, ButtonView, …
│       │   │       ├── shell/              TopBar, BottomNav, NavDrawer, DemoPanel, Snackbar
│       │   │       └── screens/            BaseScreen + one class per screen (11)
│       │   └── res/                        launcher icon, Material icons, Barlow fonts, strings, theme
│       └── test/.../InferenceEngineTest.java   locks in the demo numbers and fleet consistency
├── docs/
│   ├── ARCHITECTURE.md                     architecture, components, AI inference logic, dataset
│   ├── FACULTY_GUIDE.md                    demo workflow, 2- and 5-minute talks, viva Q&A, roadmap
│   └── sample-data/                        CSV exports of the simulated telemetry
└── .github/
    ├── workflows/android.yml               CI: tests, lint, APK build, emulator run of every screen
    └── scripts/ui-check.sh                 screenshot and demo-recording script
```

---

## 4. Documentation

- [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md): project architecture, each major component, the AI inference logic with formulas, and the sample dataset.
- [docs/FACULTY_GUIDE.md](docs/FACULTY_GUIDE.md): demonstration workflow, a 2-minute and a 5-minute explanation, likely viva questions with answers, and the future roadmap.

## 5. Tech stack

Java 17 language level · Android SDK only (framework views, `Canvas`, `ValueAnimator`, `Handler`, `Notification`) · Android Gradle Plugin 8.5 · min SDK 26 (Android 8.0), target SDK 34 · JUnit 4 for unit tests. No Kotlin, no Jetpack Compose, no AndroidX, no chart library, no network access, no backend: every dial and chart is drawn with `android.graphics.Canvas`.

Typeface: Barlow by Jeremy Tribby, SIL Open Font License (see `docs/FONT_LICENSE_OFL.txt`). Icons: Material Icons by Google, Apache License 2.0.
