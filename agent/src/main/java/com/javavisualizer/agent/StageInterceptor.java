package com.javavisualizer.agent;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.matcher.ElementMatchers;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;

public class StageInterceptor {

    public static void install(Instrumentation inst, WebSocketBridge bridge) {
        VisualizerAgent.log("[STAGE] install() called");
        VisualizerAgent.log("[STAGE] Instrumentation: " + (inst != null ? "OK" : "NULL"));
        VisualizerAgent.log("[STAGE] Bridge: " + (bridge != null ? "OK" : "NULL"));
        VisualizerAgent.flush();

        try {
            VisualizerAgent.log("[STAGE] Building AgentBuilder...");
            VisualizerAgent.flush();

            new AgentBuilder.Default()
                    .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                    .with(AgentBuilder.Listener.StreamWriting.toSystemOut())
                    .type(ElementMatchers.named("javafx.stage.Stage"))
                    .transform((builder, typeDescription, classLoader, module, protectionDomain) -> {
                        VisualizerAgent.log("[STAGE] >>> TRANSFORMING STAGE CLASS <<<");
                        VisualizerAgent.log("[STAGE] ClassLoader: " + classLoader);
                        VisualizerAgent.log("[STAGE] Module: " + module);
                        VisualizerAgent.log("[STAGE] Type: " + typeDescription);
                        VisualizerAgent.flush();
                        return builder.method(ElementMatchers.named("show"))
                                .intercept(Advice.to(ShowAdvice.class));
                    })
                    .installOn(inst);

            VisualizerAgent.log("[STAGE] AgentBuilder installed");
            VisualizerAgent.flush();
        } catch (Throwable t) {
            VisualizerAgent.logError("[STAGE] EXCEPTION during install", t);
        }

        ShowAdvice.bridge = bridge;
        VisualizerAgent.log("[STAGE] ShowAdvice.bridge set");
        VisualizerAgent.log("[STAGE] install() COMPLETED");
        VisualizerAgent.flush();
    }

    public static class ShowAdvice {
        public static volatile WebSocketBridge bridge;

        @Advice.OnMethodEnter
        public static void onShow(@Advice.This Object stage) {
            VisualizerAgent.log("[ADVICE] >>> Stage.show() INTERCEPTED! <<<");
            VisualizerAgent.flush();

            WebSocketBridge currentBridge = bridge;

            try {
                VisualizerAgent.log("[ADVICE] Bridge: " + (currentBridge != null ? "OK" : "NULL"));
                VisualizerAgent.flush();

                if (currentBridge == null) {
                    VisualizerAgent.logError("[ADVICE] Bridge is null, cannot proceed", null);
                    return;
                }

                Class<?> stageClass = stage.getClass();
                VisualizerAgent.log("[ADVICE] Stage class: " + stageClass.getName());
                VisualizerAgent.flush();

                Method setOpacity = stageClass.getMethod("setOpacity", double.class);
                Method setX = stageClass.getMethod("setX", double.class);
                Method setY = stageClass.getMethod("setY", double.class);
                Method getScene = stageClass.getMethod("getScene");

                setOpacity.invoke(stage, 0.0);
                setX.invoke(stage, -10000.0);
                setY.invoke(stage, -10000.0);
                VisualizerAgent.log("[ADVICE] Stage opacity=0, moved to (-10000, -10000)");
                VisualizerAgent.flush();

                Object scene = getScene.invoke(stage);
                VisualizerAgent.log("[ADVICE] Scene: " + (scene != null ? scene.getClass().getName() : "null"));
                VisualizerAgent.flush();

                if (scene != null) {
                    FrameCapture capture = new FrameCapture(scene, currentBridge);
                    capture.start();
                    VisualizerAgent.log("[ADVICE] FrameCapture created and started");
                    VisualizerAgent.flush();

                    EventInjector injector = new EventInjector(scene);
                    currentBridge.setEventInjector(injector);
                    VisualizerAgent.log("[ADVICE] EventInjector created and set");
                    VisualizerAgent.flush();
                } else {
                    VisualizerAgent.log("[ADVICE] Scene is null, will not capture frames");
                    VisualizerAgent.flush();
                }

            } catch (Throwable t) {
                VisualizerAgent.logError("[ADVICE] ERROR", t);
            }
        }
    }
}
