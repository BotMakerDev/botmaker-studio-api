package com.botmaker.plugin.api;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.stage.Stage;

/**
 * Making a plugin's own windows look like the rest of the application.
 *
 * <p>The host owns light/dark and the stylesheet that expresses it, and a plugin must never read which is
 * active and branch: it hands its scene over and gets the current look, whatever that is now and after the
 * next release. That is the difference between a plugin that keeps matching the application and one that
 * matched it once.
 */
public interface Theme {

    /**
     * The look of a host with no user interface ({@link StudioServices#hasUi()} false): nothing is applied, and a
     * scene or alert asked for is JavaFX's own, unstyled. Nobody would see a style there; a plugin checks
     * {@code hasUi()} before it builds a window at all, since building one needs JavaFX running, which a host without
     * a UI may not have (an {@code Alert} then fails as JavaFX fails). Added after 0.14.0, compatibly.
     */
    Theme PLAIN = new Theme() {
        @Override public void apply(Scene scene) { }

        @Override public void apply(Stage stage) { }

        @Override public void apply(Dialog<?> dialog) { }

        @Override public void apply(DialogPane pane) { }

        @Override public Scene scene(Parent root) { return new Scene(root); }

        @Override public Scene scene(Parent root, double width, double height) {
            return new Scene(root, width, height);
        }

        @Override public Alert alert(Alert.AlertType type) { return new Alert(type); }

        @Override public Alert alert(Alert.AlertType type, String message, ButtonType... buttons) {
            return new Alert(type, message, buttons);
        }

        @Override public void applyThemeClass(Parent root) { }
    };

    /** Applies the current look to a scene. */
    void apply(Scene scene);

    /** Applies the current look to a stage and its scene. */
    void apply(Stage stage);

    /** Applies the current look to a dialog. */
    void apply(Dialog<?> dialog);

    /** Applies the current look to a dialog pane — for a dialog built by hand rather than through {@link Dialog}. */
    void apply(DialogPane pane);

    /** A themed scene around this root, sized to its content. */
    Scene scene(Parent root);

    /** A themed scene around this root, at an explicit size. */
    Scene scene(Parent root, double width, double height);

    /** A themed alert. */
    Alert alert(Alert.AlertType type);

    /** A themed alert with a message and buttons. */
    Alert alert(Alert.AlertType type, String message, ButtonType... buttons);

    /**
     * Adds the host's style class to a node's subtree, for a node that will be attached to a scene the host
     * already themed.
     */
    void applyThemeClass(Parent root);
}
