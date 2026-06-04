package com.javavisualizer.agent;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class EventInjector {

    private static final Map<String, String> JS_TO_JAVAFX_KEY;

    static {
        JS_TO_JAVAFX_KEY = new HashMap<>();
        JS_TO_JAVAFX_KEY.put("Backquote", "BACK_QUOTE");
        JS_TO_JAVAFX_KEY.put("Backslash", "BACK_SLASH");
        JS_TO_JAVAFX_KEY.put("BracketLeft", "OPEN_BRACKET");
        JS_TO_JAVAFX_KEY.put("BracketRight", "CLOSE_BRACKET");
        JS_TO_JAVAFX_KEY.put("Comma", "COMMA");
        JS_TO_JAVAFX_KEY.put("Equal", "EQUALS");
        JS_TO_JAVAFX_KEY.put("Minus", "MINUS");
        JS_TO_JAVAFX_KEY.put("Period", "PERIOD");
        JS_TO_JAVAFX_KEY.put("Quote", "QUOTE");
        JS_TO_JAVAFX_KEY.put("Semicolon", "SEMICOLON");
        JS_TO_JAVAFX_KEY.put("Slash", "SLASH");
        JS_TO_JAVAFX_KEY.put("Space", "SPACE");
        JS_TO_JAVAFX_KEY.put("Tab", "TAB");
        JS_TO_JAVAFX_KEY.put("Enter", "ENTER");
        JS_TO_JAVAFX_KEY.put("Escape", "ESCAPE");
        JS_TO_JAVAFX_KEY.put("Backspace", "BACK_SPACE");
        JS_TO_JAVAFX_KEY.put("Delete", "DELETE");
        JS_TO_JAVAFX_KEY.put("Insert", "INSERT");
        JS_TO_JAVAFX_KEY.put("Home", "HOME");
        JS_TO_JAVAFX_KEY.put("End", "END");
        JS_TO_JAVAFX_KEY.put("PageUp", "PAGE_UP");
        JS_TO_JAVAFX_KEY.put("PageDown", "PAGE_DOWN");
        JS_TO_JAVAFX_KEY.put("CapsLock", "CAPS");
        JS_TO_JAVAFX_KEY.put("NumLock", "NUM_LOCK");
        JS_TO_JAVAFX_KEY.put("ScrollLock", "SCROLL_LOCK");
        JS_TO_JAVAFX_KEY.put("PrintScreen", "PRINTSCREEN");
        JS_TO_JAVAFX_KEY.put("Pause", "PAUSE");
        JS_TO_JAVAFX_KEY.put("ArrowUp", "UP");
        JS_TO_JAVAFX_KEY.put("ArrowDown", "DOWN");
        JS_TO_JAVAFX_KEY.put("ArrowLeft", "LEFT");
        JS_TO_JAVAFX_KEY.put("ArrowRight", "RIGHT");
        JS_TO_JAVAFX_KEY.put("ShiftLeft", "SHIFT");
        JS_TO_JAVAFX_KEY.put("ShiftRight", "SHIFT");
        JS_TO_JAVAFX_KEY.put("ControlLeft", "CONTROL");
        JS_TO_JAVAFX_KEY.put("ControlRight", "CONTROL");
        JS_TO_JAVAFX_KEY.put("AltLeft", "ALT");
        JS_TO_JAVAFX_KEY.put("AltRight", "ALT");
        JS_TO_JAVAFX_KEY.put("MetaLeft", "META");
        JS_TO_JAVAFX_KEY.put("MetaRight", "META");
    }

    private final Object scene;
    private ClassLoader classLoader;

    public EventInjector(Object scene) {
        this.scene = scene;
        this.classLoader = scene.getClass().getClassLoader();
    }

    public void injectMouseEvent(Map<String, Object> event) {
        try {
            Class<?> platformClass = Class.forName("javafx.application.Platform", true, classLoader);
            Method runLaterMethod = platformClass.getMethod("runLater", Runnable.class);

            runLaterMethod.invoke(null, (Runnable) () -> {
                try {
                    String type = (String) event.get("type");
                    double x = ((Number) event.get("x")).doubleValue();
                    double y = ((Number) event.get("y")).doubleValue();
                    int button = event.containsKey("button") ? ((Number) event.get("button")).intValue() : 0;

                    Class<?> mouseButtonClass = Class.forName("javafx.scene.input.MouseButton", true, classLoader);
                    Object[] buttonValues = (Object[]) mouseButtonClass.getMethod("values").invoke(null);
                    Object fxButton = buttonValues[Math.min(button, buttonValues.length - 1)];

                    Class<?> mouseEventClass = Class.forName("javafx.scene.input.MouseEvent", true, classLoader);
                    Class<?> eventTypeClass = Class.forName("javafx.event.EventType", true, classLoader);

                    String eventTypeName = switch (type) {
                        case "mouse_press" -> "MOUSE_PRESSED";
                        case "mouse_release" -> "MOUSE_RELEASED";
                        case "mouse_move" -> "MOUSE_MOVED";
                        case "mouse_click" -> "MOUSE_CLICKED";
                        default -> null;
                    };

                    if (eventTypeName != null) {
                        Object eventType = mouseEventClass.getField(eventTypeName).get(null);

                        @SuppressWarnings("unchecked")
                        Map<String, Boolean> modifiers = (Map<String, Boolean>) event.get("modifiers");
                        boolean shift = modifiers != null && modifiers.getOrDefault("shift", false);
                        boolean ctrl = modifiers != null && modifiers.getOrDefault("ctrl", false);
                        boolean alt = modifiers != null && modifiers.getOrDefault("alt", false);
                        boolean meta = modifiers != null && modifiers.getOrDefault("meta", false);

                        Constructor<?> mouseEventConstructor = mouseEventClass.getConstructor(
                            eventTypeClass, double.class, double.class, double.class, double.class,
                            mouseButtonClass, int.class,
                            boolean.class, boolean.class, boolean.class, boolean.class,
                            boolean.class, boolean.class, boolean.class, boolean.class,
                            boolean.class, boolean.class, Class.forName("javafx.scene.input.PickResult", true, classLoader)
                        );

                        Object mouseEvent = mouseEventConstructor.newInstance(
                            eventType, x, y, x, y,
                            fxButton, 1,
                            shift, ctrl, alt, meta,
                            false, false, false, false,
                            false, false, null
                        );

                        Method getRootMethod = scene.getClass().getMethod("getRoot");
                        Object root = getRootMethod.invoke(scene);

                        Class<?> eventClass = Class.forName("javafx.event.Event", true, classLoader);
                        Class<?> eventTargetClass = Class.forName("javafx.event.EventTarget", true, classLoader);
                        Method fireEventMethod = eventClass.getMethod("fireEvent",
                            eventTargetClass, eventClass);
                        fireEventMethod.invoke(null, root, mouseEvent);
                    }
                } catch (Exception e) {
                    System.err.println("[JavaVisualizer] Mouse event injection error: " + e.getMessage());
                    e.printStackTrace();
                }
            });
        } catch (Exception e) {
            System.err.println("[JavaVisualizer] Mouse event scheduling error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void injectScrollEvent(Map<String, Object> event) {
        try {
            Class<?> platformClass = Class.forName("javafx.application.Platform", true, classLoader);
            Method runLaterMethod = platformClass.getMethod("runLater", Runnable.class);

            runLaterMethod.invoke(null, (Runnable) () -> {
                try {
                    double x = ((Number) event.get("x")).doubleValue();
                    double y = ((Number) event.get("y")).doubleValue();
                    double deltaX = ((Number) event.get("deltaX")).doubleValue();
                    double deltaY = ((Number) event.get("deltaY")).doubleValue();

                    Class<?> scrollEventClass = Class.forName("javafx.scene.input.ScrollEvent", true, classLoader);
                    Class<?> eventTypeClass = Class.forName("javafx.event.EventType", true, classLoader);
                    Class<?> hTextUnitsClass = Class.forName("javafx.scene.input.ScrollEvent$HorizontalTextScrollUnits", true, classLoader);
                    Class<?> vTextUnitsClass = Class.forName("javafx.scene.input.ScrollEvent$VerticalTextScrollUnits", true, classLoader);

                    Object scrollEventType = scrollEventClass.getField("SCROLL").get(null);
                    Object hTextUnits = hTextUnitsClass.getField("NONE").get(null);
                    Object vTextUnits = vTextUnitsClass.getField("NONE").get(null);

                    Constructor<?> scrollEventConstructor = scrollEventClass.getConstructor(
                        eventTypeClass, double.class, double.class, double.class, double.class,
                        boolean.class, boolean.class, boolean.class, boolean.class,
                        boolean.class, boolean.class,
                        double.class, double.class, double.class, double.class,
                        hTextUnitsClass, double.class, vTextUnitsClass, double.class,
                        int.class, Class.forName("javafx.scene.input.PickResult", true, classLoader)
                    );

                    Object scrollEvent = scrollEventConstructor.newInstance(
                        scrollEventType, x, y, x, y,
                        false, false, false, false,
                        false, false,
                        deltaX, deltaY, 0, 0,
                        hTextUnits, 0, vTextUnits, 0,
                        0, null
                    );

                    Method getRootMethod = scene.getClass().getMethod("getRoot");
                    Object root = getRootMethod.invoke(scene);

                    Class<?> eventClass = Class.forName("javafx.event.Event", true, classLoader);
                    Class<?> eventTargetClass = Class.forName("javafx.event.EventTarget", true, classLoader);
                    Method fireEventMethod = eventClass.getMethod("fireEvent",
                        eventTargetClass, eventClass);
                    fireEventMethod.invoke(null, root, scrollEvent);
                } catch (Exception e) {
                    System.err.println("[JavaVisualizer] Scroll event injection error: " + e.getMessage());
                    e.printStackTrace();
                }
            });
        } catch (Exception e) {
            System.err.println("[JavaVisualizer] Scroll event scheduling error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void injectKeyEvent(Map<String, Object> event) {
        try {
            Class<?> platformClass = Class.forName("javafx.application.Platform", true, classLoader);
            Method runLaterMethod = platformClass.getMethod("runLater", Runnable.class);

            runLaterMethod.invoke(null, (Runnable) () -> {
                try {
                    String type = (String) event.get("type");
                    String key = (String) event.get("key");
                    String code = (String) event.get("code");

                    Class<?> keyCodeClass = Class.forName("javafx.scene.input.KeyCode", true, classLoader);
                    Object keyCode;
                    try {
                        String javaCode = mapJsCodeToJavaFx(code);
                        Method valueOfMethod = keyCodeClass.getMethod("valueOf", String.class);
                        keyCode = valueOfMethod.invoke(null, javaCode);
                    } catch (Exception e) {
                        keyCode = keyCodeClass.getField("UNDEFINED").get(null);
                    }

                    @SuppressWarnings("unchecked")
                    Map<String, Boolean> modifiers = (Map<String, Boolean>) event.get("modifiers");
                    boolean shift = modifiers != null && modifiers.getOrDefault("shift", false);
                    boolean ctrl = modifiers != null && modifiers.getOrDefault("ctrl", false);
                    boolean alt = modifiers != null && modifiers.getOrDefault("alt", false);
                    boolean meta = modifiers != null && modifiers.getOrDefault("meta", false);

                    Class<?> keyEventClass = Class.forName("javafx.scene.input.KeyEvent", true, classLoader);
                    Class<?> eventTypeClass = Class.forName("javafx.event.EventType", true, classLoader);

                    String eventTypeName = switch (type) {
                        case "key_press" -> "KEY_PRESSED";
                        case "key_release" -> "KEY_RELEASED";
                        default -> "KEY_TYPED";
                    };

                    Object eventType = keyEventClass.getField(eventTypeName).get(null);

                    String character = (type.equals("key_press") || type.equals("key_release")) ? "" : key;
                    Method getNameMethod = keyCodeClass.getMethod("getName");
                    String text = (type.equals("key_press") || type.equals("key_release")) ? (String) getNameMethod.invoke(keyCode) : "";

                    Constructor<?> keyEventConstructor = keyEventClass.getConstructor(
                        eventTypeClass, String.class, String.class, keyCodeClass,
                        boolean.class, boolean.class, boolean.class, boolean.class
                    );

                    Object keyEvent = keyEventConstructor.newInstance(
                        eventType, character, text, keyCode,
                        shift, ctrl, alt, meta
                    );

                    Method getRootMethod = scene.getClass().getMethod("getRoot");
                    Object root = getRootMethod.invoke(scene);

                    Class<?> eventClass = Class.forName("javafx.event.Event", true, classLoader);
                    Class<?> eventTargetClass = Class.forName("javafx.event.EventTarget", true, classLoader);
                    Method fireEventMethod = eventClass.getMethod("fireEvent",
                        eventTargetClass, eventClass);
                    fireEventMethod.invoke(null, root, keyEvent);
                } catch (Exception e) {
                    System.err.println("[JavaVisualizer] Key event injection error: " + e.getMessage());
                    e.printStackTrace();
                }
            });
            } catch (Exception e) {
            System.err.println("[JavaVisualizer] Key event scheduling error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static String mapJsCodeToJavaFx(String jsCode) {
        if (jsCode == null) return "UNDEFINED";

        String direct = JS_TO_JAVAFX_KEY.get(jsCode);
        if (direct != null) return direct;

        String upper = jsCode.toUpperCase();

        if (upper.startsWith("KEY")) return upper.substring(3);
        if (upper.startsWith("DIGIT")) return upper.substring(5);
        if (upper.startsWith("ARROW")) return upper.substring(5);
        if (upper.startsWith("NUMPAD")) return upper;

        return upper;
    }
}
