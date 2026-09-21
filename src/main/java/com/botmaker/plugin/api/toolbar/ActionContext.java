package com.botmaker.plugin.api.toolbar;

import com.botmaker.plugin.api.StudioServices;

import java.util.Optional;

/**
 * What a {@link ToolbarItem}'s click is handed: the host facts an action cannot get for itself.
 *
 * <p>It is small, and the rule that keeps it small is the platform's standing one — <b>the host must be the
 * only possible source.</b> Which project is open, which version of this plugin it pins, the theme and the
 * owning window are things only Studio knows. Enumerating windows, grabbing pixels, reading a Steam library
 * and everything else a toolbar action might want are things a plugin does for itself, because
 * {@code botmaker-shared} is published and any plugin may depend on it.
 *
 * <p>That rule was learned rather than assumed: the contract grew an {@code Assets} and a
 * {@code SourceChoice} in 2026-08-27 and both were deleted the same day, because the host owned those
 * policies only by accident of having been written first. Add nothing here that a plugin could answer alone.
 */
public interface ActionContext {

    /**
     * The open project's name, or empty when none is open.
     *
     * <p>A name, not a path: Studio addresses projects by name everywhere, and a path invites a plugin to
     * write into a project directory behind the host's back — which the whole-file-ownership rule exists to
     * prevent.
     *
     * <p>Empty is a state a toolbar item really is clicked in: Studio starts with no project open, and the
     * bar is drawn before one is.
     */
    Optional<String> openProjectName();

    /**
     * The version of the calling plugin this project pins, exactly as the pom spells it.
     *
     * <p>Read as {@link StudioPlugin#catalog(String)}'s argument is: the plugin alone decides what the string
     * means, since only it knows its own versioning. Never {@code null}; may be a snapshot or a spelling the
     * plugin does not recognise.
     */
    String pinnedVersion();

    /** The host capabilities — capture, dialogs, theme. The same object a slot editor is given. */
    StudioServices services();

    /** Where something sits on screen, in screen pixels. */
    record Area(int x, int y, int width, int height) {}

    /**
     * The window title the host's overlay editor is currently drawn over, or empty when no overlay is open.
     *
     * <p><b>Empty is the ordinary answer</b>, because every item on the main toolbar is clicked with no
     * overlay up. Read it the way {@link #openProjectName()} is read.
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

    /**
     * Append Java source at the overlay editor's insertion cursor.
     *
     * <p>A no-op when no overlay is open, which is the same shape as every member above: an item pressed
     * where its subject does not exist does nothing rather than throwing.
     *
     * <p>Java source text crosses and no type is named, exactly as {@link Sources#replace} and
     * {@code SlotContext.replaceEnclosingCall} already do. The cursor itself is host state by construction —
     * it is a position in the file the editor has open.
     *
     * @param statements whole statements, each ending in its own semicolon; the host places them in order at
     *                   the cursor and leaves the cursor after the last one
     */
    default void insertAtCursor(String... statements) {
    }
}
