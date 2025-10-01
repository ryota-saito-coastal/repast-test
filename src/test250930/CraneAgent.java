package test250930;

import java.util.ArrayDeque;
import java.util.Queue;

import repast.simphony.engine.schedule.ScheduledMethod;

/**
 * Crane that unloads vessels and stages materials into the yard.
 */
public class CraneAgent {

    private final int id;
    private final Position position;
    private final double radius;
    private final double handlingRate;
    private final PortEventGateway gateway;

    private final Queue<CraneTask> queue = new ArrayDeque<>();
    private CraneTask currentTask;
    private CraneStatus status = CraneStatus.IDLE;
    private int ticksRemaining;

    public CraneAgent(int id, Position position, double radius, double handlingRate, PortEventGateway gateway) {
        this.id = id;
        this.position = position;
        this.radius = radius;
        this.handlingRate = handlingRate;
        this.gateway = gateway;
    }

    public int getId() {
        return id;
    }

    public Position getPosition() {
        return position;
    }

    public synchronized void enqueueUnloadTask(CraneTask task) {
        queue.add(task);
        System.out.printf("[Crane %d] Enqueued unload task for Vessel %d containing %d materials%n",
                id, task.getVessel().getId(), task.getMaterials().size());
    }

    @ScheduledMethod(start = 1, interval = 1)
    public void step() {
        synchronized (this) {
            if (status == CraneStatus.IDLE && !queue.isEmpty()) {
                currentTask = queue.poll();
                double effort = currentTask.calculateHandlingEffort();
                ticksRemaining = Math.max(1, (int) Math.ceil(effort / handlingRate));
                status = CraneStatus.WORKING;
                System.out.printf("[Crane %d] Started processing task for Vessel %d, ETA %d ticks%n",
                        id, currentTask.getVessel().getId(), ticksRemaining);
            }
        }

        if (status == CraneStatus.WORKING) {
            ticksRemaining--;
            if (ticksRemaining <= 0) {
                completeCurrentTask();
            }
        }
    }

    private void completeCurrentTask() {
        CraneTask finishedTask;
        synchronized (this) {
            finishedTask = currentTask;
            currentTask = null;
            status = CraneStatus.IDLE;
        }
        if (finishedTask == null) {
            return;
        }
        for (Material material : finishedTask.getMaterials()) {
            finishedTask.getYard().store(material);
        }
        finishedTask.getVessel().notifyUnloadComplete(finishedTask.getMaterials());
        gateway.reportUnloaded(finishedTask.getVessel(), finishedTask.getYard(), finishedTask.getMaterials());
        System.out.printf("[Crane %d] Completed unload task for Vessel %d%n", id, finishedTask.getVessel().getId());
    }
}
