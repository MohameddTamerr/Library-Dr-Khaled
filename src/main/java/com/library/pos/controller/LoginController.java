package com.library.pos.controller;

import com.library.pos.model.User;
import com.library.pos.service.AuthService;
import com.library.pos.util.StageUtil;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.ResourceBundle;

@Component
public class LoginController {

    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Label errorLabel;
    @FXML
    private Button loginButton;
    @FXML
    private Button languageButton;

    private final AuthService authService;
    private final ConfigurableApplicationContext applicationContext;

    @Autowired
    public LoginController(AuthService authService, ConfigurableApplicationContext applicationContext) {
        this.authService = authService;
        this.applicationContext = applicationContext;
    }

    @FXML
    public void initialize() {
        errorLabel.setVisible(false);
    }

    @FXML
    private void handleLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        User user = authService.authenticate(username, password);

        if (user != null) {
            errorLabel.setVisible(false);
            System.out.println("Login successful for: " + user.getUsername());
            if (user.getRole() == com.library.pos.model.Role.DELIVERY_MEN) {
                errorLabel.setVisible(true);
                errorLabel.setText(ResourceBundle.getBundle("messages").getString("login.error.delivery"));
                return;
            }
            navigateToDashboard(user);
        } else {
            errorLabel.setVisible(true);
            errorLabel.setText(ResourceBundle.getBundle("messages").getString("login.error.invalid"));
        }
    }

    private void navigateToDashboard(User user) {
        try {
            Stage stage = (Stage) loginButton.getScene().getWindow();
            FXMLLoader loader;
            String title;
            Parent root;

            if (user.getRole() == com.library.pos.model.Role.WORKER) {
                // Navigate to Cashier System
                loader = new FXMLLoader(getClass().getResource("/fxml/cashier.fxml"));
                loader.setControllerFactory(applicationContext::getBean);
                loader.setResources(ResourceBundle.getBundle("messages"));
                root = loader.load();

                CashierController cashierController = loader.getController();
                cashierController.setUser(user);

                title = ResourceBundle.getBundle("messages").getString("cashier.title");
            } else {
                // Navigate to Owner Dashboard
                loader = new FXMLLoader(getClass().getResource("/fxml/dashboard.fxml"));
                loader.setControllerFactory(applicationContext::getBean);
                loader.setResources(ResourceBundle.getBundle("messages"));
                root = loader.load();

                // Pass user info to dashboard
                DashboardController dashboardController = loader.getController();
                dashboardController.setUser(user);

                title = ResourceBundle.getBundle("messages").getString("app.title") + " - Dashboard";
            }

            // Seamless transition: switch root of current scene
            loginButton.getScene().setRoot(root);
            stage.setTitle(title);

            if (!stage.isFullScreen()) {
                stage.setFullScreen(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void toggleLanguage() {
        Locale current = Locale.getDefault();
        if (current.getLanguage().equals("ar")) {
            Locale.setDefault(Locale.forLanguageTag("en"));
        } else {
            Locale.setDefault(Locale.forLanguageTag("ar"));
        }
        reloadScene();
    }

    private void reloadScene() {
        try {
            Stage stage = (Stage) loginButton.getScene().getWindow();
            ResourceBundle bundle = ResourceBundle.getBundle("messages");

            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            fxmlLoader.setControllerFactory(applicationContext::getBean);
            fxmlLoader.setResources(bundle);

            Scene scene = new Scene(fxmlLoader.load());
            // Maintain the same scene to keep full screen state
            Scene currentScene = loginButton.getScene();
            currentScene.setRoot(scene.getRoot());

            stage.setTitle(bundle.getString("app.title"));

            if (!stage.isFullScreen()) {
                stage.setFullScreen(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
