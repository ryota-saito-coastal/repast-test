package repast.simphony.context;

/**
 * Simplified version of Repast's {@code Context} interface. The real
 * implementation provides many more capabilities, but for the purposes of the
 * lightweight runtime we only need to support adding objects and iterating over
 * them.
 */
public interface Context<T> extends Iterable<T> {

    /**
     * Adds an object to the context.
     *
     * @param object the object to add
     * @return {@code true} when the context was modified
     */
    boolean add(T object);
}
