/**
 * A button a plugin puts on the host's toolbar, contributed as data.
 *
 * <p>{@link com.botmaker.plugin.api.toolbar.ToolbarItem} is the button,
 * {@link com.botmaker.plugin.api.toolbar.ToolbarGroup} says where on the toolbar it sits,
 * {@link com.botmaker.plugin.api.toolbar.EnabledWhen} when it may be pressed, and
 * {@link com.botmaker.plugin.api.toolbar.ActionContext} is what its click is handed.
 *
 * <h2>What is deliberately not here</h2>
 * A {@code Node}, and a predicate. The host builds the button, so a plugin cannot style the frame around
 * its own window differently from every other; and {@code EnabledWhen} is a closed set rather than a
 * callback, so the host can decide a button's state without running a plugin's code on every repaint.
 */
package com.botmaker.plugin.api.toolbar;
