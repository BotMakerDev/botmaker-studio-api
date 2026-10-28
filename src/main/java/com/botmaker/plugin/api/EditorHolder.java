package com.botmaker.plugin.api;

/**
 * A class holding the editors a type cannot choose for itself, named by
 * {@link PluginDeclaration#editors(Class[])}: each {@code public static final}
 * {@link com.botmaker.plugin.api.slot.SlotEditor} field, or a list of them. <b>Field order is the order the host
 * consults them in</b>, so declare the narrowest first.
 *
 * <p>It has no members; see {@link TypeHolder} for why it exists.
 */
public interface EditorHolder {
}
