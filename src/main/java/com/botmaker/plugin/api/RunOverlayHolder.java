package com.botmaker.plugin.api;

import com.botmaker.plugin.api.run.RunOverlayPart;

import java.util.List;

/**
 * A class giving what a plugin shows in the host's run overlay, named by
 * {@link PluginDeclaration#runOverlay(Class[])}. See {@link TypeHolder} for how a holder is built and asked.
 */
public interface RunOverlayHolder {

    /** The parts, in the order they are drawn — {@link StudioPlugin#runOverlayParts()}. */
    List<RunOverlayPart> runOverlay();
}
