package de.n21no.realtime.pubsub;

import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the PresenceMessage class.
 */
public class PresenceMessageTest {

    @Test
    public void testGetConnectionId_ValidData() {
        // Arrange
        PresenceMessage presenceMessage = createPresenceMessage("conn123", "user@example.com", Arrays.asList("read", "write"), "connected");

        // Act
        String connectionId = presenceMessage.getConnectionId();

        // Assert
        assertEquals("conn123", connectionId);
    }

    @Test
    public void testGetConnectionId_MissingData() {
        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            createPresenceMessage(null, "user@example.com", Arrays.asList("read", "write"), "connected");
        });
        assertEquals("client.connectionId must be a String", exception.getMessage());
    }

    @Test
    public void testGetSubject_ValidData() {
        // Arrange
        PresenceMessage presenceMessage = createPresenceMessage("conn123", "user@example.com", Arrays.asList("read", "write"), "connected");

        // Act
        String subject = presenceMessage.getSubject();

        // Assert
        assertEquals("user@example.com", subject);
    }

    @Test
    public void testGetSubject_MissingData() {
        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            createPresenceMessage("conn123", null, Arrays.asList("read", "write"), "connected");
        });
        assertEquals("client.subject must be a String", exception.getMessage());
    }

    @Test
    public void testGetPermissions_ValidData() {
        // Arrange
        PresenceMessage presenceMessage = createPresenceMessage("conn123", "user@example.com", Arrays.asList("read", "write"), "connected");

        // Act
        List<String> permissions = presenceMessage.getPermissions();

        // Assert
        assertEquals(Arrays.asList("read", "write"), permissions);
    }

    @Test
    public void testGetPermissions_MissingData() {
        // Arrange
        PresenceMessage presenceMessage = createPresenceMessage("conn123", "user@example.com", null, "connected");

        // Act
        List<String> permissions = presenceMessage.getPermissions();

        // Assert
        assertNull(permissions);
    }

    @Test
    public void testGetStatus_ValidData() {
        // Arrange
        PresenceMessage presenceMessage = createPresenceMessage("conn123", "user@example.com", Arrays.asList("read", "write"), "connected");

        // Act
        PresenceStatus status = presenceMessage.getStatus();

        // Assert
        assertEquals(PresenceStatus.CONNECTED, status);
    }

    @Test
    public void testGetStatus_InvalidStatus() {
        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            createPresenceMessage("conn123", "user@example.com", Arrays.asList("read", "write"), "UNKNOWN_STATUS");
        });
        assertEquals("Unknown status: UNKNOWN_STATUS", exception.getMessage());
    }

    @Test
    public void testFrom_NullIncomingMessage() {
        // Act & Assert
        assertThrows(NullPointerException.class, () -> PresenceMessage.from(null));
    }

    // Helper method to create a PresenceMessage instance with provided data
    private PresenceMessage createPresenceMessage(String connectionId, String subject, List<String> permissions, String status) {
        // Build client map
        Map<String, Object> clientMap = new HashMap<>();
        if (connectionId != null) {
            clientMap.put("connectionId", connectionId);
        }
        if (subject != null) {
            clientMap.put("subject", subject);
        }
        if (permissions != null) {
            clientMap.put("permissions", permissions);
        }

        // Build payload map
        Map<String, Object> payloadMap = new HashMap<>();
        if (status != null) {
            payloadMap.put("status", status);
        }

        // Build data map
        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("client", clientMap);
        dataMap.put("payload", payloadMap);

        // Build message map
        Map<String, Object> messageMap = new HashMap<>();
        messageMap.put("topic", "presence");
        messageMap.put("messageType", "presence.status");
        messageMap.put("compression", false);
        messageMap.put("data", dataMap);

        // Create IncomingMessage
        IncomingMessage incomingMessage = IncomingMessage.from(messageMap);

        // Create and return PresenceMessage
        return PresenceMessage.from(incomingMessage);
    }
}