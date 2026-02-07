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

        Runnable apply = () -> {
            stage.setFullScreen(true);
            stage.setFullScreenExitHint("");
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

}
