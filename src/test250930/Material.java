package test250930;

import java.util.concurrent.atomic.AtomicInteger;

public class Material {

    private static final AtomicInteger COUNTER = new AtomicInteger(1);

    private final String id;
    private final MaterialType type;
    private MaterialState state;

    private Material(MaterialType type, MaterialState state, String id) {
        this.type = type;
        this.state = state;
        this.id = id;
    }

    public static Material create(MaterialType type, MaterialState initialState) {
        String generatedId = String.format("%s-%03d", type.name(), COUNTER.getAndIncrement());
        return new Material(type, initialState, generatedId);
    }

    public String getId() {
        return id;
    }

    public MaterialType getType() {
        return type;
    }

    public MaterialState getState() {
        return state;
    }

    public void setState(MaterialState state) {
        this.state = state;
    }

    @Override
    public String toString() {
        return "Material{" +
                "id='" + id + '\'' +
                ", type=" + type +
                ", state=" + state +
                '}';
    }
}
