package test250930;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import repast.simphony.engine.schedule.ScheduledMethod;

/**
 * Yard responsible for temporarily storing components on the quay.
 */
public class YardAgent {

    private final int id;
    private final Position position;
    private final int capacity;
    private final Map<MaterialType, List<Material>> stock = new EnumMap<>(MaterialType.class);

    public YardAgent(int id, Position position, int capacity) {
        this.id = id;
        this.position = position;
        this.capacity = capacity;
        for (MaterialType type : MaterialType.values()) {
            stock.put(type, new ArrayList<>());
        }
    }

    public int getId() {
        return id;
    }

    public Position getPosition() {
        return position;
    }

    public synchronized void store(Material material) {
        int projectedSize = getStoredMaterialCount() + 1;
        if (projectedSize > capacity) {
            throw new IllegalStateException("Yard capacity exceeded");
        }
        material.assignTo(this, MaterialState.STORED);
        stock.get(material.getType()).add(material);
    }

    public synchronized List<Material> retrieve(MaterialType type, int quantity) {
        List<Material> materials = stock.get(type);
        if (materials.size() < quantity) {
            throw new IllegalArgumentException("Not enough material of type " + type);
        }
        List<Material> retrieved = new ArrayList<>(materials.subList(0, quantity));
        materials.subList(0, quantity).clear();
        for (Material material : retrieved) {
            material.assignTo(null, MaterialState.ON_QUAY);
        }
        return retrieved;
    }

    public synchronized int getStoredMaterialCount() {
        return stock.values().stream().mapToInt(List::size).sum();
    }

    @ScheduledMethod(start = 2, interval = 5)
    public void inventoryReport() {
        System.out.printf("[Yard %d] Inventory at %s: %s%n", id, position, buildStockSummary());
    }

    private synchronized String buildStockSummary() {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<MaterialType, List<Material>> entry : stock.entrySet()) {
            if (builder.length() > 0) {
                builder.append(", ");
            }
            builder.append(entry.getKey()).append(": ").append(entry.getValue().size());
        }
        return builder.toString();
    }
}
