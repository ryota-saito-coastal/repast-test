package test250930;

/**
 * Operational state of a vessel while it is within the base port simulation.
 */
public enum VesselStatus {
    APPROACHING,
    AT_QUAY,
    WAITING_FOR_CRANE,
    UNLOADING,
    READY_TO_DEPART,
    DEPARTED
}
