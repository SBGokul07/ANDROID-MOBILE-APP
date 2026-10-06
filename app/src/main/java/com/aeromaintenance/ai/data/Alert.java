package com.aeromaintenance.ai.data;

/** A maintenance alert raised by the AI. Immutable. */
public final class Alert {
    public final String id;
    public final String timestamp;
    public final String aircraftId;
    public final String component;
    public final Severity severity;
    public final String title;
    public final String explanation;
    public final String recommendedAction;
    public final boolean acknowledged;
    /** Raised during this session (shown with a NEW tag). */
    public final boolean isNew;

    public Alert(String id, String timestamp, String aircraftId, String component, Severity severity,
                 String title, String explanation, String recommendedAction,
                 boolean acknowledged, boolean isNew) {
        this.id = id;
        this.timestamp = timestamp;
        this.aircraftId = aircraftId;
        this.component = component;
        this.severity = severity;
        this.title = title;
        this.explanation = explanation;
        this.recommendedAction = recommendedAction;
        this.acknowledged = acknowledged;
        this.isNew = isNew;
    }

    public Alert acknowledge() {
        return new Alert(id, timestamp, aircraftId, component, severity, title, explanation,
                recommendedAction, true, false);
    }
}
