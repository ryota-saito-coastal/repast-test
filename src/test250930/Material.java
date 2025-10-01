package test250930;

import java.util.Objects;

/**
 * Material entity tracked within the port simulation.
 */
public class Material {

    private final String id;
    private final MaterialType type;
    private MaterialState state;
    private Object owner;

    public Material(String id, MaterialType type, MaterialState state, Object owner) {
        this.id = id;
        this.type = type;
        this.state = state;
        this.owner = owner;
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

    public Object getOwner() {
        return owner;
    }

    public void assignTo(Object owner, MaterialState state) {
        this.owner = owner;
        this.state = state;
    }

    public void markInstalled() {
        this.state = MaterialState.INSTALLED;
    }

    @Override
    public String toString() {
        return "Material{" +
                "id='" + id + '\'' +
                ", type=" + type +
                ", state=" + state +
                "}";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Material)) {
            return false;
        }
        Material other = (Material) obj;
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
