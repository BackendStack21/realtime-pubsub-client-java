package de.n21no.realtime.pubsub;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the ResponseMessage class.
 */
public class ResponseMessageTest {

    @Test
    public void testFromMap_ValidInput() {
        // Arrange
        Map<String, Object> message = new HashMap<>();
        message.put("id", "response123");
        message.put("status", "ok");
        message.put("data", "Sample Response Data");

        // Act
        ResponseMessage responseMessage = ResponseMessage.from(message);

        // Assert
        assertNotNull(responseMessage);
        assertEquals("response123", responseMessage.getId());
        assertEquals("ok", responseMessage.getStatus());
        assertEquals("Sample Response Data", responseMessage.getData());
    }

    @Test
    public void testFromMap_MissingFields() {
        // Arrange
        Map<String, Object> message = new HashMap<>();
        message.put("id", "response123");

        // Act
        ResponseMessage responseMessage = ResponseMessage.from(message);

        // Assert
        assertNotNull(responseMessage);
        assertEquals("response123", responseMessage.getId());
        assertNull(responseMessage.getStatus());
        assertNull(responseMessage.getData());
    }

    @Test
    public void testFromObject_ValidMap() {
        // Arrange
        Map<String, Object> message = new HashMap<>();
        message.put("id", "response456");
        message.put("status", "error");
        message.put("data", Map.of("key", "value"));

        // Act
        ResponseMessage responseMessage = ResponseMessage.from((Object) message);

        // Assert
        assertNotNull(responseMessage);
        assertEquals("response456", responseMessage.getId());
        assertEquals("error", responseMessage.getStatus());
        assertEquals(Map.of("key", "value"), responseMessage.getData());
    }

    @Test
    public void testFromObject_InvalidType() {
        // Arrange
        String invalidMessage = "Invalid Input";

        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            ResponseMessage.from(invalidMessage);
        });

        assertEquals("Message must be a Map", exception.getMessage());
    }

    @Test
    public void testToString() {
        // Arrange
        Map<String, Object> message = new HashMap<>();
        message.put("id", "response789");
        message.put("status", "ok");
        message.put("data", "Response Data");

        ResponseMessage responseMessage = ResponseMessage.from(message);

        // Act
        String result = responseMessage.toString();

        // Assert
        assertEquals("ResponseMessage{id='response789', status='ok', data=Response Data}", result);
    }
}