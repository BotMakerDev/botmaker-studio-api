/**
 * A plugin's part of the host's overlay editor — the panel docked beside the window a bot watches, where a
 * user builds the bot while looking at what it sees.
 *
 * <p>{@link com.botmaker.plugin.api.overlay.OverlayPart} is the part, one per plugin, declared by steps. It
 * answers four questions only a plugin can:
 *
 * <ul>
 *   <li><b>targets</b> — where blocks go: the bot methods worth editing from the overlay, such as a game
 *       bot's activities ({@link com.botmaker.plugin.api.overlay.OverlayTarget}). The host holds no list of
 *       its own.</li>
 *   <li><b>watched</b> — which screen the bot looks at, so the panel opens over it without asking
 *       ({@link com.botmaker.plugin.api.overlay.Watched}), and the plugin's own picker to change it.</li>
 *   <li><b>tools</b> — panes in the panel's tool tabs ({@link com.botmaker.plugin.api.overlay.OverlayTool}):
 *       cut a picture, place a point.</li>
 *   <li><b>probes</b> — for a call the plugin declares, what the call would answer now, on the live frame,
 *       without running the bot ({@link com.botmaker.plugin.api.overlay.Probe}). A probed call is
 *       read-only by declaration.</li>
 * </ul>
 *
 * <p>The host owns the panel, the script view, the caret and every edit: a plugin never writes Java, and what
 * it inserts crosses as a call it names by method reference plus argument values the host spells.
 *
 * <p>What is drawn over the game is the run overlay's layer
 * ({@link com.botmaker.plugin.api.run.RunOverlayPart#layer}), opened while editing too and told which by
 * {@link com.botmaker.plugin.api.run.RunOverlayContext#mode()}. Boxes the host draws for a probe or a tool are
 * {@link com.botmaker.plugin.api.overlay.Marks}.
 *
 * <p><b>A part links JavaFX</b> (a tool's pane is a {@code Node}): keep it behind
 * {@code PluginDeclaration.overlay}'s supplier. A host without JavaFX never asks for it.
 */
package com.botmaker.plugin.api.overlay;
