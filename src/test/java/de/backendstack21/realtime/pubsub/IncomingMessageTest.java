package de.backendstack21.realtime.pubsub;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the IncomingMessage class.
 */
public class IncomingMessageTest {

    @Test
    public void testFromMap_ValidInput() {
        // Arrange
        Map<String, Object> message = new HashMap<>();
        message.put("topic", "test.topic");
        message.put("messageType", "text-message");
        message.put("compression", true);
        message.put("data", "Sample Data");

        // Act
        IncomingMessage incomingMessage = IncomingMessage.from(message);

        // Assert
        assertNotNull(incomingMessage);
        assertEquals("test.topic", incomingMessage.getTopic());
        assertEquals("text-message", incomingMessage.getMessageType());
        assertTrue(incomingMessage.getCompression());
        assertEquals("Sample Data", incomingMessage.getData());
    }

    @Test
    public void testFromMap_MissingFields() {
        // Arrange
        Map<String, Object> message = new HashMap<>();
        message.put("topic", "test.topic");

        // Act
        IncomingMessage incomingMessage = IncomingMessage.from(message);

        // Assert
        assertNotNull(incomingMessage);
        assertEquals("test.topic", incomingMessage.getTopic());
        assertNull(incomingMessage.getMessageType());
        assertFalse(incomingMessage.getCompression());
        assertNull(incomingMessage.getData());
    }

    @Test
    public void testFromObject_ValidMap() {
        // Arrange
        Map<String, Object> message = new HashMap<>();
        message.put("topic", "chat.topic");
        message.put("messageType", "image-message");
        message.put("compression", false);
        message.put("data", Map.of("key", "value"));

        // Act
        IncomingMessage incomingMessage = IncomingMessage.from((Object) message);

        // Assert
        assertNotNull(incomingMessage);
        assertEquals("chat.topic", incomingMessage.getTopic());
        assertEquals("image-message", incomingMessage.getMessageType());
        assertFalse(incomingMessage.getCompression());
        assertEquals(Map.of("key", "value"), incomingMessage.getData());
    }

    @Test
    public void testFromObject_InvalidType() {
        // Arrange
        String invalidMessage = "Invalid Input";

        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            IncomingMessage.from(invalidMessage);
        });

        assertEquals("Message must be a Map", exception.getMessage());
    }

    @Test
    public void testToString() {
        // Arrange
        Map<String, Object> message = new HashMap<>();
        message.put("topic", "test.topic");
        message.put("messageType", "text-message");
        message.put("compression", true);
        message.put("data", "Sample Data");

        IncomingMessage incomingMessage = IncomingMessage.from(message);

        // Act
        String result = incomingMessage.toString();

        // Assert
        assertEquals("IncomingMessage{topic='test.topic', messageType='text-message', compression=true, data=Sample Data}", result);
    }
}