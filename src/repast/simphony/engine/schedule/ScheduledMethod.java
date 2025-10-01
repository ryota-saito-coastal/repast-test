package repast.simphony.engine.schedule;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Minimal stub of Repast's {@code @ScheduledMethod} annotation. It only stores
 * the desired start tick and invocation interval so that the lightweight
 * runtime can approximate the Repast scheduler.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ScheduledMethod {

    /**
     * Tick at which the annotated method should first run.
     */
    double start() default 1;

    /**
     * Interval in ticks between invocations once the start tick has been reached.
     */
    double interval() default 1;
}
