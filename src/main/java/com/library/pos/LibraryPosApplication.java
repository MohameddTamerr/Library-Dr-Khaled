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

        // Default to Arabic
        Locale.setDefault(Locale.forLanguageTag("ar"));

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
    }

    @Override
    public void stop() {
        applicationContext.close();
        Platform.exit();
    }
}
