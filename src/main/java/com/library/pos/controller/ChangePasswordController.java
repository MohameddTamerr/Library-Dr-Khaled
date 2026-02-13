package com.library.pos.controller;

import com.library.pos.model.User;
import com.library.pos.repository.UserRepository;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.stage.Stage;
import org.springframework.stereotype.Component;

@Component
public class ChangePasswordController {

    @FXML
    private javafx.scene.control.TextField usernameField;
    @FXML
    private PasswordField newPasswordField;
    @FXML
    private PasswordField confirmPasswordField;
    @FXML
    private Label errorLabel;

    private final UserRepository userRepository;
    private User currentUser;
    private Runnable onSuccess;

    public ChangePasswordController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void setUser(User user) {
        this.currentUser = user;
    }

    public void setOnSuccess(Runnable onSuccess) {
        this.onSuccess = onSuccess;
    }

    @FXML
    private void handleSave() {
        String newUsername = usernameField.getText() != null ? usernameField.getText().trim() : "";
        String newPass = newPasswordField.getText() != null ? newPasswordField.getText().trim() : "";
        String confirmPass = confirmPasswordField.getText() != null ? confirmPasswordField.getText().trim() : "";

        if (newUsername.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) {
            showError("يرجى ملء جميع الحقول");
            return;
        }

        if (!newPass.equals(confirmPass)) {
            showError("كلمتا المرور غير متطابقتين");
            return;
        }

        if (newPass.equals("admin")) {
            showError("لا يمكن استخدام كلمة المرور الافتراضية");
            return;
        }

        if (currentUser != null) {
            currentUser.setUsername(newUsername);
            currentUser.setPassword(newPass);
            userRepository.save(currentUser);

            backupCredentials(newUsername, newPass);

            if (onSuccess != null) {
                onSuccess.run();
            }
            closeWindow();
        }
    }

    private void backupCredentials(String username, String password) {
        try {
            java.nio.file.Path path = java.nio.file.Paths.get(".admin_recovery");
            String content = "Username: " + username + "\nPassword: " + password + "\nDate: "
                    + java.time.LocalDateTime.now();
            java.nio.file.Files.writeString(path, content);

            // Set hidden attribute on Windows
            try {
                java.nio.file.Files.setAttribute(path, "dos:hidden", true);
            } catch (Exception e) {
                System.err.println("Could not set hidden attribute: " + e.getMessage());
            }
        } catch (Exception e) {
            e.printStackTrace();
            // Don't block the user flow if backup fails, just log it
            System.err.println("Failed to backup credentials: " + e.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        closeWindow();
        // If they cancel the forced change, maybe we should close the app or return to
        // login?
        // For now, just close the dialog (LoginController handles the flow).
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }

    private void closeWindow() {
        Stage stage = (Stage) newPasswordField.getScene().getWindow();
        stage.close();
    }
}
