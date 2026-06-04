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
        VisualizerAgent.log("[WS] WebSocketBridge constructor called with port: " + port);
        this.port = port;
    }

    public void start() {
        VisualizerAgent.log("[WS] start() called");
        VisualizerAgent.flush();
        try {
            URI serverUri = new URI("ws://localhost:" + port);
            VisualizerAgent.log("[WS] Connecting to WebSocket server at " + serverUri);
            VisualizerAgent.flush();

            client = new WebSocketClient(serverUri) {
                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    VisualizerAgent.log("[WS] Connected to WebSocket server");
                    VisualizerAgent.flush();
                    connected = true;

                    String identifyMessage = "{\"type\":\"identify\",\"role\":\"agent\"}";
                    send(identifyMessage);
                    VisualizerAgent.log("[WS] Sent identification message");
                    VisualizerAgent.flush();
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
                            VisualizerAgent.logError("[WS] EventInjector is null, cannot process event", null);
                        }
                    } catch (Exception e) {
                        VisualizerAgent.logError("[WS] Error processing event", e);
                    }
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    VisualizerAgent.log("[WS] Disconnected from WebSocket server: " + reason);
                    VisualizerAgent.flush();
                    connected = false;
                }

                @Override
                public void onError(Exception ex) {
                    VisualizerAgent.logError("[WS] WebSocket error", ex);
                }
            };

            VisualizerAgent.log("[WS] Calling client.connect() (async)");
            VisualizerAgent.flush();
            client.connect();

            VisualizerAgent.log("[WS] Waiting for connection (max 5s)...");
            VisualizerAgent.flush();

            int maxRetries = 50;
            int retryCount = 0;
            while (!connected && retryCount < maxRetries) {
                Thread.sleep(100);
                retryCount++;
                if (retryCount % 10 == 0) {
                    VisualizerAgent.log("[WS] Still waiting... " + (retryCount * 100) + "ms");
                }
            }

            if (connected) {
                VisualizerAgent.log("[WS] Connection established after " + (retryCount * 100) + "ms");
            } else {
                VisualizerAgent.logError("[WS] Failed to connect to WebSocket server after " + maxRetries + " retries", null);
            }
            VisualizerAgent.flush();

        } catch (Exception e) {
            VisualizerAgent.logError("[WS] Failed to start WebSocket client", e);
        }
    }

    public void sendFrame(byte[] frameData) {
        if (client != null && client.isOpen()) {
            client.send(ByteBuffer.wrap(frameData));
        }
    }

    public void setEventInjector(EventInjector injector) {
        this.eventInjector = injector;
        VisualizerAgent.log("[WS] EventInjector set");
        VisualizerAgent.flush();
    }

    public void stop() {
        VisualizerAgent.log("[WS] stop() called");
        VisualizerAgent.flush();
        if (client != null) {
            try {
                client.close();
            } catch (Exception e) {
                VisualizerAgent.logError("[WS] Error closing WebSocket client", e);
            }
        }
    }
}
