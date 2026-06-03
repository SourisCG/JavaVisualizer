package com.javavisualizer.agent;

import java.lang.instrument.Instrumentation;

public class VisualizerAgent {

    private static WebSocketBridge bridge;

    public static void premain(String agentArgs, Instrumentation inst) {
        initialize(agentArgs, inst);
    }

    public static void agentmain(String agentArgs, Instrumentation inst) {
        initialize(agentArgs, inst);
    }

    private static void initialize(String agentArgs, Instrumentation inst) {
        int port = 9876;
        if (agentArgs != null && !agentArgs.isEmpty()) {
            try {
                port = Integer.parseInt(agentArgs);
            } catch (NumberFormatException e) {
                System.err.println("[JavaVisualizer] Invalid port argument: " + agentArgs + ", using default 9876");
            }
        }

        bridge = new WebSocketBridge(port);
        bridge.start();

        try {
            StageInterceptor.install(inst, bridge);
        } catch (Exception e) {
            System.err.println("[JavaVisualizer] Failed to install stage interceptor: " + e.getMessage());
            e.printStackTrace();
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (bridge != null) {
                bridge.stop();
            }
        }));

        System.out.println("[JavaVisualizer] Agent initialized on port " + port);
    }
}
