package de.backendstack21.realtime.pubsub;

import java.util.Map;


public class ConnectionInfo {
    private final String id;
    private final String appId;
    private final String remoteAddress;

    private ConnectionInfo(String id, String appId, String remoteAddress) {
        this.id = id;
        this.appId = appId;
        this.remoteAddress = remoteAddress;
    }

    public String getId() {
        return id;
    }

    public String getAppId() {
        return appId;
    }

    public String getRemoteAddress() {
        return remoteAddress;
    }

    public static ConnectionInfo from(Map<String, Object> message) {
        String id = (String) message.get("id");
        String appId = (String) message.get("appId");
        String remoteAddress = (String) message.get("remoteAddress");

        return new ConnectionInfo(id, appId, remoteAddress);
    }

    @SuppressWarnings("unchecked")
    public static ConnectionInfo from(Object message) {
        if (message instanceof Map) {
            return from((Map<String, Object>) message);
        } else {
            throw new IllegalArgumentException("Message must be a Map");
        }
    }

    @Override
    public String toString() {
        return "ConnectionInfo{" + "id='" + id + '\'' + ", appId='" + appId + '\'' + ", remoteAddress='" + remoteAddress + '\'' + '}';
    }
}
