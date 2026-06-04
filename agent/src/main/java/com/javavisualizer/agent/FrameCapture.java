package com.javavisualizer.agent;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Method;
import java.util.Iterator;

public class FrameCapture {

    private final Object scene;
    private final WebSocketBridge bridge;
    private Object timer;
    private long lastFrameTime = 0;
    private static final int TARGET_FPS = 30;
    private static final float JPEG_QUALITY = 0.75f;

    public FrameCapture(Object scene, WebSocketBridge bridge) {
        this.scene = scene;
        this.bridge = bridge;
    }

    public void start() {
        try {
            Class<?> animationTimerClass = Class.forName("javafx.animation.AnimationTimer");
            Object timerInstance = java.lang.reflect.Proxy.newProxyInstance(
                scene.getClass().getClassLoader(),
                new Class<?>[]{animationTimerClass},
                (proxy, method, args) -> {
                    if ("handle".equals(method.getName()) && args.length == 1) {
                        long now = (Long) args[0];
                        long elapsed = now - lastFrameTime;
                        if (elapsed < 1_000_000_000L / TARGET_FPS) {
                            return null;
                        }
                        lastFrameTime = now;
                        captureAndSend();
                    }
                    return null;
                }
            );

            Method startMethod = animationTimerClass.getMethod("start");
            startMethod.invoke(timerInstance);
            this.timer = timerInstance;

            System.out.println("[JavaVisualizer] FrameCapture started");
        } catch (Exception e) {
            System.err.println("[JavaVisualizer] Failed to start FrameCapture: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void captureAndSend() {
        try {
            Class<?> platformClass = Class.forName("javafx.application.Platform");
            Method runLaterMethod = platformClass.getMethod("runLater", Runnable.class);

            runLaterMethod.invoke(null, (Runnable) () -> {
                try {
                    Class<?> sceneClass = scene.getClass();
                    Method snapshotMethod = sceneClass.getMethod("snapshot", Class.forName("javafx.scene.SnapshotParameters"));
                    Object snapshot = snapshotMethod.invoke(scene, new Object[]{null});

                    Class<?> swingFXUtilsClass = Class.forName("javafx.embed.swing.SwingFXUtils");
                    Method fromFXImageMethod = swingFXUtilsClass.getMethod("fromFXImage",
                        Class.forName("javafx.scene.image.Image"), BufferedImage.class);
                    BufferedImage bufferedImage = (BufferedImage) fromFXImageMethod.invoke(null, snapshot, null);

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
                    if (writers.hasNext()) {
                        ImageWriter writer = writers.next();
                        ImageWriteParam param = writer.getDefaultWriteParam();
                        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                        param.setCompressionQuality(JPEG_QUALITY);

                        try (ImageOutputStream ios = ImageIO.createImageOutputStream(baos)) {
                            writer.setOutput(ios);
                            writer.write(null, new IIOImage(bufferedImage, null, null), param);
                        }
                        writer.dispose();
                    }

                    byte[] frameData = baos.toByteArray();
                    bridge.sendFrame(frameData);
                } catch (Exception e) {
                    System.err.println("[JavaVisualizer] Frame capture error: " + e.getMessage());
                }
            });
        } catch (Exception e) {
            System.err.println("[JavaVisualizer] Capture scheduling error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void stop() {
        if (timer != null) {
            try {
                Method stopMethod = timer.getClass().getMethod("stop");
                stopMethod.invoke(timer);
            } catch (Exception e) {
                System.err.println("[JavaVisualizer] Error stopping FrameCapture: " + e.getMessage());
            }
        }
    }
}
