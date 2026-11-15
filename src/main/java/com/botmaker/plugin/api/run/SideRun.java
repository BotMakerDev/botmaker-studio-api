package com.botmaker.plugin.api.run;

import java.time.Duration;
import java.util.OptionalInt;

/**
 * One run {@link SideRuns#start} started: its process, until it ends. The plugin that started it stops it; the host
 * stops what is left when it quits.
 */
public interface SideRun {

    /** Whether the run's process still lives. */
    boolean isRunning();

    /**
     * Waits up to {@code limit} for the run to end.
     *
     * @return its exit code, or empty when it still runs
     */
    OptionalInt await(Duration limit) throws InterruptedException;

    /** Ends the run, its process killed when it does not end by itself within a few seconds; nothing once it ended. */
    void stop();
}
