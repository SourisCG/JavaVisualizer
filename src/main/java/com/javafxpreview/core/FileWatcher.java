package com.javafxpreview.core;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class FileWatcher {

    private Thread pollThread;
    private volatile boolean running = false;
    private boolean autoReload = true;
    private final Consumer<Path> onFxmlChange;
    private final Consumer<Path> onCssChange;
    private final Map<String, Long> timestamps = new HashMap<>();
    private File currentFxml;
    private List<File> cssFiles;

    public FileWatcher(Consumer<Path> onFxmlChange, Consumer<Path> onCssChange) {
        this.onFxmlChange = onFxmlChange;
        this.onCssChange = onCssChange;
    }

    public void setAutoReload(boolean value) {
        this.autoReload = value;
    }

    public void watch(File rootDir, File fxml, List<File> css) {
        stop();
        this.currentFxml = fxml;
        this.cssFiles = css;

        timestamps.clear();
        scanTimestamps(rootDir);

        running = true;
        pollThread = new Thread(() -> {
            while (running) {
                try { Thread.sleep(500); } catch (InterruptedException e) { break; }
                if (!autoReload) continue;
                poll();
            }
        }, "FileWatcher");
        pollThread.setDaemon(true);
        pollThread.start();
    }

    private void poll() {
        try {
            if (currentFxml != null && currentFxml.exists()) {
                long mod = currentFxml.lastModified();
                Long prev = timestamps.get(currentFxml.getAbsolutePath());
                if (prev != null && mod > prev) {
                    timestamps.put(currentFxml.getAbsolutePath(), mod);
                    onFxmlChange.accept(currentFxml.toPath());
                    return;
                }
                timestamps.put(currentFxml.getAbsolutePath(), mod);
            }

            if (cssFiles != null) {
                for (File css : cssFiles) {
                    if (!css.exists()) continue;
                    long mod = css.lastModified();
                    Long prev = timestamps.get(css.getAbsolutePath());
                    if (prev != null && mod > prev) {
                        timestamps.put(css.getAbsolutePath(), mod);
                        onCssChange.accept(css.toPath());
                        return;
                    }
                    timestamps.put(css.getAbsolutePath(), mod);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void scanTimestamps(File dir) {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isFile()) {
                timestamps.put(f.getAbsolutePath(), f.lastModified());
            } else if (f.isDirectory() && !shouldSkip(f.getName())) {
                scanTimestamps(f);
            }
        }
    }

    private boolean shouldSkip(String name) {
        String lower = name.toLowerCase();
        return lower.startsWith(".") || lower.equals("target") || lower.equals("build")
            || lower.equals("node_modules") || lower.equals(".gradle");
    }

    public void stop() {
        running = false;
        if (pollThread != null) {
            pollThread.interrupt();
            pollThread = null;
        }
    }
}
