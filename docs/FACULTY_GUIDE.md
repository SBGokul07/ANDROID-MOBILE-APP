# Faculty presentation guide

Everything you need to present AeroMaintenance AI: the demonstration workflow, a 2-minute and a 5-minute explanation, likely viva questions with answers, and the roadmap.

Before you start: install the release APK, open the app once, allow notifications, and keep the phone on *Do not disturb* except for this app. If you mirror the phone to a projector, use the release build; it animates smoothly.

---

## 1. Demonstration workflow (3–5 minutes)

**Option A, guided (recommended).** Dashboard → **Start demo**. The app drives itself through ten steps in about 70 seconds; tap **Pause** whenever you want to talk, **Next** to move on.

| Step | What the audience sees | What to say |
|---|---|---|
| 1. Select AERO-101 | Detail page: Engine 91 %, Hydraulic 78 %, Engine Bearing 62 %, Fuel 94 % | "This A320 has 8,421 flight hours. The bearing is already the weakest component." |
| 2. Sensor telemetry | Six live dials and trend charts on normal data, all green | "Six channels, one sample per operating hour, 120 hours of history." |
| 3. Abnormal trend | Charts redraw: vibration and temperature climb, oil pressure falls, lines turn amber then red, onset marker at about −73 h | "Here degradation starts. A fixed-interval schedule would not notice this yet." |
| 4. Run AI analysis | Nine pipeline stages tick through: acquisition, preprocessing, features, model, anomaly, health, risk, RUL, recommendation | "The data is cleaned, summarised into 18 features and compared with the learned normal behaviour." |
| 5. Anomaly score | Dial sweeps to **0.91** | "0 is normal, 1 is extremely abnormal. Our alert threshold is 0.40." |
| 6. Failure risk | **HIGH**, Engine Bearing, progressive degradation detected, confidence 94.2 % | "The pattern matches the bearing fault signature." |
| 7. RUL | **42 operating hours** with the magenta projection to the 7.0 mm/s limit | "At the current wear rate it reaches the vibration limit in 42 hours." |
| 8. Recommendation | "Schedule engine-bearing inspection during the next available maintenance window" and the work package | "The recommendation comes from a rule base an engineer can audit." |
| 9. Create alert | New CRITICAL alert at the top of Alerts, plus a phone notification | "The maintenance team is told, with the explanation attached." |
| 10. Maintenance action | Work order WO-5512 in the plan, due in 42 h | "It lands in the plan; an engineer reviews it and starts the work." |

When the demo ends, tap **Generate report** to show the formatted maintenance report, then **Share report** if asked.

**Option B, manual.** Dashboard → Aircraft → AERO-101 → *Sensor data* → toggle **Abnormal data** → **Run AI analysis** → scroll through the result → **Create alert** → **Schedule work** → Maintenance → Reports → **Generate maintenance report**. Then switch back to **Normal data** and run again to show the risk drop to LOW (score 0.08).

**Good questions to answer live:** open *AI insights* to show exactly what the prototype computes, and *Faculty demo mode* for problem, solution, benefits and future scope.

---

## 2. Two-minute explanation

> Aircraft components rarely fail without warning. Bearings vibrate more, turbines run hotter, pumps pulse. The problem is that maintenance is mostly scheduled by fixed intervals, so a part that wears faster than expected can be missed, and that means delays, downtime and operational risk.
>
> AeroMaintenance AI is a native Android prototype of AI-assisted predictive maintenance, written in Java. It takes aircraft telemetry, six sensor channels such as engine temperature, vibration and hydraulic pressure, and runs it through a pipeline: preprocessing to remove sensor glitches, feature extraction, a model of normal behaviour, anomaly detection, component health assessment, failure-risk prediction, and remaining-useful-life estimation. The result is a concrete recommendation, an alert on the engineer's phone, and a work order in the maintenance plan.
>
> In the demo, aircraft AERO-101 shows rising vibration. The app gives an anomaly score of 0.91, high risk on the engine bearing, and estimates 42 operating hours before vibration reaches its limit, so it recommends inspecting the bearing at the next maintenance window.
>
> To be clear about scope: this is an academic prototype. The data is simulated and the AI is a transparent statistical pipeline standing in for a trained neural network. The production path would train an LSTM or autoencoder on real fleet data, which the architecture is designed to accept.

---

## 3. Five-minute explanation

**1. Problem (30 s).** Unexpected component degradation causes unscheduled maintenance, aircraft-on-ground time, cancelled flights and safety risk. Preventive maintenance at fixed intervals replaces some parts too early and still misses parts that wear faster than average. Modern aircraft already record a lot of sensor data; the opportunity is to use it.

**2. Solution (30 s).** Predictive maintenance: watch the condition of each component continuously, detect abnormal behaviour early, estimate how long the component has left, and plan the maintenance before it fails. AeroMaintenance AI shows that whole workflow on a phone, because maintenance engineers work on the hangar floor, not at a desk.

**3. Data (40 s).** Twelve fictional aircraft. For each, six channels sampled every operating hour for 120 hours. The app generates two data sets per aircraft: *normal*, which is noise around the fleet baseline, and *abnormal*, where a fault is injected: for example progressive bearing wear raises vibration and temperature and lowers oil pressure, with the degradation accelerating over time. Two sensor glitches are always injected so preprocessing is visible. Everything is generated locally and is reproducible.

**4. AI pipeline (90 s).**
- *Preprocessing:* a Hampel filter replaces spikes using the median and median absolute deviation; a moving average smooths noise.
- *Features:* for each channel over the last 24 hours, its level relative to baseline, its trend (slope and R²) and its volatility. Eighteen numbers summarise 720 samples.
- *Normal-behaviour model:* the fleet baseline acts as the model of healthy behaviour; the weighted deviation from it is measured in standard deviations.
- *Anomaly detection:* a logistic function turns the deviation into a 0–1 anomaly score; a CUSUM change-point test finds when the degradation started.
- *Health and risk:* each component has a health indicator with a limit, for example bearing vibration with a 7.0 mm/s limit. Health is the share of margin left; risk follows clear rules.
- *RUL:* the indicator's trend is extrapolated to its limit, rounded down to stay conservative.
- *Recommendation:* a rule base maps component and risk to an action and a work package.

**5. Results (40 s).** AERO-101 abnormal: anomaly score 0.91, HIGH risk, Engine Bearing, 62 % health, RUL 42 operating hours, confidence 94.2 %. The same aircraft on normal data scores 0.08, LOW risk. The other flagged aircraft show a hydraulic-pump warning (108 h), a turbine EGT drift (249 h) and a fuel-pump drift (217 h). Unit tests lock these numbers so the demo is reproducible.

**6. Honest limitations (30 s).** The data is synthetic, the thresholds are prototype values, and there is no trained neural network. The UI labels this as *Prototype / Simulated AI Inference*. The value of the prototype is the end-to-end workflow and an architecture where a real model can be dropped in.

**7. Future work (20 s).** Train an LSTM, CNN-LSTM or autoencoder on a public benchmark such as NASA C-MAPSS, then on operator data; run it on the phone with TensorFlow Lite; stream real sensor data; add explainability, digital twins and integration with maintenance management systems.

---

## 4. Viva questions and answers

**Q1. What is predictive maintenance, and how is it different from preventive maintenance?**
Preventive maintenance acts on a fixed schedule (hours, cycles or calendar). Predictive maintenance acts on the measured condition of the component, so work is done when the data shows it is needed: earlier for a part that wears fast, later for one that is healthy.

**Q2. Is there a real neural network in your app?**
No. The UI names the role a neural network would play, and labels the implementation *Prototype / Simulated AI Inference*. Each stage is a transparent statistical method (Hampel filter, linear trends, a baseline deviation model, a logistic score, CUSUM, trend extrapolation). That keeps every number explainable, which suits a prototype without real training data.

**Q3. Where does the data come from?**
It is generated on the phone by `TelemetrySimulator`. Each aircraft has a normal data set and an abnormal one with an injected fault signature. The generator is seeded, so the same inputs always give the same outputs. Sample CSVs are in `docs/sample-data/`.

**Q4. How is the anomaly score calculated?**
For each channel we compute how far its recent level is from the baseline (z-score) and how much more it fluctuates than normal, combine them into a deviation, and take a weighted root mean square over the six channels. A logistic function maps that deviation to 0–1: 0.5 at 4.65σ, 0.91 at 8.8σ for AERO-101.

**Q5. How do you estimate remaining useful life?**
Each component has a health indicator and a limit. We fit a straight line to the indicator's recent trend and extrapolate to the limit. For the bearing: vibration 4.79 mm/s rising 0.052 mm/s per hour, limit 7.0 mm/s, so (7.0 − 4.79) / 0.052 = 42.5, rounded down to 42 hours.

**Q6. Why round RUL down?**
Over-estimating remaining life is the dangerous error. Rounding down keeps the estimate conservative.

**Q7. Why linear extrapolation when bearing wear accelerates?**
We only use the last 24 hours for the bearing, so the slope reflects the current, faster rate. Because the wear keeps accelerating, the true RUL is likely a little shorter, which is another reason to round down. A production model would learn the non-linear degradation curve (for example with an LSTM).

**Q8. How is confidence calculated?**
A weighted average of decision certainty (how far the anomaly score is from 0.5), evidence (R² of the degradation trend) and data quality (share of samples that needed no spike correction): 0.48 × 0.912 + 0.30 × 0.95 + 0.22 × 0.990 = 94.2 %.

**Q9. How does the app know which component is failing?**
A fault-signature matrix links sensors to components: bearing wear shows mainly in vibration, then oil pressure and temperature; pump wear shows in hydraulic pressure ripple; turbine wear in EGT; fuel-pump wear in fuel flow. Each component's own indicator is assessed and the one with the highest risk is reported.

**Q10. What does the preprocessing do, and why does it matter?**
Sensors produce glitches. A single spike could trigger a false alarm. The Hampel filter replaces any sample more than 4.5 scaled median absolute deviations from its local median. In the demo it removes the two injected glitches plus a few noise outliers.

**Q11. How would you build the real model?**
Start with a public run-to-failure benchmark such as NASA C-MAPSS. Train an LSTM or CNN-LSTM to predict RUL, and an autoencoder trained only on healthy data for anomaly detection. Validate on held-out engines using RMSE and an asymmetric scoring function that penalises late predictions. Then fine-tune on operator data with maintenance records as labels.

**Q12. Why would an autoencoder suit anomaly detection?**
It learns to reconstruct healthy data. When the input is abnormal, the reconstruction error rises. It needs no labelled failures, which are rare in aviation.

**Q13. What are the risk rules?**
HIGH if RUL is under 72 hours or health under 65 %; MEDIUM if RUL is under 250 hours or health under 85 %; otherwise LOW. Overall risk also considers the anomaly-score band.

**Q14. How do you avoid false alarms?**
Spike removal, smoothing, requiring a sustained trend rather than a single reading, an explicit alert threshold, and a human engineer reviewing every alert before acting. In production you would tune thresholds on historical data to balance false alarms against missed failures.

**Q15. Why Android and why on-device?**
Engineers work on the hangar floor. On-device inference works offline, responds instantly, and keeps data on the device. A production version could still sync with a central maintenance system.

**Q16. Why Java, and why no libraries?**
Java is a first-class Android language, widely taught, and every line of the app can be read and explained without learning a second language. The app uses only the Android SDK: standard views for layout and `android.graphics.Canvas` for the dials and charts. No chart library, no AndroidX, no Kotlin or Jetpack Compose (Compose only works with Kotlin). That keeps the APK small, the build simple, and nothing hidden in a third-party dependency.

**Q17. How is the app structured?**
MVVM with the observer pattern. One activity (`MainActivity`) hosts the shell and the current screen. Eleven screen classes extend `BaseScreen` and build their views from `AppState`, the single source of truth (navigation, selected aircraft, analysis, alerts, work orders, demo). When `AppState` changes it notifies the screen, which rebuilds. The `engine` package holds the AI pipeline and the `data` package the simulator; neither imports Android, so they are unit-tested on the JVM and could be replaced by a TensorFlow Lite model without touching the UI.

**Q18. How did you draw the charts and gauges without a library?**
Each is a custom `View` subclass that overrides `onDraw(Canvas)`. For example `DialView` draws three coloured range arcs with `drawArc`, the value arc, tick marks with `drawLine` and a needle at the angle of the value; a `ValueAnimator` moves the needle. `TrendChartView` maps each sample to x/y pixels, shades the caution and warning bands, draws the raw trace with a `Path` and the smoothed trace segment by segment in the colour of its zone.

**Q19. How do the animations and the timed demo work?**
`android.os.Handler.postDelayed` schedules each pipeline stage and each demo step on the main thread, and `ValueAnimator` animates needles and bars. A token number cancels stale timers when the user changes aircraft or skips a demo step.

**Q20. Can this be used on real aircraft?**
No. It is an academic prototype with simulated data and uncertified thresholds. Real use would need real data, validated models, regulatory approval and integration with approved maintenance procedures.

**Q21. What happens when you switch to normal data?**
The deviation drops to 0.2σ, the anomaly score to 0.08, risk to LOW, and the recommendation becomes "No action required. Continue routine condition monitoring." That contrast shows the model is reacting to the data, not to fixed values.

**Q22. What was the hardest part?**
Making the pipeline consistent end to end: the same result drives the dashboard, the detail page, the AI screen, the alert, the work order and the report. The app computes every screen from one engine run instead of storing numbers separately, and the unit tests check that the fleet record agrees with the engine.

**Q23. How did you test it?**
JVM unit tests on the engine, and a CI pipeline that builds the APK, installs it on an Android emulator, opens every screen, records the guided demo and fails if the app crashes.

**Q24. What is the role of CUSUM?**
Cumulative sum is a classic change-point method. It accumulates deviations above a small allowance and signals when the sum crosses a threshold. The point where it last left zero estimates when degradation began (about 73 hours ago for AERO-101).

---

## 5. Future development roadmap

| Phase | Work |
|---|---|
| 1. Real model | Train LSTM / CNN-LSTM RUL models and an autoencoder anomaly detector on NASA C-MAPSS; compare with the current statistical baseline |
| 2. On-device AI | Export to TensorFlow Lite and run inference on the phone behind the same `InferenceEngine` interface |
| 3. Real data | Ingest real or recorded sensor data (CSV upload first, then an IoT gateway); real-time streaming |
| 4. Explainable AI | SHAP or attention maps to show which sensor and time window drove each prediction |
| 5. Digital twin | Per-aircraft model that simulates "what if we defer this inspection by 50 hours" |
| 6. Federated learning | Learn across operators without sharing raw data |
| 7. Integration | Push work orders to a maintenance management system, sign-off workflow, audit trail, user roles |
| 8. Validation | Accuracy, false-alarm and missed-detection rates on historical failures; engineering and regulatory review |
