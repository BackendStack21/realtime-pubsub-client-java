package de.backendstack21.realtime.pubsub;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a presence event message, which provides information about a client's
 * connection status and related metadata within a real-time application.
 * <p>
 * A presence event is typically emitted when a client connects to or disconnects
 * from a particular application. This message allows consumers to track
 * client presence, understand their permissions, and respond accordingly—such
 * as updating UI elements to show who is online, or adjusting available actions
 * based on the client's permissions.
 * </p>
 *
 * <h2>Key Attributes:</h2>
 * <ul>
 *     <li><b>Connection ID</b>: A unique identifier for the client's current connection.</li>
 *     <li><b>Subject</b>: An identifier (e.g., a username or user ID) representing the entity
 *         or user associated with the connection.</li>
 *     <li><b>Permissions</b>: A list of strings indicating what actions or operations the
 *         connected client is permitted to perform. This can help the server or other clients
 *         tailor the experience or restrict certain actions.</li>
 *     <li><b>Status</b>: The presence status of the client, represented as a {@link PresenceStatus}
 *         enum. Typically indicates whether the client is currently connected or disconnected.</li>
 * </ul>
 *
 * <h2>Data Structure Expected in IncomingMessage:</h2>
 * The {@link IncomingMessage} should contain a nested structure like:
 * <pre>{@code
 * {
 *   "data": {
 *     "client": {
 *       "connectionId": "abc123",
 *       "subject": "user42",
 *       "permissions": ["read", "write"]
 *     },
 *     "payload": {
 *       "status": "connected"
 *     }
 *   }
 * }
 * }</pre>
 * In this example:
 * <ul>
 *     <li>{@code connectionId} is a unique string for the client's connection.</li>
 *     <li>{@code subject} represents the user identity or subject associated with this connection.</li>
 *     <li>{@code permissions} is a list of capabilities granted to this client (optional).</li>
 *     <li>{@code status} indicates the client's presence state (e.g., "connected" or "disconnected").</li>
 * </ul>
 *
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * IncomingMessage incoming = ...; // Received from the Realtime Pub/Sub system
 * PresenceMessage presence = PresenceMessage.from(incoming);
 *
 * System.out.println("Client " + presence.getSubject() + " is " + presence.getStatus());
 * if (presence.getPermissions() != null) {
 *     System.out.println("Permissions: " + presence.getPermissions());
 * }
 * }</pre>
 *
 * <p>
 * The {@code PresenceMessage} constructor and the {@link #from(IncomingMessage)} factory method
 * perform strict validation on the message structure, throwing exceptions if expected
 * fields are missing or of invalid types.
 * </p>
 *
 * @see PresenceStatus
 * @see IncomingMessage
 */
public class PresenceMessage {

    /**
     * The unique identifier for the client's current connection.
     */
    private final String connectionId;

    /**
     * The subject associated with this connection, often representing a user or entity identifier.
     */
    private final String subject;

    /**
     * The list of permissions granted to the client. This may be {@code null} if no permissions are specified.
     */
    private final List<String> permissions;

    /**
     * The current presence status of the client (e.g., CONNECTED or DISCONNECTED).
     */
    private final PresenceStatus status;

    /**
     * Constructs a new PresenceMessage instance from an IncomingMessage.
     * <p>
     * This constructor extracts and validates all necessary fields from the provided
     * {@link IncomingMessage}. It ensures the data follows the expected structure:
     * <ul>
     *     <li>{@code data} must be a Map</li>
     *     <li>{@code data.client} must be a Map containing {@code connectionId} and {@code subject}</li>
     *     <li>{@code data.client.permissions} must be a List if present</li>
     *     <li>{@code data.payload} must be a Map containing a {@code status} field</li>
     * </ul>
     *
     * @param message the incoming message containing presence event data
     * @throws NullPointerException     if the message is null
     * @throws IllegalArgumentException if the message data is missing or malformed
     */
    private PresenceMessage(IncomingMessage message) {
        Objects.requireNonNull(message, "IncomingMessage cannot be null");

        Object dataObj = message.getData();
        if (!(dataObj instanceof Map<?, ?>)) {
            throw new IllegalArgumentException("message.data must be a Map");
        }
        @SuppressWarnings("unchecked") Map<String, Object> data = (Map<String, Object>) dataObj;

        Object clientObj = data.get("client");
        if (!(clientObj instanceof Map<?, ?>)) {
            throw new IllegalArgumentException("data.client must be a Map");
        }
        @SuppressWarnings("unchecked") Map<String, Object> client = (Map<String, Object>) clientObj;

        Object connectionIdObj = client.get("connectionId");
        if (!(connectionIdObj instanceof String)) {
            throw new IllegalArgumentException("client.connectionId must be a String");
        }
        this.connectionId = (String) connectionIdObj;

        Object subjectObj = client.get("subject");
        if (!(subjectObj instanceof String)) {
            throw new IllegalArgumentException("client.subject must be a String");
        }
        this.subject = (String) subjectObj;

        Object permissionsObj = client.get("permissions");
        if (permissionsObj != null && !(permissionsObj instanceof List<?>)) {
            throw new IllegalArgumentException("client.permissions must be a List");
        }
        @SuppressWarnings("unchecked") List<String> permissionsList = (List<String>) permissionsObj;
        this.permissions = permissionsList;

        Object payloadObj = data.get("payload");
        if (!(payloadObj instanceof Map<?, ?>)) {
            throw new IllegalArgumentException("data.payload must be a Map");
        }
        @SuppressWarnings("unchecked") Map<String, Object> payload = (Map<String, Object>) payloadObj;

        Object statusObj = payload.get("status");
        if (!(statusObj instanceof String statusStr)) {
            throw new IllegalArgumentException("payload.status must be a String");
        }

        this.status = switch (statusStr.toLowerCase()) {
            case "connected" -> PresenceStatus.CONNECTED;
            case "disconnected" -> PresenceStatus.DISCONNECTED;
            default -> throw new IllegalArgumentException("Unknown status: " + statusStr);
        };
    }

    /**
     * Returns the unique connection ID for this presence event.
     *
     * @return the connection ID as a String
     */
    public String getConnectionId() {
        return connectionId;
    }

    /**
     * Returns the subject (e.g., user or entity identifier) associated with this connection.
     *
     * @return the subject as a String
     */
    public String getSubject() {
        return subject;
    }

    /**
     * Returns the list of permissions assigned to the connected client, or {@code null}
     * if no permissions are set.
     *
     * @return a list of permission strings, or null if none are present
     */
    public List<String> getPermissions() {
        return permissions;
    }

    /**
     * Returns the current presence status of the client, indicating whether they are currently
     * connected or disconnected.
     *
     * @return the presence status as a {@link PresenceStatus} enum
     */
    public PresenceStatus getStatus() {
        return status;
    }

    /**
     * Creates a new PresenceMessage instance from an {@link IncomingMessage}.
     * This is a convenience method that wraps the private constructor.
     *
     * @param message the incoming message containing presence event data
     * @return a new PresenceMessage instance
     * @throws NullPointerException     if the message is null
     * @throws IllegalArgumentException if the message data is malformed or missing required fields
     */
    public static PresenceMessage from(IncomingMessage message) {
        return new PresenceMessage(message);
    }
}
