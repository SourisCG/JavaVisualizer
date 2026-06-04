package com.javavisualizer.agent;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.matcher.ElementMatchers;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;

public class StageInterceptor {

    public static void install(Instrumentation inst, WebSocketBridge bridge) {
        System.out.println("[JavaVisualizer] Installing Stage interceptor...");

        new AgentBuilder.Default()
                .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                .with(AgentBuilder.Listener.StreamWriting.toSystemOut())
                .type(ElementMatchers.named("javafx.stage.Stage"))
                .transform((builder, typeDescription, classLoader, module, protectionDomain) -> {
                    System.out.println("[JavaVisualizer] Transforming Stage class from classloader: " + classLoader);
                    return builder.method(ElementMatchers.named("show"))
                            .intercept(Advice.to(ShowAdvice.class));
                })
                .installOn(inst);

        ShowAdvice.bridge = bridge;
        System.out.println("[JavaVisualizer] Stage interceptor installed successfully");
    }

    public static class ShowAdvice {
        public static WebSocketBridge bridge;

        @Advice.OnMethodExit
        public static void onShow(@Advice.This Object stage) {
            try {
                System.out.println("[JavaVisualizer] Stage.show() intercepted!");

                Class<?> stageClass = stage.getClass();

                Method setX = stageClass.getMethod("setX", double.class);
                Method setY = stageClass.getMethod("setY", double.class);
                Method getScene = stageClass.getMethod("getScene");

                setX.invoke(stage, -10000.0);
                setY.invoke(stage, -10000.0);
                System.out.println("[JavaVisualizer] Stage moved to (-10000, -10000)");

                Object scene = getScene.invoke(stage);

                if (scene != null) {
                    FrameCapture capture = new FrameCapture(scene, bridge);
                    capture.start();

                    EventInjector injector = new EventInjector(scene);
                    bridge.setEventInjector(injector);

                    System.out.println("[JavaVisualizer] FrameCapture and EventInjector initialized");
                } else {
                    System.out.println("[JavaVisualizer] Scene is null, setting up listener for delayed scene");

                    Method scenePropertyMethod = stageClass.getMethod("sceneProperty");
                    Object sceneProperty = scenePropertyMethod.invoke(stage);

                    Class<?> propertyClass = Class.forName("javafx.beans.value.ObservableValue");
                    Class<?> changeListenerClass = Class.forName("javafx.beans.value.ChangeListener");

                    Object listener = java.lang.reflect.Proxy.newProxyInstance(
                        stageClass.getClassLoader(),
                        new Class<?>[]{changeListenerClass},
                        (proxy, method, args) -> {
                            if ("changed".equals(method.getName()) && args.length == 3) {
                                Object newScene = args[2];
                                if (newScene != null) {
                                    FrameCapture capture = new FrameCapture(newScene, bridge);
                                    capture.start();

                                    EventInjector injector = new EventInjector(newScene);
                                    bridge.setEventInjector(injector);

                                    System.out.println("[JavaVisualizer] FrameCapture and EventInjector initialized (delayed)");
                                }
                            }
                            return null;
                        }
                    );

                    Method addListenerMethod = propertyClass.getMethod("addListener", changeListenerClass);
                    addListenerMethod.invoke(sceneProperty, listener);
                }

                System.out.println("[JavaVisualizer] Stage intercepted and moved off-screen");
            } catch (Exception e) {
                System.err.println("[JavaVisualizer] Error intercepting stage: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
