// SPDX-FileCopyrightText: 2026 LiQiyeDev
// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package com.botmaker.plugin.api;

import com.botmaker.plugin.api.run.RunCompanion;

/**
 * A class giving one companion of the plugin's runs, named by {@link PluginDeclaration#companions(Class[])}. See
 * {@link TypeHolder} for how a holder is built and asked.
 */
public interface CompanionHolder {

    /** A new companion, asked once for each run it may serve — {@link StudioPlugin#runCompanions()}. */
    RunCompanion companion();
}
