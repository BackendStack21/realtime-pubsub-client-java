# Realtime Pub/Sub Client for Java

The `realtime-pubsub-client` is a Java client library for interacting with [Realtime Pub/Sub](https://realtime.21no.de) applications. It enables developers to manage real-time WebSocket connections, handle subscriptions, and process messages efficiently. The library provides a simple and flexible API to interact with realtime applications, supporting features like publishing/sending messages, subscribing to topics, handling acknowledgments, and waiting for replies with timeout support.

## Features

- **WebSocket Connection Management**: Seamlessly connect and disconnect from the Realtime Pub/Sub service with automatic reconnection support.
- **Topic Subscription**: Subscribe and unsubscribe to topics for receiving messages.
- **Topic Publishing**: [Publish](https://realtime.21no.de/documentation/#publishers) messages to specific topics with optional message types and compression.
- **Message Sending**: [Send](https://realtime.21no.de/documentation/#websocket-inbound-messaging) messages to backend applications with optional message types and compression.
- **Event Handling**: Handle incoming messages with custom event listeners.
- **Acknowledgments and Replies**: Wait for gateway acknowledgments or replies to messages with timeout support.
- **Error Handling**: Robust error handling and logging capabilities.
- **Strongly Typed Classes**: Provides strongly typed classes for a better development experience.

## Installation

Add the `realtime-pubsub-client` library to your project via Maven or Gradle:

### Maven

Add the following dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>de.21no</groupId>
    <artifactId>realtime-pubsub-client</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Gradle

Add the following to your `build.gradle` dependencies:

```kt
implementation 'de.21no:realtime-pubsub-client:1.0.0'
```

**Dependencies**:

- **Java WebSocket API**:

  ```kt
  dependencies {
    // https://mvnrepository.com/artifact/jakarta.websocket/jakarta.websocket-api
    compileOnly("jakarta.websocket:jakarta.websocket-api:2.2.0")

    // https://mvnrepository.com/artifact/org.glassfish.tyrus.bundles/tyrus-standalone-client
    implementation("org.glassfish.tyrus.bundles:tyrus-standalone-client:2.2.0")
  }
  ```

- **Jackson JSON Processor**:

  ```kt
  dependencies { 
    // https://mvnrepository.com/artifact/com.fasterxml.jackson.core/jackson-core
    implementation("com.fasterxml.jackson.core:jackson-core:2.18.1")
  }
  ```

- **Logging Framework**:

  Use Java's built-in logging or include a logging framework of your choice.

## Getting Started

This guide will help you set up and use the `realtime-pubsub-client` library in your Java project.

### Connecting to the Server

First, import the `RealtimeClient` class and create a new instance with the required configuration:

```java
import realtime.pubsub.RealtimeClient;
import realtime.pubsub.RealtimeClientConfig;
import realtime.pubsub.ConnectionInfo;

public class ClientDemo {
    public static void main(String[] args) {
        // Retrieve environment variables
        String APP_ID = System.getenv("APP_ID");
        String ACCESS_TOKEN = System.getenv("ACCESS_TOKEN");

        if (APP_ID == null || ACCESS_TOKEN == null) {
            System.err.println("APP_ID and ACCESS_TOKEN environment variables must be set.");
            return;
        }

        // Create the configuration
        RealtimeClientConfig config = new RealtimeClientConfig(() -> {
            // Construct the WebSocket URL with the access token and app ID
            return String.format("wss://genesis.r7.21no.de/apps/%s?access_token=%s", APP_ID, ACCESS_TOKEN);
        });

        // Initialize the RealtimeClient with the configuration
        RealtimeClient client = new RealtimeClient(config);

        // Register event listener for session started
        client.on("session.started", (Object... eventArgs) -> {
            ConnectionInfo connectionInfo = (ConnectionInfo) eventArgs[0];
            System.out.println("Connection ID: " + connectionInfo.getId());

            // Subscribe to topics here
            try {
                client.subscribeRemoteTopic("topic1");
                client.subscribeRemoteTopic("topic2");
                // ...
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        // Connect to the WebSocket server
        client.connect();

        // Wait for the session.started event
        try {
            client.waitFor("session.started", 10).get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }
}
```

### Subscribing to Incoming Messages

You can handle messages for specific topics and message types:

> **Note**: The topic and message type are concatenated with a dot (`.`) in the event name.

```java
client.on("topic1.action1", (Object... eventArgs) -> {
    IncomingMessage message = (IncomingMessage) eventArgs[0];
    // Message handling logic here
    System.out.println("Received message: " + message.get("data"));
});
```

Wildcard subscriptions are also supported:

```java
client.on("topic1.*", (Object... eventArgs) -> {
    // Handle all messages in topic1
});
```

#### Concurrency Support 

The underlying EventEmitter implementation uses a single thread for event handling. If you need to handle events concurrently, 
consider using a thread pool or executor service to process events in parallel.

You can access the client `ExecutorService` instance by calling the `getExecutorService()` method:

```java
client.on("secure/inbound.gettime", (Object... eventArgs) -> {
    var replyFn = (ReplyFunction) eventArgs[1];
    logger.info("Responding to gettime request on a separate thread...");

    client.getExecutorService().submit(() -> {
        try {
            var response = Map.of("time", new Date());
            // Send a reply and wait for acknowledgment
            replyFn.reply(response, "ok", false).waitForAck().get();
            logger.info("Response delivered!");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to send reply", e);
        }
    });
});
```

### Publishing Messages

Publish messages to a topic:

```java
client.publish("topic1", "Hello, world!", "text-message");
```

### Sending messages to your backend services:

```java
client.send(Map.of("name", "Realtime Pub/Sub", "type": "saas"), "create");
```

### Responding to Incoming Messages

Set up event listeners to handle incoming messages and send replies:

```java
client.on("topic1.text-message", (Object... eventArgs) -> {
    IncomingMessage message = (IncomingMessage) eventArgs[0];
    ReplyFunction replyFn = (ReplyFunction) eventArgs[1];

    // Message handling logic here
    System.out.println("Received message: " + message.get("data"));
    
    try {
        replyFn.reply("Message received!", "ok", false);
    } catch (Exception e) {
        e.printStackTrace();
    }
});
```

### Waiting for Acknowledgments and Replies

You can wait for acknowledgments or replies when publishing or sending messages.

- **waitForAck(int timeoutSeconds)**: Waits for an acknowledgment of the message, with an optional timeout in seconds.
- **waitForReply(int timeoutSeconds)**: Waits for a reply to the message, with an optional timeout in seconds.

Wait for the Realtime Gateway acknowledgment after publishing a message:

```java
try {
    WaitFor waitFor = client.publish("secure/peer-to-peer1", "Hi", "greeting");
    waitFor.waitForAck(5).get(); // Wait for up to 5 seconds
} catch (Exception e) {
    e.printStackTrace();
}
```

Wait for the Realtime Gateway acknowledgment after sending a message:

```java
try {
    WaitFor waitFor = client.send("Your Message", "create");
    waitFor.waitForAck(5).get();
} catch (Exception e) {
    e.printStackTrace();
}
```

Wait for a reply with a timeout:

```java
try {
    WaitFor waitFor = client.send(Map.of("name", "Realtime Pub/Sub", "type": "saas"), "create");
    ResponseMessage response = (ResponseMessage) waitFor.waitForReply(5).get()[0]; // Wait for up to 5 seconds
    System.out.println("Received reply: " + response);
} catch (Exception e) {
    e.printStackTrace();
}
```

### Error Handling

Handle errors and disconnections:

```java
client.on("error", (Object... eventArgs) -> {
    Exception error = (Exception) eventArgs[0];
    System.err.println("WebSocket error: " + error.getMessage());
});

client.on("close", (Object... eventArgs) -> {
    CloseReason closeReason = (CloseReason) eventArgs[0];
    System.out.println("WebSocket closed: " + closeReason.getReasonPhrase());
});
```

## API Reference

### RealtimeClient

#### Constructor

```java
public RealtimeClient(RealtimeClientConfig config);
```

Creates a new `RealtimeClient` instance.

- **config**: Configuration options for the client encapsulated in `RealtimeClientConfig`.

#### Methods

- **getExecutorService()**: Returns the `ExecutorService` instance used by the client.

  ```java
  public ExecutorService getExecutorService();
  ```

- **connect()**: Connects the client to the WebSocket Messaging Gateway.

  ```java
  public void connect();
  ```

- **disconnect()**: Terminates the WebSocket connection.

  ```java
  public void disconnect();
  ```

- **subscribeRemoteTopic(String topic)**: [Subscribes](https://realtime.21no.de/documentation/#subscribers) the connection to a remote topic.

  ```java
  public void subscribeRemoteTopic(String topic) throws Exception;
  ```

- **unsubscribeRemoteTopic(String topic)**: [Unsubscribes](https://realtime.21no.de/documentation/#subscribers) the connection from a remote topic.

  ```java
  public void unsubscribeRemoteTopic(String topic) throws Exception;
  ```

- **publish(String topic, Object payload, String messageType, boolean compress, String messageId)**: Publishes a message to a topic.

  ```java
  public WaitFor publish(String topic, Object payload, String messageType, boolean compress, String messageId) throws Exception;
  ```

  Returns a `WaitFor` instance to wait for acknowledgments or replies.

- **send(Object payload, String messageType, boolean compress, String messageId)**: Sends a message to the server.

  ```java
  public WaitFor send(Object payload, String messageType, boolean compress, String messageId) throws Exception;
  ```

  Returns a `WaitFor` instance to wait for acknowledgments or replies.

- **waitFor(String eventName, int timeoutSeconds)**: Waits for a specific event to occur within a timeout period.

  ```java
  public CompletableFuture<Object[]> waitFor(String eventName, int timeoutSeconds);
  ```

  Returns a `CompletableFuture` that completes with the event data.

#### Events

- **`"session.started"`**: Emitted when the session starts.

  ```java
  client.on("session.started", (Object... eventArgs) -> {
      ConnectionInfo connectionInfo = (ConnectionInfo) eventArgs[0];
      // Handle session started
  });
  ```

- **`"error"`**: Emitted on WebSocket errors.

  ```java
  client.on("error", (Object... eventArgs) -> {
      Exception error = (Exception) eventArgs[0];
      // Handle error
  });
  ```

- **`"close"`**: Emitted when the WebSocket connection closes.

  ```java
  client.on("close", (Object... eventArgs) -> {
      CloseReason closeReason = (CloseReason) eventArgs[0];
      // Handle close
  });
  ```

- **Custom Events**: Handle custom events based on topic and message type.

  ```java
  client.on("TOPIC_NAME.MESSAGE_TYPE", (Object... eventArgs) -> {
      IncomingMessage message = (IncomingMessage) eventArgs[0];
      ReplyFunction replyFn = (ReplyFunction) eventArgs[1];
      // Handle message and possibly send a reply
  });
  ```

  > **Note**: Wildcard subscriptions are also supported.

## License

This library is licensed under the MIT License.

---

For more detailed examples and advanced configurations, please refer to the [documentation](https://realtime.21no.de/introduction/).

## Notes

- Ensure that you have an account and an app set up with [Realtime Pub/Sub](https://realtime.21no.de).
- Customize the `WebSocketUrlProvider` function to retrieve the access token for connecting to your realtime application.
- Implement any authentication mechanism as required by your application.
- Optionally use a custom logger or integrate with your application's logging system.
- Handle errors and disconnections gracefully to improve the robustness of your application.
- Make sure to handle timeouts when waiting for replies to avoid hanging operations.
- Include necessary dependencies in your project's build configuration.

---

Feel free to contribute to this project by submitting issues or pull requests on [GitHub](https://github.com/YourGithubUsername/realtime-pubsub-client-java).
