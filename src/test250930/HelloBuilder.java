package test250930;

import repast.simphony.context.Context;
import repast.simphony.context.DefaultContext;
import repast.simphony.dataLoader.ContextBuilder;
import repast.simphony.engine.environment.RunEnvironmentBuilder;
import repast.simphony.scenario.ModelInitializer;
import repast.simphony.scenario.Scenario;

public class HelloBuilder implements ContextBuilder<Object>, ModelInitializer {

    @Override
    public Context<Object> build(Context<Object> context) {
        if (context == null) {
            context = new DefaultContext<>("HelloContext");
        }
        for (int i = 0; i < 5; i++) {
            context.add(new HelloAgent(i));
        }
        return context;
    }

    @Override
    public void initialize(Scenario scen, RunEnvironmentBuilder builder) {
        // ここでは何もせず、ContextBuilder の build に任せる
    }
}
