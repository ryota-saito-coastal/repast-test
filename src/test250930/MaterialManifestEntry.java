package test250930;

/**
 * Manifest entry representing a quantity of a specific material type.
 */
public class MaterialManifestEntry {

    private final MaterialType type;
    private final int quantity;

    public MaterialManifestEntry(MaterialType type, int quantity) {
        this.type = type;
        this.quantity = quantity;
    }

    public MaterialType getType() {
        return type;
    }

    public int getQuantity() {
        return quantity;
    }

    @Override
    public String toString() {
        return quantity + "x" + type;
    }
}
