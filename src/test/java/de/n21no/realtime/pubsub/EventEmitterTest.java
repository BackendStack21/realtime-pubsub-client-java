package de.n21no.realtime.pubsub;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;

public class EventEmitterTest {

    private EventEmitter emitter;

    @BeforeEach
    public void setUp() {
        emitter = new EventEmitter();
    }

    @Test
    public void testOnAndEmit() {
        AtomicInteger callCount = new AtomicInteger(0);

        EventListener listener = (args) -> callCount.incrementAndGet();

        emitter.on("test.event", listener);
        emitter.emit("test.event");

        assertEquals(1, callCount.get());
    }

    @Test
    public void testOff() {
        AtomicInteger callCount = new AtomicInteger(0);

        EventListener listener = (args) -> callCount.incrementAndGet();

        emitter.on("test.event", listener);
        emitter.off("test.event", listener);
        emitter.emit("test.event");

        assertEquals(0, callCount.get());
    }

    @Test
    public void testOnce() {
        AtomicInteger callCount = new AtomicInteger(0);

        EventListener listener = (args) -> callCount.incrementAndGet();

        emitter.once("test.event", listener);
        emitter.emit("test.event");
        emitter.emit("test.event");

        assertEquals(1, callCount.get());
    }

    @Test
    public void testWildcardSingleSegment() {
        AtomicInteger callCount = new AtomicInteger(0);

        EventListener listener = (args) -> callCount.incrementAndGet();

        emitter.on("test.*", listener);
        emitter.emit("test.event1");
        emitter.emit("test.event2");
        emitter.emit("test.event.sub");

        assertEquals(2, callCount.get()); // "test.event.sub" should not match "test.*"
    }

    @Test
    public void testWildcardMultipleSegments() {
        AtomicInteger callCount = new AtomicInteger(0);

        EventListener listener = (args) -> callCount.incrementAndGet();

        emitter.on("test.**", listener);
        emitter.emit("test.event1");
        emitter.emit("test.event.sub");
        emitter.emit("test.event.sub.sub");

        assertEquals(3, callCount.get());
    }

    @Test
    public void testExactMatchOverWildcard() {
        AtomicInteger callCountExact = new AtomicInteger(0);
        AtomicInteger callCountWildcard = new AtomicInteger(0);

        EventListener exactListener = (args) -> callCountExact.incrementAndGet();
        EventListener wildcardListener = (args) -> callCountWildcard.incrementAndGet();

        emitter.on("test.event", exactListener);
        emitter.on("test.*", wildcardListener);

        emitter.emit("test.event");

        assertEquals(1, callCountExact.get());
        assertEquals(1, callCountWildcard.get());
    }

    @Test
    public void testListenerArguments() {
        AtomicBoolean argumentMatched = new AtomicBoolean(false);

        EventListener listener = (args) -> {
            if (args.length == 2 && args[0].equals("arg1") && args[1].equals(42)) {
                argumentMatched.set(true);
            }
        };

        emitter.on("test.event", listener);
        emitter.emit("test.event", "arg1", 42);

        assertTrue(argumentMatched.get());
    }

    @Test
    public void testMultipleListenersSameEvent() {
        AtomicInteger callCount1 = new AtomicInteger(0);
        AtomicInteger callCount2 = new AtomicInteger(0);

        EventListener listener1 = (args) -> callCount1.incrementAndGet();
        EventListener listener2 = (args) -> callCount2.incrementAndGet();

        emitter.on("test.event", listener1);
        emitter.on("test.event", listener2);

        emitter.emit("test.event");

        assertEquals(1, callCount1.get());
        assertEquals(1, callCount2.get());
    }

    @Test
    public void testWildcardAtEnd() {
        AtomicInteger callCount = new AtomicInteger(0);

        EventListener listener = (args) -> callCount.incrementAndGet();

        emitter.on("test.**", listener);
        emitter.emit("test");
        emitter.emit("test.sub.event");
        emitter.emit("test.sub.sub.event");

        assertEquals(3, callCount.get());
    }

    @Test
    public void testWildcardInMiddle() {
        AtomicInteger callCount = new AtomicInteger(0);

        EventListener listener = (args) -> callCount.incrementAndGet();

        emitter.on("test.**.event", listener);
        emitter.emit("test.event");
        emitter.emit("test.sub.event");
        emitter.emit("test.sub.sub.event");

        assertEquals(3, callCount.get());
    }

    @Test
    public void testNoMatch() {
        AtomicInteger callCount = new AtomicInteger(0);

        EventListener listener = (args) -> callCount.incrementAndGet();

        emitter.on("test.event", listener);
        emitter.emit("no.match");

        assertEquals(0, callCount.get());
    }
}