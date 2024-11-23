package realtime.pubsub;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A simple event emitter class that allows registering and emitting events.
 * Supports wildcard events using '*' and '**' with '.' as the separator.
 */
public class EventEmitter {
    /**
     * Logger for the EventEmitter class.
     */
    private static final Logger logger = Logger.getLogger(EventEmitter.class.getName());


    private final Map<String, List<EventListener>> events;

    /**
     * Initialize an `EventEmitter` instance with an empty events map.
     */
    public EventEmitter() {
        this.events = new ConcurrentHashMap<>();
    }

    /**
     * Register a listener for a specific event, with support for wildcards.
     *
     * @param event    The name of the event, can include wildcards ('*' or '**').
     * @param listener The function to call when the event is emitted.
     */
    public void on(String event, EventListener listener) {
        events.computeIfAbsent(event, k -> new CopyOnWriteArrayList<>()).add(listener);
    }

    /**
     * Remove a listener for a specific event.
     *
     * @param event    The name of the event.
     * @param listener The function to remove from the event listeners.
     */
    public void off(String event, EventListener listener) {
        List<EventListener> listeners = events.get(event);
        if (listeners != null) {
            listeners.remove(listener);
            if (listeners.isEmpty()) {
                events.remove(event);
            }
        } else {
            logger.log(Level.WARNING, "Attempted to remove a listener from a non-existent event: {0}", event);
        }
    }

    /**
     * Trigger all listeners associated with an event, supporting wildcards.
     *
     * @param event The name of the event to emit.
     * @param args  Arguments to pass to the event listeners.
     */
    public void emit(String event, Object... args) {
        List<EventListener> listenersToInvoke = new ArrayList<>();
        for (Map.Entry<String, List<EventListener>> entry : events.entrySet()) {
            String eventPattern = entry.getKey();
            if (eventMatches(eventPattern, event)) {
                listenersToInvoke.addAll(entry.getValue());
            }
        }

        if (listenersToInvoke.isEmpty()) {
            return;
        }

        for (EventListener listener : listenersToInvoke) {
            try {
                listener.handle(args);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Exception occurred while handling event: " + event, e);
            }
        }
    }

    /**
     * Register a listener for a specific event that will be called at most once.
     *
     * @param event    The name of the event.
     * @param listener The function to call when the event is emitted.
     */
    public void once(String event, EventListener listener) {
        EventListener wrapperListener = new EventListener() {
            @Override
            public void handle(Object... args) throws Exception {
                listener.handle(args);
                off(event, this);
            }
        };
        on(event, wrapperListener);
    }

    /**
     * Check if an event pattern matches an event name, supporting wildcards '*' and '**'.
     *
     * @param pattern   The event pattern, may include wildcards '*' and '**'.
     * @param eventName The event name to match against.
     * @return True if the pattern matches the event name, False otherwise.
     */
    public static boolean eventMatches(String pattern, String eventName) {
        String[] patternSegments = pattern.split("\\.");
        String[] eventSegments = eventName.split("\\.");

        return matchSegments(patternSegments, eventSegments, 0, 0);
    }

    private static boolean matchSegments(String[] patternSegments, String[] eventSegments, int i, int j) {
        while (i < patternSegments.length && j < eventSegments.length) {
            if (patternSegments[i].equals("**")) {
                if (i == patternSegments.length - 1) {
                    return true;
                } else {
                    for (int k = j; k <= eventSegments.length; k++) {
                        if (matchSegments(patternSegments, eventSegments, i + 1, k)) {
                            return true;
                        }
                    }
                    return false;
                }
            } else if (patternSegments[i].equals("*")) {
                i++;
                j++;
            } else if (patternSegments[i].equals(eventSegments[j])) {
                i++;
                j++;
            } else {
                return false;
            }
        }
        while (i < patternSegments.length && patternSegments[i].equals("**")) {
            i++;
        }
        return i == patternSegments.length && j == eventSegments.length;
    }
}