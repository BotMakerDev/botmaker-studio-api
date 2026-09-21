package com.botmaker.plugin.api.toolbar;

/**
 * Where on the toolbar an item sits — a closed set the host owns.
 *
 * <p><b>A plugin picks a group and an order within it; it cannot create one.</b> That asymmetry is the whole
 * design. A toolbar is a single row of a fixed width shared by every plugin installed, and a surface that let
 * each one open its own section would produce a bar whose shape depends on the install order — the same
 * failure the value vocabulary avoids by making the host merge, not the plugins.
 *
 * <p>The groups are named for <em>what a person is doing</em>, not for who contributes them, because that is
 * the only ordering a user can predict: set the project up, then author it, then run it.
 */
public enum ToolbarGroup {

    /**
     * Getting the project ready to run — what it launches, where it looks, how it is configured.
     *
     * <p>Leftmost, because nothing else in the bar works until these are answered.
     */
    PROJECT,

    /** Building the bot: its activities, its values, its templates and the tools that author them. */
    AUTHORING,

    /** Running it and watching it — anything whose subject is a bot that is about to run, or running. */
    RUN,

    /**
     * The instruments: things that are opened <em>over</em> a running target rather than beside the code.
     *
     * <p>Separate from {@link #AUTHORING} because that is where a bot is described and this is where it is
     * observed — an overlay, a recorder, a template cutter. The distinction earns its place by keeping the
     * bar's reading order stable: the four groups below are exactly the order Studio's own toolbar was
     * hand-arranged into before any of it was data.
     */
    TOOLS,

    /**
     * The overlay editor's own row — actions whose subject is the window the HUD is drawn over.
     *
     * <p>Separate from {@link #TOOLS}, which is where the instruments are <em>opened</em> from. This is what
     * is offered once one is open and a user is looking at the running target rather than at the code: point
     * the bot at this window, cut a picture out of it, drop a recorded click into the program. An item here is
     * drawn by {@code ProgramShapeOverlay} and never appears on the main bar.
     *
     * <p>It is the one group whose items are handed a context that answers
     * {@link ActionContext#overWindowTitle()} — which is the whole reason it is a group of its own rather
     * than an order within {@link #TOOLS}.
     */
    OVERLAY,

    /**
     * The host's own items, and <b>a plugin may not claim it</b>.
     *
     * <p>Reserved for what belongs to Studio as an editor rather than to any bot: the things that would still
     * make sense with every plugin uninstalled. An item contributed into this group is refused at merge time
     * with the plugin named, which is a loud failure on purpose — silently re-homing it would put a plugin's
     * button in the one place a user reads as "this is the application, not my project".
     */
    STUDIO
}
