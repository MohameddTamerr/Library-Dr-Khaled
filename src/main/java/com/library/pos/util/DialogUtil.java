package com.library.pos.util;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;

import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

public class DialogUtil {

    public static Stage createDialog(String title, Node content, Window owner) {
        Stage stage = new Stage();
        stage.initOwner(owner);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initStyle(StageStyle.UNDECORATED);

        // Header
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;");

        Button closeBtn = new Button("X");
        closeBtn.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: white; -fx-cursor: hand; -fx-font-weight: bold;");
        closeBtn.setOnAction(e -> stage.close());

        HBox header = new HBox(10, titleLabel, new Region(), closeBtn);
        HBox.setHgrow(header.getChildren().get(1), Priority.ALWAYS);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(10));
        header.setStyle("-fx-background-color: #1e293b; -fx-border-color: #334155; -fx-border-width: 0 0 1 0;");

        // Content Wrapper
        StackPane contentWrapper = new StackPane(content);
        contentWrapper.setPadding(new Insets(15));

        // Root
        BorderPane root = new BorderPane();
        root.setTop(header);
        root.setCenter(contentWrapper);
        root.setStyle(
                "-fx-background-color: #0f172a; -fx-border-color: #334155; -fx-border-width: 1; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.5), 10, 0, 0, 0);");

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        // Apply existing stylesheets if any
        if (owner != null && owner.getScene() != null) {
            scene.getStylesheets().addAll(owner.getScene().getStylesheets());
        }

        stage.setScene(scene);
        return stage;
    }

    public static void initOwner(Dialog<?> dialog, Window preferredOwner) {
        if (dialog == null) {
            return;
        }
        Window owner = resolveOwner(preferredOwner);
        if (owner != null) {
            dialog.initOwner(owner);
        }
    }

    private static Window resolveOwner(Window preferredOwner) {
        if (preferredOwner != null && preferredOwner.isShowing()) {
            return preferredOwner;
        }
        for (Window window : Window.getWindows()) {
            if (window.isShowing() && window.isFocused()) {
                return window;
            }
        }
        for (Window window : Window.getWindows()) {
            if (window.isShowing()) {
                return window;
            }
        }
        return null;
    }
}
