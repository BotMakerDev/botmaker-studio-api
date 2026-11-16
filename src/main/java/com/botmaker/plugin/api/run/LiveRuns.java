// SPDX-FileCopyrightText: 2026 LiQiyeDev
// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package com.botmaker.plugin.api.run;

import com.botmaker.plugin.api.source.ManagedParam;

/**
 * The live runs of this PC, as a plugin's setup work sees them: what several runs at once are dealt from a plugin's
 * pool ({@link PluginPool}) is held in the host's registry of live runs, and work that is not a run, such as the SDK's
 * Apply on an emulator instance, holds the same value while it works, so no run is dealt it meanwhile.
 *
 * <p>Host-only: the registry is shared by every BotMaker process of this user, and the host alone writes it.
 */
public interface LiveRuns {

    /**
     * Holds {@code value} of {@code parameter}, a parameter a plugin's pool fills, until the hold is closed or this
     * process ends; a start of several runs is then dealt another value.
     *
     * @param by who holds it, in the user's words, as a refused start names it ({@code "the Emulators window"})
     * @throws Taken when a live run holds it; its message names the run, in the user's words
     */
    <T> Held hold(ManagedParam<T> parameter, T value, String by) throws Taken;

    /** A value held: {@link #close} frees it, and closing it again does nothing. */
    interface Held extends AutoCloseable {

        @Override
        void close();
    }

    /** A value a live run already holds. */
    final class Taken extends Exception {

        public Taken(String words) {
            super(words);
        }
    }

    /** A host with no registry of live runs: every hold is granted and holds nothing. */
    LiveRuns NONE = new LiveRuns() {
        @Override
        public <T> Held hold(ManagedParam<T> parameter, T value, String by) {
            return () -> {};
        }
    };
}
