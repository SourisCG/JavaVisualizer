package com.javavisualizer.agent;

import java.lang.instrument.Instrumentation;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringWriter;

public class VisualizerAgent {

    private static WebSocketBridge bridge;
    private static final String LOG_FILE = "/tmp/javavisualizer-agent.log";

    static {
        log("========== AGENT CLASS LOADED ==========");
        log("Classloader: " + VisualizerAgent.class.getClassLoader());
        log("Thread: " + Thread.currentThread().getName());
    }

    public static void premain(String agentArgs, Instrumentation inst) {
        log("========== PREMAIN CALLED ==========");
        log("agentArgs: " + agentArgs);
        log("Instrumentation: " + (inst != null ? "available" : "NULL"));
        flush();

        try {
            initialize(agentArgs, inst);
        } catch (Throwable t) {
            logError("========== PREMAIN FAILED ==========", t);
            flush();
        }
    }

    public static void agentmain(String agentArgs, Instrumentation inst) {
        log("========== AGENTMAIN CALLED ==========");
        flush();
        try {
            initialize(agentArgs, inst);
        } catch (Throwable t) {
            logError("========== AGENTMAIN FAILED ==========", t);
        }
    }

    private static void initialize(String agentArgs, Instrumentation inst) {
        log("[INIT] Step 1: Parsing port");
        flush();

        int port = 9876;
        if (agentArgs != null && !agentArgs.isEmpty()) {
            try {
                port = Integer.parseInt(agentArgs);
                log("[INIT] Port parsed: " + port);
            } catch (NumberFormatException e) {
                logError("Invalid port argument: " + agentArgs + ", using default 9876", e);
            }
        } else {
            log("[INIT] No agent args, using default port: " + port);
        }

        log("[INIT] Step 2: Creating WebSocketBridge");
        flush();
        try {
            bridge = new WebSocketBridge(port);
            log("[INIT] WebSocketBridge created");
        } catch (Throwable t) {
            logError("[INIT] FAILED to create WebSocketBridge", t);
            return;
        }

        log("[INIT] Step 3: Starting WebSocketBridge");
        flush();
        try {
            bridge.start();
            log("[INIT] WebSocketBridge.start() returned");
        } catch (Throwable t) {
            logError("[INIT] FAILED to start WebSocketBridge", t);
        }

        log("[INIT] Step 4: Installing StageInterceptor");
        flush();
        try {
            StageInterceptor.install(inst, bridge);
            log("[INIT] StageInterceptor.install() returned");
        } catch (Throwable t) {
            logError("[INIT] FAILED to install StageInterceptor", t);
        }

        log("[INIT] Step 5: Adding shutdown hook");
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log("Shutdown hook triggered");
            if (bridge != null) {
                bridge.stop();
            }
        }, "JavaVisualizer-ShutdownHook"));

        log("========== AGENT INITIALIZED ON PORT " + port + " ==========");
        flush();
    }

    public static synchronized void log(String message) {
        System.out.println("[JavaVisualizer] " + message);
        try (FileWriter fw = new FileWriter(LOG_FILE, true)) {
            fw.write("[" + System.currentTimeMillis() + "] " + message + "\n");
        } catch (Exception e) {
            // Ignore
        }
    }

    public static synchronized void logError(String message, Throwable t) {
        System.err.println("[JavaVisualizer] " + message);
        if (t != null) {
            t.printStackTrace();
        }
        try (FileWriter fw = new FileWriter(LOG_FILE, true)) {
            fw.write("[" + System.currentTimeMillis() + "] ERROR: " + message + "\n");
            StringWriter sw = new StringWriter();
            if (t != null) {
                t.printStackTrace(new PrintWriter(sw));
                fw.write(sw.toString());
            }
        } catch (Exception e) {
            // Ignore
        }
    }

    public static void flush() {
        System.out.flush();
        System.err.flush();
    }
}
