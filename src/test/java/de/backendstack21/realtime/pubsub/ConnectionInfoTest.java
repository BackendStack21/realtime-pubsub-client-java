package de.backendstack21.realtime.pubsub;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the ConnectionInfo class.
 */
public class ConnectionInfoTest {

    @Test
    public void testFromMap_ValidInput() {
        // Arrange
        Map<String, Object> message = new HashMap<>();
        message.put("id", "conn123");
        message.put("appId", "app456");
        message.put("remoteAddress", "192.168.1.1");

        // Act
        ConnectionInfo connectionInfo = ConnectionInfo.from(message);

        // Assert
        assertNotNull(connectionInfo);
        assertEquals("conn123", connectionInfo.getId());
        assertEquals("app456", connectionInfo.getAppId());
        assertEquals("192.168.1.1", connectionInfo.getRemoteAddress());
    }

    @Test
    public void testFromMap_MissingFields() {
        // Arrange
        Map<String, Object> message = new HashMap<>();
        message.put("id", "conn123");

        // Act
        ConnectionInfo connectionInfo = ConnectionInfo.from(message);

        // Assert
        assertNotNull(connectionInfo);
        assertEquals("conn123", connectionInfo.getId());
        assertNull(connectionInfo.getAppId());
        assertNull(connectionInfo.getRemoteAddress());
    }

    @Test
    public void testFromObject_ValidMap() {
        // Arrange
        Map<String, Object> message = new HashMap<>();
        message.put("id", "conn789");
        message.put("appId", "app987");
        message.put("remoteAddress", "10.0.0.1");

        // Act
        ConnectionInfo connectionInfo = ConnectionInfo.from((Object) message);

        // Assert
        assertNotNull(connectionInfo);
        assertEquals("conn789", connectionInfo.getId());
        assertEquals("app987", connectionInfo.getAppId());
        assertEquals("10.0.0.1", connectionInfo.getRemoteAddress());
    }

    @Test
    public void testFromObject_InvalidType() {
        // Arrange
        String invalidMessage = "Invalid Input";

        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            ConnectionInfo.from(invalidMessage);
        });

        assertEquals("Message must be a Map", exception.getMessage());
    }

    @Test
    public void testToString() {
        // Arrange
        Map<String, Object> message = new HashMap<>();
        message.put("id", "conn123");
        message.put("appId", "app456");
        message.put("remoteAddress", "192.168.1.1");

        ConnectionInfo connectionInfo = ConnectionInfo.from(message);

        // Act
        String result = connectionInfo.toString();

        // Assert
        assertEquals("ConnectionInfo{id='conn123', appId='app456', remoteAddress='192.168.1.1'}", result);
    }
}