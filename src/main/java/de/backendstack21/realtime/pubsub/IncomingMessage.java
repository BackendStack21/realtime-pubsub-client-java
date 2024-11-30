package de.backendstack21.realtime.pubsub;

import java.util.Map;

public class IncomingMessage {
    private final String topic;
    private final String messageType;
    private final Boolean compression;
    private final Object data;

    private IncomingMessage(String topic, String messageType, Boolean compression, Object data) {
        this.topic = topic;
        this.messageType = messageType;
        this.compression = compression;
        this.data = data;
    }

    public String getTopic() {
        return topic;
    }

    public String getMessageType() {
        return messageType;
    }

    public Boolean getCompression() {
        return compression;
    }

    public Object getData() {
        return data;
    }

    public static IncomingMessage from(Map<String, Object> message) {
        String topic = (String) message.get("topic");
        String messageType = (String) message.get("messageType");
        Boolean compression = (Boolean) message.get("compression");
        Object data = message.get("data");

        return new IncomingMessage(topic, messageType, compression, data);
    }

    @SuppressWarnings("unchecked")
    public static IncomingMessage from(Object message) {
        if (message instanceof Map) {
            return from((Map<String, Object>) message);
        } else {
            throw new IllegalArgumentException("Message must be a Map");
        }
    }

    @Override
    public String toString() {
        return "IncomingMessage{" + "topic='" + topic + '\'' + ", messageType='" + messageType + '\'' + ", compression=" + compression + ", data=" + data + '}';
    }
}
