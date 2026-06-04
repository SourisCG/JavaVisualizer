package com.javafxpreview.core;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class FxmlWatcher {

    private Thread pollThread;
    private volatile boolean running = false;
    private boolean autoReload = true;
    private final Consumer<Path> onFxmlChange;
    private final Consumer<Path> onCssChange;
    private final Map<String, Long> timestamps = new HashMap<>();
    private File currentFxml;
    private List<File> cssFiles = new ArrayList<>();

    public FxmlWatcher(Consumer<Path> onFxmlChange, Consumer<Path> onCssChange) {
        this.onFxmlChange = onFxmlChange;
        this.onCssChange = onCssChange;
    }

    public void setAutoReload(boolean value) { this.autoReload = value; }
    public boolean isAutoReload() { return autoReload; }

    public void setCurrentFxml(File f) { this.currentFxml = f; }
    public void setCssFiles(List<File> files) { this.cssFiles = files; }

    public void watch(File rootDir) {
        stop();

        // Seed timestamps
        timestamps.clear();
        scanTimestamps(rootDir);
        if (currentFxml != null) timestamps.put(currentFxml.getAbsolutePath(), currentFxml.lastModified());
        for (File f : cssFiles) timestamps.put(f.getAbsolutePath(), f.lastModified());

        running = true;
        System.out.println("[Watcher] Polling started for: " + rootDir);

        pollThread = new Thread(() -> {
            while (running) {
                try { Thread.sleep(500); } catch (InterruptedException e) { break; }
                poll();
            }
            System.out.println("[Watcher] Polling stopped");
        }, "FxmlWatcher-Poll");
        pollThread.setDaemon(true);
        pollThread.start();
        System.out.println("[Watcher] Poll thread alive: " + pollThread.isAlive());
    }

    private void poll() {
        try {
            if (currentFxml != null && currentFxml.exists()) {
                long mod = currentFxml.lastModified();
                Long prev = timestamps.get(currentFxml.getAbsolutePath());
                System.out.println("[Watcher] Poll FXML: mod=" + mod + " prev=" + (prev != null ? prev : "null")
                    + " changed=" + (prev != null && mod > prev));
                if (prev != null && mod > prev) {
                    System.out.println("[Watcher] FXML changed: " + currentFxml);
                    timestamps.put(currentFxml.getAbsolutePath(), mod);
                    onFxmlChange.accept(currentFxml.toPath());
                    return;
                }
                timestamps.put(currentFxml.getAbsolutePath(), mod);
            } else {
                System.out.println("[Watcher] Poll FXML: currentFxml="
                    + (currentFxml != null ? currentFxml.getAbsolutePath() : "null")
                    + " exists=" + (currentFxml != null && currentFxml.exists())
                    + " autoReload=" + autoReload);
            }

            for (File css : cssFiles) {
                if (!css.exists()) continue;
                long mod = css.lastModified();
                Long prev = timestamps.get(css.getAbsolutePath());
                if (prev != null && mod > prev) {
                    System.out.println("[Watcher] CSS changed: " + css);
                    timestamps.put(css.getAbsolutePath(), mod);
                    onCssChange.accept(css.toPath());
                    return;
                }
                timestamps.put(css.getAbsolutePath(), mod);
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
        return name.startsWith(".") || name.equals("target") || name.equals("build")
            || name.equals("node_modules") || name.equals(".gradle") || name.equals("__pycache__");
    }

    public void stop() {
        running = false;
        if (pollThread != null) {
            pollThread.interrupt();
            pollThread = null;
        }
    }
}

