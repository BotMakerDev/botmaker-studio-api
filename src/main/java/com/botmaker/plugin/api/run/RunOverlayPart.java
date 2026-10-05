package com.botmaker.plugin.api.run;

import javafx.scene.Node;

import java.util.Objects;
import java.util.Optional;

/**
 * A plugin's part of the host's run overlay: what it shows while the project's bot runs, so the user can
 * follow and stop the run without bringing the host back.
 *
 * <pre>{@code
 * static final RunOverlayPart RUN = RunOverlayPart.id("sdk.run")
 *         .bar(SdkRunOverlay::bar)
 *         .layer(SdkRunOverlay::layer);
 * }</pre>
 *
 * <p>The host owns the overlay and draws what every run has: its controls (stop, pause, run again) and the
 * last lines of its trace. A part adds what only its plugin can say, in one or both of two places:
 *
 * <ul>
 *   <li><b>{@link #bar}</b> — a node in the host's run bar, a small window the user places. It takes clicks.
 *       For a line of status: what the bot is doing now.</li>
 *   <li><b>{@link #layer}</b> — a node over the whole desktop, <b>in desktop pixels</b>: the host sizes and
 *       scales the layer so a node at {@code (x, y)} sits on desktop pixel {@code (x, y)} whatever the
 *       screens' scale, the space {@link com.botmaker.plugin.api.TraceLine#where()} already uses. It <b>takes
 *       no clicks</b>: every click, the user's and the bot's own, goes to the window under it. A host that
 *       cannot make a window click-through shows no layer, since one that caught the bot's clicks would
 *       break the run it is showing.</li>
 * </ul>
 *
 * <p>Each factory is called on the JavaFX application thread every time the overlay opens, normally once per
 * run, with a fresh {@link RunOverlayContext}; what it registers it closes in
 * {@link RunOverlayContext#onClosed}. A factory that throws costs its own part only.
 *
 * <p><b>The layer opens while editing too.</b> When the overlay editor is open the host opens each part's
 * {@link #layer} over the watched screen with {@link RunOverlayContext#mode()} {@code EDITING}, and keeps it
 * through any run started from the panel, so a plugin draws one layer for both. The {@link #bar} is shown in
 * the editor panel's header then, instead of the floating run bar.
 *
 * <p><b>A part links JavaFX</b>: a factory returns a {@link Node}, so building the list of parts loads it. Keep
 * the list behind {@code PluginDeclaration.runOverlay}'s supplier, which constructing the plugin never calls;
 * a host without JavaFX must not call {@link com.botmaker.plugin.api.StudioPlugin#runOverlayParts()}.
 *
 * <p>Declared like a toolbar item, through {@code PluginDeclaration.runOverlay(...)} or
 * {@link com.botmaker.plugin.api.StudioPlugin#runOverlayParts()}. The id is unique within the plugin.
 */
public final class RunOverlayPart {

    /** Builds a part's node for one opening of the overlay. A method reference, by the contract's rule. */
    @FunctionalInterface
    public interface Factory {
        Node create(RunOverlayContext context);
    }

    private final String id;
    private final Factory bar;
    private final Factory layer;

    private RunOverlayPart(String id, Factory bar, Factory layer) {
        this.id = id;
        this.bar = bar;
        this.layer = layer;
    }

    /** The first step: the part's id, unique within its plugin. Then {@link Drawing#bar} or {@link Drawing#layer}. */
    public static Drawing id(String id) {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("A run overlay part needs an id");
        return new Drawing(id);
    }

    /** The step after {@link #id}: a part draws in at least one place. */
    public static final class Drawing {

        private final String id;

        private Drawing(String id) {
            this.id = id;
        }

        /** Draws {@code bar} in the host's run bar. */
        public RunOverlayPart bar(Factory bar) {
            return new RunOverlayPart(id, Objects.requireNonNull(bar, "bar"), null);
        }

        /** Draws {@code layer} over the desktop, in desktop pixels, taking no clicks. */
        public RunOverlayPart layer(Factory layer) {
            return new RunOverlayPart(id, null, Objects.requireNonNull(layer, "layer"));
        }
    }

    /** This part, also drawing {@code bar} in the run bar. */
    public RunOverlayPart bar(Factory bar) {
        return new RunOverlayPart(id, Objects.requireNonNull(bar, "bar"), layer);
    }

    /** This part, also drawing {@code layer} over the desktop. */
    public RunOverlayPart layer(Factory layer) {
        return new RunOverlayPart(id, bar, Objects.requireNonNull(layer, "layer"));
    }

    public String id() {
        return id;
    }

    /** What it draws in the run bar, if anything. */
    public Optional<Factory> barFactory() {
        return Optional.ofNullable(bar);
    }

    /** What it draws over the desktop, if anything. */
    public Optional<Factory> layerFactory() {
        return Optional.ofNullable(layer);
    }
}
