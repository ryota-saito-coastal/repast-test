package test250930;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.schedule.ScheduledMethod;
import test250930.logging.SimLogger;

public class VesselAgent {

    private final int id;
    private final int arrivalTick;
    private final QuayCraneAgent crane;
    private final Queue<Material> cargoQueue = new ArrayDeque<>();
    private final List<Material> unloading = new ArrayList<>();
    private VesselStatus status = VesselStatus.AT_SEA;

    public VesselAgent(int id, int arrivalTick, List<Material> cargo, QuayCraneAgent crane) {
        this.id = id;
        this.arrivalTick = arrivalTick;
        this.crane = crane;
        cargoQueue.addAll(cargo);
    }

    public int getId() {
        return id;
    }

    @ScheduledMethod(start = 1, interval = 1)
    public void step() {
        int currentTick = (int) Math.floor(RunEnvironment.getInstance().getCurrentSchedule().getTickCount());
        if (status == VesselStatus.AT_SEA && currentTick >= arrivalTick) {
            status = VesselStatus.AT_QUAY;
            SimLogger.event(currentTick, "Vessel " + id, "arrived",
                    "cargo=" + cargoQueue.size());
        }
        if (status == VesselStatus.AT_QUAY || status == VesselStatus.UNLOADING) {
            requestNextUnload();
            if (cargoQueue.isEmpty() && unloading.isEmpty()) {
                if (status != VesselStatus.COMPLETED) {
                    status = VesselStatus.COMPLETED;
                    SimLogger.event(currentTick, "Vessel " + id, "ready to depart",
                            "all cargo unloaded");
                }
            }
        }
    }

    private void requestNextUnload() {
        Material next = cargoQueue.peek();
        if (next == null) {
            return;
        }
        if (next.getState() == MaterialState.ON_VESSEL) {
            next.setState(MaterialState.WAITING_FOR_CRANE);
            unloading.add(next);
            crane.enqueueUnload(this, next);
            status = VesselStatus.UNLOADING;
        }
    }

    public void notifyMaterialUnloaded(Material material) {
        if (!unloading.remove(material)) {
            return;
        }
        cargoQueue.poll();
        SimLogger.event("Vessel " + id, "material unloaded", material.getId());
        if (!cargoQueue.isEmpty()) {
            status = VesselStatus.AT_QUAY;
        }
    }
}
