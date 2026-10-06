# Architecture and AI inference logic

This document covers the project architecture, what each major component does, the exact inference logic, and the sample dataset.

## 1. Layers

```
┌──────────────────────── UI layer (Jetpack Compose, Material 3) ────────────────────────┐
│  AeroApp: top bar · bottom navigation · drawer · demo narration panel                   │
│  Screens: Dashboard, Aircraft, Detail, Monitoring, AI prediction, Maintenance, Alerts,  │
│           Reports, AI insights, Faculty demo, About                                     │
│  Components: InstrumentDial, TrendChart, ProjectionChart, RulRiskChart, PipelineStepper │
└──────────────────────────────────────┬──────────────────────────────────────────────────┘
                                       │ reads state / calls actions
┌──────────────────────────────────────▼──────────────────────────────────────────────────┐
│  AeroViewModel: navigation back stack, selected aircraft, data condition per aircraft,  │
│  analysis state (Idle → Running(stage) → Done), alerts, work orders, report, demo steps │
└──────────────────────────────────────┬──────────────────────────────────────────────────┘
                                       │ calls (pure functions, deterministic)
┌──────────────────────────────────────▼──────────────────────────────────────────────────┐
│  engine/  InferenceEngine · Recommendations · AlertFactory · ReportGenerator            │
└──────────────────────────────────────┬──────────────────────────────────────────────────┘
                                       │ reads
┌──────────────────────────────────────▼──────────────────────────────────────────────────┐
│  data/    FleetRepository (12 aircraft, work orders) · TelemetrySimulator · Models      │
└─────────────────────────────────────────────────────────────────────────────────────────┘
Side effects: AlertNotifier → Android notifications; Reports → Android share sheet.
```

The `data` and `engine` packages have no Android imports. That is deliberate: the AI pipeline is plain Kotlin, unit-tested on the JVM, and could be moved to a server or swapped for a TensorFlow Lite model without touching the UI.

## 2. Major components

| Component | Responsibility |
|---|---|
| `MainActivity` | Starts the Compose UI, creates the notification channel, applies optional intent extras (`screen`, `aircraft`, `condition`, `run_analysis`, `demo`, `report`, `tab`). |
| `AeroViewModel` | Single source of truth. Holds the navigation back stack, selected aircraft, normal/abnormal choice per aircraft, analysis progress, alerts, work orders, report state and the guided-demo state machine. Caches telemetry and results (they are deterministic). At start-up it runs the engine on every aircraft to create the initial alerts and to set work-order due hours from the predicted RUL, so all screens agree. |
| `DemoScript` | The ten demo steps with their durations and narration. Narration uses the live result, so the numbers spoken match the numbers on screen. |
| `FleetRepository` | Twelve fictional aircraft (AERO-101 … AERO-112) with model, flight hours, cycles, health score, status, last maintenance, the fault each one develops in the abnormal data set, and the component-health records shown on the detail screen. |
| `TelemetrySimulator` | Generates 120 hourly samples for six channels. Normal data is noise around the fleet baseline; abnormal data injects the aircraft's fault signature with a realistic degradation profile. Seeded, so every run is identical. |
| `InferenceEngine` | The prototype AI pipeline (section 3). Returns an `AnalysisResult` with the anomaly score, risk, primary component, health, RUL, confidence, explanation, per-component assessments and a trace of every stage. |
| `Recommendations` | Rule base mapping (component, risk) to predicted condition, recommendation text, short action (Inspect / Monitor / Normal) and work package. |
| `AlertFactory` | Converts a result into an alert: HIGH → CRITICAL, MEDIUM with RUL under 150 h → WARNING, other MEDIUM → MONITOR, LOW → no alert. |
| `ReportGenerator` | Builds the maintenance report and its plain-text version for sharing. |
| `AlertNotifier` | Posts alerts as Android notifications (asks for permission on Android 13+). |
| `InstrumentDial` | Glass-cockpit style gauge used for fleet health, anomaly score and the six live sensor read-outs. |
| `TrendChart` | Sensor history with caution/warning bands, raw and smoothed traces coloured by zone, removed spikes, detected onset and a live point. |
| `ProjectionChart` | Health-indicator history plus the magenta RUL projection to the failure limit. |
| `RulRiskChart` | RUL per component against the HIGH (72 h) and MEDIUM (250 h) rules. |

## 3. AI inference logic (Prototype / Simulated AI Inference)

No trained neural network runs in the app. Each stage below is a transparent statistical method chosen to mirror what a production model does, so every number on screen can be traced. The constants live in `InferenceEngine.kt`.

### 3.1 Data acquisition
Six channels × 120 operating hours = 720 samples per analysis: engine temperature (EGT, °C), vibration (mm/s), oil pressure (PSI), core speed (RPM), fuel flow (kg/h) and hydraulic pressure (PSI).

### 3.2 Preprocessing
- **Hampel filter**: for each sample, take the median of the ±5 neighbouring samples and the scaled median absolute deviation (1.4826 × MAD). A sample more than 4.5 MAD from the median is a sensor spike and is replaced by the median. Two deliberate glitches are injected in every data set, so this stage always has work to do.
- **Smoothing**: 5-point centred moving average.
- **Hydraulic ripple**: √(mean of squared sample-to-sample differences ÷ 2) over a rolling 24 h window, then a 9-point moving average. For white noise this equals the noise standard deviation; it ignores slow changes in mean pressure, so it isolates pump pulsation.

### 3.3 Feature extraction (last 24 h, per channel, 18 features)
- Level: z = (mean − μ_baseline) / σ_baseline
- Trend: least-squares slope per hour and its R²
- Volatility: v = std(first differences) / (σ_baseline · √2), where 1.0 means normal fluctuation
- Channel deviation: Dc = √( z² + (2 · max(0, v − 1))² )

### 3.4 Time-series model (normal-behaviour model)
The fleet baseline (μ, σ per channel) plays the role of a model trained on healthy data. The overall deviation is a weighted root mean square:

D = √( Σ wc · Dc² / Σ wc ), with weights vibration 0.30, EGT 0.20, oil pressure 0.15, hydraulic pressure 0.15, RPM 0.10, fuel flow 0.10.

### 3.5 Anomaly detection
- Anomaly score: s = 1 / (1 + e^(−0.56 · (D − 4.65))), a logistic curve that is 0.5 at 4.65σ.
- Alert threshold 0.40 (≈ 3.9σ), high threshold 0.75.
- Onset: one-sided CUSUM on the primary component's indicator (reference = mean of the first 24 h, k = 0.5σ, h = 8σ). The onset is the last time the CUSUM was zero before it crossed h.

### 3.6 Component health
Each component has one health indicator, a nominal value and a limit:

| Component | Indicator | Nominal | Limit | Trend window |
|---|---|---|---|---|
| Engine Bearing | vibration | 2.1 mm/s | 7.0 mm/s | 24 h |
| Hydraulic Pump | pressure ripple | 6.5 PSI | 120 PSI | 48 h |
| Turbine | EGT | 690 °C | 800 °C | 72 h |
| Fuel Pump | fuel flow | 2,800 kg/h | 3,150 kg/h | 72 h |

The current value I is the linear trend evaluated at the latest sample. Margin used m = clamp((I − nominal) / (limit − nominal), 0, 1). Health = 100 · (1 − m^1.6), capped at 98 %.

Bearing wear accelerates, so its window is short; EGT and fuel flow drift slowly, so a longer window gives a steadier slope.

### 3.7 Failure-risk prediction
- Per component: HIGH if RUL < 72 h or health < 65 %; MEDIUM if RUL < 250 h or health < 85 %; otherwise LOW.
- The primary component is the one with the highest risk, then lowest health, then shortest RUL.
- Overall risk = the higher of the primary component's risk and the anomaly-score band.
- Which sensors evidence which component (used for the explanation bars): bearing = vibration 0.55, oil pressure 0.25, EGT 0.20; turbine = EGT 0.60, fuel flow 0.25, RPM 0.15; fuel pump = fuel flow 0.70, RPM 0.30; hydraulic pump = hydraulic pressure 1.0.

### 3.8 Remaining useful life
RUL = floor( (limit − I) / max(slope, minimum wear rate) ), capped at 999 h. It is rounded down because a remaining-life estimate should never be optimistic. The minimum wear rate stops a flat trend from giving an infinite RUL.

### 3.9 Confidence
Weighted average of three 0–1 factors: 0.48 × decision certainty (max(s, 1 − s)) + 0.30 × evidence (R² of the degradation trend, or closeness to baseline when risk is LOW) + 0.22 × data quality (share of samples that needed no spike correction).

### 3.10 Recommendation, alert, work order
The rule base turns (component, risk) into the predicted condition, recommendation and work package. AlertFactory raises the alert and the view model creates or refreshes the work order, with the due time equal to the RUL.

### 3.11 Worked example: AERO-101, abnormal data

| Stage | Result |
|---|---|
| Preprocessing | 7 sensor spikes removed |
| Deviation | D = 8.8σ (vibration alone is 13.8σ above baseline) |
| Anomaly score | 1 / (1 + e^(−0.56 × (8.8 − 4.65))) = **0.91** → above 0.75, strong anomaly |
| Bearing indicator | vibration 4.79 mm/s, slope +0.052 mm/s per hour, R² 0.95 |
| Health | m = (4.79 − 2.1) / (7.0 − 2.1) = 0.55 → 100 × (1 − 0.55^1.6) = **62 %** |
| RUL | (7.0 − 4.79) / 0.052 = 42.5 → **42 operating hours** |
| Risk | RUL < 72 h → **HIGH** |
| Confidence | 0.48 × 0.912 + 0.30 × 0.95 + 0.22 × 0.990 = **94.2 %** |
| Onset | CUSUM: about 73 operating hours ago |
| Recommendation | Schedule engine-bearing inspection during the next available maintenance window |

On normal data the same aircraft gives D = 0.2σ, anomaly score 0.08 and LOW risk.

## 4. Simulated data and sample dataset

`TelemetrySimulator` generates, for sample i of 120 (p = i / 119):

value = baseline + tail offset + small flight-phase cycle + fault shift(p) + noise(p) · ε,  ε ~ N(0, 1)

The fault shift follows d(p) = ((p − onset) / (1 − onset))^shape after the onset and 0 before it:

| Fault | Onset | Shape | Effect at full severity |
|---|---|---|---|
| Engine-bearing wear | 25 % | 2 (accelerating) | vibration +2.7 mm/s, EGT +34 °C, oil pressure −4 PSI, RPM oscillation, fuel +40 kg/h |
| Hydraulic-pump wear | 10 % | 1 (linear) | pressure +33 PSI with growing pulsation (±85 PSI) |
| Turbine EGT drift | 10 % | 1 | EGT +40 °C, fuel +35 kg/h |
| Fuel-pump degradation | 5 % | 1 | fuel flow +130 kg/h, extra RPM noise |

Every aircraft has a fault for its abnormal data set; flagged aircraft (101, 103, 105, 107) show it by default, healthy ones show normal data until you switch the toggle.

`docs/sample-data/` contains CSV exports produced by the same simulator:

| File | Content |
|---|---|
| `aero-101_normal.csv` | AERO-101, normal data |
| `aero-101_abnormal.csv` | AERO-101, bearing wear plus hydraulic-pump wear |
| `aero-103_normal.csv` | AERO-103, normal data |
| `aero-103_abnormal.csv` | AERO-103, hydraulic-pump wear |

Columns: `hour_offset` (−119 … 0, operating hours before now), `engine_temp_deg_C`, `vibration_mm_per_s`, `oil_pressure_PSI`, `rpm_rpm`, `fuel_flow_kg_per_h`, `hydraulic_pressure_PSI`. The raw values include the two injected sensor spikes (hour −78 vibration, hour −42 temperature).

## 5. Testing

- `InferenceEngineTest` (JVM): the AERO-101 demo numbers, LOW risk on normal data for every aircraft, agreement between the fleet record and the engine, determinism, alert severity and report content.
- CI (`.github/workflows/android.yml`): runs the tests, builds the debug and release APKs, then installs the release APK on an Android 14 emulator, opens every screen, takes screenshots, records the guided demo, and fails if the app crashes.
