package com.botmaker.plugin.api.toolbar;

/**
 * When a toolbar item can be pressed — a closed set, and deliberately not a predicate.
 *
 * <p>A {@code BooleanSupplier} would be more expressive and worse: the host would have to call it on every
 * state change, on the JavaFX thread, once per item per plugin, with no way to know what it reads. These four
 * are the states the host already broadcasts, so an item's enablement is answered by a switch rather than by
 * running somebody else's code inside a layout pass.
 *
 * <p>An item that wants finer control disables itself by <em>doing nothing useful</em> when pressed and
 * saying why — a dialog explaining what is missing is a better answer than a greyed button with no
 * explanation, which is the usual result of a condition only the plugin understands.
 */
public enum EnabledWhen {

    /** Always pressable. The right answer for anything that opens a dialog. */
    ALWAYS,

    /** Only with a project open. */
    PROJECT_OPEN,

    /** Only while a bot is running or being debugged — stop, pause, follow. */
    BOT_RUNNING,

    /** Only while no bot is running. Anything that would start one, or edit what one would run. */
    BOT_STOPPED
}
