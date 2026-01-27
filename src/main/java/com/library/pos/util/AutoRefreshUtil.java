package com.library.pos.util;

import javafx.scene.Node;
import javafx.animation.Timeline;

public final class AutoRefreshUtil {
    private AutoRefreshUtil() {
    }

    public static void bind(Timeline timeline, Node anchor, double hiddenRate) {
        if (timeline == null || anchor == null) {
            return;
        }
        Runnable updateRate = () -> {
            boolean visible = anchor.getScene() != null && anchor.isVisible();
            timeline.setRate(visible ? 1.0 : hiddenRate);
        };
        anchor.sceneProperty().addListener((obs, old, scene) -> updateRate.run());
        anchor.visibleProperty().addListener((obs, old, visible) -> updateRate.run());
        updateRate.run();
    }
}
