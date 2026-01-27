package com.library.pos.util;

import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.util.List;

public final class StageUtil {
    private StageUtil() {
    }

    public static void applyWindowedFullScreen(Stage stage) {
        if (stage == null) {
            return;
        }
        stage.setResizable(true);

        Runnable apply = () -> {
            Rectangle2D bounds = getVisualBounds(stage);
            stage.setX(bounds.getMinX());
            stage.setY(bounds.getMinY());
            stage.setWidth(bounds.getWidth());
            stage.setHeight(bounds.getHeight());
            stage.setMaximized(true);
        };

        if (Platform.isFxApplicationThread()) {
            apply.run();
        } else {
            Platform.runLater(apply);
        }
    }

    public static void applyWindowedFullScreenIfMaximized(Stage stage) {
        if (stage == null) {
            return;
        }
        if (stage.isMaximized() || stage.isFullScreen()) {
            applyWindowedFullScreen(stage);
        }
    }

    private static Rectangle2D getVisualBounds(Stage stage) {
        double width = Math.max(stage.getWidth(), 1);
        double height = Math.max(stage.getHeight(), 1);
        List<Screen> screens = Screen.getScreensForRectangle(stage.getX(), stage.getY(), width, height);
        Screen screen = screens.isEmpty() ? Screen.getPrimary() : screens.get(0);
        return screen.getVisualBounds();
    }
}
