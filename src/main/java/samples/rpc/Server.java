package samples.rpc;

import realtime.pubsub.*;

import java.util.Date;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Server {

    public static void main(String[] args) throws Exception {
        var logger = Logger.getLogger(samples.ClientDemoMinimal.class.getName());

        var accessToken = System.getenv("ACCESS_TOKEN");
        var appId = System.getenv("APP_ID");

        var config = new RealtimeClientConfig(() -> String.format("wss://genesis.r7.21no.de/apps/%s?access_token=%s", appId, accessToken));
        var client = new RealtimeClient(config);

        client.on("session.started", (Object... params) -> {
            client.subscribeRemoteTopic("secure/inbound");
        });

        client.on("secure/inbound.presence", (Object... eventArgs) -> {
            var messageEvent = (IncomingMessage) eventArgs[0];

            var presenceMessage = PresenceMessage.from(messageEvent);
            if (presenceMessage.getStatus() == PresenceStatus.CONNECTED) {
                logger.info("Client " + presenceMessage.getConnectionId() + " connected...");
            } else if (presenceMessage.getStatus() == PresenceStatus.DISCONNECTED) {
                logger.info("Client " + presenceMessage.getConnectionId() + " disconnected...");
            }
        });

        client.on("secure/inbound.gettime", (Object... eventArgs) -> {
            var replyFn = (ReplyFunction) eventArgs[1];

            logger.info("Responding to gettime request...");

            // Send a reply
            try {
                var waitFor = replyFn.reply(Map.of("time", new Date()), "ok", false);
                waitFor.waitForAck().thenAccept(ack -> logger.info("Acknowledgment received."));
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Failed to send reply", e);
            }
        });

        client.connect();
    }
}