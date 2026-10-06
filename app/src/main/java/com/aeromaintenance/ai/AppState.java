package com.aeromaintenance.ai;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.aeromaintenance.ai.data.Aircraft;
import com.aeromaintenance.ai.data.Alert;
import com.aeromaintenance.ai.data.DataCondition;
import com.aeromaintenance.ai.data.FleetRepository;
import com.aeromaintenance.ai.data.MaintenanceTask;
import com.aeromaintenance.ai.data.RiskLevel;
import com.aeromaintenance.ai.data.TaskStatus;
import com.aeromaintenance.ai.data.TelemetrySimulator;
import com.aeromaintenance.ai.data.TelemetryWindow;
import com.aeromaintenance.ai.engine.AlertFactory;
import com.aeromaintenance.ai.engine.AnalysisResult;
import com.aeromaintenance.ai.engine.ComponentAssessment;
import com.aeromaintenance.ai.engine.InferenceEngine;
import com.aeromaintenance.ai.engine.MaintenanceReport;
import com.aeromaintenance.ai.engine.PreprocessedWindow;
import com.aeromaintenance.ai.engine.ReportGenerator;
import com.aeromaintenance.ai.notify.AlertNotifier;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Single source of truth for the whole app (the "ViewModel" of the MVVM pattern).
 *
 * <p>Holds the navigation back stack, the selected aircraft, the normal/abnormal
 * choice per aircraft, the analysis progress, alerts, work orders, the report and
 * the guided-demo state machine. Screens read from it and call its methods; it
 * tells registered {@link Listener}s what changed (observer pattern). Timed steps
 * (the animated pipeline, the demo) run on the main thread with a {@link Handler}.</p>
 *
 * <p>It is a process-wide singleton, so it survives activity re-creation.</p>
 */
public final class AppState {

    /** What changed, so each screen can decide whether it needs to redraw. */
    public enum Change {
        NAVIGATION, SELECTION, ANALYSIS, ANALYSIS_PROGRESS, ALERTS, TASKS, REPORT,
        DEMO, DEMO_PROGRESS, MAINTENANCE_TAB
    }

    public interface Listener {
        void onStateChanged(Change change);
    }

    /** Short confirmation messages ("Work order WO-5513 scheduled…"), shown as a snackbar. */
    public interface MessageListener {
        void onMessage(String message);
    }

    public enum AnalysisPhase { IDLE, RUNNING, DONE }

    public enum ReportPhase { IDLE, GENERATING, READY }

    /** State of the guided demo. */
    public static final class Demo {
        public final int step;
        public final float progress;
        public final boolean paused;
        public final boolean finished;

        Demo(int step, float progress, boolean paused, boolean finished) {
            this.step = step;
            this.progress = progress;
            this.paused = paused;
            this.finished = finished;
        }
    }

    /** Number of pipeline stages the AI screen animates through (matches AnalysisResult.stages). */
    public static final int PIPELINE_STAGES = 9;

    public static final List<String> REPORT_STEPS = Collections.unmodifiableList(Arrays.asList(
            "Collecting telemetry evidence", "Running AI assessment", "Formatting report"));

    private static AppState instance;

    public static synchronized AppState get(Context context) {
        if (instance == null) instance = new AppState(context.getApplicationContext());
        return instance;
    }

    private final Context app;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<Listener> listeners = new ArrayList<>();
    private MessageListener messageListener;

    // ------------------------------------------------------------------ data
    private final Map<String, TelemetryWindow> telemetryCache = new HashMap<>();
    private final Map<String, PreprocessedWindow> preprocessCache = new HashMap<>();
    private final Map<String, AnalysisResult> resultCache = new HashMap<>();
    /** Latest assessment of every aircraft on its default data stream. */
    private final List<AnalysisResult> fleetPredictions = new ArrayList<>();

    // ------------------------------------------------------------ navigation
    private final List<Destination> backStack = new ArrayList<>();
    private int maintenanceTab = 0;

    // ------------------------------------------------- selection & data mode
    private String selectedId = DemoScript.AIRCRAFT;
    private final Map<String, DataCondition> conditions = new HashMap<>();

    // --------------------------------------------------------------- analysis
    private AnalysisPhase analysisPhase = AnalysisPhase.IDLE;
    private String runningAircraft;
    private DataCondition runningCondition;
    private int runningStage;
    private AnalysisResult analysisResult;
    private String analysisCompletedAt;
    private int analysisToken;

    // ----------------------------------------------------------------- alerts
    private final List<Alert> alerts = new ArrayList<>();
    private String expandedAlertId;
    private int alertSeq = 2042;

    // ------------------------------------------------------------ maintenance
    private final List<MaintenanceTask> tasks = new ArrayList<>();
    private String highlightedTaskId;
    private int taskSeq = 5513;

    // ---------------------------------------------------------------- reports
    private ReportPhase reportPhase = ReportPhase.IDLE;
    private int reportStep;
    private MaintenanceReport report;
    private int reportSeq = 1;
    private int reportToken;

    // ------------------------------------------------------------------- demo
    private Demo demo;
    private String demoFocus;
    private int demoToken;
    private long demoElapsed;

    private AppState(Context app) {
        this.app = app;
        backStack.add(Destination.of(Screen.DASHBOARD));
        for (Aircraft a : FleetRepository.AIRCRAFT) {
            fleetPredictions.add(resultFor(a.id, FleetRepository.defaultCondition(a)));
        }
        seedAlerts();
        seedTasks();
    }

    /** Alerts for every aircraft whose latest assessment is not LOW risk. */
    private void seedAlerts() {
        LocalDateTime now = LocalDateTime.now();
        long[] ages = {48L, 192L, 425L, 1560L};
        List<AnalysisResult> flagged = new ArrayList<>();
        for (AnalysisResult r : fleetPredictions) {
            if (r.risk != RiskLevel.LOW) flagged.add(r);
        }
        Collections.sort(flagged, new Comparator<AnalysisResult>() {
            @Override
            public int compare(AnalysisResult a, AnalysisResult b) {
                if (a.risk.rank != b.risk.rank) return Integer.compare(b.risk.rank, a.risk.rank);
                return Integer.compare(a.primary.rulHours, b.primary.rulHours);
            }
        });
        for (int i = 0; i < flagged.size(); i++) {
            long age = i < ages.length ? ages[i] : 2000L + i * 60L;
            String ts = FleetRepository.formatTimestamp(now.minusMinutes(age));
            Alert a = AlertFactory.fromResult(flagged.get(i), "ALT-" + (2041 - i * 3), ts, false);
            if (a != null) alerts.add(a);
        }
    }

    /** Work orders: refresh AI-predicted due hours from the engine so every screen agrees. */
    private void seedTasks() {
        Map<String, AnalysisResult> byAircraft = new HashMap<>();
        for (AnalysisResult r : fleetPredictions) byAircraft.put(r.aircraftId, r);
        for (MaintenanceTask t : FleetRepository.initialTasks()) {
            AnalysisResult r = byAircraft.get(t.aircraftId);
            boolean matches = r != null && t.createdByAi && r.risk != RiskLevel.LOW
                    && AlertFactory.systemName(r.primary.component).equals(t.component);
            tasks.add(matches ? t.withDueHours(r.primary.rulHours) : t);
        }
    }

    // =====================================================================
    // Observers
    // =====================================================================

    public void addListener(Listener l) {
        if (!listeners.contains(l)) listeners.add(l);
    }

    public void removeListener(Listener l) {
        listeners.remove(l);
    }

    public void setMessageListener(MessageListener l) {
        messageListener = l;
    }

    private void notifyChange(Change change) {
        for (Listener l : new ArrayList<>(listeners)) l.onStateChanged(change);
    }

    private void message(String text) {
        if (messageListener != null) messageListener.onMessage(text);
    }

    private static String now() {
        return FleetRepository.formatTimestamp(LocalDateTime.now());
    }

    // =====================================================================
    // Data (deterministic, so cached)
    // =====================================================================

    private static String key(String id, DataCondition c) {
        return id + "|" + c.name();
    }

    public List<Aircraft> fleet() {
        return FleetRepository.AIRCRAFT;
    }

    public Aircraft aircraft(String id) {
        return FleetRepository.aircraftById(id);
    }

    public TelemetryWindow telemetry(String id, DataCondition c) {
        String k = key(id, c);
        TelemetryWindow w = telemetryCache.get(k);
        if (w == null) {
            w = TelemetrySimulator.generate(aircraft(id), c);
            telemetryCache.put(k, w);
        }
        return w;
    }

    public PreprocessedWindow preprocessed(String id, DataCondition c) {
        String k = key(id, c);
        PreprocessedWindow p = preprocessCache.get(k);
        if (p == null) {
            p = InferenceEngine.preprocess(telemetry(id, c));
            preprocessCache.put(k, p);
        }
        return p;
    }

    /** Engine output for an aircraft and data set. */
    public AnalysisResult resultFor(String id, DataCondition c) {
        String k = key(id, c);
        AnalysisResult r = resultCache.get(k);
        if (r == null) {
            r = InferenceEngine.analyze(telemetry(id, c));
            resultCache.put(k, r);
        }
        return r;
    }

    public List<AnalysisResult> fleetPredictions() {
        return Collections.unmodifiableList(fleetPredictions);
    }

    // =====================================================================
    // Navigation
    // =====================================================================

    public Destination current() {
        return backStack.get(backStack.size() - 1);
    }

    public boolean canGoBack() {
        return backStack.size() > 1;
    }

    public void navigate(Screen screen) {
        navigate(Destination.of(screen));
    }

    public void navigate(Destination d) {
        Destination before = current();
        if (d.screen.isTab) {
            backStack.clear();
            backStack.add(Destination.of(Screen.DASHBOARD));
            if (d.screen != Screen.DASHBOARD) backStack.add(d);
        } else {
            if (before.equals(d)) return;
            backStack.add(d);
        }
        if (!current().equals(before)) notifyChange(Change.NAVIGATION);
    }

    public boolean back() {
        if (backStack.size() <= 1) return false;
        backStack.remove(backStack.size() - 1);
        notifyChange(Change.NAVIGATION);
        return true;
    }

    public int maintenanceTab() {
        return maintenanceTab;
    }

    public void setMaintenanceTab(int tab) {
        if (tab == maintenanceTab) return;
        maintenanceTab = tab;
        notifyChange(Change.MAINTENANCE_TAB);
    }

    public void openMaintenance(int tab) {
        boolean changed = tab != maintenanceTab;
        maintenanceTab = tab;
        Destination before = current();
        navigate(Screen.MAINTENANCE);
        if (changed && before.screen == Screen.MAINTENANCE) notifyChange(Change.MAINTENANCE_TAB);
    }

    // =====================================================================
    // Selection and data mode
    // =====================================================================

    public String selectedId() {
        return selectedId;
    }

    public DataCondition conditionOf(String id) {
        DataCondition c = conditions.get(id);
        return c != null ? c : FleetRepository.defaultCondition(aircraft(id));
    }

    public void selectAircraft(String id) {
        if (id.equals(selectedId)) return;
        selectedId = id;
        resetAnalysisIfStale();
        resetReport();
        notifyChange(Change.SELECTION);
    }

    public void setCondition(String id, DataCondition c) {
        conditions.put(id, c);
        resetAnalysisIfStale();
        resetReport();
        notifyChange(Change.SELECTION);
    }

    private void resetAnalysisIfStale() {
        boolean stale;
        switch (analysisPhase) {
            case DONE:
                stale = !analysisResult.aircraftId.equals(selectedId) || analysisResult.condition != conditionOf(selectedId);
                break;
            case RUNNING:
                stale = !runningAircraft.equals(selectedId) || runningCondition != conditionOf(selectedId);
                break;
            default:
                stale = false;
        }
        if (stale) {
            analysisToken++;
            analysisPhase = AnalysisPhase.IDLE;
            analysisResult = null;
        }
    }

    // =====================================================================
    // AI analysis (animated pipeline)
    // =====================================================================

    public AnalysisPhase analysisPhase() {
        return analysisPhase;
    }

    /** Pipeline stage currently running (0..9) while the phase is RUNNING. */
    public int runningStage() {
        return runningStage;
    }

    public AnalysisResult analysisResult() {
        return analysisResult;
    }

    public String analysisCompletedAt() {
        return analysisCompletedAt;
    }

    /** Changes every time an analysis starts; used to replay the result animations once per run. */
    public int analysisRun() {
        return analysisToken;
    }

    public void runAnalysis() {
        runAnalysis(null);
    }

    /**
     * Runs the selected aircraft's telemetry through the pipeline. The result is
     * computed at once (it takes milliseconds); the stages are revealed one by one
     * so the audience can follow the workflow.
     */
    public void runAnalysis(final Runnable onDone) {
        final int token = ++analysisToken;
        final String id = selectedId;
        final DataCondition c = conditionOf(id);
        final AnalysisResult result = resultFor(id, c);
        analysisPhase = AnalysisPhase.RUNNING;
        runningAircraft = id;
        runningCondition = c;
        runningStage = 0;
        analysisResult = null;
        notifyChange(Change.ANALYSIS);

        long t = 650;
        for (int stage = 1; stage <= PIPELINE_STAGES; stage++) {
            final int s = stage;
            handler.postDelayed(() -> {
                if (token != analysisToken) return;
                runningStage = s;
                notifyChange(Change.ANALYSIS_PROGRESS);
            }, t);
            t += stage == 4 ? 700 : 430;
        }
        handler.postDelayed(() -> {
            if (token != analysisToken) return;
            analysisPhase = AnalysisPhase.DONE;
            analysisResult = result;
            analysisCompletedAt = now();
            notifyChange(Change.ANALYSIS);
            if (onDone != null) onDone.run();
        }, t);
    }

    // =====================================================================
    // Alerts
    // =====================================================================

    public List<Alert> alerts() {
        return Collections.unmodifiableList(alerts);
    }

    public int unacknowledgedCount() {
        int n = 0;
        for (Alert a : alerts) {
            if (!a.acknowledged) n++;
        }
        return n;
    }

    public String expandedAlertId() {
        return expandedAlertId;
    }

    public void setExpandedAlert(String id) {
        expandedAlertId = id;
        notifyChange(Change.ALERTS);
    }

    /** Creates (or refreshes) the alert for an analysis result. Returns null for LOW risk. */
    public Alert createAlert(AnalysisResult result, boolean postNotification) {
        Alert alert = AlertFactory.fromResult(result, "ALT-" + (alertSeq++), now(), true);
        if (alert == null) return null;
        for (Iterator<Alert> it = alerts.iterator(); it.hasNext(); ) {
            Alert a = it.next();
            if (a.aircraftId.equals(alert.aircraftId) && a.component.equals(alert.component)) it.remove();
        }
        alerts.add(0, alert);
        expandedAlertId = alert.id;
        if (postNotification) AlertNotifier.post(app, alert);
        notifyChange(Change.ALERTS);
        message(alert.severity.label + " alert raised for " + alert.aircraftId);
        return alert;
    }

    public void acknowledge(String alertId) {
        for (int i = 0; i < alerts.size(); i++) {
            if (alerts.get(i).id.equals(alertId)) {
                alerts.set(i, alerts.get(i).acknowledge());
                notifyChange(Change.ALERTS);
                return;
            }
        }
    }

    // =====================================================================
    // Maintenance work orders
    // =====================================================================

    public List<MaintenanceTask> tasks() {
        return Collections.unmodifiableList(tasks);
    }

    public String highlightedTaskId() {
        return highlightedTaskId;
    }

    /** Creates a work order from the AI result, or refreshes the open one for the same system. */
    public MaintenanceTask scheduleMaintenance(AnalysisResult result) {
        if (result.risk == RiskLevel.LOW) return null;
        ComponentAssessment p = result.primary;
        String system = AlertFactory.systemName(p.component);
        String at = now();
        int existing = -1;
        for (int i = 0; i < tasks.size(); i++) {
            MaintenanceTask t = tasks.get(i);
            if (t.aircraftId.equals(result.aircraftId) && t.component.equals(system) && t.status != TaskStatus.COMPLETED) {
                existing = i;
                break;
            }
        }
        MaintenanceTask task;
        if (existing >= 0) {
            task = tasks.remove(existing).refreshedByAi(p.rulHours, p.workPackage, at);
        } else {
            task = new MaintenanceTask("WO-" + (taskSeq++), result.aircraftId, AlertFactory.workOrderTitle(p.component),
                    system, p.rulHours, TaskStatus.SCHEDULED, "AI prediction", p.workPackage, true, at);
        }
        tasks.add(0, task);
        highlightedTaskId = task.id;
        notifyChange(Change.TASKS);
        message("Work order " + task.id + " scheduled: due in " + InferenceEngine.rulText(task.dueHours));
        return task;
    }

    /** Scheduled → In progress → Completed. */
    public void advanceTask(String taskId) {
        for (int i = 0; i < tasks.size(); i++) {
            MaintenanceTask t = tasks.get(i);
            if (!t.id.equals(taskId)) continue;
            TaskStatus next;
            if (t.status == TaskStatus.SCHEDULED) next = TaskStatus.IN_PROGRESS;
            else if (t.status == TaskStatus.IN_PROGRESS) next = TaskStatus.COMPLETED;
            else return;
            tasks.set(i, t.withStatus(next));
            notifyChange(Change.TASKS);
            message(t.id + " is now " + next.label.toLowerCase(Locale.ROOT));
            return;
        }
    }

    // =====================================================================
    // Reports
    // =====================================================================

    public ReportPhase reportPhase() {
        return reportPhase;
    }

    public int reportStep() {
        return reportStep;
    }

    public MaintenanceReport report() {
        return report;
    }

    private void resetReport() {
        reportToken++;
        reportPhase = ReportPhase.IDLE;
        report = null;
    }

    public void generateReport() {
        final int token = ++reportToken;
        final String id = selectedId;
        final DataCondition c = conditionOf(id);
        reportPhase = ReportPhase.GENERATING;
        reportStep = 0;
        notifyChange(Change.REPORT);
        for (int s = 1; s <= REPORT_STEPS.size(); s++) {
            final int step = s;
            handler.postDelayed(() -> {
                if (token != reportToken) return;
                if (step < REPORT_STEPS.size()) {
                    reportStep = step;
                    notifyChange(Change.REPORT);
                    return;
                }
                AnalysisResult result = analysisPhase == AnalysisPhase.DONE && analysisResult.aircraftId.equals(id)
                        && analysisResult.condition == c ? analysisResult : resultFor(id, c);
                report = ReportGenerator.build(aircraft(id), result, now(), reportSeq++);
                reportPhase = ReportPhase.READY;
                notifyChange(Change.REPORT);
            }, 520L * s);
        }
    }

    // =====================================================================
    // Guided demo (START DEMO)
    // =====================================================================

    public Demo demo() {
        return demo;
    }

    /** Which part of the current screen the demo is pointing at (scroll target and highlight). */
    public String demoFocus() {
        return demoFocus;
    }

    /** The analysis result for the demo aircraft, once the demo has run it. */
    public AnalysisResult demoResult() {
        if (analysisPhase == AnalysisPhase.DONE && analysisResult != null
                && analysisResult.aircraftId.equals(DemoScript.AIRCRAFT)) {
            return analysisResult;
        }
        return null;
    }

    public void startDemo() {
        demo = null;
        runDemoStep(0);
    }

    public void demoNext() {
        if (demo == null || demo.finished) return;
        if (demo.step >= DemoScript.lastIndex()) finishDemo();
        else runDemoStep(demo.step + 1);
    }

    public void demoPrevious() {
        if (demo == null) return;
        int target = demo.finished ? DemoScript.lastIndex() : demo.step - 1;
        runDemoStep(Math.max(0, target));
    }

    public void demoTogglePause() {
        if (demo == null || demo.finished) return;
        demo = new Demo(demo.step, demo.progress, !demo.paused, false);
        notifyChange(Change.DEMO);
    }

    public void exitDemo() {
        demoToken++;
        demo = null;
        demoFocus = null;
        notifyChange(Change.DEMO);
    }

    private void finishDemo() {
        demoToken++;
        demo = new Demo(DemoScript.lastIndex(), 1f, false, true);
        demoFocus = null;
        notifyChange(Change.DEMO);
    }

    private void setDemoFocus(String focus) {
        demoFocus = focus;
        notifyChange(Change.DEMO);
    }

    private void runDemoStep(final int index) {
        final int token = ++demoToken;
        boolean paused = demo != null && demo.paused && !demo.finished;
        demo = new Demo(index, 0f, paused, false);
        notifyChange(Change.DEMO);
        performDemoAction(index, () -> {
            if (token != demoToken) return;
            demoElapsed = 0;
            tickDemo(token, index);
        });
    }

    private void tickDemo(final int token, final int index) {
        handler.postDelayed(() -> {
            if (token != demoToken || demo == null) return;
            long duration = DemoScript.STEPS.get(index).durationMs;
            if (!demo.paused) {
                demoElapsed += 50;
                demo = new Demo(index, Math.min(1f, demoElapsed / (float) duration), false, false);
                notifyChange(Change.DEMO_PROGRESS);
            }
            if (demoElapsed >= duration) {
                if (index >= DemoScript.lastIndex()) finishDemo();
                else runDemoStep(index + 1);
            } else {
                tickDemo(token, index);
            }
        }, 50);
    }

    private void performDemoAction(int index, final Runnable next) {
        final String id = DemoScript.AIRCRAFT;
        switch (index) {
            case 0:
                demoFocus = "components";
                selectAircraft(id);
                navigate(Screen.FLEET);
                navigate(Destination.detail(id));
                setDemoFocus("components");
                next.run();
                break;
            case 1:
                demoFocus = "tiles";
                selectAircraft(id);
                setCondition(id, DataCondition.NORMAL);
                navigate(Screen.MONITORING);
                setDemoFocus("tiles");
                next.run();
                break;
            case 2:
                demoFocus = "charts";
                navigate(Screen.MONITORING);
                setCondition(id, DataCondition.ABNORMAL);
                setDemoFocus("charts");
                next.run();
                break;
            case 3:
                demoFocus = "pipeline";
                selectAircraft(id);
                if (conditionOf(id) != DataCondition.ABNORMAL) setCondition(id, DataCondition.ABNORMAL);
                navigate(Screen.ANALYSIS);
                setDemoFocus("pipeline");
                runAnalysis(next);
                break;
            case 4:
            case 5:
            case 6:
            case 7: {
                final String focus = index == 4 ? "score" : index == 5 ? "risk" : index == 6 ? "rul" : "recommendation";
                navigate(Screen.ANALYSIS);
                Runnable show = () -> {
                    setDemoFocus(focus);
                    next.run();
                };
                if (demoResult() == null) {
                    selectAircraft(id);
                    if (conditionOf(id) != DataCondition.ABNORMAL) setCondition(id, DataCondition.ABNORMAL);
                    runAnalysis(show);
                } else {
                    show.run();
                }
                break;
            }
            case 8: {
                AnalysisResult r = demoResult() != null ? demoResult() : resultFor(id, DataCondition.ABNORMAL);
                demoFocus = "alert";
                createAlert(r, true);
                navigate(Screen.ALERTS);
                setDemoFocus("alert");
                next.run();
                break;
            }
            case 9:
            default: {
                AnalysisResult r = demoResult() != null ? demoResult() : resultFor(id, DataCondition.ABNORMAL);
                demoFocus = "task";
                scheduleMaintenance(r);
                openMaintenance(1);
                setDemoFocus("task");
                next.run();
                break;
            }
        }
    }

    // =====================================================================
    // Launch options (deep links, CI screenshots)
    // =====================================================================

    public void applyLaunchOptions(LaunchOptions o) {
        if (o.aircraft != null && FleetRepository.exists(o.aircraft)) selectAircraft(o.aircraft);
        if (o.condition != null) {
            String c = o.condition.toLowerCase(Locale.ROOT);
            if (c.equals("normal")) setCondition(selectedId, DataCondition.NORMAL);
            else if (c.equals("abnormal")) setCondition(selectedId, DataCondition.ABNORMAL);
        }
        if (o.tab != null) maintenanceTab = o.tab;
        Screen s = Screen.fromKey(o.screen);
        if (s == Screen.AIRCRAFT_DETAIL) {
            navigate(Screen.FLEET);
            navigate(Destination.detail(selectedId));
        } else if (s != null) {
            navigate(s);
            notifyChange(Change.MAINTENANCE_TAB);
        }
        if (o.runAnalysis) runAnalysis();
        if (o.generateReport) generateReport();
        if (o.startDemo) startDemo();
    }
}
