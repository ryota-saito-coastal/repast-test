package test250930;

import java.util.List;

/**
 * Hook for sending messages to the external (C++) model.
 */
public interface PortEventGateway {

    void reportUnloaded(VesselAgent vessel, YardAgent yard, List<Material> materials);
}
