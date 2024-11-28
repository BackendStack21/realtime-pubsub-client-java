package de.n21no.realtime.pubsub;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.websocket.*;

/**
 * RealtimeClient class encapsulates WebSocket connection, subscription, and message handling.
 * The RealtimeClient is the core class for interacting with the Realtime Pub/Sub service.
 * It manages the WebSocket connection, handles message publishing and subscribing, and
 * provides mechanisms to wait for acknowledgments and replies.
 */
public class RealtimeClient extends EventEmitter {
    /**
     * Logger for the RealtimeClient class.
     */
    private static final Logger logger = Logger.getLogger(RealtimeClient.class.getName());

    private volatile Session session;
    private final RealtimeClientConfig config;
    private final ExecutorService executorService;
    private final Set<String> subscribedTopics;
    private final AtomicBoolean isConnecting;
    private final ObjectMapper objectMapper;
    private final ScheduledExecutorService scheduler;
    private final WebSocketContainer container;

    /**
     * Initializes a new instance of the RealtimeClient class.
     *
     * @param config The client configuration options encapsulated in RealtimeClientConfig.
     */
    public RealtimeClient(RealtimeClientConfig config) {
        super();
        this.config = config;
        this.executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        this.scheduler = Executors.newScheduledThreadPool(1);
        this.subscribedTopics = ConcurrentHashMap.newKeySet();
        this.isConnecting = new AtomicBoolean(false);
        this.objectMapper = new ObjectMapper();
        this.container = createWebSocketContainer();

        // Configure WebSocket container properties if needed
        this.configureWebSocketContainer();

        // Register event listeners for specific events
        this.on("priv/acks.ack", this::onAck);
        this.on("*.response", this::onResponse);
        this.on("main.welcome", this::onWelcome);
    }

    /**
     * Factory method to create a WebSocketContainer.
     * This can be overridden for testing purposes.
     *
     * @return A new instance of WebSocketContainer.
     */
    protected WebSocketContainer createWebSocketContainer() {
        logger.info("Creating WebSocketContainer...");
        WebSocketContainer container = ContainerProvider.getWebSocketContainer();
        if (container == null) {
            logger.severe("Failed to create WebSocketContainer.");
        } else {
            logger.info("WebSocketContainer created successfully.");
        }
        return container;
    }

    private void configureWebSocketContainer() {
        // Set default max text message buffer size
        container.setDefaultMaxTextMessageBufferSize(config.getMaxTextMessageBufferSize());

        // Set default max binary message buffer size
        container.setDefaultMaxBinaryMessageBufferSize(config.getMaxBinaryMessageBufferSize());
    }

    /**
     * Creates a reply function for the given message.
     *
     * @param message The incoming message to which the reply is responding.
     * @return A ReplyFunction that can be used to send a reply message.
     */
    public ReplyFunction createReplyFunction(IncomingMessage message) {
        return (data, status, compress) -> {
            Map<String, Object> messageData = safeCastToMap(message.getData());
            if (messageData == null) {
                throw new IllegalArgumentException("Message data is not available");
            }
            Map<String, Object> clientInfo = safeCastToMap(messageData.get("client"));
            if (clientInfo == null) {
                throw new IllegalArgumentException("Client info is not available in the message");
            }
            String connectionId = (String) clientInfo.get("connectionId");
            if (connectionId == null) {
                throw new IllegalArgumentException("Connection ID is not available in the message");
            }
            String messageId = (String) messageData.get("id");

            Map<String, Object> payload = new HashMap<>();
            payload.put("data", data);
            payload.put("status", status != null ? status : "ok");
            payload.put("id", messageId);

            return publish("priv/" + connectionId, payload, "response", compress);
        };
    }

    /**
     * Gets the ExecutorService used by the RealtimeClient. A fixed thread pool (size = number of processors).
     *
     * @return The ExecutorService instance.
     */
    public ExecutorService getExecutorService() {
        return executorService;
    }

    private void onAck(Object[] args) {
        if (args.length == 0) return;
        var message = (IncomingMessage) (args[0]);
        if (message == null) return;

        Map<String, Object> data = safeCastToMap(message.getData());
        if (data == null) return;

        Object ackData = data.get("data");
        if (ackData != null) {
            logger.log(Level.FINE, "Received ack: {0}", ackData);
            this.emit("ack." + ackData);
        }
    }

    private void onResponse(Object[] args) {
        if (args.length == 0) return;
        var message = (IncomingMessage) (args[0]);
        if (message == null) return;

        String topic = (String) message.getTopic();
        if (topic != null && topic.startsWith("priv/")) {
            Map<String, Object> data = safeCastToMap(message.getData());
            if (data == null) return;

            Map<String, Object> payload = safeCastToMap(data.get("payload"));
            if (payload == null) return;

            String id = (String) payload.get("id");
            if (id != null) {
                logger.log(Level.FINE, "Received response for topic {0}: {1}", new Object[]{topic, data});
                this.emit("response." + id, ResponseMessage.from(payload));
            }
        }
    }

    private void onWelcome(Object[] args) {
        if (args.length == 0) return;
        var message = (IncomingMessage) (args[0]);
        if (message == null) return;

        Map<String, Object> data = safeCastToMap(message.getData());
        if (data == null) return;

        Map<String, Object> connection = safeCastToMap(data.get("connection"));
        if (connection != null) {
            logger.log(Level.INFO, "Session started, connection details: {0}", connection);
            this.emit("session.started", ConnectionInfo.from(connection));
        }
    }

    /**
     * Establishes a connection to the WebSocket server.
     */
    public void connect() {
        if (!isConnecting.compareAndSet(false, true)) {
            logger.warning("Already in the process of connecting. Ignoring new connection attempt.");
            return;
        }

        executorService.submit(() -> {
            int backoff = 1;
            int maxBackoff = config.getMaxReconnectBackoffSeconds();

            while (!Thread.currentThread().isInterrupted()) {
                try {
                    String wsUrl = getWebSocketUrl();

                    if (wsUrl == null || wsUrl.isEmpty()) {
                        throw new IllegalArgumentException("WebSocket URL is not provided");
                    }

                    // Mask sensitive information in the URL for logging
                    logger.log(Level.INFO, "Connecting to WebSocket URL: {0}", maskUrl(wsUrl));

                    final ClientEndpointConfig clientConfig = ClientEndpointConfig.Builder.create().build();
                    session = container.connectToServer(new WSEndpoint(), clientConfig, new URI(wsUrl));

                    // Reset backoff on successful connection
                    backoff = 1;
                    break;
                } catch (Exception e) {
                    handleError(e);
                }

                logger.log(Level.WARNING, "Retrying connection in {0} seconds", backoff);
                try {
                    TimeUnit.SECONDS.sleep(backoff);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
                backoff = Math.min(maxBackoff, backoff * 2);
            }
            isConnecting.set(false);
        });
    }

    /**
     * Disconnects from the WebSocket server.
     */
    public void disconnect() {
        if (session != null && session.isOpen()) {
            try {
                logger.info("Disconnecting from WebSocket...");
                session.close(new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, "Normal closure"));
            } catch (IOException e) {
                handleError(e);
            } finally {
                session = null;
            }
        }
    }

    /**
     * Checks if the client is currently connected.
     *
     * @return True if connected, false otherwise.
     */
    public boolean isConnected() {
        return session != null && session.isOpen();
    }

    /**
     * Publishes a message to a specified topic.
     *
     * @param topic
     * @param payload
     * @param messageType
     * @return
     * @throws Exception
     */
    public WaitFor publish(String topic, Object payload, String messageType) throws Exception {
        return publish(topic, payload, messageType, false, null);
    }

    /**
     * Publishes a message to a specified topic.
     *
     * @param topic       The topic to publish the message to.
     * @param payload     The message payload.
     * @param messageType The type of message being published.
     * @param compress    Whether to compress the message payload.
     * @return A WaitFor instance to wait for acknowledgments or replies.
     */
    public WaitFor publish(String topic, Object payload, String messageType, boolean compress) throws Exception {
        return publish(topic, payload, messageType, compress, null);
    }

    /**
     * Publishes a message to a specified topic.
     *
     * @param topic       The topic to publish the message to.
     * @param payload     The message payload.
     * @param messageType The type of message being published.
     * @param compress    Whether to compress the message payload.
     * @param messageId   The unique identifier for the message.
     * @return A WaitFor instance to wait for acknowledgments or replies.
     */
    public WaitFor publish(String topic, Object payload, String messageType, boolean compress, String messageId) throws Exception {
        if (session == null || !session.isOpen()) {
            logger.severe("Attempted to publish without an active WebSocket connection.");
            throw new IllegalStateException("WebSocket connection is not established");
        }

        if (messageId == null || messageId.isEmpty()) {
            messageId = getRandomId();
        }

        Map<String, Object> messageMap = new HashMap<>();
        messageMap.put("type", "publish");
        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("topic", topic);
        dataMap.put("messageType", messageType);
        dataMap.put("compress", compress);
        dataMap.put("payload", payload);
        dataMap.put("id", messageId);
        messageMap.put("data", dataMap);

        String message = objectToJson(messageMap);
        logger.log(Level.FINE, "Publishing message to topic {0}: {1}", new Object[]{topic, message});
        session.getAsyncRemote().sendText(message);

        return new WaitFor(this, messageId);
    }

    /**
     * Sends a message directly to the server.
     *
     * @param payload     The message payload.
     * @param messageType The type of message being sent.
     * @return A WaitFor instance to wait for acknowledgments or replies.
     */
    public WaitFor send(Object payload, String messageType) throws Exception {
        return send(payload, messageType, false, null);
    }

    /**
     * Sends a message directly to the server.
     *
     * @param payload
     * @param messageType
     * @param compress
     * @return
     * @throws Exception
     */
    public WaitFor send(Object payload, String messageType, Boolean compress) throws Exception {
        return send(payload, messageType, compress, null);
    }

    /**
     * Sends a message directly to the server.
     *
     * @param payload     The message payload.
     * @param messageType The type of message being sent.
     * @param compress    Whether to compress the message payload.
     * @param messageId   The unique identifier for the message.
     * @return A WaitFor instance to wait for acknowledgments or replies.
     */
    public WaitFor send(Object payload, String messageType, boolean compress, String messageId) throws Exception {
        if (session == null || !session.isOpen()) {
            logger.severe("Attempted to send without an active WebSocket connection.");
            throw new IllegalStateException("WebSocket connection is not established");
        }

        if (messageId == null || messageId.isEmpty()) {
            messageId = getRandomId();
        }

        Map<String, Object> messageMap = new HashMap<>();
        messageMap.put("type", "message");
        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("messageType", messageType);
        dataMap.put("compress", compress);
        dataMap.put("payload", payload);
        dataMap.put("id", messageId);
        messageMap.put("data", dataMap);

        String message = objectToJson(messageMap);
        logger.log(Level.FINE, "Sending message: {0}", payload);
        session.getAsyncRemote().sendText(message);

        return new WaitFor(this, messageId);
    }

    /**
     * Subscribes to a remote topic to receive messages.
     *
     * @param topic The topic to subscribe to.
     */
    public void subscribeRemoteTopic(String topic) throws Exception {
        if (session == null || !session.isOpen()) {
            logger.severe("Attempted to subscribe without an active WebSocket connection.");
            throw new IllegalStateException("WebSocket connection is not established");
        }

        subscribedTopics.add(topic);

        Map<String, Object> messageMap = new HashMap<>();
        messageMap.put("type", "subscribe");
        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("topic", topic);
        messageMap.put("data", dataMap);

        String message = objectToJson(messageMap);
        logger.log(Level.INFO, "Subscribing to topic: {0}", message);
        session.getAsyncRemote().sendText(message);
    }

    /**
     * Unsubscribes from a previously subscribed topic.
     *
     * @param topic The topic to unsubscribe from.
     */
    public void unsubscribeRemoteTopic(String topic) throws Exception {
        if (session == null || !session.isOpen()) {
            logger.severe("Attempted to unsubscribe without an active WebSocket connection.");
            throw new IllegalStateException("WebSocket connection is not established");
        }

        subscribedTopics.remove(topic);

        Map<String, Object> messageMap = new HashMap<>();
        messageMap.put("type", "unsubscribe");
        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("topic", topic);
        messageMap.put("data", dataMap);

        String message = objectToJson(messageMap);
        logger.log(Level.INFO, "Unsubscribing from topic: {0}", topic);
        session.getAsyncRemote().sendText(message);
    }

    /**
     * Waits for a specific event to occur within a default timeout period of 30 seconds.
     *
     * @param eventName The name of the event to wait for.
     * @return A CompletableFuture that completes with the event data.
     */
    public CompletableFuture<Object[]> waitFor(String eventName) {
        return waitFor(eventName, 30);
    }

    /**
     * Waits for a specific event to occur within a timeout period.
     *
     * @param eventName      The name of the event to wait for.
     * @param timeoutSeconds The maximum time to wait in seconds.
     * @return A CompletableFuture that completes with the event data.
     */
    public CompletableFuture<Object[]> waitFor(String eventName, int timeoutSeconds) {
        CompletableFuture<Object[]> future = new CompletableFuture<>();

        EventListener listener = future::complete;

        this.on(eventName, listener);

        ScheduledFuture<?> timeoutFuture = scheduler.schedule(() -> {
            if (!future.isDone()) {
                TimeoutException timeoutException = new TimeoutException("Timeout waiting for event: " + eventName);
                future.completeExceptionally(timeoutException);
                logger.log(Level.WARNING, "Timeout waiting for event '{0}'", eventName);
            }
        }, timeoutSeconds, TimeUnit.SECONDS);

        future.whenComplete((result, throwable) -> {
            this.off(eventName, listener);
            timeoutFuture.cancel(true);
        });

        return future;
    }

    private void handleError(Exception error) {
        logger.log(Level.SEVERE, "WebSocket error: {0}", error.getMessage());
        logger.log(Level.FINE, "Exception stack trace:", error);
        this.emit("error", error);
    }

    private void handleClose(CloseReason closeReason) {
        logger.log(Level.INFO, "WebSocket closed: {0}", closeReason);
        if (closeReason.getCloseCode() == CloseReason.CloseCodes.NORMAL_CLOSURE) {
            // interrrupt threads
            executorService.shutdownNow();
            scheduler.shutdownNow();

            return; // Skip reconnection
        }
        this.connect(); // Attempt to reconnect for other close reasons
    }

    private String getWebSocketUrl() throws Exception {
        String url = config.getWebSocketUrlProvider().getWebSocketUrl();
        if (url == null || url.isEmpty()) {
            throw new IllegalArgumentException("WebSocket URL is not provided");
        }
        return url;
    }

    protected String getRandomId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    protected String objectToJson(Object obj) throws Exception {
        return objectMapper.writeValueAsString(obj);
    }

    protected Map<String, Object> jsonToMap(String json) throws Exception {
        return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {
        });
    }

    /**
     * Inner class to handle WebSocket events.
     */
    @ClientEndpoint
    public class WSEndpoint extends Endpoint {

        @Override
        public void onOpen(Session userSession, EndpointConfig endpointConfig) {
            logger.info("WebSocket connection opened.");
            session = userSession;

            // Set message handlers
            session.addMessageHandler(String.class, this::onMessage);
            session.addMessageHandler(ByteBuffer.class, bytes -> {
                // Handle binary messages if needed
            });
        }

        @Override
        public void onClose(Session userSession, CloseReason reason) {
            logger.log(Level.INFO, "WebSocket connection closed: {0}", reason);
            handleClose(reason);
        }

        @Override
        public void onError(Session session, Throwable throwable) {
            handleError(new Exception(throwable));
        }

        public void onMessage(String message) {
            try {
                Map<String, Object> messageData = jsonToMap(message);
                String topic = (String) messageData.get("topic");
                String messageType = (String) messageData.get("messageType");
                Object data = messageData.get("data");
                Boolean compression = (Boolean) messageData.get("compression");


                Map<String, Object> messageEvent = new HashMap<>();
                messageEvent.put("topic", topic);
                messageEvent.put("messageType", messageType);
                messageEvent.put("data", data);
                messageEvent.put("compression", compression);

                var incomingMessage = IncomingMessage.from(messageEvent);

                logger.log(Level.FINE, "Incoming message: {0}", incomingMessage);

                if (messageType != null) {
                    ReplyFunction replyFn = createReplyFunction(incomingMessage);
                    emit(topic + "." + messageType, incomingMessage, replyFn);
                }
            } catch (Exception e) {
                handleError(e);
            }
        }
    }

    private String maskUrl(String url) {
        // Implement URL masking to hide sensitive query parameters
        try {
            URI uri = new URI(url);
            String maskedQuery = (uri.getQuery() != null) ? "****" : null;
            URI maskedUri = new URI(uri.getScheme(), uri.getUserInfo(), uri.getHost(), uri.getPort(), uri.getPath(), maskedQuery, uri.getFragment());
            return maskedUri.toString();
        } catch (URISyntaxException e) {
            // If masking fails, return a generic message
            return "ws://***";
        }
    }

    private Map<String, Object> safeCastToMap(Object obj) {
        if (obj instanceof Map<?, ?>) {
            Map<?, ?> rawMap = (Map<?, ?>) obj;
            Map<String, Object> typedMap = new HashMap<>();
            for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
                Object key = entry.getKey();
                if (key instanceof String) {
                    typedMap.put((String) key, entry.getValue());
                } else {
                    logger.log(Level.WARNING, "Non-string key encountered in map: {0}", key);
                }
            }
            return typedMap;
        } else {
            logger.log(Level.WARNING, "Expected a Map but found: {0}", obj);
            return null;
        }
    }
}