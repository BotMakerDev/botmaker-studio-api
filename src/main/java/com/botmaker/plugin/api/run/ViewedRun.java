// SPDX-FileCopyrightText: 2026 LiQiyeDev
// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package com.botmaker.plugin.api.run;

import java.util.Objects;

/**
 * The live run a {@link RunView} was built for, as the host names it (2026-11-27). The host builds it; a plugin only
 * reads it, to show what this run shows (its own emulator or window) when several runs of one plugin go at once.
 *
 * @param id  the host's id for the run, the one the Remote Pilot lists and a phone picks; empty when the host has not
 *            given the run one yet
 * @param bot what the run plays, as the host lists it
 */
public record ViewedRun(String id, String bot) {

    public ViewedRun {
        id = Objects.requireNonNullElse(id, "");
        bot = Objects.requireNonNullElse(bot, "");
    }
}
