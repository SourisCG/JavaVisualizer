package com.javavisualizer.agent;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.matcher.ElementMatchers;

import java.lang.instrument.Instrumentation;

public class StageInterceptor {

    public static void install(Instrumentation inst, WebSocketBridge bridge) {
        new AgentBuilder.Default()
                .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                .type(ElementMatchers.named("javafx.stage.Stage"))
                .transform((builder, typeDescription, classLoader, module, protectionDomain) ->
                        builder.method(ElementMatchers.named("show"))
                                .intercept(Advice.to(ShowAdvice.class))
                )
                .installOn(inst);

        ShowAdvice.bridge = bridge;
    }

    public static class ShowAdvice {
        public static WebSocketBridge bridge;

        @Advice.OnMethodExit
        public static void onShow(@Advice.This Object stage) {
            try {
                javafx.stage.Stage fxStage = (javafx.stage.Stage) stage;

                fxStage.setX(-10000);
                fxStage.setY(-10000);

                javafx.scene.Scene scene = fxStage.getScene();
                if (scene != null) {
                    FrameCapture capture = new FrameCapture(scene, bridge);
                    capture.start();
                } else {
                    fxStage.sceneProperty().addListener((obs, oldScene, newScene) -> {
                        if (newScene != null) {
                            FrameCapture capture = new FrameCapture(newScene, bridge);
                            capture.start();
                        }
                    });
                }

                System.out.println("[JavaVisualizer] Stage intercepted and moved off-screen");
            } catch (Exception e) {
                System.err.println("[JavaVisualizer] Error intercepting stage: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
