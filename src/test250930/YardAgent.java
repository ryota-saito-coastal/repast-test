package test250930;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import repast.simphony.engine.schedule.ScheduledMethod;
import test250930.logging.SimLogger;

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
        int stockCount = inventory.get(material.getType()).size();
        SimLogger.event("Yard " + id, "stored",
                "type=" + material.getType() + ", id=" + material.getId() + ", source=" + source
                        + ", count=" + stockCount);
    }

    @ScheduledMethod(start = 5, interval = 5)
    public void inventoryReport() {
        StringBuilder summary = new StringBuilder();
        for (Map.Entry<MaterialType, List<Material>> entry : inventory.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                if (summary.length() > 0) {
                    summary.append("; ");
                }
                summary.append(entry.getKey()).append('=').append(entry.getValue().size());
            }
        }
        if (summary.length() == 0) {
            summary.append("empty");
        }
        SimLogger.event("Yard " + id, "inventory report", summary.toString());
    }
}
