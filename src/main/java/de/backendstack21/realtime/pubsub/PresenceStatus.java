package de.backendstack21.realtime.pubsub;

/**
 * Enum representing the presence status of an external application connection.
 */
public enum PresenceStatus {
    /**
     * Indicates that the connection is currently active and connected.
     */
    CONNECTED("connected"),

    /**
     * Indicates that the connection has been disconnected.
     */
    DISCONNECTED("disconnected");

    private final String status;

    /**
     * Constructs a PresenceStatus enum with the specified status string.
     *
     * @param status the status string representing the presence status
     */
    PresenceStatus(String status) {
        this.status = status;
    }

    /**
     * Gets the status string associated with the presence status.
     *
     * @return the status string
     */
    public String getStatus() {
        return status;
    }
}