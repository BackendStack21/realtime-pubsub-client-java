package de.backendstack21.realtime.pubsub;

/**
 * A functional interface for event listeners.
 * Implementations of this interface can be used to handle events emitted by the EventEmitter.
 */
@FunctionalInterface
public interface EventListener {
    /**
     * Handles an event with the given arguments.
     *
     * @param args the arguments passed to the event
     * @throws Exception if an error occurs while handling the event
     */
    void handle(Object... args) throws Exception;
}