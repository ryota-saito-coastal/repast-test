package test250930;

import repast.simphony.context.Context;
import repast.simphony.context.DefaultContext;
import repast.simphony.dataLoader.ContextBuilder;
import repast.simphony.engine.environment.DefaultControllerAction;
import repast.simphony.engine.environment.RunEnvironmentBuilder;
import repast.simphony.engine.environment.RunState;
import repast.simphony.parameter.Parameters;
import repast.simphony.scenario.ModelInitializer;
import repast.simphony.scenario.Scenario;

public class HelloBuilder implements ContextBuilder<Object>, ModelInitializer {

    @Override
    public Context<Object> build(Context<Object> context) {
        if (context == null) {
            context = new DefaultContext<>("test250930");
        }
        for (int i = 0; i < 5; i++) {
            context.add(new HelloAgent(i));
        }
        return context;
    }

    @Override
    public void initialize(Scenario scen, RunEnvironmentBuilder builder) {
        scen.addMasterControllerAction(new DefaultControllerAction() {
            @Override
            public void runInitialize(RunState runState, Object contextId, Parameters runParams) {
                Context<Object> context = build(new DefaultContext<>("test250930"));
                runState.setMasterContext(context);
            }
        });
    }
}
