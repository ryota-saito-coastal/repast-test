package test250930;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class VesselArrival {

    private final int vesselId;
    private final int arrivalTick;
    private final List<MaterialType> cargoTypes;

    public VesselArrival(int vesselId, int arrivalTick, List<MaterialType> cargoTypes) {
        this.vesselId = vesselId;
        this.arrivalTick = arrivalTick;
        this.cargoTypes = new ArrayList<>(cargoTypes);
    }

    public int getVesselId() {
        return vesselId;
    }

    public int getArrivalTick() {
        return arrivalTick;
    }

    public List<MaterialType> getCargoTypes() {
        return Collections.unmodifiableList(cargoTypes);
    }

    public List<Material> instantiateCargo(MaterialState initialState) {
        List<Material> cargo = new ArrayList<>();
        for (MaterialType type : cargoTypes) {
            cargo.add(Material.create(type, initialState));
        }
        return cargo;
    }
}
