package test250930;

import repast.simphony.context.Context;
import repast.simphony.context.DefaultContext;
import repast.simphony.dataLoader.ContextBuilder;

/**
 * Builds the port-handling scenario with vessels, crane, and yard agents.
 */
public class HelloBuilder implements ContextBuilder<Object> {

    @Override
    public Context<Object> build(Context<Object> context) {
        if (context == null) {
            context = new DefaultContext<>("test250930");
        }

        PortEventGateway gateway = new LoggingPortEventGateway();
        YardAgent yard = new YardAgent(1, new Position(10, 5), 40);
        CraneAgent crane = new CraneAgent(1, new Position(12, 5), 35, 4.0, gateway);
        ArrivalSchedule schedule = new StaticArrivalSchedule();

        context.add(yard);
        context.add(crane);

        PortController controller = new PortController(context, schedule, crane, yard);
        context.add(controller);

        return context;
    }
}
