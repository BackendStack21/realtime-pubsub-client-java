package de.n21no.realtime.pubsub;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A message representing a presence event.
 */
public class PresenceMessage {

    private final String connectionId;
    private final String subject;
    private final List<String> permissions;
    private final PresenceStatus status;

    private PresenceMessage(IncomingMessage message) {
        // Validate that the message is not null
        Objects.requireNonNull(message, "IncomingMessage cannot be null");

        // Extract and validate the 'data' map
        Object dataObj = message.getData();
        if (!(dataObj instanceof Map<?, ?>)) {
            throw new IllegalArgumentException("message.data must be a Map");
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) dataObj;

        // Extract and validate the 'client' map
        Object clientObj = data.get("client");
        if (!(clientObj instanceof Map<?, ?>)) {
            throw new IllegalArgumentException("data.client must be a Map");
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> client = (Map<String, Object>) clientObj;

        // Extract and validate 'connectionId'
        Object connectionIdObj = client.get("connectionId");
        if (!(connectionIdObj instanceof String)) {
            throw new IllegalArgumentException("client.connectionId must be a String");
        }
        this.connectionId = (String) connectionIdObj;

        // Extract and validate 'subject'
        Object subjectObj = client.get("subject");
        if (!(subjectObj instanceof String)) {
            throw new IllegalArgumentException("client.subject must be a String");
        }
        this.subject = (String) subjectObj;

        // Extract and validate 'permissions'
        Object permissionsObj = client.get("permissions");
        if (permissionsObj != null && !(permissionsObj instanceof List<?>)) {
            throw new IllegalArgumentException("client.permissions must be a List");
        }
        @SuppressWarnings("unchecked")
        List<String> permissionsList = (List<String>) permissionsObj;
        this.permissions = permissionsList;

        // Extract and validate the 'payload' map
        Object payloadObj = data.get("payload");
        if (!(payloadObj instanceof Map<?, ?>)) {
            throw new IllegalArgumentException("data.payload must be a Map");
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (Map<String, Object>) payloadObj;

        // Extract and validate 'status'
        Object statusObj = payload.get("status");
        if (!(statusObj instanceof String statusStr)) {
            throw new IllegalArgumentException("payload.status must be a String");
        }

        // Map 'status' string to PresenceStatus enum
        this.status = switch (statusStr.toLowerCase()) {
            case "connected" -> PresenceStatus.CONNECTED;
            case "disconnected" -> PresenceStatus.DISCONNECTED;
            default -> throw new IllegalArgumentException("Unknown status: " + statusStr);
        };
    }

    public String getConnectionId() {
        return connectionId;
    }

    public String getSubject() {
        return subject;
    }

    public List<String> getPermissions() {
        return permissions;
    }

    public PresenceStatus getStatus() {
        return status;
    }

    public static PresenceMessage from(IncomingMessage message) {
        return new PresenceMessage(message);
    }
}