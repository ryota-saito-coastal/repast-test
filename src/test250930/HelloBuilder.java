package test250930;

import repast.simphony.context.Context;
import repast.simphony.dataLoader.ContextBuilder;

public class HelloBuilder implements ContextBuilder<Object> {

    @Override
    public Context<Object> build(Context<Object> context) {
        context.setId("HelloContext");
        for (int i = 0; i < 5; i++) {
            context.add(new HelloAgent(i));
        }
        return context;
    }
}
