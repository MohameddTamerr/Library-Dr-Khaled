package com.library.pos.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import org.springframework.stereotype.Component;

@Component
public class SuccessPopupController {

    @FXML
    private Label messageLabel;
    @FXML
    private Label detailsLabel;

    @FXML
    private void handleClose() {
        Stage stage = (Stage) messageLabel.getScene().getWindow();
        stage.close();
    }

    public void setMessage(String message) {
        if (messageLabel != null) {
            messageLabel.setText(message);
        }
    }

    public void setDetails(String details) {
        if (detailsLabel != null) {
            detailsLabel.setText(details);
        }
    }
}
