package test250930;

import repast.simphony.context.Context;
import repast.simphony.context.DefaultContext;
import repast.simphony.dataLoader.ContextBuilder;

public class HelloBuilder implements ContextBuilder<Object> {

    @Override
    public Context<Object> build(Context<Object> context) {
        if (context == null) {
            context = new DefaultContext<>("test250930"); // ← context ID
        }
        for (int i = 0; i < 5; i++) {
            context.add(new HelloAgent(i));
        }
        return context;
    }
}
