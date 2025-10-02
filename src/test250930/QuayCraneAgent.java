package test250930;

import java.util.ArrayDeque;
import java.util.Map;
import java.util.Queue;

import repast.simphony.engine.schedule.ScheduledMethod;
import test250930.logging.SimLogger;

public class QuayCraneAgent {

    private final int id;
    private final YardAgent yard;
    private final Queue<CraneTask> taskQueue = new ArrayDeque<>();
    private CraneTask currentTask;

    private static final Map<MaterialType, Integer> HANDLING_TIMES = Map.of(
            MaterialType.BLADE, 2,
            MaterialType.NACELLE, 3,
            MaterialType.TOWER, 2,
            MaterialType.FOUNDATION, 4
    );

    public QuayCraneAgent(int id, YardAgent yard) {
        this.id = id;
        this.yard = yard;
    }

    public void enqueueUnload(VesselAgent vessel, Material material) {
        int handling = HANDLING_TIMES.getOrDefault(material.getType(), 2);
        taskQueue.offer(new CraneTask(vessel, material, handling));
        SimLogger.event("Crane " + id, "queued unloading",
                material.getId() + " from vessel " + vessel.getId());
    }

    @ScheduledMethod(start = 1, interval = 1)
    public void step() {
        long currentTick = SimLogger.currentTick();
        if (currentTask == null && !taskQueue.isEmpty()) {
            currentTask = taskQueue.poll();
            SimLogger.event(currentTick, "Crane " + id, "started unloading",
                    currentTask.getMaterial().getId() + " from vessel " + currentTask.getVessel().getId());
        }
        if (currentTask != null) {
            currentTask.workOneTick();
            if (currentTask.isComplete()) {
                Material material = currentTask.getMaterial();
                material.setState(MaterialState.ON_QUAY);
                yard.receiveMaterial(material, "crane " + id);
                currentTask.getVessel().notifyMaterialUnloaded(material);
                SimLogger.event(currentTick, "Crane " + id, "completed unloading", material.getId());
                currentTask = null;
            }
        }
    }
}
