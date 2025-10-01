package test250930;

import java.util.List;

/**
 * Current placeholder implementation that simply logs port events to stdout.
 */
public class LoggingPortEventGateway implements PortEventGateway {

    @Override
    public void reportUnloaded(VesselAgent vessel, YardAgent yard, List<Material> materials) {
        System.out.printf("[Gateway] Vessel %d unloaded %d items to Yard %d%n",
                vessel.getId(), materials.size(), yard.getId());
    }
}
