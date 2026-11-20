// SPDX-FileCopyrightText: 2026 LiQiyeDev
// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package com.botmaker.plugin.api.run;

import java.util.List;

/**
 * Code a plugin has run beside each live run of one of its own {@link RunEntry entries} (2026-11-20), such as what
 * answers a run's questions from the plugin's side while nobody watches. Given by a
 * {@link com.botmaker.plugin.api.CompanionHolder}, one new instance for each run.
 *
 * <p>The host runs it only in the process that owns the project, never in a copy of the plugin a window loaded: one
 * instance per run, whether no client, one or several watch. It {@link #start starts} once the run's process is up and
 * {@link #stop stops} once it ended, from the host's own threads; a companion that works on its own starts a thread
 * of its own and ends it on {@code stop}. A run of the bot's {@code main}, a try or a run of an entry it does not
 * {@link #serves() serve} starts none.
 */
public interface RunCompanion {

    /** The entries whose runs it serves, compared by {@link RunEntry#equals}; none serves no run. */
    List<RunEntry> serves();

    /** The run {@code run} began: called once, before any {@link #stop()}. */
    void start(RunContext run);

    /** The run ended, or the project is closing: called once after {@link #start}. */
    void stop();
}
