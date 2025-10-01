package repast.simphony.dataLoader;

import repast.simphony.context.Context;

/**
 * Simplified version of Repast's {@code ContextBuilder}. Builders construct
 * the agent context before the simulation starts.
 */
public interface ContextBuilder<T> {

    Context<T> build(Context<T> context);
}
