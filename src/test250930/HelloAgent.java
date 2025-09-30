package test250930;

import repast.simphony.engine.schedule.ScheduledMethod;

public class HelloAgent {

    private int id;

    public HelloAgent(int id) {
        this.id = id;
    }

    @ScheduledMethod(start = 1, interval = 1)
    public void step() {
        System.out.println("Agent " + id + " says hello!");
    }
}
