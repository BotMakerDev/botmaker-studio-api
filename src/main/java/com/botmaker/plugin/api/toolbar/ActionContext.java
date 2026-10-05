package com.botmaker.plugin.api.toolbar;

import com.botmaker.plugin.api.StudioServices;

import java.util.Optional;

/**
 * What a {@link ToolbarItem}'s click is handed: the host facts an action cannot get for itself.
 *
 * <p>It is small, and the rule that keeps it small is the platform's standing one — <b>the host must be the
 * only possible source.</b> Which project is open, the theme, the owning window and the window the overlay is
 * drawn over are things only Studio knows. Enumerating windows, grabbing pixels, reading a Steam library
 * and everything else a toolbar action might want are things a plugin does for itself, because
 * {@code botmaker-shared} is published and any plugin may depend on it.
 *
 * <p>A host that answers something only because it was written first owns that policy by accident. Add
 * nothing here that a plugin could answer alone.
 */
public interface ActionContext {

    /** The host capabilities — the open project, dialogs, theme. The same object a slot editor is given. */
    StudioServices services();

    /** Where something sits on screen, in screen pixels. */
    record Area(int x, int y, int width, int height) {}

    /**
     * The window title the host's overlay editor is currently drawn over, or empty when no overlay is open.
     *
     * <p><b>Empty is the ordinary answer</b>, because every item on the main toolbar is clicked with no
     * overlay up.
     *
     * <p>It passes this interface's own host-only test, and not obviously. A plugin <em>can</em> enumerate
     * every window on this machine for itself — {@code botmaker-shared} is published. What it cannot know is
     * <em>which one Studio chose to draw its own HUD over</em>, which is a fact about the host's surface
     * rather than about the desktop: the same category as which project is open.
     */
    default Optional<String> overWindowTitle() {
        return Optional.empty();
    }

    /**
     * Where that window sits right now, or empty when no overlay is open.
     *
     * <p>Asked at click time rather than carried, because a user drags and resizes the thing the overlay is
     * drawn over while the overlay is up.
     */
    default Optional<Area> overBounds() {
        return Optional.empty();
    }
}
