package de.backendstack21.realtime.pubsub;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provides a convenient way to wait for acknowledgments or replies related to a specific message
 * sent through a {@link RealtimeClient}.
 * <p>
 * When you publish or send a message using {@link RealtimeClient}, you receive a {@code WaitFor} instance
 * associated with that message. This instance allows you to asynchronously wait for specific events:
 * <ul>
 *     <li><b>Acknowledgments</b>: Confirmations from the messaging gateway
 *         that the message was received and processed.</li>
 *     <li><b>Replies</b>: Responses from other subscribers or backend services responding to your message.</li>
 * </ul>
 * Both acknowledgments and replies are represented by events that occur on the underlying
 * {@link RealtimeClient} event bus.
 *
 * <h2>Typical Usage Example:</h2>
 * <pre>{@code
 * // After sending a message with a unique messageId...
 * WaitFor waitFor = client.publish("someTopic", payload, "someType");
 *
 * // Wait for an acknowledgment (default 30s timeout)
 * waitFor.waitForAck().thenAccept(args -> {
 *     System.out.println("Acknowledgment received!");
 * }).exceptionally(ex -> {
 *     System.err.println("Failed to receive acknowledgment: " + ex.getMessage());
 *     return null;
 * });
 *
 * // Wait for a reply (with a custom timeout of 10 seconds)
 * waitFor.waitForReply(10).thenAccept(args -> {
 *     var reply = (ResponseMessage) args[0];
 *     logger.info("Reply received: " + reply);
 * }).exceptionally(ex -> {
 *     System.err.println("No reply received within the timeout.");
 *     return null;
 * });
 * }</pre>
 *
 * <p>
 * Internally, this class leverages the {@link RealtimeClient#waitFor(String, int)} method to
 * listen for events named according to the message ID. For acknowledgments, it listens to
 * events in the form {@code "ack.<messageId>"}, and for replies, it listens to
 * {@code "response.<messageId>"}.
 * </p>
 */
public class WaitFor {
    /**
     * Logger for the WaitFor class, used to log informational and error messages.
     */
    private static final Logger logger = Logger.getLogger(WaitFor.class.getName());

    /**
     * The {@link RealtimeClient} instance that this WaitFor is associated with.
     * It is used to register event listeners and wait for acknowledgments or replies.
     */
    private final RealtimeClient client;

    /**
     * The unique identifier for the message being tracked.
     * This is used to correlate acknowledgments or replies with the original message.
     */
    private final String messageId;

    /**
     * Constructs a new {@code WaitFor} instance associated with a specific message.
     *
     * @param client    The {@link RealtimeClient} that was used to send the message.
     * @param messageId The unique ID of the message for which acknowledgments or replies are awaited.
     */
    public WaitFor(RealtimeClient client, String messageId) {
        this.client = client;
        this.messageId = messageId;
        logger.log(Level.INFO, "WaitFor instance created for message ID: {0}", messageId);
    }

    /**
     * Waits for an acknowledgment event related to the given message ID, using a default timeout of 30 seconds.
     * <p>
     * The returned {@link CompletableFuture} will complete when an event named
     * {@code "ack.<messageId>"} is emitted. If the event does not occur within the timeout,
     * the future completes exceptionally with a {@link TimeoutException}.
     *
     * @return A {@link CompletableFuture} that completes with the acknowledgment event data ({@code Object[]})
     * or completes exceptionally if no acknowledgment is received in time.
     */
    public CompletableFuture<Object[]> waitForAck() {
        return waitForAck(30);
    }

    /**
     * Waits for an acknowledgment event related to the given message ID, using a specified timeout.
     * <p>
     * The returned {@link CompletableFuture} will complete when an event named
     * {@code "ack.<messageId>"} is emitted. If the event does not occur within the given timeout,
     * the future completes exceptionally with a {@link TimeoutException}.
     *
     * @param timeoutSeconds The maximum number of seconds to wait before timing out.
     * @return A {@link CompletableFuture} that completes with the acknowledgment event data ({@code Object[]})
     * or completes exceptionally if no acknowledgment is received in time.
     */
    public CompletableFuture<Object[]> waitForAck(int timeoutSeconds) {
        String ackEvent = String.format("ack.%s", messageId);
        logger.log(Level.INFO, "Waiting for acknowledgment on event: {0} with timeout: {1} seconds", new Object[]{ackEvent, timeoutSeconds});
        CompletableFuture<Object[]> ackFuture = client.waitFor(ackEvent, timeoutSeconds);

        ackFuture.whenComplete((result, throwable) -> {
            if (throwable != null) {
                if (throwable instanceof TimeoutException) {
                    logger.log(Level.WARNING, "Timeout while waiting for acknowledgment on event: {0}", ackEvent);
                } else {
                    logger.log(Level.SEVERE, "Exception while waiting for acknowledgment on event: " + ackEvent, throwable);
                }
            } else {
                logger.log(Level.INFO, "Acknowledgment received for event: {0}", ackEvent);
            }
        });

        return ackFuture;
    }

    /**
     * Waits for a reply event related to the given message ID, using a default timeout of 30 seconds.
     * <p>
     * The returned {@link CompletableFuture} will complete when an event named
     * {@code "response.<messageId>"} is emitted. If the event does not occur within the timeout,
     * the future completes exceptionally with a {@link TimeoutException}.
     *
     * @return A {@link CompletableFuture} that completes with the reply event data ({@code Object[]})
     * or completes exceptionally if no reply is received in time.
     */
    public CompletableFuture<Object[]> waitForReply() {
        return waitForReply(30);
    }

    /**
     * Waits for a reply event related to the given message ID, using a specified timeout.
     * <p>
     * The returned {@link CompletableFuture} will complete when an event named
     * {@code "response.<messageId>"} is emitted. If the event does not occur within the given timeout,
     * the future completes exceptionally with a {@link TimeoutException}.
     *
     * @param timeoutSeconds The maximum number of seconds to wait before timing out.
     * @return A {@link CompletableFuture} that completes with the reply event data ({@code Object[]})
     * or completes exceptionally if no reply is received in time.
     */
    public CompletableFuture<Object[]> waitForReply(int timeoutSeconds) {
        String replyEvent = String.format("response.%s", messageId);
        logger.log(Level.INFO, "Waiting for reply on event: {0} with timeout: {1} seconds", new Object[]{replyEvent, timeoutSeconds});
        CompletableFuture<Object[]> replyFuture = client.waitFor(replyEvent, timeoutSeconds);

        replyFuture.whenComplete((result, throwable) -> {
            if (throwable != null) {
                if (throwable instanceof TimeoutException) {
                    logger.log(Level.WARNING, "Timeout while waiting for reply on event: {0}", replyEvent);
                } else {
                    logger.log(Level.SEVERE, "Exception while waiting for reply on event: " + replyEvent, throwable);
                }
            } else {
                logger.log(Level.INFO, "Reply received for event: {0}", replyEvent);
            }
        });

        return replyFuture;
    }
}
