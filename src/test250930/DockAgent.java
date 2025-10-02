package test250930;

import java.util.ArrayDeque;
import java.util.Queue;

import repast.simphony.engine.schedule.ScheduledMethod;
import test250930.logging.SimLogger;

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
        SimLogger.info("[Dock %d] Scheduled towing for %s", id, foundation.getId());
    }

    @ScheduledMethod(start = 2, interval = 3)
    public void step() {
        if (towQueue.isEmpty()) {
            return;
        }
        Material foundation = towQueue.poll();
        foundation.setState(MaterialState.ON_QUAY);
        long currentTick = SimLogger.currentTick();
        SimLogger.event(currentTick, "Dock " + id, "delivered to quay", foundation.getId());
        yard.receiveMaterial(foundation, "dock " + id);
    }
}
