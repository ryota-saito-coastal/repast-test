package test250930;

import java.util.List;

/**
 * Abstraction for providing arrival events from external or internal sources.
 */
public interface ArrivalSchedule {

    List<ArrivalEvent> pollArrivals(int tick);
}
