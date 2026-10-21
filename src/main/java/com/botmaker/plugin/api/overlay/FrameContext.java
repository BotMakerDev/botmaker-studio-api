package com.botmaker.plugin.api.overlay;

import com.botmaker.plugin.api.toolbar.ActionContext.Area;

import java.awt.image.BufferedImage;
import java.util.Optional;

/**
 * An {@link OverlayContext} once the host watches a screen — what a tool, a probe and an assistant tool are given.
 * The bare {@link OverlayContext} is what a part is asked before that, to say what to watch.
 */
public interface FrameContext extends OverlayContext {

    /** The watched screen now — the plugin's {@link OverlayPart#watchedFor} — empty when it cannot be captured. */
    Optional<BufferedImage> frame();

    /** Where {@link #frame()} sits, in the bot's pixels (see the package comment). */
    Optional<Area> watchedArea();
}
