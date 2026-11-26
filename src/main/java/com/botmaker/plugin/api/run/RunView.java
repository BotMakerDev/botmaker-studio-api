// SPDX-FileCopyrightText: 2026 LiQiyeDev
// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package com.botmaker.plugin.api.run;

import com.botmaker.plugin.api.StudioServices;

import java.util.Optional;

/**
 * What the host's Remote Pilot shows of one live run (2026-11-26): the plugin only names the thing, the host does the
 * capture, the encoding and the input. Given by a {@link com.botmaker.plugin.api.RunViewHolder}, one new instance for
 * each run and one while no run goes (2026-11-27: the pilot then shows what a run would), built only in the process
 * that owns the project. An instance is dropped without a call when its run starts or ends, so it holds nothing that
 * needs closing.
 *
 * <p>A run whose plugin gives no view shows the run's own place instead. The host asks {@link #target()} again each
 * time the callback {@link #start} gave was called, and may ask at any time, from any thread; answer fast and never block.
 */
public interface RunView {

    /** What the run shows now, or empty when there is nothing to show; see {@link #reason()}. */
    Optional<ViewTarget> target();

    /** Why {@link #target()} is empty, a sentence for the phone ("The game window is closed."); none by default. */
    default Optional<String> reason() {
        return Optional.empty();
    }

    /**
     * One telemetry frame of this run, as the bytes the run's own agent wrote (the same bytes
     * {@link com.botmaker.plugin.api.Runs#onTelemetry} gives), as the message the Remote Pilot's phone draws over the
     * picture (a match box, a click): the host knows no plugin's frame format, so the plugin turns it. Empty when the
     * frame draws nothing or cannot be read. Called on the thread that delivers frames; answer fast.
     */
    default Optional<String> overlay(byte[] frame) {
        return Optional.empty();
    }

    /**
     * The project this view belongs to and what to call when {@link #target()} may have changed (the window opened
     * or closed). Called once, before the first {@link #target()}, from a host thread; a view that needs neither may
     * ignore it.
     */
    default void start(StudioServices services, Runnable changed) {
    }
}
