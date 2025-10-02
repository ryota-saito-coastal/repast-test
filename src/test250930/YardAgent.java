package test250930;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.schedule.ScheduledMethod;

public class YardAgent {

    private final int id;
    private final Map<MaterialType, List<Material>> inventory = new EnumMap<>(MaterialType.class);

    public YardAgent(int id) {
        this.id = id;
        for (MaterialType type : MaterialType.values()) {
            inventory.put(type, new ArrayList<>());
        }
    }

    public void receiveMaterial(Material material, String source) {
        material.setState(MaterialState.IN_YARD);
        inventory.get(material.getType()).add(material);
        int currentTick = (int) Math.floor(RunEnvironment.getInstance().getCurrentSchedule().getTickCount());
        int stock = inventory.get(material.getType()).size();
        SimLogger.event(currentTick, "Yard-" + id, "store_material",
                String.format("material=%s type=%s stock=%d source=%s", material.getId(),
                        material.getType(), stock, source));
    }

    @ScheduledMethod(start = 5, interval = 5)
    public void inventoryReport() {
        int currentTick = (int) Math.floor(RunEnvironment.getInstance().getCurrentSchedule().getTickCount());
        SimLogger.info(String.format("[Yard %d] Inventory report at tick %d", id, currentTick));
        for (Map.Entry<MaterialType, List<Material>> entry : inventory.entrySet()) {
            SimLogger.info(String.format("  - %s: %d items", entry.getKey(), entry.getValue().size()));
            SimLogger.event(currentTick, "Yard-" + id, "inventory",
                    String.format("type=%s stock=%d", entry.getKey(), entry.getValue().size()));
        }
    }
}
