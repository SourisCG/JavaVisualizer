package com.javavisualizer.agent;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.matcher.ElementMatchers;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.security.ProtectionDomain;

public class StageInterceptor {

    public static void install(Instrumentation inst, WebSocketBridge bridge) {
        System.out.println("[JavaVisualizer] [STAGE] install() called");
        System.out.println("[JavaVisualizer] [STAGE] Instrumentation: " + (inst != null ? "OK" : "NULL"));
        System.out.println("[JavaVisualizer] [STAGE] Bridge: " + (bridge != null ? "OK" : "NULL"));
        System.out.flush();

        try {
            System.out.println("[JavaVisualizer] [STAGE] Building AgentBuilder...");
            System.out.flush();

            new AgentBuilder.Default()
                    .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                    .with(AgentBuilder.Listener.StreamWriting.toSystemOut())
                    .type(ElementMatchers.named("javafx.stage.Stage"))
                    .transform((builder, typeDescription, classLoader, module, protectionDomain) -> {
                        System.out.println("[JavaVisualizer] [STAGE] >>> TRANSFORMING STAGE CLASS <<<");
                        System.out.println("[JavaVisualizer] [STAGE] ClassLoader: " + classLoader);
                        System.out.println("[JavaVisualizer] [STAGE] Module: " + module);
                        System.out.println("[JavaVisualizer] [STAGE] Type: " + typeDescription);
                        System.out.flush();
                        return builder.method(ElementMatchers.named("show"))
                                .intercept(Advice.to(ShowAdvice.class));
                    })
                    .installOn(inst);

            System.out.println("[JavaVisualizer] [STAGE] AgentBuilder installed");
            System.out.flush();
        } catch (Throwable t) {
            System.err.println("[JavaVisualizer] [STAGE] EXCEPTION during install:");
            t.printStackTrace();
            System.err.flush();
        }

        ShowAdvice.bridge = bridge;
        System.out.println("[JavaVisualizer] [STAGE] ShowAdvice.bridge set");
        System.out.println("[JavaVisualizer] [STAGE] install() COMPLETED");
        System.out.flush();
    }

    public static class ShowAdvice {
        public static volatile WebSocketBridge bridge;

        @Advice.OnMethodExit
        public static void onShow(@Advice.This Object stage) {
            System.out.println("[JavaVisualizer] [ADVICE] >>> Stage.show() INTERCEPTED! <<<");
            System.out.flush();

            WebSocketBridge currentBridge = bridge;

            try {
                System.out.println("[JavaVisualizer] [ADVICE] Bridge: " + (currentBridge != null ? "OK" : "NULL"));
                System.out.flush();

                if (currentBridge == null) {
                    System.err.println("[JavaVisualizer] [ADVICE] Bridge is null, cannot proceed");
                    return;
                }

                Class<?> stageClass = stage.getClass();
                System.out.println("[JavaVisualizer] [ADVICE] Stage class: " + stageClass.getName());
                System.out.flush();

                Method setX = stageClass.getMethod("setX", double.class);
                Method setY = stageClass.getMethod("setY", double.class);
                Method getScene = stageClass.getMethod("getScene");

                setX.invoke(stage, -10000.0);
                setY.invoke(stage, -10000.0);
                System.out.println("[JavaVisualizer] [ADVICE] Stage moved to (-10000, -10000)");
                System.out.flush();

                Object scene = getScene.invoke(stage);
                System.out.println("[JavaVisualizer] [ADVICE] Scene: " + (scene != null ? scene.getClass().getName() : "null"));
                System.out.flush();

                if (scene != null) {
                    FrameCapture capture = new FrameCapture(scene, currentBridge);
                    capture.start();
                    System.out.println("[JavaVisualizer] [ADVICE] FrameCapture created and started");
                    System.out.flush();

                    EventInjector injector = new EventInjector(scene);
                    currentBridge.setEventInjector(injector);
                    System.out.println("[JavaVisualizer] [ADVICE] EventInjector created and set");
                    System.out.flush();
                } else {
                    System.out.println("[JavaVisualizer] [ADVICE] Scene is null, will not capture frames");
                    System.out.flush();
                }

            } catch (Throwable t) {
                System.err.println("[JavaVisualizer] [ADVICE] ERROR: " + t.getClass().getName() + ": " + t.getMessage());
                t.printStackTrace();
                System.err.flush();
            }
        }
    }
}
