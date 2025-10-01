package test250930;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import repast.simphony.context.Context;
import repast.simphony.engine.schedule.ScheduledMethod;

/**
 * Coordinates high level port events such as arrivals and resource allocation.
 */
public class PortController {

    private final Context<Object> context;
    private final ArrivalSchedule arrivalSchedule;
    private final CraneAgent crane;
    private final YardAgent yard;
    private int currentTick;
    private int nextVesselId = 1;
    private int nextMaterialId = 1;

    private final List<VesselAgent> activeVessels = new ArrayList<>();

    public PortController(Context<Object> context, ArrivalSchedule arrivalSchedule, CraneAgent crane, YardAgent yard) {
        this.context = context;
        this.arrivalSchedule = arrivalSchedule;
        this.crane = crane;
        this.yard = yard;
    }

    @ScheduledMethod(start = 1, interval = 1)
    public void step() {
        List<ArrivalEvent> arrivals = arrivalSchedule.pollArrivals(currentTick);
        for (ArrivalEvent event : arrivals) {
            createVessel(event);
        }
        pruneDepartedVessels();
        currentTick++;
    }

    private void createVessel(ArrivalEvent event) {
        List<Material> materials = new ArrayList<>();
        for (MaterialManifestEntry entry : event.getManifest()) {
            for (int i = 0; i < entry.getQuantity(); i++) {
                materials.add(new Material(generateMaterialId(), entry.getType(), MaterialState.INBOUND, null));
            }
        }
        Position berth = selectBerthPosition(event.getCarrierType());
        VesselAgent vessel = new VesselAgent(nextVesselId++, event.getCarrierType(), berth, crane, yard, materials);
        context.add(vessel);
        activeVessels.add(vessel);
        System.out.printf("[Controller] Registered arrival for Vessel %d with manifest %s%n", vessel.getId(),
                event.getManifest());
    }

    private Position selectBerthPosition(CarrierType carrierType) {
        if (carrierType == CarrierType.TOWED_FOUNDATION) {
            return new Position(80, 25);
        }
        return new Position(20, 10);
    }

    private void pruneDepartedVessels() {
        Iterator<VesselAgent> iterator = activeVessels.iterator();
        while (iterator.hasNext()) {
            VesselAgent vessel = iterator.next();
            if (vessel.isDeparted()) {
                context.remove(vessel);
                iterator.remove();
                System.out.printf("[Controller] Removed departed Vessel %d from context.%n", vessel.getId());
            }
        }
    }

    private String generateMaterialId() {
        return "MAT-" + nextMaterialId++;
    }
}
