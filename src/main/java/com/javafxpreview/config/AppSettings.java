package com.javafxpreview.config;

public class AppSettings {

    private static String lastDirectory = System.getProperty("user.home");
    private static boolean autoReload = true;

    public static String getLastDirectory() { return lastDirectory; }
    public static void setLastDirectory(String dir) { lastDirectory = dir; }
    public static boolean isAutoReload() { return autoReload; }
    public static void setAutoReload(boolean value) { autoReload = value; }
}
