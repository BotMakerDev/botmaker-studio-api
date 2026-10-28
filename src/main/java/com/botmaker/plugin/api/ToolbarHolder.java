package com.botmaker.plugin.api;

/**
 * A class holding a plugin's toolbar buttons, named by {@link PluginDeclaration#toolbar(Class[])}: each
 * {@code public static final} {@link com.botmaker.plugin.api.toolbar.ToolbarItem} field, or a list of them, in
 * the order the fields are declared.
 *
 * <p>It has no members; see {@link TypeHolder} for why it exists.
 */
public interface ToolbarHolder {
}
