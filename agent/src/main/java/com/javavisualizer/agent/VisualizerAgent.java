package com.javavisualizer.agent;

import java.lang.instrument.Instrumentation;

public class VisualizerAgent {

    private static WebSocketBridge bridge;

    static {
        System.out.println("[JavaVisualizer] ========== AGENT CLASS LOADED ==========");
        System.out.println("[JavaVisualizer] Classloader: " + VisualizerAgent.class.getClassLoader());
        System.out.println("[JavaVisualizer] Thread: " + Thread.currentThread().getName());
    }

    public static void premain(String agentArgs, Instrumentation inst) {
        System.out.println("[JavaVisualizer] ========== PREMAIN CALLED ==========");
        System.out.println("[JavaVisualizer] agentArgs: " + agentArgs);
        System.out.println("[JavaVisualizer] Instrumentation: " + (inst != null ? "available" : "NULL"));
        System.out.flush();

        try {
            initialize(agentArgs, inst);
        } catch (Throwable t) {
            System.err.println("[JavaVisualizer] ========== PREMAIN FAILED ==========");
            t.printStackTrace();
            System.err.flush();
        }
    }

    public static void agentmain(String agentArgs, Instrumentation inst) {
        System.out.println("[JavaVisualizer] ========== AGENTMAIN CALLED ==========");
        System.out.flush();
        try {
            initialize(agentArgs, inst);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private static void initialize(String agentArgs, Instrumentation inst) {
        System.out.println("[JavaVisualizer] [INIT] Step 1: Parsing port");
        System.out.flush();

        int port = 9876;
        if (agentArgs != null && !agentArgs.isEmpty()) {
            try {
                port = Integer.parseInt(agentArgs);
                System.out.println("[JavaVisualizer] [INIT] Port parsed: " + port);
            } catch (NumberFormatException e) {
                System.err.println("[JavaVisualizer] Invalid port argument: " + agentArgs + ", using default 9876");
            }
        } else {
            System.out.println("[JavaVisualizer] [INIT] No agent args, using default port: " + port);
        }

        System.out.println("[JavaVisualizer] [INIT] Step 2: Creating WebSocketBridge");
        System.out.flush();
        try {
            bridge = new WebSocketBridge(port);
            System.out.println("[JavaVisualizer] [INIT] WebSocketBridge created");
        } catch (Throwable t) {
            System.err.println("[JavaVisualizer] [INIT] FAILED to create WebSocketBridge");
            t.printStackTrace();
            return;
        }

        System.out.println("[JavaVisualizer] [INIT] Step 3: Starting WebSocketBridge");
        System.out.flush();
        try {
            bridge.start();
            System.out.println("[JavaVisualizer] [INIT] WebSocketBridge.start() returned");
        } catch (Throwable t) {
            System.err.println("[JavaVisualizer] [INIT] FAILED to start WebSocketBridge");
            t.printStackTrace();
        }

        System.out.println("[JavaVisualizer] [INIT] Step 4: Installing StageInterceptor");
        System.out.flush();
        try {
            StageInterceptor.install(inst, bridge);
            System.out.println("[JavaVisualizer] [INIT] StageInterceptor.install() returned");
        } catch (Throwable t) {
            System.err.println("[JavaVisualizer] [INIT] FAILED to install StageInterceptor");
            t.printStackTrace();
        }

        System.out.println("[JavaVisualizer] [INIT] Step 5: Adding shutdown hook");
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("[JavaVisualizer] Shutdown hook triggered");
            if (bridge != null) {
                bridge.stop();
            }
        }, "JavaVisualizer-ShutdownHook"));

        System.out.println("[JavaVisualizer] ========== AGENT INITIALIZED ON PORT " + port + " ==========");
        System.out.flush();
    }
}
