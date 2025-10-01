package test250930;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import repast.simphony.engine.schedule.ScheduledMethod;

/**
 * Vessel or towed structure that enters the port bringing turbine components.
 */
public class VesselAgent {

    private final int id;
    private final CarrierType carrierType;
    private final Position position;
    private final CraneAgent crane;
    private final YardAgent yard;

    private final List<Material> cargo = new ArrayList<>();
    private VesselStatus status = VesselStatus.APPROACHING;
    private boolean unloadTaskQueued;

    public VesselAgent(int id, CarrierType carrierType, Position position, CraneAgent crane, YardAgent yard,
            List<Material> manifest) {
        this.id = id;
        this.carrierType = carrierType;
        this.position = position;
        this.crane = crane;
        this.yard = yard;
        for (Material material : manifest) {
            material.assignTo(this, MaterialState.INBOUND);
            cargo.add(material);
        }
    }

    public int getId() {
        return id;
    }

    public CarrierType getCarrierType() {
        return carrierType;
    }

    public Position getPosition() {
        return position;
    }

    public List<Material> getCargo() {
        return Collections.unmodifiableList(cargo);
    }

    public VesselStatus getStatus() {
        return status;
    }

    @ScheduledMethod(start = 1, interval = 1)
    public void step() {
        switch (status) {
            case APPROACHING:
                status = VesselStatus.AT_QUAY;
                System.out.printf("[Vessel %d] Arrived at quay (%s) as %s%n", id, position, carrierType);
                break;
            case AT_QUAY:
                status = VesselStatus.WAITING_FOR_CRANE;
                break;
            case WAITING_FOR_CRANE:
                if (!unloadTaskQueued && !cargo.isEmpty()) {
                    crane.enqueueUnloadTask(new CraneTask(this, yard, new ArrayList<>(cargo)));
                    unloadTaskQueued = true;
                    status = VesselStatus.UNLOADING;
                }
                break;
            case READY_TO_DEPART:
                status = VesselStatus.DEPARTED;
                System.out.printf("[Vessel %d] Departed after unloading.%n", id);
                break;
            default:
                break;
        }
    }

    public void notifyUnloadComplete(List<Material> unloaded) {
        cargo.removeAll(unloaded);
        if (cargo.isEmpty()) {
            status = VesselStatus.READY_TO_DEPART;
        }
    }

    public boolean isDeparted() {
        return status == VesselStatus.DEPARTED;
    }
}
