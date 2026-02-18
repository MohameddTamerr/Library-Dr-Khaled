package com.library.pos;

import atlantafx.base.theme.PrimerLight;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.Locale;
import java.util.ResourceBundle;

public class LibraryPosApplication extends Application {

    private ConfigurableApplicationContext applicationContext;

    @Override
    public void init() {
        applicationContext = new SpringApplicationBuilder(LibraryPosApplicationLauncher.class).run();
    }

    @Override
    public void start(Stage stage) throws Exception {
        Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());

        // Set App Icon
        try {
            javafx.scene.image.Image icon = new javafx.scene.image.Image(
                    getClass().getResourceAsStream("/images/app_icon.png"));
            stage.getIcons().add(icon);
        } catch (Exception e) {
            System.err.println("Failed to load app icon: " + e.getMessage());
        }

        // Default to Arabic
        Locale.setDefault(Locale.forLanguageTag("ar"));

        // Check License
        if (!isLicenseValid()) {
            showActivationScreen(stage);
        } else {
            showLoginScreen(stage);
        }
    }

    private boolean isLicenseValid() {
        try {
            if (com.library.pos.util.SecurityUtil.isCurrentMachineRevoked()) {
                return false;
            }
            java.nio.file.Path path = java.nio.file.Paths.get("license.dat");
            if (!java.nio.file.Files.exists(path)) {
                return false;
            }
            String key = java.nio.file.Files.readString(path).trim();
            if (!com.library.pos.util.SecurityUtil.validateKey(key)) {
                return false;
            }
            return !com.library.pos.util.SecurityUtil.isCurrentMachineRevoked();
        } catch (Exception e) {
            return false;
        }
    }

    private void showActivationScreen(Stage stage) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/activation.fxml"));
            Scene scene = new Scene(fxmlLoader.load());

            com.library.pos.controller.ActivationController controller = fxmlLoader.getController();
            controller.setStage(stage);
            controller.setOnActivationSuccess(() -> showLoginScreen(stage));

            stage.setTitle("Product Activation");
            stage.setScene(scene);
            stage.centerOnScreen();
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showLoginScreen(Stage stage) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            fxmlLoader.setControllerFactory(applicationContext::getBean);
            fxmlLoader.setResources(ResourceBundle.getBundle("messages")); // Will look for messages_ar.properties

            // Don't set fixed scene size - let it use full screen dimensions
            Scene scene = new Scene(fxmlLoader.load());
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());

            stage.setMinWidth(900);
            stage.setMinHeight(600);
            stage.setTitle(fxmlLoader.getResources().getString("app.title"));
            stage.setScene(scene);

            stage.setFullScreen(true);
            stage.setFullScreenExitHint("");
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void stop() {
        applicationContext.close();
        Platform.exit();
    }
}
