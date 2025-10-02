package test250930;

import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Queue;

import repast.simphony.context.Context;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.schedule.ScheduledMethod;

public class PortArrivalScheduler {

    private final Context<Object> context;
    private final QuayCraneAgent crane;
    private final Queue<VesselArrival> pendingArrivals = new PriorityQueue<>(Comparator.comparingInt(VesselArrival::getArrivalTick));

    public PortArrivalScheduler(Context<Object> context, QuayCraneAgent crane) {
        this.context = context;
        this.crane = crane;
    }

    public void registerArrival(VesselArrival arrival) {
        pendingArrivals.offer(arrival);
        SimLogger.info(String.format("[ArrivalScheduler] Queued Vessel %d for tick %d",
                arrival.getVesselId(), arrival.getArrivalTick()));
        SimLogger.event(0, "ArrivalScheduler",
                "arrival_registered",
                String.format("vessel=%d arrivalTick=%d", arrival.getVesselId(), arrival.getArrivalTick()));
    }

    @ScheduledMethod(start = 1, interval = 1)
    public void step() {
        int currentTick = (int) Math.floor(RunEnvironment.getInstance().getCurrentSchedule().getTickCount());
        SimLogger.event(currentTick, "ArrivalScheduler", "process_pending",
                String.format("queueSize=%d", pendingArrivals.size()));
        while (!pendingArrivals.isEmpty() && pendingArrivals.peek().getArrivalTick() <= currentTick) {
            VesselArrival arrival = pendingArrivals.poll();
            VesselAgent vessel = new VesselAgent(arrival.getVesselId(), arrival.getArrivalTick(),
                    arrival.instantiateCargo(MaterialState.ON_VESSEL), crane);
            context.add(vessel);
            SimLogger.info(String.format("[ArrivalScheduler] Registered Vessel %d for arrival tick %d",
                    arrival.getVesselId(), arrival.getArrivalTick()));
            SimLogger.event(currentTick, "ArrivalScheduler", "spawn_vessel",
                    String.format("vessel=%d arrivalTick=%d", arrival.getVesselId(), arrival.getArrivalTick()));
        }
    }
}
