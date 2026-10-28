package com.botmaker.plugin.api;

/**
 * A class holding what a plugin shows in the host's run overlay, named by
 * {@link PluginDeclaration#runOverlay(Class[])}: each {@code public static final}
 * {@link com.botmaker.plugin.api.run.RunOverlayPart} field, or a list of them, in the order the fields are declared.
 *
 * <p>It has no members; see {@link TypeHolder} for why it exists.
 */
public interface RunOverlayHolder {
}
