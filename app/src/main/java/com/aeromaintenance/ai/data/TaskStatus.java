package com.aeromaintenance.ai.data;

/** Work-order status on the maintenance planning screen. */
public enum TaskStatus {
    SCHEDULED("Scheduled"),
    IN_PROGRESS("In progress"),
    COMPLETED("Completed");

    public final String label;

    TaskStatus(String label) {
        this.label = label;
    }
}
