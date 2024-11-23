package samples.rpc;

import realtime.pubsub.*;

import java.util.logging.Logger;

public class Client {

    public static void main(String[] args) throws Exception {
        var logger = Logger.getLogger(samples.ClientDemoMinimal.class.getName());

        var accessToken = System.getenv("ACCESS_TOKEN");
        var appId = System.getenv("APP_ID");

        var config = new RealtimeClientConfig(()
                -> String.format("wss://genesis.r7.21no.de/apps/%s?access_token=%s", appId, accessToken));
        var client = new RealtimeClient(config);
        client.connect();

        client.waitFor("session.started").get();

        var publishWaitFor = client.send("", "gettime");
        var response = publishWaitFor.waitForReply().get()[0];
        logger.info("Server Time: " + response);

        client.disconnect();
    }
}