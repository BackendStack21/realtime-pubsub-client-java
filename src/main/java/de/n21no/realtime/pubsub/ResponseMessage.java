package de.n21no.realtime.pubsub;

import java.util.Map;

/**
 * A message that is sent as a response to a request.
 */
public class ResponseMessage {
    private final String id;
    private final String status;
    private final Object data;

    private ResponseMessage(String id, String status, Object data) {
        this.id = id;
        this.status = status;
        this.data = data;
    }

    public String getId() {
        return id;
    }

    public String getStatus() {
        return status;
    }

    public Object getData() {
        return data;
    }

    public static ResponseMessage from(Map<String, Object> message) {
        String id = (String) message.get("id");
        String status = (String) message.get("status");
        Object data = message.get("data");

        return new ResponseMessage(id, status, data);
    }

    @SuppressWarnings("unchecked")
    public static ResponseMessage from(Object message) {
        if (message instanceof Map) {
            return from((Map<String, Object>) message);
        } else {
            throw new IllegalArgumentException("Message must be a Map");
        }
    }

    @Override
    public String toString() {
        return "ResponseMessage{" + "id='" + id + '\'' + ", status='" + status + '\'' + ", data=" + data + '}';
    }
}
