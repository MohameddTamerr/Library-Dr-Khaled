package com.library.pos.controller;

import com.library.pos.model.User;
import com.library.pos.service.AuthService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
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
            navigateToDashboard(user);
        } else {
            errorLabel.setVisible(true);
            errorLabel.setText(ResourceBundle.getBundle("messages").getString("login.error.invalid"));
        }
    }

    private void navigateToDashboard(User user) {
        try {
            Stage stage = (Stage) loginButton.getScene().getWindow();

            if (user.getRole() == com.library.pos.model.Role.WORKER) {
                // Navigate to Cashier System
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/cashier.fxml"));
                loader.setControllerFactory(applicationContext::getBean);
                loader.setResources(ResourceBundle.getBundle("messages"));
                Scene scene = new Scene(loader.load(), 1200, 800);
                scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());

                CashierController cashierController = loader.getController();
                cashierController.setUser(user);

                stage.setTitle(ResourceBundle.getBundle("messages").getString("cashier.title"));
                stage.setScene(scene);
            } else {
                // Navigate to Owner Dashboard
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/dashboard.fxml"));
                loader.setControllerFactory(applicationContext::getBean);
                loader.setResources(ResourceBundle.getBundle("messages")); // Using same messages bundle

                Scene scene = new Scene(loader.load(), 1200, 800);
                scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());

                // Pass user info to dashboard
                DashboardController dashboardController = loader.getController();
                dashboardController.setUsername(user.getFullName());

                stage.setTitle(ResourceBundle.getBundle("messages").getString("app.title") + " - Dashboard");
                stage.setScene(scene);
            }

            stage.setResizable(true);
            stage.centerOnScreen();
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

            Scene scene = new Scene(fxmlLoader.load(), 800, 600);
            stage.setScene(scene);
            stage.setTitle(bundle.getString("app.title"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
