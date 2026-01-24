package com.library.pos.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ResourceBundle;

@Component
public class DashboardController {

    @FXML
    private Label welcomeLabel;
    @FXML
    private StackPane contentArea;
    @FXML
    private Button workersButton;

    private final ConfigurableApplicationContext applicationContext;

    public DashboardController(ConfigurableApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public void setUsername(String username) {
        welcomeLabel.setText(ResourceBundle.getBundle("messages").getString("dash.welcome").replace("{0}", username));
    }

    @FXML
    public void showWorkers() {
        loadView("/fxml/workers.fxml");
    }

    @FXML
    public void showProducts() {
        // TODO: Load Products View
        System.out.println("Show Products");
    }

    private void loadView(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            loader.setControllerFactory(applicationContext::getBean);
            loader.setResources(ResourceBundle.getBundle("messages"));
            Parent view = loader.load();
            contentArea.getChildren().setAll(view);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
