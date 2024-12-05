package de.backendstack21.realtime.pubsub;

import java.util.Map;

/**
 * Represents the connection information for a client.
 *
 * <h2>Typical Usage Example:</h2>
 * <pre>{@code
 * // Register event listener for session started
 * client.on("session.started", (Object... eventArgs) -> {
 *     logger.info("Session started: " + (ConnectionInfo) eventArgs[0]);
 *
 *     // ...
 * });
 * }</pre>
 */
public class ConnectionInfo {
    /**
     * A unique identifier for this connection. This ID typically remains constant
     * throughout the lifecycle of a single session or connection instance.
     */
    private final String id;

    /**
     * The application identifier associated with this connection. This value is often used
     * to distinguish one application's client connections from another's.
     */
    private final String appId;

    /**
     * The remote network address of the connected client (e.g., IP address).
     * This information can be vital for auditing, debugging, or policy enforcement.
     */
    private final String remoteAddress;

    /**
     * Constructs a new {@code ConnectionInfo} instance with the provided attributes.
     *
     * @param id            the unique identifier of the connection
     * @param appId         the application identifier associated with the connection
     * @param remoteAddress the remote address (e.g., IP address) of the connection
     */
    private ConnectionInfo(String id, String appId, String remoteAddress) {
        this.id = id;
        this.appId = appId;
        this.remoteAddress = remoteAddress;
    }

    /**
     * Retrieves the unique identifier of this connection.
     *
     * @return the connection ID as a {@link String}
     */
    public String getId() {
        return id;
    }

    /**
     * Retrieves the application ID associated with this connection.
     *
     * @return the application ID as a {@link String}
     */
    public String getAppId() {
        return appId;
    }

    /**
     * Retrieves the remote address of the client. This may be an IP address or
     * a domain name depending on the server's configuration.
     *
     * @return the remote address as a {@link String}
     */
    public String getRemoteAddress() {
        return remoteAddress;
    }

    /**
     * Constructs a new {@code ConnectionInfo} instance from a map of attributes.
     *
     * @param message a {@link Map} containing connection attributes
     * @return a new {@code ConnectionInfo} instance constructed from the provided attributes
     */
    public static ConnectionInfo from(Map<String, Object> message) {
        String id = (String) message.get("id");
        String appId = (String) message.get("appId");
        String remoteAddress = (String) message.get("remoteAddress");

        return new ConnectionInfo(id, appId, remoteAddress);
    }

    /**
     * Constructs a new {@code ConnectionInfo} instance from a generic object,
     * which must be a {@link Map} containing connection attributes.
     * <p>
     * If the provided object is not a {@link Map}, this method will throw
     * an {@link IllegalArgumentException}.
     * </p>
     *
     * @param message the object expected to be a {@link Map} of connection attributes
     * @return a new {@code ConnectionInfo} instance
     * @throws IllegalArgumentException if the provided object is not a {@link Map}
     */
    @SuppressWarnings("unchecked")
    public static ConnectionInfo from(Object message) {
        if (message instanceof Map) {
            return from((Map<String, Object>) message);
        } else {
            throw new IllegalArgumentException("Message must be a Map");
        }
    }

    /**
     * Returns a string representation of this {@code ConnectionInfo},
     * including the connection ID, application ID, and remote address.
     *
     * @return a string in the format {@code ConnectionInfo{id='...', appId='...', remoteAddress='...'}}
     */
    @Override
    public String toString() {
        return "ConnectionInfo{" + "id='" + id + '\'' + ", appId='" + appId + '\'' + ", remoteAddress='" + remoteAddress + '\'' + '}';
    }
}