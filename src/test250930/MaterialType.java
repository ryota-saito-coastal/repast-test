package test250930;

/**
 * Types of offshore wind turbine materials handled inside the base port.
 */
public enum MaterialType {

    BLADE(2.5),
    NACELLE(5.0),
    TOWER(3.5),
    FOUNDATION(7.5);

    private final double handlingEffort;

    MaterialType(double handlingEffort) {
        this.handlingEffort = handlingEffort;
    }

    /**
     * Estimated handling effort required for this material. Used by the crane to
     * approximate the time required to process a manifest.
     */
    public double getHandlingEffort() {
        return handlingEffort;
    }
}
