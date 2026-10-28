package com.botmaker.plugin.api;

import com.botmaker.plugin.api.toolbar.ToolbarItem;

import java.util.List;

/**
 * A class giving a plugin's toolbar buttons, named by {@link PluginDeclaration#toolbar(Class[])}. See
 * {@link TypeHolder} for how a holder is built and asked.
 */
public interface ToolbarHolder {

    /** The buttons, in the order a user sees them — {@link StudioPlugin#toolbarItems()}. */
    List<ToolbarItem> toolbar();
}
