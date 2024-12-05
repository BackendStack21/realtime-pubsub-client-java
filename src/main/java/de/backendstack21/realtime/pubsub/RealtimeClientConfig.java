package de.backendstack21.realtime.pubsub;

/**
 * Configuration class for the {@link RealtimeClient}.
 * <p>
 * This class manages configuration parameters, such as
 * buffer sizes for text and binary messages, reconnect backoff limits, and
 * the provider for WebSocket URLs. The configuration is designed to be flexible
 * and customizable.
 * </p>
 *
 * <h2>Key Features:</h2>
 * <ul>
 *     <li>Manages WebSocket URL provider via a functional interface.</li>
 *     <li>Supports configurable buffer sizes for both text and binary messages.</li>
 *     <li>Provides a mechanism to set maximum reconnect backoff duration.</li>
 * </ul>
 */
public class RealtimeClientConfig {

    /**
     * The provider for the WebSocket URL.
     */
    private final WebSocketUrlProvider webSocketUrlProvider;

    /**
     * The maximum buffer size for text messages, in bytes.
     * Defaults to 65536 (64 KB).
     */
    private int maxTextMessageBufferSize = 65536;

    /**
     * The maximum buffer size for binary messages, in bytes.
     * Defaults to 65536 (64 KB).
     */
    private int maxBinaryMessageBufferSize = 65536;

    /**
     * The maximum reconnect backoff time, in seconds.
     * Defaults to 60 seconds.
     */
    private int maxReconnectBackoffSeconds = 60;

    /**
     * Constructs a new {@code RealtimeClientConfig} with the specified
     * WebSocket URL provider.
     *
     * @param webSocketUrlProvider the provider for the WebSocket URL. This
     *                             parameter is mandatory and cannot be {@code null}.
     * @throws IllegalArgumentException if {@code webSocketUrlProvider} is {@code null}.
     */
    public RealtimeClientConfig(WebSocketUrlProvider webSocketUrlProvider) {
        if (webSocketUrlProvider == null) {
            throw new IllegalArgumentException("WebSocketUrlProvider cannot be null");
        }
        this.webSocketUrlProvider = webSocketUrlProvider;
    }

    /**
     * Retrieves the WebSocket URL provider.
     *
     * @return the {@link WebSocketUrlProvider} instance used by this configuration.
     */
    public WebSocketUrlProvider getWebSocketUrlProvider() {
        return webSocketUrlProvider;
    }

    /**
     * Retrieves the maximum buffer size for text messages.
     *
     * @return the maximum text message buffer size, in bytes.
     */
    public int getMaxTextMessageBufferSize() {
        return maxTextMessageBufferSize;
    }

    /**
     * Sets the maximum buffer size for text messages.
     *
     * @param maxTextMessageBufferSize the maximum text message buffer size, in bytes.
     */
    public void setMaxTextMessageBufferSize(int maxTextMessageBufferSize) {
        this.maxTextMessageBufferSize = maxTextMessageBufferSize;
    }

    /**
     * Retrieves the maximum buffer size for binary messages.
     *
     * @return the maximum binary message buffer size, in bytes.
     */
    public int getMaxBinaryMessageBufferSize() {
        return maxBinaryMessageBufferSize;
    }

    /**
     * Sets the maximum buffer size for binary messages.
     *
     * @param maxBinaryMessageBufferSize the maximum binary message buffer size, in bytes.
     */
    public void setMaxBinaryMessageBufferSize(int maxBinaryMessageBufferSize) {
        this.maxBinaryMessageBufferSize = maxBinaryMessageBufferSize;
    }

    /**
     * Retrieves the maximum reconnect backoff time.
     *
     * @return the maximum reconnect backoff time, in seconds.
     */
    public int getMaxReconnectBackoffSeconds() {
        return maxReconnectBackoffSeconds;
    }

    /**
     * Sets the maximum reconnect backoff time.
     *
     * @param maxReconnectBackoffSeconds the maximum reconnect backoff time, in seconds.
     */
    public void setMaxReconnectBackoffSeconds(int maxReconnectBackoffSeconds) {
        this.maxReconnectBackoffSeconds = maxReconnectBackoffSeconds;
    }

    /**
     * A functional interface for providing WebSocket URLs.
     * <p>
     * Implementations of this interface must supply a method for generating or
     * retrieving WebSocket URLs dynamically.
     * </p>
     *
     * <h2>Example Usage:</h2>
     *
     * <pre>
     * {@code
     * RealtimeClientConfig.WebSocketUrlProvider provider = () -> "ws://example.com/socket";
     * RealtimeClientConfig config = new RealtimeClientConfig(provider);
     * }
     * </pre>
     */
    @FunctionalInterface
    public interface WebSocketUrlProvider {

        /**
         * Retrieves the WebSocket URL.
         *
         * @return the WebSocket URL as a {@link String}.
         * @throws Exception if the URL cannot be generated or retrieved.
         */
        String getWebSocketUrl() throws Exception;
    }
}
