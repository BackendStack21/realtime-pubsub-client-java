package realtime.pubsub;

import jakarta.websocket.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.net.URI;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the RealtimeClient class.
 */
public class RealtimeClientTest {

    private RealtimeClient client;
    private RealtimeClientConfig config;

    // Mocks
    private WebSocketContainer containerMock;
    private Session sessionMock;
    private RemoteEndpoint.Async asyncRemoteMock;

    @BeforeEach
    public void setUp() throws Exception {
        // Mock the WebSocketContainer and Session
        containerMock = mock(WebSocketContainer.class);
        sessionMock = mock(Session.class);
        asyncRemoteMock = mock(RemoteEndpoint.Async.class);

        when(sessionMock.getAsyncRemote()).thenReturn(asyncRemoteMock);
        when(sessionMock.isOpen()).thenReturn(true);

        // Prepare configuration
        RealtimeClientConfig.WebSocketUrlProvider urlProvider = () -> "ws://localhost:8080/websocket";
        config = new RealtimeClientConfig(urlProvider);

        // Initialize the client with a custom container
        client = new RealtimeClient(config) {
            @Override
            protected WebSocketContainer createWebSocketContainer() {
                return containerMock;
            }
        };

        // Mock the connection behavior
        doAnswer(invocation -> {
            Endpoint endpoint = invocation.getArgument(0);
            ClientEndpointConfig clientConfig = invocation.getArgument(1);
            endpoint.onOpen(sessionMock, clientConfig);
            return sessionMock;
        }).when(containerMock).connectToServer(any(Endpoint.class), any(ClientEndpointConfig.class), any(URI.class));
    }

    @Test
    public void testGetExecutorService() {
        // Act
        ExecutorService executorService = client.getExecutorService();

        // Assert
        assertNotNull(executorService);
        assertInstanceOf(ThreadPoolExecutor.class, executorService);
    }

    @Test
    public void testConnect() throws Exception {
        // Act
        client.connect();

        // Allow time for the async connection to be established
        TimeUnit.MILLISECONDS.sleep(100);

        // Assert
        verify(containerMock, times(1)).connectToServer(any(Endpoint.class), any(ClientEndpointConfig.class), any(URI.class));
        assertTrue(client.isConnected());
    }

    @Test
    public void testDisconnect() throws Exception {
        // Arrange
        client.connect();
        TimeUnit.MILLISECONDS.sleep(100);

        // Act
        client.disconnect();

        // Assert
        assertFalse(client.isConnected());
    }

    @Test
    public void testPublish() throws Exception {
        // Arrange
        client.connect();
        TimeUnit.MILLISECONDS.sleep(100);

        String topic = "test.topic";
        String payload = "Test Message";
        String messageType = "broadcast";
        boolean compress = false;
        String messageId = "messageId123";

        // Act
        WaitFor waitFor = client.publish(topic, payload, messageType, compress, messageId);

        // Assert
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(asyncRemoteMock, times(1)).sendText(messageCaptor.capture());

        String sentMessage = messageCaptor.getValue();
        assertNotNull(sentMessage);

        // Parse the sent message
        Map<String, Object> sentMessageMap = client.jsonToMap(sentMessage);
        assertEquals("publish", sentMessageMap.get("type"));

        Map<String, Object> dataMap = safeCastToMap(sentMessageMap.get("data"));
        assertNotNull(dataMap);
        assertEquals(topic, dataMap.get("topic"));
        assertEquals(payload, dataMap.get("payload"));
        assertEquals(messageType, dataMap.get("messageType"));
        assertEquals(compress, dataMap.get("compress"));
        assertEquals(messageId, dataMap.get("id"));

        assertNotNull(waitFor);
    }

    @Test
    public void testSend() throws Exception {
        // Arrange
        client.connect();
        TimeUnit.MILLISECONDS.sleep(100);

        String payload = "Test Message";
        String messageType = "direct";
        boolean compress = false;
        String messageId = "messageId123";

        // Act
        WaitFor waitFor = client.send(payload, messageType, compress, messageId);

        // Assert
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(asyncRemoteMock, times(1)).sendText(messageCaptor.capture());

        String sentMessage = messageCaptor.getValue();
        assertNotNull(sentMessage);

        // Parse the sent message
        Map<String, Object> sentMessageMap = client.jsonToMap(sentMessage);
        assertEquals("message", sentMessageMap.get("type"));

        Map<String, Object> dataMap = safeCastToMap(sentMessageMap.get("data"));
        assertNotNull(dataMap);
        assertEquals(payload, dataMap.get("payload"));
        assertEquals(messageType, dataMap.get("messageType"));
        assertEquals(compress, dataMap.get("compress"));
        assertEquals(messageId, dataMap.get("id"));

        assertNotNull(waitFor);
    }

    @Test
    public void testSubscribeRemoteTopic() throws Exception {
        // Arrange
        client.connect();
        TimeUnit.MILLISECONDS.sleep(100);

        String topic = "test.topic";

        // Act
        client.subscribeRemoteTopic(topic);

        // Assert
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(asyncRemoteMock, times(1)).sendText(messageCaptor.capture());

        String sentMessage = messageCaptor.getValue();
        assertNotNull(sentMessage);

        // Parse the sent message
        Map<String, Object> sentMessageMap = client.jsonToMap(sentMessage);
        assertEquals("subscribe", sentMessageMap.get("type"));

        Map<String, Object> dataMap = safeCastToMap(sentMessageMap.get("data"));
        assertNotNull(dataMap);
        assertEquals(topic, dataMap.get("topic"));
    }

    @Test
    public void testUnsubscribeRemoteTopic() throws Exception {
        // Arrange
        client.connect();
        TimeUnit.MILLISECONDS.sleep(100);

        String topic = "test.topic";
        client.subscribeRemoteTopic(topic);

        // Act
        client.unsubscribeRemoteTopic(topic);

        // Assert
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        // Called twice: once for subscribe and once for unsubscribe
        verify(asyncRemoteMock, times(2)).sendText(messageCaptor.capture());

        List<String> sentMessages = messageCaptor.getAllValues();
        String unsubscribeMessage = sentMessages.get(1);

        // Parse the sent message
        Map<String, Object> sentMessageMap = client.jsonToMap(unsubscribeMessage);
        assertEquals("unsubscribe", sentMessageMap.get("type"));

        Map<String, Object> dataMap = safeCastToMap(sentMessageMap.get("data"));
        assertNotNull(dataMap);
        assertEquals(topic, dataMap.get("topic"));
    }

    @Test
    public void testWaitFor() throws Exception {
        // Arrange
        String eventName = "test.event";
        int timeoutSeconds = 5;

        // Act
        CompletableFuture<Object[]> future = client.waitFor(eventName, timeoutSeconds);

        // Simulate event emission
        Object[] eventData = new Object[]{"Event Data"};
        client.emit(eventName, eventData);

        // Assert
        assertTrue(future.isDone());
        assertArrayEquals(eventData, future.get());
    }

    @Test
    public void testWaitFor_Timeout() throws Exception {
        // Arrange
        String eventName = "test.event.timeout";
        int timeoutSeconds = 1;

        // Act
        CompletableFuture<Object[]> future = client.waitFor(eventName, timeoutSeconds);

        // Wait longer than the timeout
        TimeUnit.SECONDS.sleep(2);

        // Assert
        assertTrue(future.isCompletedExceptionally());
        try {
            future.get();
            fail("Expected a TimeoutException");
        } catch (ExecutionException e) {
            assertInstanceOf(TimeoutException.class, e.getCause());
        }
    }

    @Test
    public void testReplyFunction() throws Exception {
        // Arrange
        client.connect();
        TimeUnit.MILLISECONDS.sleep(100);

        Map<String, Object> messageEvent = new HashMap<>();
        Map<String, Object> dataMap = new HashMap<>();
        Map<String, Object> clientInfo = new HashMap<>();
        clientInfo.put("connectionId", "conn123");
        dataMap.put("client", clientInfo);
        dataMap.put("id", "msg123");
        messageEvent.put("data", dataMap);
        messageEvent.put("compression", false);

        var replyFn = client.createReplyFunction(IncomingMessage.from(messageEvent));

        // Act
        WaitFor waitFor = replyFn.reply("Reply Data", "ok", false);

        // Assert
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(asyncRemoteMock, times(1)).sendText(messageCaptor.capture());

        String sentMessage = messageCaptor.getValue();
        assertNotNull(sentMessage);

        // Parse the sent message
        Map<String, Object> sentMessageMap = client.jsonToMap(sentMessage);
        assertEquals("publish", sentMessageMap.get("type"));

        Map<String, Object> sentDataMap = safeCastToMap(sentMessageMap.get("data"));
        assertNotNull(sentDataMap);
        assertEquals("priv/conn123", sentDataMap.get("topic"));
        assertEquals("response", sentDataMap.get("messageType"));

        Map<String, Object> payload = safeCastToMap(sentDataMap.get("payload"));
        assertNotNull(payload);
        assertEquals("Reply Data", payload.get("data"));
        assertEquals("ok", payload.get("status"));
        assertEquals("msg123", payload.get("id"));

        assertNotNull(waitFor);
    }

    // Helper method to safely cast to Map<String, Object>
    @SuppressWarnings("unchecked")
    private Map<String, Object> safeCastToMap(Object obj) {
        if (obj instanceof Map<?, ?>) {
            return (Map<String, Object>) obj;
        } else {
            fail("Expected a Map but found: " + (obj != null ? obj.getClass() : "null"));
            return null;
        }
    }
}