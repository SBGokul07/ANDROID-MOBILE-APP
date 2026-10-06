package com.aeromaintenance.ai;

/** An entry in the navigation back stack: a screen plus, for the detail page, which aircraft. */
public final class Destination {
    public final Screen screen;
    /** Only used by {@link Screen#AIRCRAFT_DETAIL}. */
    public final String aircraftId;

    private Destination(Screen screen, String aircraftId) {
        this.screen = screen;
        this.aircraftId = aircraftId;
    }

    public static Destination of(Screen screen) {
        return new Destination(screen, null);
    }

    public static Destination detail(String aircraftId) {
        return new Destination(Screen.AIRCRAFT_DETAIL, aircraftId);
    }

    public String title() {
        return screen == Screen.AIRCRAFT_DETAIL ? aircraftId : screen.title;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Destination)) return false;
        Destination d = (Destination) o;
        return d.screen == screen && (aircraftId == null ? d.aircraftId == null : aircraftId.equals(d.aircraftId));
    }

    @Override
    public int hashCode() {
        return screen.hashCode() * 31 + (aircraftId == null ? 0 : aircraftId.hashCode());
    }
}
