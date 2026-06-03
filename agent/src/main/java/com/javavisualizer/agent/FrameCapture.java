package com.javavisualizer.agent;

import javafx.animation.AnimationTimer;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.WritableImage;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Iterator;

public class FrameCapture {

    private final Scene scene;
    private final WebSocketBridge bridge;
    private AnimationTimer timer;
    private long lastFrameTime = 0;
    private static final int TARGET_FPS = 30;
    private static final float JPEG_QUALITY = 0.75f;

    public FrameCapture(Scene scene, WebSocketBridge bridge) {
        this.scene = scene;
        this.bridge = bridge;
    }

    public void start() {
        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                long elapsed = now - lastFrameTime;
                if (elapsed < 1_000_000_000L / TARGET_FPS) {
                    return;
                }
                lastFrameTime = now;
                captureAndSend();
            }
        };
        timer.start();
    }

    private void captureAndSend() {
        try {
            javafx.application.Platform.runLater(() -> {
                try {
                    WritableImage snapshot = scene.snapshot(null);
                    BufferedImage bufferedImage = SwingFXUtils.fromFXImage(snapshot, null);

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
        }
    }

    public void stop() {
        if (timer != null) {
            timer.stop();
        }
    }
}
