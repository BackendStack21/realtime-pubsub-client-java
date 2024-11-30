package de.backendstack21.realtime.pubsub;

public enum PresenceStatus {
    CONNECTED("connected"),
    DISCONNECTED("disconnected");

    private final String status;

    PresenceStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }
}
