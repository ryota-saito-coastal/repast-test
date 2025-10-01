package test250930;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Simple deterministic schedule used until a connection with the C++ model is wired in.
 */
public class StaticArrivalSchedule implements ArrivalSchedule {

    private final Map<Integer, List<ArrivalEvent>> eventsByTick = new HashMap<>();

    public StaticArrivalSchedule() {
        register(new ArrivalEvent(1, CarrierType.CARGO_VESSEL, List.of(
                new MaterialManifestEntry(MaterialType.NACELLE, 1),
                new MaterialManifestEntry(MaterialType.BLADE, 2))));
        register(new ArrivalEvent(3, CarrierType.CARGO_VESSEL, List.of(
                new MaterialManifestEntry(MaterialType.TOWER, 2))));
        register(new ArrivalEvent(5, CarrierType.TOWED_FOUNDATION, List.of(
                new MaterialManifestEntry(MaterialType.FOUNDATION, 1))));
        register(new ArrivalEvent(7, CarrierType.CARGO_VESSEL, List.of(
                new MaterialManifestEntry(MaterialType.BLADE, 1),
                new MaterialManifestEntry(MaterialType.TOWER, 1))));
    }

    private void register(ArrivalEvent event) {
        eventsByTick.computeIfAbsent(event.getTick(), k -> new ArrayList<>()).add(event);
    }

    @Override
    public List<ArrivalEvent> pollArrivals(int tick) {
        return eventsByTick.getOrDefault(tick, List.of());
    }
}
