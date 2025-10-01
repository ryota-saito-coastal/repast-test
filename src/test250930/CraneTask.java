package test250930;

class CraneTask {

    private final VesselAgent vessel;
    private final Material material;
    private int remainingTicks;

    CraneTask(VesselAgent vessel, Material material, int handlingDuration) {
        this.vessel = vessel;
        this.material = material;
        this.remainingTicks = handlingDuration;
    }

    public VesselAgent getVessel() {
        return vessel;
    }

    public Material getMaterial() {
        return material;
    }

    public void workOneTick() {
        if (remainingTicks > 0) {
            remainingTicks--;
        }
    }

    public boolean isComplete() {
        return remainingTicks <= 0;
    }
}
