package de.n21no.realtime.pubsub;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class represents a factory for waiting on acknowledgments or replies.
 * It provides methods to wait for acknowledgments from the Messaging Gateway
 * or replies from other subscribers or backend services.
 */
public class WaitFor {
    /**
     * Logger for the WaitFor class.
     */
    private static final Logger logger = Logger.getLogger(WaitFor.class.getName());

    private final RealtimeClient client;
    private final String messageId;

    /**
     * Initializes a new instance of the `WaitFor` class.
     *
     * @param client    The `RealtimeClient` instance associated with this factory.
     * @param messageId The unique identifier for the message being acknowledged or replied to.
     */
    public WaitFor(RealtimeClient client, String messageId) {
        this.client = client;
        this.messageId = messageId;
        logger.log(Level.INFO, "WaitFor instance created for message ID: {0}", messageId);
    }

    /**
     * Waits for an acknowledgment event with a default timeout of 30 seconds.
     *
     * @return A CompletableFuture that completes with the acknowledgment data received.
     */
    public CompletableFuture<Object[]> waitForAck() {
        return waitForAck(30);
    }

    /**
     * Waits for an acknowledgment event with a specified timeout.
     *
     * @param timeoutSeconds The maximum time to wait for the acknowledgment in seconds.
     * @return A CompletableFuture that completes with the acknowledgment data received.
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
     * Waits for an acknowledgment event with a default timeout of 30 seconds.
     *
     * @return A CompletableFuture that completes with the acknowledgment data received.
     */
    public CompletableFuture<Object[]> waitForReply() {
        return waitForReply(30);
    }

    /**
     * Waits for a reply event with a specified timeout.
     *
     * @param timeoutSeconds The maximum time to wait for the reply in seconds.
     * @return A CompletableFuture that completes with the reply data received.
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