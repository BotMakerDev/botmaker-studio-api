package com.botmaker.plugin.api.run;

import com.botmaker.plugin.api.StudioServices;

/**
 * What a {@link RunOverlayPart} is given each time the host opens its run overlay.
 *
 * <p>The run itself is {@code services().runs()}: start, stop, pause, its trace and its telemetry. Nothing
 * else is needed to draw a run, and nothing here names what the bot is built on or what it drives.
 */
public interface RunOverlayContext {

    /** The open project's services; {@link StudioServices#runs()} is the bot this overlay shows. */
    StudioServices services();

    /**
     * Why the overlay is open: the bot is running, or the user is editing it in the overlay editor. A layer
     * opened for editing stays open when a run starts from the panel, so a part that draws differently in the
     * two follows {@code services().runs().onStateChanged} rather than this; this says why it was opened.
     */
    default Mode mode() {
        return Mode.RUNNING;
    }

    /** Why the overlay opened. */
    enum Mode {
        /** A run is showing. */
        RUNNING,
        /** The overlay editor is open over the watched screen. */
        EDITING
    }

    /**
     * Runs {@code action} once, on the JavaFX application thread, when this overlay goes away — the run ended,
     * the user hid it or closed the overlay editor, or the project closed.
     *
     * <p>This is where a part closes what it registered while building its node: a listener on
     * {@link com.botmaker.plugin.api.Runs#onTelemetry} left open keeps feeding a node nobody sees, and one is
     * added for every run. A host with no overlay never opens one, so never calls it.
     */
    void onClosed(Runnable action);
}
