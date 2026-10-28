package com.botmaker.plugin.api;

import com.botmaker.plugin.api.overlay.OverlayPart;

/**
 * The class giving a plugin's one part of the overlay editor, named by {@link PluginDeclaration#overlay(Class)}.
 * See {@link TypeHolder} for how a holder is built and asked.
 */
public interface OverlayHolder {

    /** The part: targets, watched screen, tools, probes — {@link StudioPlugin#overlayPart()}. */
    OverlayPart overlay();
}
