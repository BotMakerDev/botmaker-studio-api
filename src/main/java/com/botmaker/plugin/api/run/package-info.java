/**
 * A plugin's part of the host's run overlay — what is shown while the project's bot runs.
 *
 * <p>{@link com.botmaker.plugin.api.run.RunOverlayPart} is the part, declared by steps, and
 * {@link com.botmaker.plugin.api.run.RunOverlayContext} is what each opening of the overlay hands it.
 *
 * <h2>Why the part is a {@code Node} when a toolbar item is data</h2>
 * What a run looks like is the plugin's vocabulary: which activity the bot is in, where it matched, where it
 * clicked. The host cannot draw any of it without learning that vocabulary, so it gives the plugin a place to
 * draw and stays out. What every run has — stop, pause, run again, its log — the host draws itself, so a
 * project with no plugin still gets an overlay that stops its bot.
 *
 * <p>Nothing here says what the bot drives. A bot need not drive a game, or any window at all: the layer is
 * the desktop and the bar is the host's, and a part that knows about a target says so in its own pixels.
 */
package com.botmaker.plugin.api.run;
