package test250930;

import java.util.ArrayDeque;
import java.util.Queue;

import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.schedule.ScheduledMethod;

public class DockAgent {

    private final int id;
    private final YardAgent yard;
    private final Queue<Material> towQueue = new ArrayDeque<>();

    public DockAgent(int id, YardAgent yard) {
        this.id = id;
        this.yard = yard;
    }

    public void scheduleTow(Material foundation) {
        towQueue.offer(foundation);
        SimLogger.info(String.format("[Dock %d] Scheduled towing for %s", id, foundation.getId()));
        SimLogger.event(0, "Dock-" + id, "tow_scheduled", "material=" + foundation.getId());
    }

    @ScheduledMethod(start = 2, interval = 3)
    public void step() {
        if (towQueue.isEmpty()) {
            return;
        }
        Material foundation = towQueue.poll();
        foundation.setState(MaterialState.ON_QUAY);
        int currentTick = (int) Math.floor(RunEnvironment.getInstance().getCurrentSchedule().getTickCount());
        SimLogger.event(currentTick, "Dock-" + id, "towed_to_quay", "material=" + foundation.getId());
        yard.receiveMaterial(foundation, "dock " + id);
    }
}
