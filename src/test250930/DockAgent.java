package test250930;

import java.util.ArrayDeque;
import java.util.Queue;

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
        System.out.printf("[Dock %d] Scheduled towing for %s%n", id, foundation.getId());
    }

    @ScheduledMethod(start = 2, interval = 3)
    public void step() {
        if (towQueue.isEmpty()) {
            return;
        }
        Material foundation = towQueue.poll();
        foundation.setState(MaterialState.ON_QUAY);
        System.out.printf("[Dock %d] Delivered %s to quay%n", id, foundation.getId());
        yard.receiveMaterial(foundation, "dock " + id);
    }
}
