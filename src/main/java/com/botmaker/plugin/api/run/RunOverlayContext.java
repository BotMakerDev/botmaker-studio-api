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
     * Runs {@code action} once, on the JavaFX application thread, when this overlay goes away — the run ended,
     * the user hid it, or the project closed.
     *
     * <p>This is where a part closes what it registered while building its node: a listener on
     * {@link com.botmaker.plugin.api.Runs#onTelemetry} left open keeps feeding a node nobody sees, and one is
     * added for every run. A host with no overlay never opens one, so never calls it.
     */
    void onClosed(Runnable action);
}
