package de.backendstack21.realtime.pubsub;

import java.util.Map;

/**
 * Represents a response message.
 * <p>
 * The {@code ResponseMessage} class encapsulates a unique message identifier,
 * a status indicating the outcome of the request, and associated response data.
 * It is commonly used to convey the results of asynchronous operations or
 * to provide acknowledgments and responses to previously sent messages.
 * </p>
 *
 * <h2>Key Features:</h2>
 * <ul>
 *     <li>Maintains a unique message ID to correlate responses with their requests.</li>
 *     <li>Holds a status string indicating success or failure, or any other custom state.</li>
 *     <li>Includes arbitrary payload data, which can be of any type, typically a {@link Map} or a {@link String}.</li>
 * </ul>
 *
 * <h2>Typical Usage:</h2>
 * <pre>{@code
 * // Publish a message to 'chat' topic
 * var waitForPublish = client.publish("chat", "Hello out there!", "text-message");
 *
 * // Wait for reply
 * waitForPublish.waitForReply().thenAccept(replyArgs -> {
 *     var reply = (ResponseMessage) replyArgs[0];
 *     logger.info("Reply received: " + reply);
 * }).exceptionally(ex -> {
 *     logger.log(Level.WARNING, "Reply not received", ex);
 *     return null;
 * });
 * }</pre>
 *
 * @see IncomingMessage
 * @see WaitFor
 */
public class ResponseMessage {

    /**
     * A unique identifier for the response message, used to correlate
     * the response with a specific request.
     */
    private final String id;

    /**
     * The status of the request or operation. For instance, it could be:
     * <ul>
     *     <li>"success" if the requested action completed successfully.</li>
     *     <li>"failure" if an error or issue occurred.</li>
     *     <li>Custom values depending on the application’s logic.</li>
     * </ul>
     */
    private final String status;

    /**
     * The data associated with the response. This field can hold any type
     * of object, though it is often a {@link Map} or a {@link String}.
     * <p>
     * The content of this data is determined by the logic of the server and
     * the request being processed. It may contain additional information,
     * error messages, or results from a computation.
     * </p>
     */
    private final Object data;

    /**
     * Constructs a new {@code ResponseMessage} with the specified properties.
     * This constructor is private to enforce the use of {@link #from(Map)} or
     * {@link #from(Object)} factory methods for creating instances.
     *
     * @param id     the unique identifier for the response.
     * @param status the status indicating the outcome of the request.
     * @param data   the payload or content of the response.
     */
    private ResponseMessage(String id, String status, Object data) {
        this.id = id;
        this.status = status;
        this.data = data;
    }

    /**
     * Returns the unique identifier of the response message.
     *
     * @return the response message ID as a {@link String}.
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the status of the response, providing insight into whether
     * the request succeeded, failed, or encountered another state.
     *
     * @return the response status as a {@link String}.
     */
    public String getStatus() {
        return status;
    }

    /**
     * Returns the data associated with the response.
     * <p>
     * Depending on the message source, this could be a simple string message,
     * a structured object represented as a {@link Map}, or another type
     * of serialized data.
     * </p>
     *
     * @return the response data as an {@link Object}.
     */
    public Object getData() {
        return data;
    }

    /**
     * Creates a new {@code ResponseMessage} object from a given {@link Map} representation.
     *
     * @param message a {@link Map} containing the response message properties.
     * @return a new {@code ResponseMessage} instance constructed from the map.
     */
    public static ResponseMessage from(Map<String, Object> message) {
        String id = (String) message.get("id");
        String status = (String) message.get("status");
        Object data = message.get("data");
        return new ResponseMessage(id, status, data);
    }

    /**
     * Creates a {@code ResponseMessage} from a generic object. The object
     * must be a {@link Map} in order to be converted into a {@code ResponseMessage}.
     * <p>
     * If the provided object is not a {@link Map}, this method throws
     * an {@link IllegalArgumentException}.
     * </p>
     *
     * @param message an {@link Object} expected to be a {@link Map} of response properties.
     * @return a {@code ResponseMessage} instance.
     * @throws IllegalArgumentException if the object is not a {@link Map}.
     */
    @SuppressWarnings("unchecked")
    public static ResponseMessage from(Object message) {
        if (message instanceof Map) {
            return from((Map<String, Object>) message);
        } else {
            throw new IllegalArgumentException("Message must be a Map");
        }
    }

    /**
     * Returns a string representation of this {@code ResponseMessage}, including
     * the ID, status, and data fields. This is useful for debugging and logging.
     *
     * @return a {@link String} describing the response message.
     */
    @Override
    public String toString() {
        return "ResponseMessage{" + "id='" + id + '\'' + ", status='" + status + '\'' + ", data=" + data + '}';
    }
}
