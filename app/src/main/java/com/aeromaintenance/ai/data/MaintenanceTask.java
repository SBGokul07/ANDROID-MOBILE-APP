package com.aeromaintenance.ai.data;

import java.util.Collections;
import java.util.List;

/** A work order in the maintenance plan. Immutable: "with" methods return a changed copy. */
public final class MaintenanceTask {
    public final String id;
    public final String aircraftId;
    public final String title;
    public final String component;
    public final int dueHours;
    public final TaskStatus status;
    public final String source;
    public final List<String> workPackage;
    public final boolean createdByAi;
    /** Set when the AI analysis created or refreshed this work order during the session. */
    public final String updatedByAiAt;

    public MaintenanceTask(String id, String aircraftId, String title, String component, int dueHours,
                           TaskStatus status, String source, List<String> workPackage,
                           boolean createdByAi, String updatedByAiAt) {
        this.id = id;
        this.aircraftId = aircraftId;
        this.title = title;
        this.component = component;
        this.dueHours = dueHours;
        this.status = status;
        this.source = source;
        this.workPackage = Collections.unmodifiableList(workPackage);
        this.createdByAi = createdByAi;
        this.updatedByAiAt = updatedByAiAt;
    }

    public MaintenanceTask withStatus(TaskStatus newStatus) {
        return new MaintenanceTask(id, aircraftId, title, component, dueHours, newStatus, source,
                workPackage, createdByAi, updatedByAiAt);
    }

    public MaintenanceTask withDueHours(int hours) {
        return new MaintenanceTask(id, aircraftId, title, component, hours, status, source,
                workPackage, createdByAi, updatedByAiAt);
    }

    /** Copy refreshed by a new AI prediction. */
    public MaintenanceTask refreshedByAi(int hours, List<String> newWorkPackage, String at) {
        return new MaintenanceTask(id, aircraftId, title, component, hours, status, "AI prediction",
                newWorkPackage, true, at);
    }
}
