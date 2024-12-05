package de.backendstack21.realtime.pubsub;

/**
 * A functional interface for implementing reply functions. This interface defines
 * a single abstract method, {@code reply},
 * which can be used to send a response with specific data, a status code, and an
 * optional compression flag.
 *
 * <p>
 * This interface can be used as a lambda expression or method reference target,
 * following the principles of functional programming in Java.
 * </p>
 *
 * <h2>Usage Example:</h2>
 *
 * <pre>
 * {@code
 * var waitFor = replyFn.reply(Map.of("text", "Hello, back!"), "ok", false);
 * }
 * </pre>
 *
 * @see WaitFor
 */
@FunctionalInterface
public interface ReplyFunction {

    /**
     * Sends a reply with the specified data, status, and compression flag.
     *
     * @param data     the data to be sent in the reply, typically a message
     *                 payload or object.
     * @param status   a status code or description indicating the result of the
     *                 operation, such as "200 OK" or "500 Internal Server Error".
     * @param compress a flag indicating whether the response data should be
     *                 compressed before being sent.
     * @return a {@link WaitFor} object representing the response or subsequent
     * asynchronous processing of the reply.
     * @throws Exception if any error occurs while processing the reply.
     */
    WaitFor reply(Object data, String status, boolean compress) throws Exception;
}
