package com.library.pos.controller;

import com.library.pos.util.SecurityUtil;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ActivationController {

    @FXML
    private TextField machineIdField;
    @FXML
    private TextField licenseKeyField;
    @FXML
    private Button copyButton;
    @FXML
    private Button activateButton;

    private Stage stage;
    private boolean activated = false;
    private Runnable onActivationSuccess;

    public void setOnActivationSuccess(Runnable callback) {
        this.onActivationSuccess = callback;
    }

    @FXML
    public void initialize() {
        String machineId = SecurityUtil.getMachineId();
        machineIdField.setText(machineId);
        machineIdField.setEditable(false);
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public boolean isActivated() {
        return activated;
    }

    @FXML
    private void handleCopy() {
        ClipboardContent content = new ClipboardContent();
        content.putString(machineIdField.getText());
        Clipboard.getSystemClipboard().setContent(content);
        copyButton.setText("Copied!");
    }

    @FXML
    private void handleActivate() {
        String key = licenseKeyField.getText().trim();
        if (key.isEmpty()) {
            showAlert("Please enter a license key.");
            return;
        }

        if (SecurityUtil.validateKey(key)) {
            try {
                // Save license to file
                Path path = Paths.get("license.dat");
                Files.writeString(path, key);

                showAlert("Activation Successful!");
                activated = true;
                if (onActivationSuccess != null) {
                    onActivationSuccess.run();
                }
            } catch (IOException e) {
                showAlert("Error saving license file: " + e.getMessage());
            }
        } else {
            showAlert("Invalid License Key. Please check and try again.");
        }
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Activation");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
