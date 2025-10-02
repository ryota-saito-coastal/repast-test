package test250930;

import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Queue;

import repast.simphony.context.Context;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.schedule.ScheduledMethod;
import test250930.logging.SimLogger;

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
        SimLogger.info("[ArrivalScheduler] Queued Vessel %d for tick %d",
                arrival.getVesselId(), arrival.getArrivalTick());
    }

    @ScheduledMethod(start = 1, interval = 1)
    public void step() {
        int currentTick = (int) Math.floor(RunEnvironment.getInstance().getCurrentSchedule().getTickCount());
        SimLogger.event(currentTick, "ArrivalScheduler", "processing arrivals",
                "pending=" + pendingArrivals.size());
        while (!pendingArrivals.isEmpty() && pendingArrivals.peek().getArrivalTick() <= currentTick) {
            VesselArrival arrival = pendingArrivals.poll();
            VesselAgent vessel = new VesselAgent(arrival.getVesselId(), arrival.getArrivalTick(),
                    arrival.instantiateCargo(MaterialState.ON_VESSEL), crane);
            context.add(vessel);
            SimLogger.event(currentTick, "ArrivalScheduler", "registered vessel",
                    "vessel=" + arrival.getVesselId() + ", eta=" + arrival.getArrivalTick());
        }
    }
}
