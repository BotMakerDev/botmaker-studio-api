// SPDX-FileCopyrightText: 2026 LiQiyeDev
// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package com.botmaker.plugin.api.run;

import com.botmaker.plugin.api.StudioServices;

import java.util.Optional;

/**
 * What the host's Remote Pilot shows of one live run (2026-11-26): the plugin only names the thing, the host does the
 * capture, the encoding and the input. Given by a {@link com.botmaker.plugin.api.RunViewHolder}, one new instance for
 * each run, built only in the process that owns the project.
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
     * The project this view belongs to and what to call when {@link #target()} may have changed (the window opened
     * or closed). Called once, before the first {@link #target()}, from a host thread; a view that needs neither may
     * ignore it.
     */
    default void start(StudioServices services, Runnable changed) {
    }
}
