package com.botmaker.plugin.api.assist;

import com.botmaker.plugin.api.overlay.FrameContext;
import com.botmaker.plugin.api.overlay.Marks;

/**
 * What an {@link AssistantTool}'s handler is given: the open project, the screen the bot watches, and the boxes
 * the host draws over it to show the user what the assistant means.
 *
 * <p>Called <b>off the JavaFX thread</b>; a handler that opens a window hops to it itself. The plugin's own
 * values are {@code services().pluginValues()}, written through the same gate as the plugin's own windows.
 */
public interface AgentContext extends FrameContext {

    /** Boxes over the watched screen, shown while the overlay is open; a host without one draws nothing. */
    default Marks marks() {
        return Marks.NONE;
    }
}
