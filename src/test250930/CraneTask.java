package test250930;

import java.util.Collections;
import java.util.List;

/**
 * A task for the crane representing unloading a set of materials from a vessel to the yard.
 */
public class CraneTask {

    private final VesselAgent vessel;
    private final YardAgent yard;
    private final List<Material> materials;

    public CraneTask(VesselAgent vessel, YardAgent yard, List<Material> materials) {
        this.vessel = vessel;
        this.yard = yard;
        this.materials = List.copyOf(materials);
    }

    public VesselAgent getVessel() {
        return vessel;
    }

    public YardAgent getYard() {
        return yard;
    }

    public List<Material> getMaterials() {
        return Collections.unmodifiableList(materials);
    }

    public double calculateHandlingEffort() {
        return materials.stream().mapToDouble(material -> material.getType().getHandlingEffort()).sum();
    }
}
