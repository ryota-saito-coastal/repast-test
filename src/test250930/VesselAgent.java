package test250930;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.schedule.ScheduledMethod;

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
            System.out.printf("[Vessel %d] Arrived at tick %d with %d cargo items%n", id, currentTick, cargoQueue.size());
        }
        if (status == VesselStatus.AT_QUAY || status == VesselStatus.UNLOADING) {
            requestNextUnload();
            if (cargoQueue.isEmpty() && unloading.isEmpty()) {
                if (status != VesselStatus.COMPLETED) {
                    status = VesselStatus.COMPLETED;
                    System.out.printf("[Vessel %d] Completed unloading and ready to depart%n", id);
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
        if (!cargoQueue.isEmpty()) {
            status = VesselStatus.AT_QUAY;
        }
    }
}
