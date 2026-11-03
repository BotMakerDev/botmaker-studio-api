package com.botmaker.plugin.api;

import com.botmaker.plugin.api.run.RunEntry;

import java.util.List;

/**
 * A class giving the runs a plugin offers beside the bot's {@code main}, named by
 * {@link PluginDeclaration#runs(Class[])}. See {@link TypeHolder} for how a holder is built and asked.
 */
public interface RunEntryHolder {

    /** The entries, in the order the host lists them — {@link StudioPlugin#runEntries()}. */
    List<RunEntry> runs();
}
