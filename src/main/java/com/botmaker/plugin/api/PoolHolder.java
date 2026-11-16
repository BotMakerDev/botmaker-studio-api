// SPDX-FileCopyrightText: 2026 LiQiyeDev
// SPDX-License-Identifier: PolyForm-Noncommercial-1.0.0

package com.botmaker.plugin.api;

import com.botmaker.plugin.api.run.PluginPool;

import java.util.List;

/**
 * A class giving the pools several runs at once are dealt from, each for one of the plugin's own parameters, named
 * by {@link PluginDeclaration#pools(Class[])}. See {@link TypeHolder} for how a holder is built and asked.
 */
public interface PoolHolder {

    /** The pools, in the order a row is dealt them — {@link StudioPlugin#pools()}. */
    List<PluginPool<?>> pools();
}
