package test250930;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

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
        System.out.printf("[Yard %d] Stored %s delivered from %s. Current stock: %d%n", id,
                material.getId(), source, inventory.get(material.getType()).size());
    }

    @ScheduledMethod(start = 5, interval = 5)
    public void inventoryReport() {
        System.out.printf("[Yard %d] Inventory report:%n", id);
        for (Map.Entry<MaterialType, List<Material>> entry : inventory.entrySet()) {
            System.out.printf("  - %s: %d items%n", entry.getKey(), entry.getValue().size());
        }
    }
}
