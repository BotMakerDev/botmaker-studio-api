package com.botmaker.plugin.api;

/**
 * The class holding a plugin's part of the overlay editor, named by {@link PluginDeclaration#overlay(Class)}: its
 * one {@code public static final} {@link com.botmaker.plugin.api.overlay.OverlayPart} field. A plugin has one
 * part, so a holder with two is refused when the host asks.
 *
 * <p>It has no members; see {@link TypeHolder} for why it exists.
 */
public interface OverlayHolder {
}
