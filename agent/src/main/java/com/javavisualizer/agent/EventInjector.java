package com.javavisualizer.agent;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;

import java.util.Map;

public class EventInjector {

    private final Scene scene;

    public EventInjector(Scene scene) {
        this.scene = scene;
    }

    public void injectMouseEvent(Map<String, Object> event) {
        Platform.runLater(() -> {
            try {
                String type = (String) event.get("type");
                double x = ((Number) event.get("x")).doubleValue();
                double y = ((Number) event.get("y")).doubleValue();
                int button = event.containsKey("button") ? ((Number) event.get("button")).intValue() : 0;

                MouseButton fxButton = switch (button) {
                    case 0 -> MouseButton.PRIMARY;
                    case 1 -> MouseButton.MIDDLE;
                    case 2 -> MouseButton.SECONDARY;
                    default -> MouseButton.NONE;
                };

                javafx.event.EventType<MouseEvent> eventType = switch (type) {
                    case "mouse_press" -> MouseEvent.MOUSE_PRESSED;
                    case "mouse_release" -> MouseEvent.MOUSE_RELEASED;
                    case "mouse_move" -> MouseEvent.MOUSE_MOVED;
                    case "mouse_click" -> MouseEvent.MOUSE_CLICKED;
                    default -> null;
                };

                if (eventType != null) {
                    MouseEvent mouseEvent = new MouseEvent(
                            eventType,
                            x, y, x, y,
                            fxButton, 1,
                            false, false, false, false,
                            false, false, false, false,
                            false, false, null
                    );
                    javafx.event.Event.fireEvent(scene.getRoot(), mouseEvent);
                }
            } catch (Exception e) {
                System.err.println("[JavaVisualizer] Mouse event injection error: " + e.getMessage());
            }
        });
    }

    public void injectScrollEvent(Map<String, Object> event) {
        Platform.runLater(() -> {
            try {
                double x = ((Number) event.get("x")).doubleValue();
                double y = ((Number) event.get("y")).doubleValue();
                double deltaX = ((Number) event.get("deltaX")).doubleValue();
                double deltaY = ((Number) event.get("deltaY")).doubleValue();

                ScrollEvent scrollEvent = new ScrollEvent(
                        ScrollEvent.SCROLL,
                        x, y, x, y,
                        false, false, false, false,
                        false, false,
                        deltaX, deltaY, 0, 0,
                        ScrollEvent.HorizontalTextScrollUnits.NONE, 0,
                        ScrollEvent.VerticalTextScrollUnits.NONE, 0,
                        0, null
                );
                javafx.event.Event.fireEvent(scene.getRoot(), scrollEvent);
            } catch (Exception e) {
                System.err.println("[JavaVisualizer] Scroll event injection error: " + e.getMessage());
            }
        });
    }

    public void injectKeyEvent(Map<String, Object> event) {
        Platform.runLater(() -> {
            try {
                String type = (String) event.get("type");
                String key = (String) event.get("key");
                String code = (String) event.get("code");

                javafx.scene.input.KeyCode keyCode;
                try {
                    keyCode = javafx.scene.input.KeyCode.valueOf(code.toUpperCase().replace("KEY", "").replace("DIGIT", "").replace("ARROW", ""));
                } catch (IllegalArgumentException e) {
                    keyCode = javafx.scene.input.KeyCode.UNDEFINED;
                }

                @SuppressWarnings("unchecked")
                Map<String, Boolean> modifiers = (Map<String, Boolean>) event.get("modifiers");
                boolean shift = modifiers != null && modifiers.getOrDefault("shift", false);
                boolean ctrl = modifiers != null && modifiers.getOrDefault("ctrl", false);
                boolean alt = modifiers != null && modifiers.getOrDefault("alt", false);
                boolean meta = modifiers != null && modifiers.getOrDefault("meta", false);

                javafx.event.EventType<javafx.scene.input.KeyEvent> eventType = switch (type) {
                    case "key_press" -> javafx.scene.input.KeyEvent.KEY_PRESSED;
                    case "key_release" -> javafx.scene.input.KeyEvent.KEY_RELEASED;
                    default -> javafx.scene.input.KeyEvent.KEY_TYPED;
                };

                String character = (type.equals("key_press") || type.equals("key_release")) ? "" : key;
                String text = (type.equals("key_press") || type.equals("key_release")) ? keyCode.getName() : "";

                javafx.scene.input.KeyEvent keyEvent = new javafx.scene.input.KeyEvent(
                        eventType,
                        character,
                        text,
                        keyCode,
                        shift, ctrl, alt, meta
                );
                javafx.event.Event.fireEvent(scene.getRoot(), keyEvent);
            } catch (Exception e) {
                System.err.println("[JavaVisualizer] Key event injection error: " + e.getMessage());
            }
        });
    }
}
