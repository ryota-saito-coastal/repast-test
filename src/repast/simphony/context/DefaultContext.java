package repast.simphony.context;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Minimal, thread-safe implementation of {@link Context} backed by a
 * {@link CopyOnWriteArrayList}. The copy-on-write semantics keep iteration
 * stable even if agents add other agents to the context while the runtime is
 * traversing it.
 */
public class DefaultContext<T> implements Context<T> {

    private final String id;
    private final List<T> objects = new CopyOnWriteArrayList<>();

    public DefaultContext(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    @Override
    public boolean add(T object) {
        return objects.add(object);
    }

    @Override
    public Iterator<T> iterator() {
        return objects.iterator();
    }
}
