package test250930;

import java.util.ArrayDeque;
import java.util.Map;
import java.util.Queue;

import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.schedule.ScheduledMethod;

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
        int currentTick = (int) Math.floor(RunEnvironment.getInstance().getCurrentSchedule().getTickCount());
        SimLogger.event(currentTick, "Crane-" + id, "queue_unload",
                String.format("material=%s vessel=%d", material.getId(), vessel.getId()));
    }

    @ScheduledMethod(start = 1, interval = 1)
    public void step() {
        if (currentTask == null && !taskQueue.isEmpty()) {
            currentTask = taskQueue.poll();
            int currentTick = (int) Math.floor(RunEnvironment.getInstance().getCurrentSchedule().getTickCount());
            SimLogger.event(currentTick, "Crane-" + id, "start_unload",
                    String.format("material=%s vessel=%d", currentTask.getMaterial().getId(), currentTask.getVessel().getId()));
        }
        if (currentTask != null) {
            currentTask.workOneTick();
            if (currentTask.isComplete()) {
                Material material = currentTask.getMaterial();
                material.setState(MaterialState.ON_QUAY);
                yard.receiveMaterial(material, "crane " + id);
                currentTask.getVessel().notifyMaterialUnloaded(material);
                int currentTick = (int) Math.floor(RunEnvironment.getInstance().getCurrentSchedule().getTickCount());
                SimLogger.event(currentTick, "Crane-" + id, "complete_unload",
                        String.format("material=%s vessel=%d", material.getId(), currentTask.getVessel().getId()));
                currentTask = null;
            }
        }
    }
}
