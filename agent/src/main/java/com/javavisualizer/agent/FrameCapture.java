package com.javavisualizer.agent;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.implementation.InvocationHandlerAdapter;
import net.bytebuddy.matcher.ElementMatchers;

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

            Class<?> concreteClass = new ByteBuddy()
                .subclass(animationTimerClass)
                .method(ElementMatchers.named("handle").and(ElementMatchers.takesArguments(long.class)))
                .intercept(InvocationHandlerAdapter.of((proxy, method, args) -> {
                    long now = (Long) args[0];
                    long elapsed = now - lastFrameTime;
                    if (elapsed < 1_000_000_000L / TARGET_FPS) {
                        return null;
                    }
                    lastFrameTime = now;
                    captureAndSend();
                    return null;
                }))
                .make()
                .load(scene.getClass().getClassLoader())
                .getLoaded();

            Object timerInstance = concreteClass.getDeclaredConstructor().newInstance();

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
            Class<?> sceneClass = scene.getClass();

            Method getRootMethod = sceneClass.getMethod("getRoot");
            Object root = getRootMethod.invoke(scene);

            Class<?> writableImageClass = Class.forName("javafx.scene.image.WritableImage");
            Class<?> snapshotParamsClass = Class.forName("javafx.scene.SnapshotParameters");

            Method getWidthMethod = sceneClass.getMethod("getWidth");
            Method getHeightMethod = sceneClass.getMethod("getHeight");
            double w = (double) getWidthMethod.invoke(scene);
            double h = (double) getHeightMethod.invoke(scene);

            if (w <= 0 || h <= 0) return;

            Object writableImage = writableImageClass.getConstructor(int.class, int.class)
                .newInstance((int) w, (int) h);

            Method snapshotMethod = root.getClass().getMethod("snapshot", snapshotParamsClass,
                Class.forName("javafx.scene.image.WritableImage"));
            snapshotMethod.invoke(root, null, writableImage);

            Class<?> swingFXUtilsClass = Class.forName("javafx.embed.swing.SwingFXUtils");
            Method fromFXImageMethod = swingFXUtilsClass.getMethod("fromFXImage",
                Class.forName("javafx.scene.image.Image"), BufferedImage.class);
            BufferedImage fxImage = (BufferedImage) fromFXImageMethod.invoke(null, writableImage, null);

            BufferedImage rgbImage = new BufferedImage(
                fxImage.getWidth(), fxImage.getHeight(), BufferedImage.TYPE_INT_RGB);
            java.awt.Graphics2D g = rgbImage.createGraphics();
            g.drawImage(fxImage, 0, 0, null);
            g.dispose();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
            if (writers.hasNext()) {
                ImageWriter writer = writers.next();
                ImageWriteParam param = writer.getDefaultWriteParam();
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                param.setCompressionQuality(JPEG_QUALITY);

                try (ImageOutputStream ios = ImageIO.createImageOutputStream(baos)) {
                    writer.setOutput(ios);
                    writer.write(null, new IIOImage(rgbImage, null, null), param);
                }
                writer.dispose();
            }

            byte[] frameData = baos.toByteArray();
            bridge.sendFrame(frameData);
        } catch (Exception e) {
            System.err.println("[JavaVisualizer] Frame capture error: " + e.getMessage());
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
