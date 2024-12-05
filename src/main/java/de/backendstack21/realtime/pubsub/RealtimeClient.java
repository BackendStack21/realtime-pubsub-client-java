package de.backendstack21.realtime.pubsub;

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
 * The {@code RealtimeClient} class provides a WebSocket-based real-time
 * Pub/Sub client that connects to a server, subscribes/unsubscribes to topics,
 * publishes and sends messages, and handles responses and acknowledgments.
 * <p>
 * This client builds upon a pub/sub pattern, enabling communication through
 * named topics. It uses the {@link RealtimeClientConfig} for configuration
 * details such as buffer sizes, WebSocket URLs, and maximum reconnect backoff.
 * </p>
 *
 * <h2>Main Responsibilities:</h2>
 * <ul>
 *     <li>Establishes and manages a WebSocket connection with a server.</li>
 *     <li>Subscribes and unsubscribes to topics remotely.</li>
 *     <li>Publishes messages to specific topics.</li>
 *     <li>Handles incoming messages, responses, and acknowledgments from the server.</li>
 *     <li>Automatically attempts reconnection with exponential backoff if the connection closes unexpectedly.</li>
 * </ul>
 *
 * <h2>Event Handling:</h2>
 * This class extends {@link EventEmitter}, allowing event-driven interaction.
 * Events emitted include (but are not limited to):
 * <ul>
 *     <li>{@code "priv/acks.ack"}: Triggered on acknowledgment messages.</li>
 *     <li>{@code "*.response"}: Triggered when a response to a previously sent message is received.</li>
 *     <li>{@code "main.welcome"}: Triggered when a welcome message is received, signaling a new session.</li>
 *     <li>{@code "error"}: Triggered when WebSocket errors occur.</li>
 *     <li>{@code "session.started"}: Triggered when the server provides connection/session details.</li>
 * </ul>
 *
 * <h2>Concurrency and Threading Model:</h2>
 * <ul>
 *     <li>An {@link ExecutorService} is used for handling connection attempts and related tasks asynchronously.</li>
 *     <li>A {@link ScheduledExecutorService} is used to schedule timeouts when waiting for certain events.</li>
 *     <li>Atomic operations (e.g., {@link AtomicBoolean}) are used to manage connection states.</li>
 * </ul>
 *
 * <h2>Typical Usage:</h2>
 * <pre>{@code
 * RealtimeClientConfig config = new RealtimeClientConfig(() -> String.format("wss://genesis.r7.21no.de/apps/%s?access_token=%s", appId, accessToken));
 * RealtimeClient client = new RealtimeClient(config);
 *
 * // Connect and listen for session started events
 * client.on("session.started", args -> {
 *     System.out.println("Session started!");
 *     // Subscribe to a topic once the session is established
 *     try {
 *         client.subscribeRemoteTopic("myTopic");
 *     } catch (Exception e) {
 *         e.printStackTrace();
 *     }
 * });
 *
 * client.connect();
 * }</pre>
 */
public class RealtimeClient extends EventEmitter {
    /**
     * A {@link Logger} instance for logging debug, info, and error messages associated
     * with the {@code RealtimeClient} class.
     */
    private static final Logger logger = Logger.getLogger(RealtimeClient.class.getName());

    /**
     * The current WebSocket {@link Session}. This represents the active connection.
     * It may be null if the client is not currently connected.
     */
    private volatile Session session;

    /**
     * The configuration object containing settings for this client, such as buffer
     * sizes, WebSocket URL provider, and max reconnect backoff.
     */
    private final RealtimeClientConfig config;

    /**
     * A thread pool used for running tasks related to the client operations
     * (e.g., connecting or processing messages) asynchronously.
     */
    private final ExecutorService executorService;

    /**
     * A set of currently subscribed topics managed by this client. This helps
     * track which topics the client is interested in receiving messages from.
     */
    private final Set<String> subscribedTopics;

    /**
     * An atomic flag indicating if the client is currently trying to connect.
     * Prevents multiple concurrent connection attempts.
     */
    private final AtomicBoolean isConnecting;

    /**
     * A JSON object mapper for serializing and deserializing message payloads.
     */
    private final ObjectMapper objectMapper;

    /**
     * A scheduler for tasks such as timeouts when waiting for events or responses.
     */
    private final ScheduledExecutorService scheduler;

    /**
     * The WebSocketContainer responsible for creating and managing the WebSocket
     * connection. This can be overridden to provide a custom implementation.
     */
    private final WebSocketContainer container;

    /**
     * Initializes a new {@code RealtimeClient} instance with the given configuration.
     * <p>
     * This constructor sets up the required services and event handlers. It does not
     * immediately connect; call {@link #connect()} to initiate the WebSocket connection.
     * </p>
     *
     * @param config The {@link RealtimeClientConfig} that provides client settings.
     * @throws IllegalArgumentException if the {@code config} is null or invalid.
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

        // Configure container properties such as buffer sizes
        configureWebSocketContainer();

        // Register event listeners for built-in events
        this.on("priv/acks.ack", this::onAck);
        this.on("*.response", this::onResponse);
        this.on("main.welcome", this::onWelcome);
    }

    /**
     * Factory method to create a new {@link WebSocketContainer}. This method can be
     * overridden in subclasses or during testing to provide a custom container.
     *
     * @return A new instance of {@link WebSocketContainer}.
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

    /**
     * Configures the WebSocket container properties based on the {@link RealtimeClientConfig}.
     * <p>
     * Sets the default maximum text and binary message buffer sizes.
     * </p>
     */
    private void configureWebSocketContainer() {
        container.setDefaultMaxTextMessageBufferSize(config.getMaxTextMessageBufferSize());
        container.setDefaultMaxBinaryMessageBufferSize(config.getMaxBinaryMessageBufferSize());
    }

    /**
     * Creates a {@link ReplyFunction} for the given incoming message. This allows the client
     * to send a direct reply to a message, often used for request/response patterns.
     *
     * @param message The incoming {@link IncomingMessage} to reply to.
     * @return A {@link ReplyFunction} that can be used to send a response message.
     * @throws IllegalArgumentException if required fields (like connectionId or messageId) are missing.
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
     * Gets the {@link ExecutorService} used by the client for asynchronous tasks.
     * Typically, this is a fixed-size thread pool sized to the number of available processors.
     *
     * @return The {@link ExecutorService} instance.
     */
    public ExecutorService getExecutorService() {
        return executorService;
    }

    /**
     * Internal event handler for acknowledgment messages received from the server.
     * This method emits an "ack.[messageId]" event when an acknowledgment is received.
     *
     * @param args The event arguments; expected to contain an {@link IncomingMessage}.
     */
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

    /**
     * Internal event handler for response messages received from the server.
     * When a response is received, it emits a "response.[messageId]" event, passing
     * a {@link ResponseMessage} object.
     *
     * @param args The event arguments; expected to contain an {@link IncomingMessage}.
     */
    private void onResponse(Object[] args) {
        if (args.length == 0) return;
        var message = (IncomingMessage) (args[0]);
        if (message == null) return;

        String topic = message.getTopic();
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

    /**
     * Internal event handler for welcome messages received from the server.
     * This event indicates that the session has started and provides connection details.
     *
     * @param args The event arguments; expected to contain an {@link IncomingMessage}.
     */
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
     * Establishes a connection to the WebSocket server using the URL provided by the
     * {@link RealtimeClientConfig}. If the connection fails, the client will retry
     * with exponential backoff until it successfully connects or is interrupted.
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
     * Disconnects from the WebSocket server if currently connected.
     * This will attempt a normal closure of the WebSocket session.
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
     * Checks whether the client is currently connected to the WebSocket server.
     *
     * @return {@code true} if connected and the session is open; {@code false} otherwise.
     */
    public boolean isConnected() {
        return session != null && session.isOpen();
    }

    /**
     * Publishes a message to a specified topic with no compression and no custom message ID.
     *
     * @param topic       The topic to publish the message to.
     * @param payload     The message payload (any serializable object).
     * @param messageType The type of the message, used by the receiver to understand the payload format.
     * @return A {@link WaitFor} instance that can be used to wait for acknowledgments or responses.
     * @throws Exception if the message cannot be published (e.g., no active connection).
     */
    public WaitFor publish(String topic, Object payload, String messageType) throws Exception {
        return publish(topic, payload, messageType, false, null);
    }

    /**
     * Publishes a message to a specified topic, with an option to compress the payload.
     *
     * @param topic       The topic to publish the message to.
     * @param payload     The message payload.
     * @param messageType The message type.
     * @param compress    Whether to compress the message payload.
     * @return A {@link WaitFor} instance for awaiting server acknowledgments or responses.
     * @throws Exception if the message cannot be published.
     */
    public WaitFor publish(String topic, Object payload, String messageType, boolean compress) throws Exception {
        return publish(topic, payload, messageType, compress, null);
    }

    /**
     * Publishes a message to a specified topic with full control over compression and message ID.
     *
     * @param topic       The topic to publish the message to.
     * @param payload     The message payload.
     * @param messageType The type of the message.
     * @param compress    Whether to compress the payload.
     * @param messageId   A unique identifier for the message. If null or empty, a random ID is generated.
     * @return A {@link WaitFor} instance for awaiting acknowledgments or responses.
     * @throws Exception if unable to publish (e.g., no active session).
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
     * Sends a message directly to the server without specifying a topic. This is similar
     * to publish but uses a message type of "message" instead of "publish".
     *
     * @param payload     The message payload.
     * @param messageType The type of the message.
     * @return A {@link WaitFor} instance for awaiting acknowledgments or responses.
     * @throws Exception if the message cannot be sent.
     */
    public WaitFor send(Object payload, String messageType) throws Exception {
        return send(payload, messageType, false, null);
    }

    /**
     * Sends a message directly to the server with a compression option.
     *
     * @param payload     The message payload.
     * @param messageType The type of message.
     * @param compress    Whether to compress the payload.
     * @return A {@link WaitFor} instance for awaiting acknowledgments or responses.
     * @throws Exception if unable to send.
     */
    public WaitFor send(Object payload, String messageType, Boolean compress) throws Exception {
        return send(payload, messageType, compress, null);
    }

    /**
     * Sends a message directly to the server with full control over compression and message ID.
     *
     * @param payload     The message payload.
     * @param messageType The message type.
     * @param compress    Whether to compress the payload.
     * @param messageId   A unique identifier for the message; generated if not provided.
     * @return A {@link WaitFor} instance for awaiting acknowledgments or responses.
     * @throws Exception if unable to send (e.g., no active connection).
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
     * Subscribes to a remote topic, allowing the client to receive messages published
     * to that topic by the server or other clients.
     *
     * @param topic The name of the topic to subscribe to.
     * @throws Exception if the subscription cannot be completed (e.g., not connected).
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
        logger.log(Level.INFO, "Subscribing to topic: {0}", topic);
        session.getAsyncRemote().sendText(message);
    }

    /**
     * Unsubscribes from a previously subscribed topic, stopping further messages from that topic.
     *
     * @param topic The name of the topic to unsubscribe from.
     * @throws Exception if the unsubscription cannot be completed (e.g., not connected).
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
     * Waits for a given event to be emitted. Uses a default timeout of 30 seconds.
     * If the event does not occur within the specified timeout, a {@link TimeoutException}
     * is thrown via the returned future.
     *
     * @param eventName The name of the event to wait for.
     * @return A {@link CompletableFuture} that completes with the event arguments when the event occurs.
     */
    public CompletableFuture<Object[]> waitFor(String eventName) {
        return waitFor(eventName, 30);
    }

    /**
     * Waits for a given event to be emitted within the specified timeout. If the event does not occur
     * before the timeout, the returned {@link CompletableFuture} is completed exceptionally with
     * a {@link TimeoutException}.
     *
     * @param eventName      The name of the event to wait for.
     * @param timeoutSeconds The maximum number of seconds to wait.
     * @return A {@link CompletableFuture} that will complete with the event arguments when the event occurs.
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

    /**
     * Handles errors encountered during WebSocket operations.
     * Emits an "error" event with the exception for external handling.
     *
     * @param error The exception that occurred.
     */
    private void handleError(Exception error) {
        logger.log(Level.SEVERE, "WebSocket error: {0}", error.getMessage());
        logger.log(Level.FINE, "Exception stack trace:", error);
        this.emit("error", error);
    }

    /**
     * Handles the WebSocket close event. If the closure is abnormal, tries to reconnect.
     * If it's a normal closure, it shuts down the executor services.
     *
     * @param closeReason The reason provided for the WebSocket closure.
     */
    private void handleClose(CloseReason closeReason) {
        logger.log(Level.INFO, "WebSocket closed: {0}", closeReason);
        if (closeReason.getCloseCode() == CloseReason.CloseCodes.NORMAL_CLOSURE) {
            // Normal closure: Shut down threads and do not attempt reconnection.
            executorService.shutdownNow();
            scheduler.shutdownNow();
            return;
        }
        // Attempt reconnection for non-normal closures
        this.connect();
    }

    /**
     * Retrieves the WebSocket URL from the configured {@link RealtimeClientConfig}'s provider.
     *
     * @return A valid WebSocket URL as a String.
     * @throws Exception if the URL cannot be retrieved or is invalid.
     */
    private String getWebSocketUrl() throws Exception {
        String url = config.getWebSocketUrlProvider().getWebSocketUrl();
        if (url == null || url.isEmpty()) {
            throw new IllegalArgumentException("WebSocket URL is not provided");
        }
        return url;
    }

    /**
     * Generates a random, unique identifier for messages that require correlation.
     *
     * @return A unique string ID (UUID without dashes).
     */
    protected String getRandomId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * Serializes an object to its JSON string representation.
     *
     * @param obj The object to serialize.
     * @return The JSON string representation of the object.
     * @throws Exception if serialization fails.
     */
    protected String objectToJson(Object obj) throws Exception {
        return objectMapper.writeValueAsString(obj);
    }

    /**
     * Deserializes a JSON string into a Map of String to Object.
     *
     * @param json The JSON string to parse.
     * @return A Map representing the JSON object.
     * @throws Exception if deserialization fails.
     */
    protected Map<String, Object> jsonToMap(String json) throws Exception {
        return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {
        });
    }

    /**
     * A private helper method to mask sensitive parameters in the WebSocket URL before logging.
     *
     * @param url The original WebSocket URL.
     * @return A masked URL with sensitive parts obfuscated.
     */
    private String maskUrl(String url) {
        try {
            URI uri = new URI(url);
            String maskedQuery = (uri.getQuery() != null) ? "****" : null;
            URI maskedUri = new URI(uri.getScheme(), uri.getUserInfo(), uri.getHost(), uri.getPort(), uri.getPath(), maskedQuery, uri.getFragment());
            return maskedUri.toString();
        } catch (URISyntaxException e) {
            // If masking fails, return a generic masked URL
            return "ws://***";
        }
    }

    /**
     * Safely attempts to cast an object to a {@code Map<String, Object>} by creating a new map and copying entries.
     * Logs a warning if keys are not strings or if the object is not a map.
     *
     * @param obj The object to cast and convert.
     * @return A Map<String, Object> if successful; null otherwise.
     */
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

    /**
     * The {@code WSEndpoint} inner class defines a WebSocket endpoint that handles
     * the WebSocket lifecycle events (open, message, close, error) and routes them
     * back to the parent {@link RealtimeClient}.
     */
    @ClientEndpoint
    public class WSEndpoint extends Endpoint {

        /**
         * Creates a new instance of WSEndpoint. This constructor is used implicitly
         * by the container when establishing the WebSocket connection.
         */
        public WSEndpoint() {
            super();
        }

        /**
         * Called when the WebSocket connection is opened. Sets message handlers for
         * incoming text and binary messages.
         *
         * @param userSession    The newly opened session.
         * @param endpointConfig The endpoint configuration.
         */
        @Override
        public void onOpen(Session userSession, EndpointConfig endpointConfig) {
            logger.info("WebSocket connection opened.");
            session = userSession;

            // Set message handlers
            session.addMessageHandler(String.class, this::onMessage);
            session.addMessageHandler(ByteBuffer.class, bytes -> {
                // Handle binary messages if needed (not implemented by default)
            });
        }

        /**
         * Called when the WebSocket connection is closed. Delegates handling to
         * {@link RealtimeClient#handleClose(CloseReason)} to manage reconnection logic.
         *
         * @param userSession The session that was closed.
         * @param reason      The reason the session was closed.
         */
        @Override
        public void onClose(Session userSession, CloseReason reason) {
            logger.log(Level.INFO, "WebSocket connection closed: {0}", reason);
            handleClose(reason);
        }

        /**
         * Called when a WebSocket error occurs. Delegates error handling to
         * {@link RealtimeClient#handleError(Exception)}.
         *
         * @param session   The session in which the error occurred.
         * @param throwable The cause of the error.
         */
        @Override
        public void onError(Session session, Throwable throwable) {
            handleError(new Exception(throwable));
        }

        /**
         * Called when a text message is received. Deserializes the message,
         * constructs an {@link IncomingMessage}, and emits the corresponding event.
         *
         * @param message The incoming text message as a string.
         */
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
}
