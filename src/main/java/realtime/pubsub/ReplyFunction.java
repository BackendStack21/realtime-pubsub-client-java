package realtime.pubsub;

/**
 * A functional interface for reply functions.
 */
public interface ReplyFunction {
    WaitFor reply(Object data, String status, boolean compress) throws Exception;
}
