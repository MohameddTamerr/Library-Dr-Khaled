package com.library.pos.util;

import java.io.*;
import java.nio.file.*;
import java.util.Properties;

/**
 * Reads installer-written settings from settings.properties placed beside the
 * running EXE / in the working directory.
 *
 * Keys written by the NSIS installer:
 *   backup.dir            – absolute path where DB backups should be stored
 *   receipt.printer.name  – exact name of the receipt printer chosen at install
 */
public final class AppSettings {

    private static final String SETTINGS_FILE = "settings.properties";
    private static final Properties props = new Properties();

    static {
        reload();
    }

    private AppSettings() {}

    /** Reload settings from disk (called at startup and can be called again after install). */
    public static synchronized void reload() {
        props.clear();
        // 1. Try the directory the JVM was launched from (= folder containing the EXE)
        Path p1 = Path.of(System.getProperty("user.dir", ".")).resolve(SETTINGS_FILE);
        // 2. Fallback: same directory as the JAR (when running from IDE or via java -jar)
        Path p2 = Path.of(".").toAbsolutePath().normalize().resolve(SETTINGS_FILE);

        for (Path candidate : new Path[]{p1, p2}) {
            if (Files.exists(candidate)) {
                try (InputStream in = Files.newInputStream(candidate)) {
                    props.load(in);
                    System.out.println("[AppSettings] Loaded: " + candidate.toAbsolutePath());
                    return;
                } catch (IOException e) {
                    System.err.println("[AppSettings] Failed to read " + candidate + ": " + e.getMessage());
                }
            }
        }
        System.out.println("[AppSettings] No settings.properties found – using built-in defaults.");
    }

    /**
     * Returns the backup directory chosen during installation.
     * Falls back to ~/Documents/LibraryPOS_Backups if not set.
     */
    public static String getBackupDir() {
        String val = props.getProperty("backup.dir", "").trim();
        if (val.isEmpty()) {
            return System.getProperty("user.home") + File.separator
                    + "Documents" + File.separator + "LibraryPOS_Backups";
        }
        return val;
    }

    /**
     * Returns the receipt printer name chosen during installation.
     * Returns null if not configured (ReceiptPrinter falls back to application.properties).
     */
    public static String getPrinterName() {
        String val = props.getProperty("receipt.printer.name", "").trim();
        return val.isEmpty() ? null : val;
    }

    /** Saves a single key-value pair back to settings.properties on disk. */
    public static synchronized void set(String key, String value) {
        props.setProperty(key, value);
        Path out = Path.of(System.getProperty("user.dir", ".")).resolve(SETTINGS_FILE);
        try (OutputStream os = Files.newOutputStream(out, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            props.store(os, "Library POS Settings");
        } catch (IOException e) {
            System.err.println("[AppSettings] Could not save settings: " + e.getMessage());
        }
    }

    /** Raw property getter for any other future keys. */
    public static String get(String key, String defaultValue) {
        return props.getProperty(key, defaultValue);
    }
}
