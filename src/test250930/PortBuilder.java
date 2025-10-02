package test250930;

import java.util.Arrays;
import java.util.List;

import repast.simphony.context.Context;
import repast.simphony.context.DefaultContext;
import repast.simphony.dataLoader.ContextBuilder;
import repast.simphony.engine.environment.RunEnvironment;

public class PortBuilder implements ContextBuilder<Object> {

    @Override
    public Context<Object> build(Context<Object> context) {
        if (context == null) {
            context = new DefaultContext<>("offshore_port");
        }

        YardAgent yard = new YardAgent(1);
        QuayCraneAgent crane = new QuayCraneAgent(1, yard);
        DockAgent dock = new DockAgent(1, yard);
        PortArrivalScheduler arrivalScheduler = new PortArrivalScheduler(context, crane);

        SimLogger.info("[PortBuilder] Initialising offshore port context");

        context.add(yard);
        context.add(crane);
        context.add(dock);
        context.add(arrivalScheduler);

        List<VesselArrival> arrivals = Arrays.asList(
                new VesselArrival(101, 2, Arrays.asList(
                        MaterialType.NACELLE,
                        MaterialType.BLADE,
                        MaterialType.BLADE,
                        MaterialType.TOWER)),
                new VesselArrival(102, 6, Arrays.asList(
                        MaterialType.NACELLE,
                        MaterialType.BLADE,
                        MaterialType.TOWER))
        );

        for (VesselArrival arrival : arrivals) {
            arrivalScheduler.registerArrival(arrival);
        }

        SimLogger.info(String.format("[PortBuilder] Registered %d vessel arrivals", arrivals.size()));

        Material foundation1 = Material.create(MaterialType.FOUNDATION, MaterialState.IN_TRANSIT);
        Material foundation2 = Material.create(MaterialType.FOUNDATION, MaterialState.IN_TRANSIT);
        dock.scheduleTow(foundation1);
        dock.scheduleTow(foundation2);

        SimLogger.info("[PortBuilder] Scheduled initial foundation towing jobs");

        RunEnvironment.getInstance().endAt(30);
        SimLogger.info("[PortBuilder] Simulation will run until tick 30");

        return context;
    }
}
