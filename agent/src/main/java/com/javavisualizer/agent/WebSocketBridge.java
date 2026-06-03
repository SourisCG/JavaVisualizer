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
    private boolean connected = false;

    public WebSocketBridge(int port) {
        this.port = port;
    }

    public void start() {
        try {
            URI serverUri = new URI("ws://localhost:" + port);
            System.out.println("[JavaVisualizer] Connecting to WebSocket server at " + serverUri);

            client = new WebSocketClient(serverUri) {
                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    System.out.println("[JavaVisualizer] Connected to WebSocket server");
                    connected = true;
                    
                    String identifyMessage = "{\"type\":\"identify\",\"role\":\"agent\"}";
                    send(identifyMessage);
                    System.out.println("[JavaVisualizer] Sent identification message");
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
                            System.err.println("[JavaVisualizer] EventInjector is null, cannot process event");
                        }
                    } catch (Exception e) {
                        System.err.println("[JavaVisualizer] Error processing event: " + e.getMessage());
                        e.printStackTrace();
                    }
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    System.out.println("[JavaVisualizer] Disconnected from WebSocket server: " + reason);
                    connected = false;
                }

                @Override
                public void onError(Exception ex) {
                    System.err.println("[JavaVisualizer] WebSocket error: " + ex.getMessage());
                    ex.printStackTrace();
                }
            };

            client.connect();
            
            int maxRetries = 50;
            int retryCount = 0;
            while (!connected && retryCount < maxRetries) {
                Thread.sleep(100);
                retryCount++;
            }
            
            if (!connected) {
                System.err.println("[JavaVisualizer] Failed to connect to WebSocket server after " + maxRetries + " retries");
            }
            
        } catch (Exception e) {
            System.err.println("[JavaVisualizer] Failed to start WebSocket client: " + e.getMessage());
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
        System.out.println("[JavaVisualizer] EventInjector set");
    }

    public void stop() {
        if (client != null) {
            try {
                client.close();
            } catch (Exception e) {
                System.err.println("[JavaVisualizer] Error closing WebSocket client: " + e.getMessage());
            }
        }
    }
}
