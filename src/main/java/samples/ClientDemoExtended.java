package samples;

import realtime.pubsub.*;

import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ClientDemo class demonstrates how to use the RealtimeClient with the new implementation.
 */
public class ClientDemoExtended {

    private static final Logger logger = Logger.getLogger(ClientDemoExtended.class.getName());

    public static void main(String[] args) {
        // Configure logging level
        logger.setLevel(Level.ALL);

        // Retrieve environment variables
        var accessToken = System.getenv("ACCESS_TOKEN");
        var appId = System.getenv("APP_ID");

        if (accessToken == null || appId == null) {
            logger.severe("ACCESS_TOKEN and APP_ID environment variables must be set.");
            return;
        }

        // Create the configuration
        var config = new RealtimeClientConfig(() -> {
            // Construct the WebSocket URL with the access token and app ID
            return String.format("wss://genesis.r7.21no.de/apps/%s?access_token=%s", appId, accessToken);
        });

        // Initialize the RealtimeClient with the configuration
        var client = new RealtimeClient(config);

        // Register event listener for session started
        client.on("session.started", (Object... eventArgs) -> {
            logger.info("Session started: " + (ConnectionInfo) eventArgs[0]);

            // Subscribe to the 'chat' topic
            try {
                client.subscribeRemoteTopic("chat");
                logger.info("Subscribed to topic 'chat'");
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Failed to subscribe to topic 'chat'", e);
            }
        });

        // Register message handler for 'chat.text-message' events
        client.on("chat.text-message", (Object... eventArgs) -> {
            var messageEvent = (IncomingMessage) eventArgs[0];
            var replyFn = (ReplyFunction) eventArgs[1];

            logger.info("New chat message arrived: " + messageEvent.getData());

            // Send a reply
            try {
                replyFn.reply(Map.of("text", "Hello, back!"), "ok", false);
                logger.info("Sent reply to message");
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Failed to send reply", e);
            }
        });

        // Connect to the WebSocket server
        client.connect();

        // Wait for the session to start
        var sessionFuture = client.waitFor("session.started", 10);
        sessionFuture.thenRun(() -> {
            // Send a message
            try {
                var waitFor = client.send("Hello, world!", "text-message");

                // Wait for acknowledgment
                waitFor.waitForAck().thenAccept(ackArgs -> logger.info("Acknowledgment received for message")).exceptionally(ex -> {
                    logger.log(Level.WARNING, "Acknowledgment not received", ex);
                    return null;
                });
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Failed to send message", e);
            }
        }).exceptionally(ex -> {
            logger.log(Level.SEVERE, "Session did not start", ex);
            return null;
        });

        // Publish a message to 'chat' topic and wait for reply
        try {
            // Wait until the session has started
            sessionFuture.get();

            // Publish a message to 'chat' topic
            var waitForPublish = client.publish("chat", "Hello out there!", "text-message");

            // Wait for reply
            waitForPublish.waitForReply().thenAccept(replyArgs -> {
                var reply = (ResponseMessage) replyArgs[0];
                logger.info("Reply received: " + reply);
            }).exceptionally(ex -> {
                logger.log(Level.WARNING, "Reply not received", ex);
                return null;
            });
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.log(Level.SEVERE, "Thread was interrupted", e);
        } catch (ExecutionException e) {
            logger.log(Level.SEVERE, "Error during execution", e);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to publish message", e);
        }

        // client.disconnect();
    }
}