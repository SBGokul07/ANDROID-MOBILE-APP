package com.aeromaintenance.ai.data;

/** Components the AI assesses. Each is linked to one health indicator and a limit. */
public enum ComponentType {
    ENGINE_BEARING("Engine Bearing", "Engine"),
    HYDRAULIC_PUMP("Hydraulic Pump", "Hydraulics"),
    TURBINE("Turbine", "Engine hot section"),
    FUEL_PUMP("Fuel Pump", "Fuel system");

    public final String label;
    public final String system;

    ComponentType(String label, String system) {
        this.label = label;
        this.system = system;
    }
}
