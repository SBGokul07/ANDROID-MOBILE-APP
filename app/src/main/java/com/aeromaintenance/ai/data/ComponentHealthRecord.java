package com.aeromaintenance.ai.data;

/** One row of the "Component health" card on the aircraft detail screen. */
public final class ComponentHealthRecord {
    public final String name;
    public final int health;
    public final String status;
    public final String note;

    public ComponentHealthRecord(String name, int health, String status, String note) {
        this.name = name;
        this.health = health;
        this.status = status;
        this.note = note;
    }
}
