package test250930;

import java.util.Collections;
import java.util.List;

/**
 * Scheduled arrival of a carrier bringing materials into the port.
 */
public class ArrivalEvent {

    private final int tick;
    private final CarrierType carrierType;
    private final List<MaterialManifestEntry> manifest;

    public ArrivalEvent(int tick, CarrierType carrierType, List<MaterialManifestEntry> manifest) {
        this.tick = tick;
        this.carrierType = carrierType;
        this.manifest = List.copyOf(manifest);
    }

    public int getTick() {
        return tick;
    }

    public CarrierType getCarrierType() {
        return carrierType;
    }

    public List<MaterialManifestEntry> getManifest() {
        return Collections.unmodifiableList(manifest);
    }
}
