package de.backendstack21.realtime.pubsub;

/**
 * Configuration class for RealtimeClient.
 */
public class RealtimeClientConfig {

    private final WebSocketUrlProvider webSocketUrlProvider;
    private int maxTextMessageBufferSize = 65536;
    private int maxBinaryMessageBufferSize = 65536;
    private int maxReconnectBackoffSeconds = 60;

    /**
     * Constructor with mandatory WebSocket URL provider.
     *
     * @param webSocketUrlProvider The provider for the WebSocket URL.
     */
    public RealtimeClientConfig(WebSocketUrlProvider webSocketUrlProvider) {
        if (webSocketUrlProvider == null) {
            throw new IllegalArgumentException("WebSocketUrlProvider cannot be null");
        }
        this.webSocketUrlProvider = webSocketUrlProvider;
    }

    /**
     * Gets the WebSocket URL provider.
     *
     * @return The WebSocketUrlProvider instance.
     */
    public WebSocketUrlProvider getWebSocketUrlProvider() {
        return webSocketUrlProvider;
    }

    /**
     * Gets the maximum text message buffer size.
     *
     * @return The max text message buffer size.
     */
    public int getMaxTextMessageBufferSize() {
        return maxTextMessageBufferSize;
    }

    /**
     * Sets the maximum text message buffer size.
     *
     * @param maxTextMessageBufferSize The max text message buffer size.
     */
    public void setMaxTextMessageBufferSize(int maxTextMessageBufferSize) {
        this.maxTextMessageBufferSize = maxTextMessageBufferSize;
    }

    /**
     * Gets the maximum binary message buffer size.
     *
     * @return The max binary message buffer size.
     */
    public int getMaxBinaryMessageBufferSize() {
        return maxBinaryMessageBufferSize;
    }

    /**
     * Sets the maximum binary message buffer size.
     *
     * @param maxBinaryMessageBufferSize The max binary message buffer size.
     */
    public void setMaxBinaryMessageBufferSize(int maxBinaryMessageBufferSize) {
        this.maxBinaryMessageBufferSize = maxBinaryMessageBufferSize;
    }

    /**
     * Gets the maximum reconnect backoff in seconds.
     *
     * @return The max reconnect backoff in seconds.
     */
    public int getMaxReconnectBackoffSeconds() {
        return maxReconnectBackoffSeconds;
    }

    /**
     * Sets the maximum reconnect backoff in seconds.
     *
     * @param maxReconnectBackoffSeconds The max reconnect backoff in seconds.
     */
    public void setMaxReconnectBackoffSeconds(int maxReconnectBackoffSeconds) {
        this.maxReconnectBackoffSeconds = maxReconnectBackoffSeconds;
    }

    /**
     * Interface for providing the WebSocket URL.
     */
    @FunctionalInterface
    public interface WebSocketUrlProvider {
        String getWebSocketUrl() throws Exception;
    }
}