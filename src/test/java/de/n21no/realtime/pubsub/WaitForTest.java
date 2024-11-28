package de.n21no.realtime.pubsub;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the WaitFor class.
 */
public class WaitForTest {

    private RealtimeClient clientMock;
    private WaitFor waitFor;
    private final String messageId = "testMessageId";

    @BeforeEach
    public void setUp() {
        clientMock = mock(RealtimeClient.class);
        waitFor = new WaitFor(clientMock, messageId);
    }

    @Test
    public void testWaitForAck_Success() throws Exception {
        // Arrange
        CompletableFuture<Object[]> future = new CompletableFuture<>();
        when(clientMock.waitFor("ack." + messageId, 5)).thenReturn(future);

        // Act
        CompletableFuture<Object[]> resultFuture = waitFor.waitForAck(5);

        // Simulate event emission
        Object[] ackData = new Object[]{"Acknowledgment Data"};
        future.complete(ackData);

        // Assert
        assertTrue(resultFuture.isDone());
        assertArrayEquals(ackData, resultFuture.get());
        verify(clientMock).waitFor("ack." + messageId, 5);
    }

    @Test
    public void testWaitForAck_Timeout() {
        // Arrange
        CompletableFuture<Object[]> future = new CompletableFuture<>();
        when(clientMock.waitFor("ack." + messageId, 2)).thenReturn(future);

        // Act
        CompletableFuture<Object[]> resultFuture = waitFor.waitForAck(2);

        // Simulate timeout
        future.completeExceptionally(new TimeoutException("Timeout waiting for event"));

        // Assert
        assertTrue(resultFuture.isCompletedExceptionally());
        try {
            resultFuture.get();
            fail("Expected a TimeoutException");
        } catch (Exception e) {
            assertInstanceOf(TimeoutException.class, e.getCause());
        }
        verify(clientMock).waitFor("ack." + messageId, 2);
    }

    @Test
    public void testWaitForReply_Success() throws Exception {
        // Arrange
        CompletableFuture<Object[]> future = new CompletableFuture<>();
        when(clientMock.waitFor("response." + messageId, 5)).thenReturn(future);

        // Act
        CompletableFuture<Object[]> resultFuture = waitFor.waitForReply(5);

        // Simulate event emission
        Object[] replyData = new Object[]{"Reply Data"};
        future.complete(replyData);

        // Assert
        assertTrue(resultFuture.isDone());
        assertArrayEquals(replyData, resultFuture.get());
        verify(clientMock).waitFor("response." + messageId, 5);
    }

    @Test
    public void testWaitForReply_Timeout() {
        // Arrange
        CompletableFuture<Object[]> future = new CompletableFuture<>();
        when(clientMock.waitFor("response." + messageId, 2)).thenReturn(future);

        // Act
        CompletableFuture<Object[]> resultFuture = waitFor.waitForReply(2);

        // Simulate timeout
        future.completeExceptionally(new TimeoutException("Timeout waiting for event"));

        // Assert
        assertTrue(resultFuture.isCompletedExceptionally());
        try {
            resultFuture.get();
            fail("Expected a TimeoutException");
        } catch (Exception e) {
            assertInstanceOf(TimeoutException.class, e.getCause());
        }
        verify(clientMock).waitFor("response." + messageId, 2);
    }
}