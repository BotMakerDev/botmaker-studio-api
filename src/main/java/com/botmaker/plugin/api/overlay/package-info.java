/**
 * A plugin's part of the host's overlay editor — the panel docked beside the window a bot watches, where a
 * user builds the bot while looking at what it sees.
 *
 * <p>{@link com.botmaker.plugin.api.overlay.OverlayPart} is the part, one per plugin, declared by steps. It
 * answers five questions only a plugin can:
 *
 * <ul>
 *   <li><b>targets</b> — where blocks go: the bot methods worth editing from the overlay, such as a game
 *       bot's activities. The plugin names a functional interface of its own, and the host offers every bot
 *       method passed by reference where that interface is expected
 *       ({@link com.botmaker.plugin.api.overlay.OverlayPart#targets(Class, String)}). No method name crosses
 *       as text.</li>
 *   <li><b>watched</b> — which screen the bot looks at, so the panel opens over it without asking
 *       ({@link com.botmaker.plugin.api.overlay.Watched}), and the plugin's own picker to change it.</li>
 *   <li><b>frames</b> — what the bot sees there, grabbed the way the bot grabs it
 *       ({@link com.botmaker.plugin.api.overlay.OverlayPart.FrameSource}), so a probe, a tool and the assistant
 *       read the bot's own frame. The watched screen then only says where the panel docks.</li>
 *   <li><b>tools</b> — panes in the panel's tool tabs ({@link com.botmaker.plugin.api.overlay.OverlayTool}):
 *       cut a picture, place a point.</li>
 *   <li><b>probes</b> — for a call the plugin declares, what the call would answer now, on the live frame,
 *       without running the bot ({@link com.botmaker.plugin.api.overlay.Probe}). A probed call is
 *       read-only by declaration, unless its probe is {@link com.botmaker.plugin.api.overlay.Probe#acting}.</li>
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
 * <p><b>Every position here is in the bot's pixels</b>, the space its clicks land in: the desktop's for a bot
 * that watches a window or a part of the desktop, and a private display session's own when it watches the
 * session — that display is not the desktop, and the window showing it may be moved, clipped or scaled. A
 * frame, its {@code watchedArea}, a pick, a probe's area and a mark all use it; the host maps a mark onto the
 * desktop to draw it.
 *
 * <p><b>A part links JavaFX</b> (a tool's pane is a {@code Node}): keep it behind
 * {@code PluginDeclaration.overlay}'s supplier. A host without JavaFX never asks for it.
 */
package com.botmaker.plugin.api.overlay;
