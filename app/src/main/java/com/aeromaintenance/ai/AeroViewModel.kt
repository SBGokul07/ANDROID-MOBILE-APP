package com.aeromaintenance.ai

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aeromaintenance.ai.data.Aircraft
import com.aeromaintenance.ai.data.Alert
import com.aeromaintenance.ai.data.DataCondition
import com.aeromaintenance.ai.data.FleetRepository
import com.aeromaintenance.ai.data.MaintenanceTask
import com.aeromaintenance.ai.data.RiskLevel
import com.aeromaintenance.ai.data.TaskStatus
import com.aeromaintenance.ai.data.TelemetrySimulator
import com.aeromaintenance.ai.data.TelemetryWindow
import com.aeromaintenance.ai.engine.AlertFactory
import com.aeromaintenance.ai.engine.AnalysisResult
import com.aeromaintenance.ai.engine.InferenceEngine
import com.aeromaintenance.ai.engine.MaintenanceReport
import com.aeromaintenance.ai.engine.PreprocessedWindow
import com.aeromaintenance.ai.engine.ReportGenerator
import com.aeromaintenance.ai.notify.AlertNotifier
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime

sealed interface AnalysisUi {
    data object Idle : AnalysisUi
    data class Running(val aircraftId: String, val condition: DataCondition, val stage: Int) : AnalysisUi
    data class Done(val result: AnalysisResult, val completedAt: String) : AnalysisUi
}

sealed interface ReportUi {
    data object Idle : ReportUi
    data class Generating(val step: Int) : ReportUi
    data class Ready(val report: MaintenanceReport) : ReportUi
}

data class DemoUi(
    val step: Int,
    val progress: Float,
    val paused: Boolean,
    val finished: Boolean = false,
)

/** Launch options, used for deep links and for the automated screenshot run in CI. */
data class LaunchOptions(
    val screen: String? = null,
    val aircraft: String? = null,
    val condition: String? = null,
    val runAnalysis: Boolean = false,
    val startDemo: Boolean = false,
    val tab: Int? = null,
    val generateReport: Boolean = false,
)

class AeroViewModel(app: Application) : AndroidViewModel(app) {

    // ------------------------------------------------------------------ data
    val fleet: List<Aircraft> = FleetRepository.aircraft
    private val telemetryCache = HashMap<Pair<String, DataCondition>, TelemetryWindow>()
    private val preprocessCache = HashMap<Pair<String, DataCondition>, PreprocessedWindow>()
    private val resultCache = HashMap<Pair<String, DataCondition>, AnalysisResult>()

    fun aircraft(id: String): Aircraft = FleetRepository.aircraftById(id)

    fun telemetry(id: String, c: DataCondition): TelemetryWindow =
        telemetryCache.getOrPut(id to c) { TelemetrySimulator.generate(aircraft(id), c) }

    fun preprocessed(id: String, c: DataCondition): PreprocessedWindow =
        preprocessCache.getOrPut(id to c) { InferenceEngine.preprocess(telemetry(id, c)) }

    /** Engine output for an aircraft and data set. Deterministic, so it is cached. */
    fun resultFor(id: String, c: DataCondition): AnalysisResult =
        resultCache.getOrPut(id to c) { InferenceEngine.analyze(telemetry(id, c)) }

    /** Latest assessment of every aircraft on its default data stream. */
    val fleetPredictions: List<AnalysisResult> = fleet.map { resultFor(it.id, FleetRepository.defaultCondition(it)) }

    // ------------------------------------------------------------ navigation
    val backStack = mutableStateListOf<Screen>(Screen.Dashboard)
    val current: Screen get() = backStack.last()

    fun navigate(screen: Screen) {
        if (screen in Screen.tabs) {
            backStack.clear()
            backStack.add(Screen.Dashboard)
            if (screen != Screen.Dashboard) backStack.add(screen)
        } else {
            if (current == screen) return
            backStack.add(screen)
        }
    }

    fun back(): Boolean {
        if (backStack.size <= 1) return false
        backStack.removeAt(backStack.lastIndex)
        return true
    }

    var maintenanceTab by mutableIntStateOf(0)

    fun openMaintenance(tab: Int) {
        maintenanceTab = tab
        navigate(Screen.Maintenance)
    }

    // ------------------------------------------------- selection & data mode
    var selectedId by mutableStateOf(DemoScript.AIRCRAFT)
        private set
    private val conditions = mutableStateMapOf<String, DataCondition>()

    fun conditionOf(id: String): DataCondition = conditions[id] ?: FleetRepository.defaultCondition(aircraft(id))

    fun selectAircraft(id: String) {
        if (id == selectedId) return
        selectedId = id
        resetAnalysisIfStale()
        reportUi = ReportUi.Idle
    }

    fun setCondition(id: String, c: DataCondition) {
        conditions[id] = c
        resetAnalysisIfStale()
        reportUi = ReportUi.Idle
    }

    private fun resetAnalysisIfStale() {
        val a = analysisUi
        val stale = when (a) {
            is AnalysisUi.Done -> a.result.aircraftId != selectedId || a.result.condition != conditionOf(selectedId)
            is AnalysisUi.Running -> a.aircraftId != selectedId || a.condition != conditionOf(selectedId)
            AnalysisUi.Idle -> false
        }
        if (stale) {
            analysisJob?.cancel()
            analysisUi = AnalysisUi.Idle
        }
    }

    // --------------------------------------------------------------- analysis
    var analysisUi by mutableStateOf<AnalysisUi>(AnalysisUi.Idle)
        private set
    private var analysisJob: Job? = null

    /** Number of pipeline stages the UI animates through (matches AnalysisResult.stages). */
    val pipelineStageCount = 9

    fun runAnalysis(): Job {
        analysisJob?.cancel()
        val id = selectedId
        val c = conditionOf(id)
        val job = viewModelScope.launch {
            analysisUi = AnalysisUi.Running(id, c, 0)
            delay(650)
            val result = resultFor(id, c)
            for (stage in 1..pipelineStageCount) {
                analysisUi = AnalysisUi.Running(id, c, stage)
                delay(if (stage == 4) 700 else 430)
            }
            analysisUi = AnalysisUi.Done(result, FleetRepository.formatTimestamp(LocalDateTime.now()))
        }
        analysisJob = job
        return job
    }

    // ----------------------------------------------------------------- alerts
    val alerts = mutableStateListOf<Alert>()
    var expandedAlertId by mutableStateOf<String?>(null)
    private var alertSeq = 2042

    val unacknowledgedCount: Int get() = alerts.count { !it.acknowledged }

    /** Creates (or refreshes) the alert for an analysis result. Returns null for LOW risk. */
    fun createAlert(result: AnalysisResult, notify: Boolean = true): Alert? {
        val ts = FleetRepository.formatTimestamp(LocalDateTime.now())
        val alert = AlertFactory.fromResult(result, "ALT-${alertSeq++}", ts, isNew = true) ?: return null
        alerts.removeAll { it.aircraftId == alert.aircraftId && it.component == alert.component }
        alerts.add(0, alert)
        expandedAlertId = alert.id
        if (notify) AlertNotifier.post(getApplication(), alert)
        emit("${alert.severity.label} alert raised for ${alert.aircraftId}")
        return alert
    }

    fun acknowledge(alertId: String) {
        val i = alerts.indexOfFirst { it.id == alertId }
        if (i >= 0) alerts[i] = alerts[i].copy(acknowledged = true, isNew = false)
    }

    // ------------------------------------------------------------ maintenance
    val tasks = mutableStateListOf<MaintenanceTask>()
    var highlightedTaskId by mutableStateOf<String?>(null)
    private var taskSeq = 5513

    fun scheduleMaintenance(result: AnalysisResult): MaintenanceTask? {
        if (result.risk == RiskLevel.LOW) return null
        val p = result.primary
        val system = AlertFactory.systemName(p.component)
        val now = FleetRepository.formatTimestamp(LocalDateTime.now())
        val existing = tasks.indexOfFirst {
            it.aircraftId == result.aircraftId && it.component == system && it.status != TaskStatus.COMPLETED
        }
        val task: MaintenanceTask
        if (existing >= 0) {
            task = tasks[existing].copy(
                dueHours = p.rulHours,
                workPackage = p.workPackage,
                source = "AI prediction",
                createdByAi = true,
                updatedByAiAt = now,
            )
            tasks.removeAt(existing)
        } else {
            task = MaintenanceTask(
                id = "WO-${taskSeq++}",
                aircraftId = result.aircraftId,
                title = AlertFactory.workOrderTitle(p.component),
                component = system,
                dueHours = p.rulHours,
                status = TaskStatus.SCHEDULED,
                source = "AI prediction",
                workPackage = p.workPackage,
                createdByAi = true,
                updatedByAiAt = now,
            )
        }
        tasks.add(0, task)
        highlightedTaskId = task.id
        emit("Work order ${task.id} scheduled: due in ${InferenceEngine.rulText(task.dueHours)}")
        return task
    }

    fun advanceTask(taskId: String) {
        val i = tasks.indexOfFirst { it.id == taskId }
        if (i < 0) return
        val t = tasks[i]
        val next = when (t.status) {
            TaskStatus.SCHEDULED -> TaskStatus.IN_PROGRESS
            TaskStatus.IN_PROGRESS -> TaskStatus.COMPLETED
            TaskStatus.COMPLETED -> return
        }
        tasks[i] = t.copy(status = next)
        emit("${t.id} is now ${next.label.lowercase()}")
    }

    // ---------------------------------------------------------------- reports
    var reportUi by mutableStateOf<ReportUi>(ReportUi.Idle)
        private set
    private var reportSeq = 1
    private var reportJob: Job? = null
    val reportSteps = listOf("Collecting telemetry evidence", "Running AI assessment", "Formatting report")

    fun generateReport() {
        reportJob?.cancel()
        val id = selectedId
        val c = conditionOf(id)
        reportJob = viewModelScope.launch {
            for (s in reportSteps.indices) {
                reportUi = ReportUi.Generating(s)
                delay(520)
            }
            val done = analysisUi as? AnalysisUi.Done
            val result = if (done != null && done.result.aircraftId == id && done.result.condition == c) done.result else resultFor(id, c)
            val report = ReportGenerator.build(
                aircraft(id), result, FleetRepository.formatTimestamp(LocalDateTime.now()), reportSeq++,
            )
            reportUi = ReportUi.Ready(report)
        }
    }

    // ------------------------------------------------------------- messages
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val messages: SharedFlow<String> = _messages
    private fun emit(msg: String) { _messages.tryEmit(msg) }

    // ------------------------------------------------------------------ demo
    var demo by mutableStateOf<DemoUi?>(null)
        private set
    /** Which part of the current screen the demo is pointing at (scroll target + highlight). */
    var demoFocus by mutableStateOf<String?>(null)
        private set
    private var demoJob: Job? = null

    val demoResult: AnalysisResult?
        get() = (analysisUi as? AnalysisUi.Done)?.result?.takeIf { it.aircraftId == DemoScript.AIRCRAFT }

    fun startDemo() {
        runDemoStep(0)
    }

    fun demoNext() {
        val d = demo ?: return
        if (d.finished) return
        if (d.step >= DemoScript.steps.lastIndex) finishDemo() else runDemoStep(d.step + 1)
    }

    fun demoPrev() {
        val d = demo ?: return
        runDemoStep((if (d.finished) DemoScript.steps.lastIndex else d.step - 1).coerceAtLeast(0))
    }

    fun demoTogglePause() {
        val d = demo ?: return
        demo = d.copy(paused = !d.paused)
    }

    fun exitDemo() {
        demoJob?.cancel()
        demo = null
        demoFocus = null
    }

    private fun finishDemo() {
        demoJob?.cancel()
        demo = demo?.copy(step = DemoScript.steps.lastIndex, progress = 1f, finished = true)
        demoFocus = null
    }

    private fun runDemoStep(index: Int) {
        demoJob?.cancel()
        val step = DemoScript.steps[index]
        demo = DemoUi(step = index, progress = 0f, paused = demo?.paused ?: false)
        demoJob = viewModelScope.launch {
            performDemoAction(index)
            var elapsed = 0L
            while (elapsed < step.durationMs) {
                delay(50)
                if (demo?.paused != true) {
                    elapsed += 50
                    demo = demo?.copy(progress = elapsed / step.durationMs.toFloat())
                }
            }
            if (index >= DemoScript.steps.lastIndex) finishDemo() else runDemoStep(index + 1)
        }
    }

    private suspend fun performDemoAction(index: Int) {
        val id = DemoScript.AIRCRAFT
        when (index) {
            0 -> {
                selectAircraft(id)
                navigate(Screen.Fleet)
                navigate(Screen.AircraftDetail(id))
                demoFocus = "components"
            }
            1 -> {
                selectAircraft(id)
                setCondition(id, DataCondition.NORMAL)
                navigate(Screen.Monitoring)
                demoFocus = "tiles"
            }
            2 -> {
                navigate(Screen.Monitoring)
                setCondition(id, DataCondition.ABNORMAL)
                demoFocus = "charts"
            }
            3 -> {
                selectAircraft(id)
                if (conditionOf(id) != DataCondition.ABNORMAL) setCondition(id, DataCondition.ABNORMAL)
                navigate(Screen.Analysis)
                demoFocus = "pipeline"
                runAnalysis().join()
            }
            4, 5, 6, 7 -> {
                navigate(Screen.Analysis)
                if (demoResult == null) runAnalysis().join()
                demoFocus = when (index) {
                    4 -> "score"
                    5 -> "risk"
                    6 -> "rul"
                    else -> "recommendation"
                }
            }
            8 -> {
                val r = demoResult ?: resultFor(id, DataCondition.ABNORMAL)
                createAlert(r)
                navigate(Screen.Alerts)
                demoFocus = "alert"
            }
            9 -> {
                val r = demoResult ?: resultFor(id, DataCondition.ABNORMAL)
                scheduleMaintenance(r)
                openMaintenance(1)
                demoFocus = "task"
            }
        }
    }

    // ----------------------------------------------------------- launch opts
    fun applyLaunchOptions(o: LaunchOptions) {
        o.aircraft?.let { a -> if (fleet.any { it.id == a }) selectAircraft(a) }
        when (o.condition?.lowercase()) {
            "normal" -> setCondition(selectedId, DataCondition.NORMAL)
            "abnormal" -> setCondition(selectedId, DataCondition.ABNORMAL)
        }
        o.tab?.let { maintenanceTab = it }
        Screen.fromKey(o.screen)?.let { navigate(it) }
        if (o.screen == "detail") navigate(Screen.AircraftDetail(selectedId))
        if (o.runAnalysis) runAnalysis()
        if (o.generateReport) generateReport()
        if (o.startDemo) startDemo()
    }

    // ------------------------------------------------------------------ init
    init {
        // Alerts for every aircraft whose latest assessment is not LOW risk.
        val now = LocalDateTime.now()
        val ages = listOf(48L, 192L, 425L, 1560L)
        val flagged = fleetPredictions.filter { it.risk != RiskLevel.LOW }
            .sortedWith(compareByDescending<AnalysisResult> { it.risk.rank }.thenBy { it.primary.rulHours })
        flagged.forEachIndexed { i, r ->
            val ts = FleetRepository.formatTimestamp(now.minusMinutes(ages.getOrElse(i) { 2000L + i * 60 }))
            AlertFactory.fromResult(r, "ALT-${2041 - i * 3}", ts, isNew = false)?.let { alerts.add(it) }
        }

        // Work orders: refresh AI-predicted due hours from the engine.
        val byAircraft = fleetPredictions.associateBy { it.aircraftId }
        FleetRepository.initialTasks().forEach { t ->
            val r = byAircraft[t.aircraftId]
            val matches = r != null && t.createdByAi && r.risk != RiskLevel.LOW &&
                AlertFactory.systemName(r.primary.component) == t.component
            tasks.add(if (matches) t.copy(dueHours = r!!.primary.rulHours) else t)
        }
    }
}
