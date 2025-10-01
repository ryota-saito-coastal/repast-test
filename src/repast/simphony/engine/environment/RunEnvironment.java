package repast.simphony.engine.environment;

/**
 * Extremely small substitute for Repast's {@code RunEnvironment}. It exposes a
 * mutable {@link Schedule} so agents can query the current tick.
 */
public final class RunEnvironment {

    private static final RunEnvironment INSTANCE = new RunEnvironment();

    private final Schedule schedule = new Schedule();

    private RunEnvironment() {
    }

    public static RunEnvironment getInstance() {
        return INSTANCE;
    }

    public Schedule getCurrentSchedule() {
        return schedule;
    }

    /**
     * Lightweight representation of Repast's schedule. Only the current tick is
     * tracked because that is all the demo model requires.
     */
    public static final class Schedule {

        private double tickCount = 0.0;

        public double getTickCount() {
            return tickCount;
        }

        public void setTickCount(double tickCount) {
            this.tickCount = tickCount;
        }
    }
}
