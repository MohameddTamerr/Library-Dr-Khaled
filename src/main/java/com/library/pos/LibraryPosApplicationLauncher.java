package com.library.pos;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.boot.autoconfigure.SpringBootApplication;

import javafx.application.Application;

@SpringBootApplication
public class LibraryPosApplicationLauncher {

    private static final String DB_NAME = "library.db";
    // Backup Locations
    private static final String APP_BACKUP_DIR = "backups";
    private static final String DOCS_BACKUP_DIR = System.getProperty("user.home") + "/Documents/LibraryPOS_Backups";

    public static void main(String[] args) {
        performDatabaseMaintenance();
        Application.launch(LibraryPosApplication.class, args);
    }

    private static void performDatabaseMaintenance() {
        try {
            File dbFile = new File(DB_NAME);
            File appBackupDir = new File(APP_BACKUP_DIR);
            File docsBackupDir = new File(DOCS_BACKUP_DIR);

            // Ensure backup directories exist
            if (!appBackupDir.exists())
                appBackupDir.mkdirs();
            if (!docsBackupDir.exists())
                docsBackupDir.mkdirs();

            if (dbFile.exists()) {
                // DATABASE EXISTS: Create Backups
                System.out.println("Database found. Creating backups...");
                backupDatabase(dbFile, new File(appBackupDir, DB_NAME));
                backupDatabase(dbFile, new File(docsBackupDir, DB_NAME));

                // create a timestamped backup too for safety (keep last 5 maybe? simplistic for
                // now)
                String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm"));
                backupDatabase(dbFile, new File(docsBackupDir, "library_" + timestamp + ".db"));

            } else {
                // DATABASE MISSING: Attempt Restore
                System.out.println("CRITICAL: Database missing! Attempting restore...");
                boolean restored = false;

                // Try from App Backup
                File backup1 = new File(appBackupDir, DB_NAME);
                if (backup1.exists()) {
                    restoreDatabase(backup1, dbFile);
                    restored = true;
                    System.out.println("Restored from App Backup.");
                } else {
                    // Try from Docs Backup
                    File backup2 = new File(docsBackupDir, DB_NAME);
                    if (backup2.exists()) {
                        restoreDatabase(backup2, dbFile);
                        restored = true;
                        System.out.println("Restored from Documents Backup.");
                    }
                }

                if (!restored) {
                    System.out.println("No backups found. Starting with empty database.");
                }
            }

            // HIDE DATABASE FILE
            try {
                Path dbPath = dbFile.toPath();
                if (Files.exists(dbPath)) {
                    Files.setAttribute(dbPath, "dos:hidden", true);
                }
            } catch (Exception e) {
                System.out.println("Could not hide database file: " + e.getMessage());
            }

        } catch (Exception e) {
            e.printStackTrace();
            // Don't stop app, just log error
        }
    }

    private static void backupDatabase(File source, File dest) throws IOException {
        Files.copy(source.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    private static void restoreDatabase(File source, File dest) throws IOException {
        Files.copy(source.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }
}