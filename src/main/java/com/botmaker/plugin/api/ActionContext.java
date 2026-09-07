package com.botmaker.plugin.api;

import com.botmaker.plugin.api.meta.ReplacedBy;

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
    default Optional<String> openProjectName() {
        return Optional.ofNullable(projectName());
    }

    /**
     * The open project's name, or {@code null} when none is open.
     *
     * @deprecated by {@link #openProjectName()}. Studio starts with no project open and the toolbar is drawn
     *         before one is, so this is {@code null} at the moment a plugin is most likely to be tested by
     *         hand. Behaviour is unchanged and the host still implements this one.
     */
    @Deprecated
    @ReplacedBy("com.botmaker.plugin.api.ActionContext#openProjectName")
    String projectName();

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
}
