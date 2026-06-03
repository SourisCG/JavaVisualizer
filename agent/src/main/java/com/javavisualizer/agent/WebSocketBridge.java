package com.javavisualizer.agent;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Map;

public class WebSocketBridge {

    private final int port;
    private WebSocketServer server;
    private WebSocket clientConnection;
    private final Gson gson = new Gson();
    private EventInjector eventInjector;

    public WebSocketBridge(int port) {
        this.port = port;
    }

    public void start() {
        server = new WebSocketServer(new InetSocketAddress(port)) {
            @Override
            public void onOpen(WebSocket conn, ClientHandshake handshake) {
                clientConnection = conn;
                System.out.println("[JavaVisualizer] Webview connected via WebSocket");
            }

            @Override
            public void onClose(WebSocket conn, int code, String reason, boolean remote) {
                if (conn == clientConnection) {
                    clientConnection = null;
                }
                System.out.println("[JavaVisualizer] Webview disconnected");
            }

            @Override
            public void onMessage(WebSocket conn, String message) {
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
                    }
                } catch (Exception e) {
                    System.err.println("[JavaVisualizer] Error processing event: " + e.getMessage());
                }
            }

            @Override
            public void onMessage(WebSocket conn, ByteBuffer message) {
            }

            @Override
            public void onError(WebSocket conn, Exception ex) {
                System.err.println("[JavaVisualizer] WebSocket error: " + ex.getMessage());
            }

            @Override
            public void onStart() {
                System.out.println("[JavaVisualizer] WebSocket server started on port " + port);
            }
        };

        server.start();
    }

    public void sendFrame(byte[] frameData) {
        if (clientConnection != null && clientConnection.isOpen()) {
            clientConnection.send(ByteBuffer.wrap(frameData));
        }
    }

    public void setEventInjector(EventInjector injector) {
        this.eventInjector = injector;
    }

    public void stop() {
        if (server != null) {
            try {
                server.stop(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
