package samples;

import realtime.pubsub.*;

import java.util.Map;
import java.util.logging.Logger;

public class ClientDemoMinimal {

    public static void main(String[] args) throws Exception {
        var logger = Logger.getLogger(ClientDemoMinimal.class.getName());

        var accessToken = System.getenv("ACCESS_TOKEN");
        var appId = System.getenv("APP_ID");

        var config = new RealtimeClientConfig(() -> String.format("wss://genesis.r7.21no.de/apps/%s?access_token=%s", appId, accessToken));

        var client = new RealtimeClient(config);

        client.on("session.started", (Object... params) -> {
            logger.info("Session started: " + params[0]);
            client.subscribeRemoteTopic("chat");
        });

        client.on("chat.text-message", (Object... params) -> {
            var messageEvent = (IncomingMessage) params[0];
            var replyFn = (ReplyFunction) params[1];

            logger.info("New chat message: " + messageEvent.getData());
            replyFn.reply(Map.of("text", "Hello, back!"), "ok", false);
        });

        client.connect();

        var sessionFuture = client.waitFor("session.started");
        sessionFuture.thenRun(() -> {
            try {
                var waitFor = client.send("Hello, world!", "text-message");
                waitFor.waitForAck().thenAccept(ack -> logger.info("Acknowledgment received."));
            } catch (Exception e) {
                logger.severe("Failed to send message: " + e.getMessage());
            }
        });

        sessionFuture.get();

        var publishWaitFor = client.publish("chat", "Hello out there!", "text-message");
        publishWaitFor.waitForReply().thenAccept(reply -> logger.info("Reply received: " + reply[0]));

        // Disconnect is intentionally left out for this minimal demo
    }
}