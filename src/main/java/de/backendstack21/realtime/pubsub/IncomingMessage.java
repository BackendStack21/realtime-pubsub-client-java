package de.backendstack21.realtime.pubsub;

import java.util.Map;

/**
 * Represents an incoming topic message.
 * <p>
 * This class encapsulates the details of a message received on a specific topic,
 * including its type, payload, and optional compression flag.
 * </p>
 *
 * <h2>Key Features:</h2>
 * <ul>
 *     <li>Stores information about the topic, message type, compression status, and payload.</li>
 *     <li>Supports creation from a {@link Map} representation or a generic object.</li>
 *     <li>Provides getters for accessing message details.</li>
 * </ul>
 */
public class IncomingMessage {

    /**
     * The topic on which the message was published.
     */
    private final String topic;

    /**
     * The type of the message. Used to identify the message format or purpose.
     */
    private final String messageType;

    /**
     * Whether the message data is compressed. Defaults to {@code false} if not specified.
     */
    private final Boolean compression;

    /**
     * The actual message payload. Can be String or Map.
     */
    private final Object data;

    /**
     * Private constructor for creating an IncomingMessage object with the specified properties.
     *
     * @param topic       the topic associated with the message.
     * @param messageType the type or purpose of the message.
     * @param compression a flag indicating if the message is compressed (optional).
     * @param data        the payload or data of the message.
     */
    private IncomingMessage(String topic, String messageType, Boolean compression, Object data) {
        this.topic = topic;
        this.messageType = messageType;
        this.compression = compression;
        this.data = data;
    }

    /**
     * Returns the topic on which the message was published.
     *
     * @return the topic as a {@link String}.
     */
    public String getTopic() {
        return topic;
    }

    /**
     * Returns the type of the message.
     *
     * @return the message type as a {@link String}.
     */
    public String getMessageType() {
        return messageType;
    }

    /**
     * Returns whether the message data is compressed.
     * Defaults to {@code false} if not explicitly specified.
     *
     * @return a {@link Boolean} indicating if the data is compressed.
     */
    public Boolean getCompression() {
        return compression;
    }

    /**
     * Returns the message data.
     *
     * @return the payload as an {@link Object}.
     */
    public Object getData() {
        return data;
    }

    /**
     * Creates an {@link IncomingMessage} object from a {@link Map} representation.
     * The map is expected to contain the following keys:
     * <ul>
     *     <li>{@code "topic"}: The topic of the message (required).</li>
     *     <li>{@code "messageType"}: The type of the message (required).</li>
     *     <li>{@code "compression"}: A flag indicating compression (optional, defaults to {@code false}).</li>
     *     <li>{@code "data"}: The message payload (required).</li>
     * </ul>
     *
     * @param message a {@link Map} containing the message properties.
     * @return an {@link IncomingMessage} object constructed from the map.
     */
    public static IncomingMessage from(Map<String, Object> message) {
        String topic = (String) message.get("topic");
        String messageType = (String) message.get("messageType");
        Boolean compression = (Boolean) message.getOrDefault("compression", false);
        Object data = message.get("data");

        return new IncomingMessage(topic, messageType, compression, data);
    }

    /**
     * Creates an {@link IncomingMessage} object from a generic object.
     * The object must be a {@link Map}, or an {@link IllegalArgumentException} will be thrown.
     *
     * @param message an {@link Object}, expected to be a {@link Map} representation of a message.
     * @return an {@link IncomingMessage} object constructed from the object.
     * @throws IllegalArgumentException if the input object is not a {@link Map}.
     */
    @SuppressWarnings("unchecked")
    public static IncomingMessage from(Object message) {
        if (message instanceof Map) {
            return from((Map<String, Object>) message);
        } else {
            throw new IllegalArgumentException("Message must be a Map");
        }
    }

    /**
     * Returns a string representation of the {@link IncomingMessage}.
     * The string includes the topic, message type, compression status, and payload.
     *
     * @return a {@link String} representing the message details.
     */
    @Override
    public String toString() {
        return "IncomingMessage{" + "topic='" + topic + '\'' + ", messageType='" + messageType + '\'' + ", compression=" + compression + ", data=" + data + '}';
    }
}
