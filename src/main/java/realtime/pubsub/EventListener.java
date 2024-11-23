package realtime.pubsub;

/**
 * A functional interface for event listeners.
 */
@FunctionalInterface
public interface EventListener {
    void handle(Object... args) throws Exception;
}