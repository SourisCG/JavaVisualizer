package com.javavisualizer.agent;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Map;

public class WebSocketBridge {

    private final int port;
    private WebSocketClient client;
    private final Gson gson = new Gson();
    private EventInjector eventInjector;
    private volatile boolean connected = false;

    public WebSocketBridge(int port) {
        System.out.println("[JavaVisualizer] [WS] WebSocketBridge constructor called with port: " + port);
        this.port = port;
    }

    public void start() {
        System.out.println("[JavaVisualizer] [WS] start() called");
        System.out.flush();
        try {
            URI serverUri = new URI("ws://localhost:" + port);
            System.out.println("[JavaVisualizer] [WS] Connecting to WebSocket server at " + serverUri);
            System.out.flush();

            client = new WebSocketClient(serverUri) {
                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    System.out.println("[JavaVisualizer] [WS] Connected to WebSocket server");
                    System.out.flush();
                    connected = true;

                    String identifyMessage = "{\"type\":\"identify\",\"role\":\"agent\"}";
                    send(identifyMessage);
                    System.out.println("[JavaVisualizer] [WS] Sent identification message");
                    System.out.flush();
                }

                @Override
                public void onMessage(String message) {
                    try {
                        Map<String, Object> event = gson.fromJson(message,
                                new TypeToken<Map<String, Object>>() {}.getType());

                        if (eventInjector != null) {
                            String type = (String) event.get("type");
                            if (type != null) {
                                if (type.startsWith("mouse_") && !type.equals("mouse_scroll")) {
                                    eventInjector.injectMouseEvent(event);
                                } else if (type.equals("mouse_scroll")) {
                                    eventInjector.injectScrollEvent(event);
                                } else if (type.startsWith("key_")) {
                                    eventInjector.injectKeyEvent(event);
                                }
                            }
                        } else {
                            System.err.println("[JavaVisualizer] [WS] EventInjector is null, cannot process event");
                        }
                    } catch (Exception e) {
                        System.err.println("[JavaVisualizer] [WS] Error processing event: " + e.getMessage());
                        e.printStackTrace();
                    }
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    System.out.println("[JavaVisualizer] [WS] Disconnected from WebSocket server: " + reason);
                    System.out.flush();
                    connected = false;
                }

                @Override
                public void onError(Exception ex) {
                    System.err.println("[JavaVisualizer] [WS] WebSocket error: " + ex.getMessage());
                    ex.printStackTrace();
                }
            };

            System.out.println("[JavaVisualizer] [WS] Calling client.connect() (async)");
            System.out.flush();
            client.connect();

            System.out.println("[JavaVisualizer] [WS] Waiting for connection (max 5s)...");
            System.out.flush();

            int maxRetries = 50;
            int retryCount = 0;
            while (!connected && retryCount < maxRetries) {
                Thread.sleep(100);
                retryCount++;
                if (retryCount % 10 == 0) {
                    System.out.println("[JavaVisualizer] [WS] Still waiting... " + (retryCount * 100) + "ms");
                }
            }

            if (connected) {
                System.out.println("[JavaVisualizer] [WS] Connection established after " + (retryCount * 100) + "ms");
            } else {
                System.err.println("[JavaVisualizer] [WS] Failed to connect to WebSocket server after " + maxRetries + " retries");
            }
            System.out.flush();

        } catch (Exception e) {
            System.err.println("[JavaVisualizer] [WS] Failed to start WebSocket client: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void sendFrame(byte[] frameData) {
        if (client != null && client.isOpen()) {
            client.send(ByteBuffer.wrap(frameData));
        }
    }

    public void setEventInjector(EventInjector injector) {
        this.eventInjector = injector;
        System.out.println("[JavaVisualizer] [WS] EventInjector set");
        System.out.flush();
    }

    public void stop() {
        System.out.println("[JavaVisualizer] [WS] stop() called");
        System.out.flush();
        if (client != null) {
            try {
                client.close();
            } catch (Exception e) {
                System.err.println("[JavaVisualizer] [WS] Error closing WebSocket client: " + e.getMessage());
            }
        }
    }
}
