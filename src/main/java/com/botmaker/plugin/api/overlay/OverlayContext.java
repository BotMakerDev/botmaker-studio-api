package com.botmaker.plugin.api.overlay;

import com.botmaker.plugin.api.StudioServices;

/**
 * What the host hands an {@link OverlayPart}'s questions about the open project — which targets, which
 * screen. The plugin reads its own values through {@link StudioServices#pluginValues()}.
 */
public interface OverlayContext {

    /** The open project's services. */
    StudioServices services();
}
