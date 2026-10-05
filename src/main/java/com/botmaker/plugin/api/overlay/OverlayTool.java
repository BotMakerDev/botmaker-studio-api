package com.botmaker.plugin.api.overlay;

import javafx.scene.Node;

import java.util.Objects;

/**
 * A pane in the overlay panel's tool tabs: what turns the screen into blocks — cut a picture, place a point.
 *
 * <pre>{@code
 * static final OverlayTool PICTURE = OverlayTool.id("sdk.picture").named("Picture").pane(SdkTools::picture);
 * }</pre>
 *
 * <p>The pane is built on the FX thread each time the panel opens, with a fresh {@link OverlayToolContext};
 * what it registers it closes in {@link OverlayToolContext#onClosed}. A pane that throws costs its own tab.
 */
public final class OverlayTool {

    /** Builds the pane for one opening of the panel. A method reference, by the contract's rule. */
    @FunctionalInterface
    public interface Pane {
        Node create(OverlayToolContext context);
    }

    private final String id;
    private final String label;
    private final Pane pane;

    private OverlayTool(String id, String label, Pane pane) {
        this.id = id;
        this.label = label;
        this.pane = pane;
    }

    /** The first step: the tool's id, unique within its plugin. */
    public static Naming id(String id) {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("An overlay tool needs an id");
        return new Naming(id);
    }

    /** After {@link #id}: the tab's label. */
    public static final class Naming {

        private final String id;

        private Naming(String id) {
            this.id = id;
        }

        public Building named(String label) {
            if (label == null || label.isBlank()) throw new IllegalArgumentException(id + ": a tool needs a label");
            return new Building(id, label);
        }
    }

    /** After {@link Naming#named}: the pane. */
    public static final class Building {

        private final String id;
        private final String label;

        private Building(String id, String label) {
            this.id = id;
            this.label = label;
        }

        public OverlayTool pane(Pane pane) {
            return new OverlayTool(id, label, Objects.requireNonNull(pane, "pane"));
        }
    }

    public String id() {
        return id;
    }

    public String label() {
        return label;
    }

    public Pane pane() {
        return pane;
    }
}
