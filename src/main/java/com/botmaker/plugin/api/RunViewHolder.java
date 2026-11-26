// SPDX-FileCopyrightText: 2026 LiQiyeDev
// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package com.botmaker.plugin.api;

import com.botmaker.plugin.api.run.RunView;

/**
 * A class giving what the host's Remote Pilot shows of the plugin's runs, named by
 * {@link PluginDeclaration#views(Class[])}. See {@link TypeHolder} for how a holder is built and asked.
 */
public interface RunViewHolder {

    /** A new view, asked once for each live run and once while none goes — {@link StudioPlugin#runViews()}. */
    RunView view();
}
