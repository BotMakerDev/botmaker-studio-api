// SPDX-FileCopyrightText: 2026 LiQiyeDev
// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package com.botmaker.plugin.api.run;

import com.botmaker.plugin.api.StudioServices;

/**
 * What the host answered {@link StudioServices#devReload(RunEntry)}: the reload was accepted and goes on without the
 * caller, or it was refused, in words the plugin hands its user as they are (2026-11-19).
 *
 * <p>The host builds both; a plugin only reads them.
 */
public sealed interface DevReload {

    /** The answer of a host that does not reload plugins: every host but a dev-mode project's. */
    DevReload REFUSED_HERE = new Refused("This host does not reload plugins.");

    /** The host stopped the run, reloads the plugins and starts the entry; the caller's instance is closing. */
    record Started() implements DevReload {
    }

    /**
     * Nothing was stopped or reloaded.
     *
     * @param why one line saying why, for the plugin's user: not in dev mode, the plugin did not load, no such entry
     */
    record Refused(String why) implements DevReload {
    }
}
